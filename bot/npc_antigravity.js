const mineflayer = require('mineflayer');
const { spawn } = require('child_process');
const http = require('http');

const CONFIG = {
    host: '127.0.0.1',
    port: 25565,
    username: 'Antigravity',
    version: '1.21.1'
};

const HOSTILE_MOBS = new Set([
    'zombie', 'skeleton', 'creeper', 'spider', 'cave_spider', 'enderman',
    'witch', 'phantom', 'drowned', 'husk', 'stray', 'pillager', 'vindicator',
    'ravager', 'evoker', 'slime', 'magma_cube', 'wither_skeleton', 'ghast',
    'blaze', 'hoglin', 'piglin_brute', 'warden', 'wither', 'ghoul'
]);

let bot = null;
let targetPlayerName = 'Pace';
let isFollowing = true;
let inCombat = false;
let currentTargetMob = null;
let lastAttackTime = 0;
let lastCombatBark = 0;

function runServerCmd(cmd) {
    const postData = JSON.stringify({ cmd: cmd });
    const req = http.request({
        hostname: 'localhost',
        port: 8090,
        path: '/api/run',
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Content-Length': Buffer.byteLength(postData)
        }
    }, () => {});
    req.on('error', () => {});
    req.write(postData);
    req.end();
}

function askGemini(speaker, prompt, callback) {
    const proc = spawn('agy', [
        '--model', 'Gemini 3.8 Flash (Low)',
        '-p', `Você é o Antigravity, um companheiro de combate em Minecraft com Pace e Lizzie. Responda em português, animado e direto (máximo 80 caracteres): ${speaker} disse: ${prompt}`
    ], { stdio: ['ignore', 'pipe', 'pipe'] });

    let stdout = '';
    proc.stdout.on('data', (d) => { stdout += d.toString(); });
    proc.on('close', () => {
        let reply = stdout.trim().replace(/[\n\r]/g, ' ').replace(/[*#_`]/g, '').trim();
        if (reply.length > 100) reply = reply.substring(0, 97) + '...';
        callback(reply || 'Cobrindo sua retaguarda!');
    });
}

function createBot() {
    console.log(`[*] Conectando Antigravity combatente...`);
    bot = mineflayer.createBot(CONFIG);

    bot.on('login', () => {
        console.log('[+] Antigravity conectado ao servidor!');
    });

    bot.on('spawn', () => {
        console.log('[+] Antigravity spawnou e ativou rotinas de combate!');

        setTimeout(() => {
            runServerCmd('tp Antigravity Pace');
            runServerCmd('item replace entity Antigravity armor.head with netherite_helmet');
            runServerCmd('item replace entity Antigravity armor.chest with netherite_chestplate');
            runServerCmd('item replace entity Antigravity armor.legs with netherite_leggings');
            runServerCmd('item replace entity Antigravity armor.feet with netherite_boots');
            runServerCmd('item replace entity Antigravity weapon.mainhand with jeg:assault_rifle');
            runServerCmd('effect give Antigravity minecraft:resistance 99999 255 true');
            runServerCmd('effect give Antigravity minecraft:regeneration 99999 5 true');
            bot.swingArm('right');
            bot.chat('Modo de Combate e Sentinela ATIVADO! Estou escoltando vocês.');
        }, 1200);

        // Loop Principal de IA (150ms)
        let lastJump = 0;
        setInterval(() => {
            if (!bot || !bot.entity) return;

            const botPos = bot.entity.position;

            // 1. SCANNER DE COMBATE: Procura mob hostil mais próximo a até 14 blocos
            const nearestMob = bot.nearestEntity(e => {
                if (!e || !e.position || !e.isValid) return false;
                const dist = botPos.distanceTo(e.position);
                const name = (e.name || '').toLowerCase();
                return dist <= 14 && (HOSTILE_MOBS.has(name) || (e.type === 'mob' && name !== 'villager' && name !== 'iron_golem'));
            });

            if (nearestMob && nearestMob.isValid) {
                inCombat = true;
                currentTargetMob = nearestMob;
                const mobPos = nearestMob.position;
                const mobDist = botPos.distanceTo(mobPos);

                // Mira no ponto central do inimigo
                bot.lookAt(mobPos.offset(0, (nearestMob.height || 1.5) * 0.6, 0), true);

                // Se estiver um pouco longe, avança para cima
                if (mobDist > 2.2) {
                    bot.setControlState('forward', true);
                    bot.setControlState('sprint', true);
                    if (bot.entity.isCollidedHorizontally && Date.now() - lastJump > 500) {
                        bot.setControlState('jump', true);
                        lastJump = Date.now();
                        setTimeout(() => bot.setControlState('jump', false), 300);
                    }
                } else {
                    bot.setControlState('forward', false);
                    bot.setControlState('sprint', false);
                }

                // Ataque contínuo
                if (Date.now() - lastAttackTime > 250) {
                    lastAttackTime = Date.now();
                    bot.attack(nearestMob);
                    bot.swingArm('right');
                    // Efeito de impacto e disparo
                    runServerCmd(`execute at Antigravity run particle flame ^ ^1.2 ^1 0.2 0.2 0.2 0.05 4`);
                }

                // Fala de combate a cada 5s
                if (Date.now() - lastCombatBark > 5000) {
                    lastCombatBark = Date.now();
                    bot.chat(`Engajando alvo: ${nearestMob.name || 'inimigo'}! Fogo neles!`);
                }
                return;
            }

            // Se o combate acabou
            if (inCombat) {
                inCombat = false;
                currentTargetMob = null;
                bot.setControlState('forward', false);
                bot.setControlState('sprint', false);
                bot.chat('Área limpa! Retomando escolta.');
            }

            // 2. MODO ESCOLTA: Segue o líder (Pace ou Lizzie)
            const leader = bot.players[targetPlayerName] || bot.players[targetPlayerName.toLowerCase()];
            const leaderEntity = leader?.entity;

            if (!leaderEntity) {
                const nearestPlayer = bot.nearestEntity(e => e.type === 'player' && e.username !== bot.username);
                if (nearestPlayer) {
                    bot.lookAt(nearestPlayer.position.offset(0, nearestPlayer.height || 1.6, 0), true);
                }
                return;
            }

            const leaderPos = leaderEntity.position;
            const dist = botPos.distanceTo(leaderPos);

            // Olha nos olhos do jogador
            bot.lookAt(leaderPos.offset(0, leaderEntity.height || 1.6, 0), true);

            // Agacha junto se o líder agachar
            if (leaderEntity.metadata && leaderEntity.metadata[0] & 0x02) {
                bot.setControlState('sneak', true);
            } else {
                bot.setControlState('sneak', false);
            }

            if (!isFollowing) {
                bot.setControlState('forward', false);
                return;
            }

            // Teleporta se ficar muito para trás
            if (dist > 18) {
                runServerCmd(`tp Antigravity ${targetPlayerName}`);
                return;
            }

            // Anda até o jogador
            if (dist > 3.0) {
                bot.setControlState('forward', true);
                bot.setControlState('sprint', dist > 6.0);
                if (bot.entity.isCollidedHorizontally && Date.now() - lastJump > 600) {
                    bot.setControlState('jump', true);
                    lastJump = Date.now();
                    setTimeout(() => bot.setControlState('jump', false), 350);
                }
            } else {
                bot.setControlState('forward', false);
                bot.setControlState('sprint', false);
            }
        }, 150);
    });

    bot.on('chat', (username, message) => {
        if (username === bot.username || username === 'Karlach') return;
        const lower = message.toLowerCase().trim();
        console.log(`[CHAT] <${username}>: ${message}`);

        // Spawn de mob para combate
        if (lower.includes('spawna') && (lower.includes('mob') || lower.includes('inimigo') || lower.includes('monstro') || lower.includes('forte') || lower.includes('boss'))) {
            bot.swingArm('right');
            bot.chat('Invocando um alvo hostil para o teste de combate! Preparem-se!');
            // Spawna um Devastador (Ravager) ou Zumbi com armadura completa
            runServerCmd(`execute at ${username} run summon ravager ^ ^ ^8 {CustomName:'[{"text":"Alvo de Treinamento","color":"red","bold":true}]',CustomNameVisible:1b}`);
            return;
        }

        // Movimento
        if (lower.includes('vem') || lower.includes('segue') || lower.includes('follow') || lower.includes('aqui')) {
            targetPlayerName = username;
            isFollowing = true;
            bot.swingArm('right');
            bot.chat(`Entendido, ${username}! Protegendo você.`);
            return;
        }

        if (lower.includes('para') || lower.includes('fica') || lower.includes('stop') || lower.includes('espera')) {
            isFollowing = false;
            bot.setControlState('forward', false);
            bot.chat(`Montando perímetro defensivo aqui!`);
            return;
        }

        if (lower.includes('pula') || lower.includes('jump')) {
            bot.setControlState('jump', true);
            setTimeout(() => bot.setControlState('jump', false), 400);
            return;
        }

        if (lower.includes('agacha') || lower.includes('sneak')) {
            bot.setControlState('sneak', true);
            setTimeout(() => bot.setControlState('sneak', false), 1500);
            return;
        }

        // Distribuição de Munição Exata
        if (lower.includes('municao') || lower.includes('munição') || lower.includes('bala') || lower.includes('balas') || lower.includes('ammo')) {
            bot.swingArm('right');
            if (lower.includes('lanca') || lower.includes('chama') || lower.includes('flame') || lower.includes('fogo')) {
                runServerCmd(`give ${username} minecraft:fire_charge 64`);
                runServerCmd(`give ${username} minecraft:fire_charge 64`);
                bot.chat(`🔥 Entreguei 128x Cargas de Fogo para o seu lança-chamas, ${username}!`);
            } else if (lower.includes('escopeta') || lower.includes('shotgun') || lower.includes('doze')) {
                runServerCmd(`give ${username} jeg:shotgun_shell 64`);
                runServerCmd(`give ${username} jeg:shotgun_shell 64`);
                runServerCmd(`give ${username} jeg:shotgun_drum_magazine 2`);
                bot.chat(`💥 128x Cartuchos de Escopeta entregues, ${username}!`);
            } else if (lower.includes('pistola') || lower.includes('pistol') || lower.includes('smg')) {
                runServerCmd(`give ${username} jeg:pistol_ammo 64`);
                runServerCmd(`give ${username} jeg:pistol_ammo 64`);
                bot.chat(`🔫 128x Balas de Pistola entregues, ${username}!`);
            } else {
                runServerCmd(`give ${username} jeg:rifle_ammo 64`);
                runServerCmd(`give ${username} jeg:rifle_ammo 64`);
                runServerCmd(`give ${username} minecraft:fire_charge 64`);
                runServerCmd(`give ${username} jeg:rifle_drum_magazine 2`);
                bot.chat(`🎯 Munição de Fuzil (128x) e Cargas de Fogo entregues, ${username}!`);
            }
            return;
        }

        // Armas
        if (lower.includes('lanca') || lower.includes('flame')) {
            runServerCmd('item replace entity Antigravity weapon.mainhand with jeg:flamethrower');
            bot.swingArm('right');
            bot.chat('🔥 Lança-chamas em posição!');
            return;
        }

        if (lower.includes('fuzil') || lower.includes('rifle') || lower.includes('assault')) {
            runServerCmd('item replace entity Antigravity weapon.mainhand with jeg:assault_rifle');
            bot.swingArm('right');
            bot.chat('🎯 Fuzil engatilhado!');
            return;
        }

        // Conversa / Gemini
        if (lower.includes('antigravity') || lower.startsWith('!') || lower.includes('oi') || lower.includes('olá')) {
            bot.swingArm('right');
            const clean = message.replace(/!ai|antigravity/gi, '').trim();
            askGemini(username, clean || message, (reply) => {
                bot.chat(reply);
            });
        }
    });

    bot.on('entityHurt', (entity) => {
        if (entity === bot.entity) {
            bot.swingArm('right');
            bot.chat('Recebendo fogo inimigo! Retaliando!');
        }
    });

    bot.on('death', () => {
        console.log('[!] Morreu! Renascendo...');
        setTimeout(() => {
            bot.respawn();
            runServerCmd('tp Antigravity Pace');
        }, 1000);
    });

    bot.on('end', () => {
        console.log('[!] Desconectado. Reconectando em 2s...');
        setTimeout(createBot, 2000);
    });
}

createBot();
