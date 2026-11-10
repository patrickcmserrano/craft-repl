#!/usr/bin/env node
/**
 * npc_picard.js - Autonomous Interactive NPC for Captain Jean-Luc Picard
 * Station: Bridge of the USS Enterprise NCC-1701-D
 * Features:
 * - Bridge command routine (Command Chair, Conn/Ops, Tactical, Memorial Plaque)
 * - Philosophical dialogue & Starfleet commands ("Make it so", "Engage", "Tea. Earl Grey. Hot.")
 * - Natural interaction with players (addresses Pace as "Número Um")
 * - Smart pathfinder across the Bridge and Memorial Walkway
 * - Starfleet Command uniform & equipment
 */

const mineflayer = require('mineflayer');
const { pathfinder, Movements, goals } = require('mineflayer-pathfinder');
const { Vec3 } = require('vec3');
const { exec } = require('child_process');
const http = require('http');

const CONFIG = {
    host: '127.0.0.1',
    port: 25565,
    username: 'Picard',
    version: '1.21.1'
};

// Bridge & Memorial Key Waypoints
const BRIDGE_STATIONS = [
    {
        name: 'Command Chair (Center Seat)',
        pos: new Vec3(-65, 145, 101),
        lookYaw: 0, // South (+Z) towards the Main Viewscreen
        lookPitch: 0,
        bark: 'Computador, manter posição orbital sincronizada sobre o Castelo.',
        dwellMs: 14000
    },
    {
        name: 'Console Tática de Ré',
        pos: new Vec3(-65, 145, 99.2),
        lookYaw: 3.14, // North (-Z) inspecting tactical horseshoe
        lookPitch: 0.3,
        bark: 'Escudos defletores a 100%. Nenhuma anomalia no quadrante.',
        dwellMs: 8000
    },
    {
        name: 'Estação de Operações (Ops)',
        pos: new Vec3(-67, 145, 102),
        lookYaw: 0, // South
        lookPitch: 0.4,
        bark: 'Sensores de longo alcance indicam ventos solares calmos.',
        dwellMs: 7000
    },
    {
        name: 'Placa Dedicatória Memorial',
        pos: new Vec3(-65, 145, 98.5),
        lookYaw: 3.14, // Facing the memorial sign on the bulkhead
        lookPitch: -0.1,
        bark: '"O tempo é um companheiro que nos acompanha na viagem... e nos lembra de valorizar cada momento."',
        dwellMs: 10000
    },
    {
        name: 'Estação de Navegação (Conn)',
        pos: new Vec3(-63, 145, 102),
        lookYaw: 0, // South
        lookPitch: 0.4,
        bark: 'Curso traçado. À espera das suas ordens, Número Um.',
        dwellMs: 7000
    }
];

const MEMORIAL_POS = new Vec3(-82, 139, 112);

let bot = null;
let currentWpIndex = 0;
let isAtStation = false;
let isFollowing = false;
let followTarget = null;
let lastBarkTime = 0;
let lastLookPlayerTime = 0;

function runServerCmd(cmd) {
    return new Promise((resolve) => {
        try {
            const postData = JSON.stringify({ cmd: cmd });
            const req = http.request({
                hostname: '192.168.1.20',
                port: 8090,
                path: '/api/run',
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Content-Length': Buffer.byteLength(postData)
                },
                timeout: 3000
            }, (res) => {
                let data = '';
                res.on('data', chunk => data += chunk);
                res.on('end', () => resolve(true));
            });
            req.on('error', () => resolve(false));
            req.write(postData);
            req.end();
        } catch (e) {
            resolve(false);
        }
    });
}

function initBot() {
    console.log(`[*] Picard conectando a ${CONFIG.host}:${CONFIG.port}...`);
    bot = mineflayer.createBot(CONFIG);
    bot.loadPlugin(pathfinder);

    bot.once('spawn', () => {
        console.log('[+] Capitão Jean-Luc Picard a bordo da USS Enterprise-D!');
        
        // Setup movement
        const defaultMove = new Movements(bot);
        defaultMove.canDig = false;
        defaultMove.allowParkour = false;
        defaultMove.allow1by1towers = false;
        bot.pathfinder.setMovements(defaultMove);

        // Equip Uniform & Set Skin via server
        setTimeout(async () => {
            // Apply authentic Starfleet Captain Picard skin via Java Bytecode Injector
            exec('/home/pace/dev/minecraft/craft-repl/scripts/mc skin-restore Picard picard', (err, stdout, stderr) => {
                if (err) console.error('[ERR] Skin restore error:', err);
                else console.log('[+] Skin oficial do Capitão Picard injetada com sucesso!');
            });
            
            // Teleport to Bridge Command Position
            await runServerCmd('tp Picard -65 145 101 0 0');

            // Equip Starfleet Command Uniform (Crimson Dyed Leather Chestplate)
            await runServerCmd('item replace entity Picard armor.chest with minecraft:leather_chestplate[dyed_color={rgb:10031872},custom_name=\'{"text":"Túnica de Comando da Frota","color":"red"}\']');
            await runServerCmd('item replace entity Picard armor.legs with minecraft:netherite_leggings');
            await runServerCmd('item replace entity Picard armor.feet with minecraft:netherite_boots');
            await runServerCmd('item replace entity Picard weapon.mainhand with minecraft:spyglass');
            await runServerCmd('item replace entity Picard weapon.offhand with minecraft:compass');

            bot.chat('Capitão Picard assumindo a ponte. Todos os sistemas prontos.');
            
            // Start routine
            startBridgeRoutine();
        }, 3000);
    });

    bot.on('chat', (username, message) => {
        if (username === bot.username || username === 'Karlach' || username === 'Antigravity') return;
        handleChat(username, message.trim());
    });

    bot.on('death', () => {
        console.log('[!] Picard sofreu dano crítico. Reiniciando na ponte...');
        setTimeout(() => {
            runServerCmd('tp Picard -65 145 101 0 0');
        }, 2000);
    });

    bot.on('error', (err) => {
        console.error('[ERR] Mineflayer error:', err.message);
    });

    bot.on('end', () => {
        console.log('[*] Conexão encerrada. Reconectando em 5 segundos...');
        setTimeout(initBot, 5000);
    });
}

function handleChat(username, text) {
    const lower = text.toLowerCase();
    const now = Date.now();

    // Check if addressed to Picard or general command
    const isDirect = lower.includes('picard') || lower.includes('capitao') || lower.includes('capitão');

    if (lower.includes('engajar') || lower.includes('engage') || lower.includes('acelerar')) {
        bot.chat('Make it so! Dobra 9 acionada, leme à frente!');
        runServerCmd('playsound minecraft:block.beacon.power_select master @a -65 145 100 1.0 1.2');
        return;
    }

    if (lower.includes('cha') || lower.includes('chá') || lower.includes('tea') || lower.includes('earl grey')) {
        bot.chat('Tea. Earl Grey. Hot. Nada como um bom chá para clarear os pensamentos, Número Um.');
        runServerCmd('item replace entity Picard weapon.mainhand with minecraft:potion');
        setTimeout(() => {
            runServerCmd('item replace entity Picard weapon.mainhand with minecraft:spyglass');
        }, 5000);
        return;
    }

    if (lower.includes('relatorio') || lower.includes('relatório') || lower.includes('status')) {
        bot.chat('Relatório: USS Enterprise-D em órbita estacionária. Sensores em máxima precisão. Santuário pacífico e protegido.');
        return;
    }

    if (isDirect && (lower.includes('seguir') || lower.includes('vem') || lower.includes('acompanhe') || lower.includes('follow'))) {
        isFollowing = true;
        followTarget = username;
        bot.chat(`Com prazer, ${username}. Liderando o caminho.`);
        followLoop();
        return;
    }

    if (isDirect && (lower.includes('parar') || lower.includes('ficar') || lower.includes('ponte') || lower.includes('stay') || lower.includes('voltar'))) {
        isFollowing = false;
        followTarget = null;
        bot.chat('Entendido. Retornando ao meu posto na ponte.');
        returnToBridge();
        return;
    }

    if (isDirect && lower.includes('memorial')) {
        isFollowing = false;
        bot.chat('Vamos prestar nossas homenagens ao memorial celestial.');
        bot.pathfinder.setGoal(new goals.GoalNear(MEMORIAL_POS.x, MEMORIAL_POS.y, MEMORIAL_POS.z, 1.5));
        return;
    }

    if (isDirect && (lower.includes('oi') || lower.includes('olá') || lower.includes('ola') || lower.includes('bom dia') || lower.includes('boa noite'))) {
        const greetings = [
            `Saudações, ${username}. Que honra recebê-lo a bordo da Enterprise.`,
            `Olá, Número Um. A galáxia aguarda nossas próximas descobertas.`,
            `Bom vê-lo na ponte, ${username}. "As coisas só são impossíveis até deixarem de ser."`
        ];
        const pick = greetings[Math.floor(Math.random() * greetings.length)];
        bot.chat(pick);
        lookAtPlayer(username);
        return;
    }

    if (isDirect) {
        const quotes = [
            'É possível não cometer erros e ainda assim perder. Isso não é fraqueza; isso é a vida.',
            'O primeiro dever de cada oficial da Frota Estelar é com a verdade, seja ela científica, histórica ou pessoal!',
            'Seja qual for a tempestade cósmica lá fora, a mente humana sempre encontrará um caminho.',
            'Make it so, soldado.'
        ];
        const pick = quotes[Math.floor(Math.random() * quotes.length)];
        bot.chat(pick);
        lookAtPlayer(username);
    }
}

function lookAtPlayer(username) {
    const player = bot.players[username];
    if (player && player.entity) {
        const eyePos = player.entity.position.offset(0, 1.6, 0);
        bot.lookAt(eyePos, true);
    }
}

async function followLoop() {
    if (!isFollowing || !followTarget) return;

    const targetPlayer = bot.players[followTarget];
    if (targetPlayer && targetPlayer.entity) {
        const p = targetPlayer.entity.position;
        const dist = bot.entity.position.distanceTo(p);

        if (dist > 25) {
            // Teleport close if too far
            await runServerCmd(`tp Picard ${p.x.toFixed(1)} ${p.y.toFixed(1)} ${p.z.toFixed(1)}`);
        } else if (dist > 3) {
            bot.pathfinder.setGoal(new goals.GoalNear(p.x, p.y, p.z, 2));
        }
    }

    setTimeout(followLoop, 1500);
}

function returnToBridge() {
    const chairStation = BRIDGE_STATIONS[0];
    bot.pathfinder.setGoal(new goals.GoalNear(chairStation.pos.x, chairStation.pos.y, chairStation.pos.z, 0.5));
    setTimeout(() => {
        bot.look(chairStation.lookYaw, chairStation.lookPitch, true);
        startBridgeRoutine();
    }, 4000);
}

function startBridgeRoutine() {
    setInterval(() => {
        if (isFollowing) return;

        // Check if players are nearby on the bridge (within 8 blocks)
        let playerNearby = null;
        for (const name in bot.players) {
            if (name === bot.username || name === 'Karlach') continue;
            const p = bot.players[name];
            if (p.entity && bot.entity.position.distanceTo(p.entity.position) < 8) {
                playerNearby = p;
                break;
            }
        }

        if (playerNearby && Date.now() - lastLookPlayerTime > 6000) {
            lastLookPlayerTime = Date.now();
            const eyePos = playerNearby.entity.position.offset(0, 1.6, 0);
            bot.lookAt(eyePos, true);
            return;
        }

        // Advance station in routine
        if (!isAtStation) {
            isAtStation = true;
            const station = BRIDGE_STATIONS[currentWpIndex];
            currentWpIndex = (currentWpIndex + 1) % BRIDGE_STATIONS.length;

            bot.pathfinder.setGoal(new goals.GoalNear(station.pos.x, station.pos.y, station.pos.z, 0.8));

            setTimeout(async () => {
                if (!bot || !bot.entity) return;
                // Face station direction
                bot.look(station.lookYaw, station.lookPitch, true);

                // Dwell bark
                if (Date.now() - lastBarkTime > 30000 && Math.random() < 0.6) {
                    lastBarkTime = Date.now();
                    bot.chat(station.bark);
                }

                setTimeout(() => {
                    isAtStation = false;
                }, station.dwellMs);
            }, 3500);
        }
    }, 5000);
}

// Start
initBot();
