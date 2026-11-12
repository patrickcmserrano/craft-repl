/**
 * Testes do núcleo de patrulha da Karlach.
 *
 * Cobrem os quatro defeitos medidos em 2026-11-10 (bot node pid 626270,
 * 24,4% CPU / 913 MB, Pos [-163.52,65,179.5], Motion [0,-0.0784,0],
 * "moved wrongly!" alternando com "moved too quickly!", e 49.423 sockets
 * ESTAB no :8090 das 02:16 às 16:00):
 *
 *   1. `forward` fica preso quando o await de stepThroughGate é interrompido.
 *   2. O loop de patrulha morre em silêncio: nenhum watchdog re-arma o goal.
 *   3. O `tp` de fallback não reconcilia a posição que o pathfinder usa.
 *   4. Cada runServerCmd vaza um socket porque a resposta nunca é drenada.
 */

const test = require('node:test');
const assert = require('node:assert/strict');
const http = require('node:http');

const patrol = require('../patrol.js');

// ---------------------------------------------------------------------------
// 1. forward sempre liberado, mesmo quando o passo é interrompido
// ---------------------------------------------------------------------------

test('stepThroughGate desliga o forward quando o caminho feliz termina', async () => {
    const calls = [];
    const fakeBot = {
        entity: { position: { y: 65 } },
        lookAt: async () => {},
        setControlState: (name, state) => calls.push([name, state]),
    };

    await patrol.stepThroughGate(fakeBot, -168, 171, 20);

    const forward = calls.filter(([n]) => n === 'forward');
    assert.deepEqual(forward[forward.length - 1], ['forward', false],
        'a última mudança de forward tem de ser false');
});

test('stepThroughGate libera o forward mesmo se lookAt lançar', async () => {
    const calls = [];
    const fakeBot = {
        entity: { position: { y: 65 } },
        lookAt: async () => { throw new Error('socket caiu no meio do passo'); },
        setControlState: (name, state) => calls.push([name, state]),
    };

    await assert.rejects(() => patrol.stepThroughGate(fakeBot, -168, 171, 20));

    const forward = calls.filter(([n]) => n === 'forward' && n !== false);
    const lastForward = calls.filter(([n]) => n === 'forward').pop();
    assert.deepEqual(lastForward, ['forward', false],
        'o forward tem de ser liberado no finally, não deixado ligado');
});

test('stepThroughGate libera o forward quando o cancelamento vem de fora', async () => {
    const calls = [];
    const controller = new AbortController();
    const fakeBot = {
        entity: { position: { y: 65 } },
        lookAt: async () => {},
        setControlState: (name, state) => calls.push([name, state]),
    };

    const p = patrol.stepThroughGate(fakeBot, -168, 171, 5000, { signal: controller.signal });
    setTimeout(() => controller.abort(), 20);

    await assert.rejects(() => p);

    const lastForward = calls.filter(([n]) => n === 'forward').pop();
    assert.deepEqual(lastForward, ['forward', false],
        'abortar um passo em curso não pode deixar o forward ligado');
});

// ---------------------------------------------------------------------------
// 2. watchdog re-arma a patrulha quando goal_reached nunca chega
// ---------------------------------------------------------------------------

test('o watchdog re-arma a patrulha quando o objetivo nunca é atingido', async () => {
    let rearmed = 0;
    const wd = patrol.createPatrolWatchdog({
        timeoutMs: 60,
        onStuck: () => { rearmed += 1; },
    });

    wd.arm(0, 0);          // objetivo em (0,0)
    await new Promise(r => setTimeout(r, 140));

    assert.ok(rearmed >= 1, 'o watchdog tem de disparar onStuck ao vencer');
    wd.dispose();
});

test('o watchdog fica quieto enquanto a posição avança para o objetivo', async () => {
    let rearmed = 0;
    const wd = patrol.createPatrolWatchdog({
        timeoutMs: 80,
        onStuck: () => { rearmed += 1; },
    });

    wd.arm(100, 0);
    // a cada 30ms a posição avança 25 blocos na direção do objetivo
    let x = 0;
    const timer = setInterval(() => {
        x += 25;
        wd.progress({ x, z: 0 });
    }, 30);

    await new Promise(r => setTimeout(r, 250));
    clearInterval(timer);
    wd.dispose();

    assert.equal(rearmed, 0, 'progresso real não pode ser tratado como travamento');
});

test('o watchdog re-arma quando a posição estagna longe do objetivo', async () => {
    let rearmed = 0;
    const wd = patrol.createPatrolWatchdog({
        timeoutMs: 70,
        onStuck: () => { rearmed += 1; },
    });

    wd.arm(100, 0);
    const timer = setInterval(() => wd.progress({ x: 5, z: 5 }), 30);

    await new Promise(r => setTimeout(r, 240));
    clearInterval(timer);
    wd.dispose();

    assert.ok(rearmed >= 1, 'parado longe do objetivo por tempo demais é travamento');
});

test('desarmar o watchdog impede o disparo', async () => {
    let rearmed = 0;
    const wd = patrol.createPatrolWatchdog({
        timeoutMs: 50,
        onStuck: () => { rearmed += 1; },
    });

    wd.arm(100, 0);
    wd.disarm();

    await new Promise(r => setTimeout(r, 120));
    wd.dispose();

    assert.equal(rearmed, 0, 'goal_reached a tempo desarma e não dispara');
});

// ---------------------------------------------------------------------------
// 3. o tp de fallback reconcilia a posição usada pelo pathfinder
// ---------------------------------------------------------------------------

test('reconcileAfterTeleport grava a posição que o servidor confirmou', () => {
    const fakeBot = {
        entity: { position: { x: -163.52, y: 65, z: 179.5 } },
        pathfinder: { setGoal: () => {} },
    };

    patrol.reconcileAfterTeleport(fakeBot, { x: -173, y: 65, z: 180 });

    assert.equal(fakeBot.entity.position.x, -173);
    assert.equal(fakeBot.entity.position.z, 180);
});

test('reconcileAfterTeleport limpa o goal antigo em vez de recalcular do fantasma', () => {
    let cleared = false;
    const fakeBot = {
        entity: { position: { x: -163.52, y: 65, z: 179.5 } },
        pathfinder: { setGoal: (g) => { if (g === null) cleared = true; } },
    };

    patrol.reconcileAfterTeleport(fakeBot, { x: -173, y: 65, z: 180 });

    assert.ok(cleared, 'o goal calculado a partir da posição fantasma tem de ser limpo');
});

// ---------------------------------------------------------------------------
// 4. runServerCmd não acumula sockets no dashboard
// ---------------------------------------------------------------------------

test('runServerCmd drena a resposta e não deixa sockets pendurados', async () => {
    let served = 0;
    const server = http.createServer((req, res) => {
        served += 1;
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ ok: true }));
    });

    await new Promise(r => server.listen(0, '127.0.0.1', r));
    const port = server.address().port;

    const sender = patrol.createServerCmdSender({ port, host: '127.0.0.1', maxSockets: 4 });

    try {
        for (let i = 0; i < 40; i++) {
            sender.run(`data get entity Karlach Pos #${i}`);
        }
        // dá tempo de os pedidos concluírem
        await new Promise(r => setTimeout(r, 400));

        assert.equal(served, 40, 'todas as 40 requisições têm de chegar');

        const stats = sender.stats();
        assert.ok(stats.activeSockets <= 4,
            `sockets ativos não podem exceder maxSockets (obtido: ${stats.activeSockets})`);
        assert.ok(stats.pendingRequests === 0,
            `não pode sobrar requisição pendente (obtido: ${stats.pendingRequests})`);
    } finally {
        sender.dispose();
        await new Promise(r => server.close(r));
    }
});

test('runServerCmd não quebra quando o dashboard está fora do ar', async () => {
    // porta fechada de propósito
    const sender = patrol.createServerCmdSender({ port: 1, host: '127.0.0.1', maxSockets: 2 });

    assert.doesNotThrow(() => sender.run('setblock 0 0 0 stone'));
    await new Promise(r => setTimeout(r, 150));

    sender.dispose();
});

test('runServerCmd sobrevive a uma resposta sem corpo (só cabeçalhos)', async () => {
    let served = 0;
    const server = http.createServer((req, res) => {
        served += 1;
        res.writeHead(204);
        res.end();
    });

    await new Promise(r => server.listen(0, '127.0.0.1', r));
    const port = server.address().port;
    const sender = patrol.createServerCmdSender({ port, host: '127.0.0.1', maxSockets: 2 });

    try {
        sender.run('playsound minecraft:block.fence_gate.open block @a 0 0 0 1 1');
        await new Promise(r => setTimeout(r, 200));

        assert.equal(served, 1);
        assert.equal(sender.stats().activeSockets, 0, 'resposta 204 também precisa ser drenada');
    } finally {
        sender.dispose();
        await new Promise(r => server.close(r));
    }
});
