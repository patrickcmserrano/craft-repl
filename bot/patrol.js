/**
 * patrol.js — núcleo testável da patrulha da Karlach.
 *
 * Extraído de npc_karlach.js para que os defeitos medidos em 2026-11-10
 * possam ser verificados sem subir um bot real. Cada peça aqui é uma
 * correção de um defeito observado:
 *
 *   stepThroughGate  — o `forward` era ligado antes de um await e só desligado
 *                      no caminho feliz; qualquer interrupção o deixava preso,
 *                      produzindo a pose de natação contra o bloco.
 *   PatrolWatchdog   — não havia watchdog: quando goal_reached não vinha, o loop
 *                      de patrulha morria em silêncio por horas.
 *   reconcileAfterTeleport — o tp de fallback mudava a posição no servidor mas
 *                      deixava o pathfinder calculando a partir de uma posição
 *                      fantasma, o que re-emitia `forward` indefinidamente.
 *   createServerCmdSender — cada runServerCmd abria um http.request cujo handler
 *                      de resposta era vazio, então o socket nunca era drenado;
 *                      acumulou 49.423 conexões ESTAB no dashboard (:8090).
 */

'use strict';

const http = require('node:http');
const { Vec3 } = require('vec3');

// ---------------------------------------------------------------------------
// 1. Travessia de portão que nunca deixa o `forward` preso
// ---------------------------------------------------------------------------

/**
 * Dá um passo em frente controlado, garantindo que o `forward` seja liberado
 * em qualquer saída (sucesso, exceção ou cancelamento).
 *
 * @param {object} bot           — precisa de entity.position.y, lookAt e setControlState
 * @param {number} targetX       — destino X a encarar
 * @param {number} targetZ       — destino Z a encarar
 * @param {number} durationMs    — quanto tempo manter o passo
 * @param {{signal?: AbortSignal}} [opts]
 */
async function stepThroughGate(bot, targetX, targetZ, durationMs, opts = {}) {
    const { signal } = opts;
    const y = bot.entity.position.y + 1.6;

    if (signal?.aborted) throw new Error('passo cancelado antes de começar');

    try {
        await bot.lookAt(new Vec3(targetX, y, targetZ), true);
        await sleep(150, signal);
        bot.setControlState('forward', true);
        await sleep(durationMs, signal);
    } finally {
        // O ponto inteiro do conserto: aconteça o que acontecer, o forward sai.
        bot.setControlState('forward', false);
    }
}

/**
 * Espera `ms` e rejeita se o AbortSignal disparar no meio.
 * @param {number} ms
 * @param {AbortSignal} [signal]
 */
function sleep(ms, signal) {
    return new Promise((resolve, reject) => {
        if (signal?.aborted) {
            reject(new Error('cancelado'));
            return;
        }
        const timer = setTimeout(() => {
            signal?.removeEventListener('abort', onAbort);
            resolve();
        }, ms);
        const onAbort = () => {
            clearTimeout(timer);
            reject(new Error('cancelado'));
        };
        signal?.addEventListener('abort', onAbort, { once: true });
    });
}

// ---------------------------------------------------------------------------
// 2. Watchdog: re-arma a patrulha quando o avanço para de verdade
// ---------------------------------------------------------------------------

/**
 * Cria um watchdog de progresso. Chame `arm(x, z)` ao emitir um novo goal e
 * `progress(pos)` a cada tique; se a posição parar de se aproximar do objetivo
 * por tempo demais, `onStuck` é chamado — o loop re-arma a patrulha em vez de
 * morrer em silêncio.
 *
 * @param {{timeoutMs:number, minProgress?:number, onStuck:Function}} cfg
 */
function createPatrolWatchdog(cfg) {
    const timeoutMs = cfg.timeoutMs ?? 15000;
    const minProgress = cfg.minProgress ?? 0.5;

    let timer = null;
    let goal = null;
    let bestDist = Infinity;

    function clear() {
        if (timer) {
            clearTimeout(timer);
            timer = null;
        }
    }

    function fire() {
        clear();
        if (goal) cfg.onStuck(goal);
    }

    function rearm() {
        clear();
        timer = setTimeout(fire, timeoutMs);
    }

    return {
        /** Fixa o objetivo e começa a vigiar. */
        arm(x, z) {
            goal = { x, z };
            bestDist = Infinity;
            rearm();
        },

        /**
         * Informa a posição atual. Aproximar-se do objetivo reinicia o relógio;
         * ficar parado deixa o relógio correr até o disparo.
         */
        progress(pos) {
            if (!goal || !pos) return;
            const dist = Math.hypot(pos.x - goal.x, pos.z - goal.z);
            if (dist < bestDist - minProgress) {
                bestDist = dist;
                rearm();
            }
            if (dist <= 1.5) {
                // chegou o bastante; o goal_reached do pathfinder conclui o ciclo
                clear();
            }
        },

        /** Cancela a vigilância (objetivo atingido por outro caminho). */
        disarm: clear,

        dispose: clear,
    };
}

// ---------------------------------------------------------------------------
// 3. Reconciliação após teleporte de fallback
// ---------------------------------------------------------------------------

/**
 * O servidor confirma uma posição nova via `tp`, mas o objeto do bot continua
 * com a posição antiga até o próximo update de rede. Enquanto isso, o
 * pathfinder recalcula de um ponto que não existe mais e re-emite `forward`.
 * Aqui a posição local é sincronizada e o goal fantasma é limpo.
 *
 * @param {object} bot      — precisa de entity.position e pathfinder.setGoal
 * @param {{x:number,y:number,z:number}} pos — posição confirmada pelo servidor
 */
function reconcileAfterTeleport(bot, pos) {
    if (!bot?.entity?.position) return;
    bot.entity.position.x = pos.x;
    bot.entity.position.y = pos.y;
    bot.entity.position.z = pos.z;
    if (bot.pathfinder?.setGoal) bot.pathfinder.setGoal(null);
}

// ---------------------------------------------------------------------------
// 4. Envio de comandos ao dashboard sem vazar sockets
// ---------------------------------------------------------------------------

/**
 * Cria um remetente HTTP persistente para o dashboard `/api/run`.
 *
 * O defeito original: cada chamada fazia `http.request(..., () => {})` e nunca
 * consumia a resposta — o socket ficava ESTAB para sempre. Aqui há um único
 * Agent com keep-alive e teto de sockets, e toda resposta é drenada com
 * `res.resume()`, o que devolve o socket ao pool em vez de acumulá-lo.
 *
 * @param {{host?:string, port:number, path?:string, maxSockets?:number, timeoutMs?:number}} cfg
 */
function createServerCmdSender(cfg) {
    const host = cfg.host ?? 'localhost';
    const port = cfg.port;
    const path = cfg.path ?? '/api/run';
    const timeoutMs = cfg.timeoutMs ?? 2000;

    const agent = new http.Agent({
        keepAlive: true,
        maxSockets: cfg.maxSockets ?? 8,
    });

    let inFlight = 0;

    function run(cmd, cb) {
        const body = JSON.stringify({ cmd });
        inFlight += 1;

        const req = http.request({ host, port, path, method: 'POST', agent,
            headers: { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(body) } },
            (res) => {
                // Drenar é o conserto: sem isto o socket nunca volta ao pool.
                res.resume();
                res.on('end', () => {
                    inFlight -= 1;
                    cb?.();
                });
                res.on('error', () => { inFlight -= 1; cb?.(); });
            });

        req.setTimeout(timeoutMs, () => req.destroy(new Error('timeout')));
        req.on('error', () => {
            inFlight -= 1;
            cb?.();
        });
        req.end(body);
    }

    return {
        run,

        stats() {
            const sockets = Object.values(agent.sockets).flat();
            const pending = Object.values(agent.requests).flat();
            return {
                activeSockets: sockets.length,
                pendingRequests: pending.length + Math.max(0, inFlight - sockets.length),
            };
        },

        dispose() {
            agent.destroy();
        },
    };
}

module.exports = {
    stepThroughGate,
    sleep,
    createPatrolWatchdog,
    reconcileAfterTeleport,
    createServerCmdSender,
};
