const mineflayer = require('mineflayer');
const { pathfinder, Movements, goals } = require('mineflayer-pathfinder');
const { Vec3 } = require('vec3');
const { spawn } = require('child_process');
const http = require('http');

const CONFIG = {
    host: '127.0.0.1',
    port: 25565,
    username: 'Karlach',
    version: '1.21.1'
};

const HOSTILE_MOBS = new Set([
    'zombie', 'skeleton', 'creeper', 'spider', 'cave_spider', 'enderman',
    'witch', 'phantom', 'drowned', 'husk', 'stray', 'pillager', 'vindicator',
    'ravager', 'evoker', 'slime', 'magma_cube', 'wither_skeleton', 'ghast'
]);

// Circular Promenade Waypoints & Animal Enclosures
const PROMENADE_WAYPOINTS = [
    {
        name: 'Praça Norte do Chafariz',
        isEnclosure: false,
        pos: new Vec3(-175, 65, 175),
        lookYaw: 0,
        treat: 'minecraft:golden_carrot',
        bark: 'Que brisa boa! O ar fresco daqui faz um bem danado pro meu motor.'
    },
    {
        name: 'Bosque das Capivaras',
        isEnclosure: true,
        pos: new Vec3(-173, 65, 171), // Entrada externa do portão
        gatePos: new Vec3(-171, 65, 171),
        gateFacing: 'west',
        gateType: 'dark_oak_fence_gate',
        insidePenPos: new Vec3(-168, 65, 171),
        penCenter: new Vec3(-160, 65, 174),
        treat: 'minecraft:apple',
        bark: 'Capivarinha linda! Se alguém te estressar, eu corto no meio com o machado!'
    },
    {
        name: 'Corredor Leste do Chafariz',
        isEnclosure: false,
        pos: new Vec3(-173, 65, 180),
        lookYaw: 1.57,
        treat: 'minecraft:sweet_berries',
        bark: 'Patinhos e cervos... que calmaria. É bom demais ter paz.'
    },
    {
        name: 'Reserva Oriental dos Pandas',
        isEnclosure: true,
        pos: new Vec3(-173, 65, 189), // Entrada externa do portão
        gatePos: new Vec3(-171, 65, 189),
        gateFacing: 'west',
        gateType: 'dark_oak_fence_gate',
        insidePenPos: new Vec3(-168, 65, 189),
        penCenter: new Vec3(-155, 65, 188),
        treat: 'minecraft:bamboo',
        bark: 'Bambu crocante saindo do forno! Come tudo, grandão!'
    },
    {
        name: 'Praça Sul & Bancos',
        isEnclosure: false,
        pos: new Vec3(-175, 65, 185),
        lookYaw: 3.14,
        treat: 'minecraft:wheat',
        bark: 'Esse santuário tá um brinco, soldado! Tudo sob controle absoluto.'
    },
    {
        name: 'Tundra dos Mamutes',
        isEnclosure: true,
        pos: new Vec3(-177, 65, 189), // Entrada externa do portão
        gatePos: new Vec3(-179, 65, 189),
        gateFacing: 'west',
        gateType: 'dark_oak_fence_gate',
        insidePenPos: new Vec3(-182, 65, 189),
        penCenter: new Vec3(-190, 65, 188),
        treat: 'minecraft:hay_block',
        bark: 'Um mamute de verdade! O frio dessa neve até alivia o fogo no meu peito!'
    },
    {
        name: 'Corredor Oeste do Chafariz',
        isEnclosure: false,
        pos: new Vec3(-177, 65, 180),
        lookYaw: -1.57,
        treat: 'minecraft:wheat',
        bark: 'Girafas e zebras todas alimentadas e felizes!'
    },
    {
        name: 'Savana dos Elefantes',
        isEnclosure: true,
        pos: new Vec3(-177, 65, 171), // Entrada externa do portão
        gatePos: new Vec3(-179, 65, 171),
        gateFacing: 'west',
        gateType: 'spruce_fence_gate',
        insidePenPos: new Vec3(-182, 65, 171),
        penCenter: new Vec3(-190, 65, 174),
        treat: 'minecraft:wheat',
        bark: 'Olha o porte desse elefante, soldado! Uma verdadeira fortaleza gentil!'
    }
];

let bot = null;
let currentWpIndex = 0;
let isPatrolling = true;
let isFollowing = false;
let followTarget = 'Pace';
let isRestingAtWp = false;
let isFeeding = false;
let inCombat = false;
let lastAttackTime = 0;
let lastHeartBeat = 0;
let lastBarkTime = 0;
let lastChatReplyTime = 0;

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

function askGeminiKarlach(speaker, prompt, callback) {
    const sysPrompt = `Você é a Karlach de Baldur's Gate 3, uma bárbara Tiefling com um motor infernal de Zariel queimando no peito, agora protetora oficial do Santuário de Animais do castelo com Pace e Lizzie.
Personalidade:
- Chama o jogador de 'soldado' (soldier) ou 'docinho'.
- Super animada, calorosa, divertida, sem filtro e apaixonada por bichos fofos (especialmente capivaras, patos e elefantes).
- Faz piadas sobre seu coração pegar fogo ('aquecedor ambulante!').
- Se alguém machucar os animais, ameaça partir no meio com o machado.
- Responda em Português de forma DIRETA, VIVA e CURTA (máximo 110 caracteres):`;

    const proc = spawn('agy', [
        '--model', 'Gemini 3.8 Flash (Low)',
        '-p', `${sysPrompt}\n${speaker} disse: ${prompt}`
    ], { stdio: ['ignore', 'pipe', 'pipe'] });

    let stdout = '';
    proc.stdout.on('data', (d) => { stdout += d.toString(); });
    proc.on('close', () => {
        let reply = stdout.trim().replace(/[\n\r]/g, ' ').replace(/[*#_`]/g, '').trim();
        if (reply.length > 120) reply = reply.substring(0, 117) + '...';
        callback(reply || 'Tô de olho nesses bichinhos fofos, soldado! Pode confiar na Karlach!');
    });
}

function moveToNextWaypoint() {
    if (!bot || !bot.pathfinder || !isPatrolling || inCombat || isFollowing) return;

    isRestingAtWp = false;
    bot.setControlState('sneak', false);

    const wp = PROMENADE_WAYPOINTS[currentWpIndex];
    console.log(`[KARLACH-NAV] Caminhando suavemente até: ${wp.name} [${wp.pos.x}, ${wp.pos.y}, ${wp.pos.z}]`);

    // Equipa o petisco correspondente
    runServerCmd(`item replace entity Karlach weapon.mainhand with ${wp.treat}`);

    // Define o objetivo do pathfinder com precisão suave (raio de 1 bloco)
    bot.pathfinder.setGoal(new goals.GoalNear(wp.pos.x, wp.pos.y, wp.pos.z, 1.0));
}

async function toggleGate(gateX, gateY, gateZ, facing, gateType, shouldOpen) {
    const state = shouldOpen ? 'open=true' : 'open=false';
    const sound = shouldOpen ? 'minecraft:block.fence_gate.open' : 'minecraft:block.fence_gate.close';

    runServerCmd(`setblock ${gateX} ${gateY} ${gateZ} ${gateType}[facing=${facing},${state}] replace`);
    runServerCmd(`setblock ${gateX} ${gateY + 1} ${gateZ} ${gateType}[facing=${facing},${state}] replace`);
    runServerCmd(`playsound ${sound} block @a ${gateX} ${gateY} ${gateZ} 1.0 1.0`);
    if (bot) bot.swingArm('right');
    await new Promise(r => setTimeout(r, 250));
}

async function stepThroughGate(targetX, targetZ, durationMs = 900) {
    if (!bot || !bot.entity) return;
    await bot.lookAt(new Vec3(targetX, bot.entity.position.y + 1.6, targetZ), true);
    await new Promise(r => setTimeout(r, 150));
    bot.setControlState('forward', true);
    await new Promise(r => setTimeout(r, durationMs));
    bot.setControlState('forward', false);
}

async function handleEnclosureVisit(wp) {
    if (!bot || !isPatrolling || inCombat || isFollowing) return;

    isFeeding = true;
    console.log(`[KARLACH-ROUTINE] Entrando no cercado: ${wp.name}...`);

    try {
        // 1. Olhar para o portão
        await bot.lookAt(new Vec3(wp.gatePos.x + 0.5, wp.gatePos.y + 1.2, wp.gatePos.z + 0.5), true);
        await new Promise(r => setTimeout(r, 400));

        // 2. Abrir o portão
        await toggleGate(wp.gatePos.x, wp.gatePos.y, wp.gatePos.z, wp.gateFacing, wp.gateType, true);

        // 3. Dar passo decidido para dentro do cercado
        await stepThroughGate(wp.insidePenPos.x, wp.insidePenPos.z, 1100);

        // Garantir que está dentro antes de fechar o portão
        const isEast = wp.gatePos.x > -175;
        if (isEast && bot.entity.position.x < -170.5) {
            runServerCmd(`tp Karlach ${wp.insidePenPos.x} 65 ${wp.insidePenPos.z}`);
            await new Promise(r => setTimeout(r, 200));
        } else if (!isEast && bot.entity.position.x > -179.5) {
            runServerCmd(`tp Karlach ${wp.insidePenPos.x} 65 ${wp.insidePenPos.z}`);
            await new Promise(r => setTimeout(r, 200));
        }

        // 4. Fechar o portão atrás de si com segurança
        await toggleGate(wp.gatePos.x, wp.gatePos.y, wp.gatePos.z, wp.gateFacing, wp.gateType, false);
        console.log(`[KARLACH-ROUTINE] Dentro do cercado ${wp.name}. Portão trancado.`);

        // 5. Procurar o animal mais próximo no cercado
        const botPos = bot.entity.position;
        const animal = bot.nearestEntity(e => {
            if (!e || !e.position || e.id === bot.entity.id) return false;
            if (e.type === 'player' || e.name === 'player' || e.name === 'armor_stand' || e.name === 'item') return false;
            return botPos.distanceTo(e.position) <= 22.0;
        });

        if (animal) {
            console.log(`[KARLACH-ROUTINE] Animal encontrado: ${animal.name} em [${animal.position.x.toFixed(1)}, ${animal.position.z.toFixed(1)}]. Indo até ele...`);
            bot.pathfinder.setGoal(new goals.GoalNear(animal.position.x, animal.position.y, animal.position.z, 2.0));
            await new Promise(r => setTimeout(r, 3500));
        } else {
            bot.pathfinder.setGoal(new goals.GoalNear(wp.penCenter.x, wp.penCenter.y, wp.penCenter.z, 3.0));
            await new Promise(r => setTimeout(r, 2500));
        }

        // 6. Interação cara a cara com o bicho
        const target = bot.nearestEntity(e => {
            if (!e || !e.position || e.id === bot.entity.id) return false;
            if (e.type === 'player' || e.name === 'player' || e.name === 'armor_stand' || e.name === 'item') return false;
            return bot.entity.position.distanceTo(e.position) <= 8.0;
        });

        if (target) {
            await bot.lookAt(target.position.offset(0, (target.height || 1) * 0.6, 0), true);
        }

        bot.swingArm('right');

        // Mecânicas reais de alimentação
        runServerCmd('execute at Karlach as @e[type=!player,type=!item,type=!armor_stand,type=!experience_orb,distance=..8,sort=nearest,limit=3] run data merge entity @s {InLove:600}');
        runServerCmd('execute at Karlach as @e[type=!player,type=!item,type=!armor_stand,type=!experience_orb,distance=..8,sort=nearest,limit=3] at @s run particle minecraft:heart ~ ~0.9 ~ 0.35 0.35 0.35 0 6 force');
        runServerCmd('execute at Karlach as @e[type=!player,type=!item,type=!armor_stand,type=!experience_orb,distance=..8,sort=nearest,limit=1] at @s run playsound minecraft:entity.generic.eat ambient @a ~ ~ ~ 0.85 1.15');
        runServerCmd(`execute at Karlach run summon item ^ ^1.1 ^0.7 {Item:{id:"${wp.treat}",Count:1b},Motion:[0.0,0.06,0.0],PickupDelay:25,Age:5880s}`);
        runServerCmd('execute at Karlach as @e[type=!player,type=!item,type=!armor_stand,type=!experience_orb,distance=..8,sort=nearest,limit=3] run effect give @s minecraft:instant_health 1 0 true');

        // Segundo afago
        await new Promise(r => setTimeout(r, 2000));
        bot.swingArm('right');
        runServerCmd('execute at Karlach as @e[type=!player,type=!item,type=!armor_stand,type=!experience_orb,distance=..8,sort=nearest,limit=2] at @s run particle minecraft:heart ~ ~0.9 ~ 0.25 0.25 0.25 0 4 force');
        runServerCmd('execute at Karlach as @e[type=!player,type=!item,type=!armor_stand,type=!experience_orb,distance=..8,sort=nearest,limit=1] at @s run playsound minecraft:entity.generic.eat ambient @a ~ ~ ~ 0.7 1.25');

        // Comentário da Karlach
        if (Date.now() - lastBarkTime > 20000) {
            lastBarkTime = Date.now();
            bot.chat(wp.bark);
        }

        await new Promise(r => setTimeout(r, 1500));

        // 7. Retornar ao portão (lado de dentro)
        console.log(`[KARLACH-ROUTINE] Retornando ao portão de saída ${wp.name}...`);
        bot.pathfinder.setGoal(new goals.GoalNear(wp.insidePenPos.x, wp.insidePenPos.y, wp.insidePenPos.z, 0.8));
        for (let i = 0; i < 8; i++) {
            if (bot.entity.position.distanceTo(wp.insidePenPos) <= 1.8) break;
            await new Promise(r => setTimeout(r, 500));
        }

        // 8. Abrir o portão
        await bot.lookAt(new Vec3(wp.gatePos.x + 0.5, wp.gatePos.y + 1.2, wp.gatePos.z + 0.5), true);
        await toggleGate(wp.gatePos.x, wp.gatePos.y, wp.gatePos.z, wp.gateFacing, wp.gateType, true);

        // 9. Dar passo para fora no corredor
        await stepThroughGate(wp.pos.x, wp.pos.z, 1200);

        // Garantir que ela está 100% FORA do cercado na calçada
        if (isEast && bot.entity.position.x > -172.0) {
            runServerCmd(`tp Karlach ${wp.pos.x} 65 ${wp.pos.z}`);
            await new Promise(r => setTimeout(r, 200));
        } else if (!isEast && bot.entity.position.x < -178.0) {
            runServerCmd(`tp Karlach ${wp.pos.x} 65 ${wp.pos.z}`);
            await new Promise(r => setTimeout(r, 200));
        }

        // 10. Fechar o portão trancando com segurança
        await toggleGate(wp.gatePos.x, wp.gatePos.y, wp.gatePos.z, wp.gateFacing, wp.gateType, false);
        console.log(`[KARLACH-ROUTINE] Portão fechado com segurança!`);

    } catch (e) {
        console.error('[KARLACH-ROUTINE] Erro no cuidado do cercado:', e);
        await toggleGate(wp.gatePos.x, wp.gatePos.y, wp.gatePos.z, wp.gateFacing, wp.gateType, false);
    } finally {
        isFeeding = false;
        isRestingAtWp = false;
        if (isPatrolling && !inCombat && !isFollowing) {
            currentWpIndex = (currentWpIndex + 1) % PROMENADE_WAYPOINTS.length;
            moveToNextWaypoint();
        }
    }
}

function handlePlazaVisit(wp) {
    if (!bot || !isPatrolling || inCombat) return;

    bot.look(wp.lookYaw, 0.05, true);

    if (Date.now() - lastBarkTime > 25000) {
        lastBarkTime = Date.now();
        setTimeout(() => {
            if (isRestingAtWp && !inCombat) bot.chat(wp.bark);
        }, 2000);
    }

    setTimeout(() => {
        if (!isPatrolling || inCombat || isFollowing) return;
        isRestingAtWp = false;
        currentWpIndex = (currentWpIndex + 1) % PROMENADE_WAYPOINTS.length;
        moveToNextWaypoint();
    }, 6000);
}

function onArrivedAtWaypoint() {
    if (!bot || !isPatrolling || inCombat || isRestingAtWp) return;

    isRestingAtWp = true;
    const wp = PROMENADE_WAYPOINTS[currentWpIndex];
    console.log(`[KARLACH-ROUTINE] Chegou em: ${wp.name}.`);

    if (wp.isEnclosure) {
        handleEnclosureVisit(wp);
    } else {
        handlePlazaVisit(wp);
    }
}

function createBot() {
    console.log(`[*] Conectando Karlach com Navegação A* Pathfinder...`);
    bot = mineflayer.createBot(CONFIG);
    bot.loadPlugin(pathfinder);

    bot.on('login', () => {
        console.log('[+] Karlach conectada ao servidor!');
    });

    bot.on('spawn', () => {
        console.log('[+] Karlach spawnou no mundo! Configurando pathfinder e física...');

        // Configuração de Movimentação Natural e Suave
        const move = new Movements(bot);
        move.canDig = false;             // Nunca quebra blocos
        move.allowParkour = false;       // Não tenta saltos acrobáticos em cercas
        move.allowSprinting = false;     // Caminhada humana suave e elegante
        move.canOpenDoors = false;       // Não mexe em portões
        bot.pathfinder.setMovements(move);

        // Setup inicial
        setTimeout(() => {
            runServerCmd('tp Karlach -175 65 174');
            runServerCmd('skin set Karlach Karlach');
            runServerCmd('item replace entity Karlach weapon.mainhand with minecraft:wheat');
            runServerCmd('item replace entity Karlach weapon.offhand with minecraft:netherite_axe');
            runServerCmd('item replace entity Karlach armor.chest with minecraft:leather_chestplate[dyed_color=11546150]');
            runServerCmd('item replace entity Karlach armor.legs with minecraft:leather_leggings[dyed_color=3944747]');
            runServerCmd('item replace entity Karlach armor.feet with minecraft:leather_boots[dyed_color=1908001]');
            runServerCmd('effect give Karlach minecraft:resistance 99999 255 true');
            runServerCmd('effect give Karlach minecraft:fire_resistance 99999 255 true');
            runServerCmd('effect give Karlach minecraft:regeneration 99999 5 true');

            bot.swingArm('right');
            bot.chat('Passos firmes e suaves, soldado! Cuidando da bicharada no santuário.');

            // Inicia a patrulha suave pelo santuário
            setTimeout(moveToNextWaypoint, 2000);
        }, 1500);

        // Evento de chegada ao objetivo do Pathfinder
        bot.on('goal_reached', () => {
            if (isPatrolling && !inCombat && !isFollowing && !isRestingAtWp) {
                onArrivedAtWaypoint();
            }
        });

        // Loop de Efeitos Visuais & Monitoramento (a cada 250ms)
        setInterval(() => {
            if (!bot || !bot.entity) return;

            // 1. Motor Infernal (Chamas & Fumaça no peito a cada 2s)
            if (Date.now() - lastHeartBeat > 2000) {
                lastHeartBeat = Date.now();
                runServerCmd('execute at Karlach run particle minecraft:flame ~ ~1.1 ~ 0.08 0.1 0.08 0.01 2 force');
                runServerCmd('execute at Karlach run particle minecraft:smoke ~ ~1.2 ~ 0.05 0.08 0.05 0.01 1 force');
            }

            // 2. Olhar para o jogador se estiver bem perto (< 3.8 blocos) enquanto estiver parada e não estiver ocupada alimentando
            if (isRestingAtWp && !isFeeding && !inCombat) {
                const botPos = bot.entity.position;
                const nearbyPlayer = bot.nearestEntity(e => 
                    e.type === 'player' && 
                    e.username !== bot.username && 
                    e.username !== 'Antigravity' && 
                    botPos.distanceTo(e.position) < 3.8
                );
                if (nearbyPlayer) {
                    bot.lookAt(nearbyPlayer.position.offset(0, nearbyPlayer.height || 1.6, 0), true);
                }
            }

            // 3. Detecção e Combate a Mobs Hostis (12 blocos)
            const botPos = bot.entity.position;
            const nearestMob = bot.nearestEntity(e => {
                if (!e || !e.position || !e.isValid) return false;
                const dist = botPos.distanceTo(e.position);
                const name = (e.name || '').toLowerCase();
                return dist <= 12 && HOSTILE_MOBS.has(name);
            });

            if (nearestMob && nearestMob.isValid) {
                if (!inCombat) {
                    inCombat = true;
                    isRestingAtWp = false;
                    bot.setControlState('sneak', false);
                    runServerCmd('item replace entity Karlach weapon.mainhand with minecraft:netherite_axe');
                    bot.chat('Hostil no santuário?! Vai virar picadinho! Ninguém mexe com meus bichos!');
                }
                const mobPos = nearestMob.position;
                bot.pathfinder.setGoal(new goals.GoalNear(mobPos.x, mobPos.y, mobPos.z, 2.0));
                bot.lookAt(mobPos.offset(0, (nearestMob.height || 1.5) * 0.6, 0), true);

                if (botPos.distanceTo(mobPos) <= 3.2 && Date.now() - lastAttackTime > 350) {
                    lastAttackTime = Date.now();
                    bot.attack(nearestMob);
                    bot.swingArm('right');
                    runServerCmd('execute at Karlach run particle flame ^ ^1.2 ^1 0.2 0.2 0.2 0.05 3');
                }
                return;
            }

            // Se o combate acabou, retoma a rota
            if (inCombat) {
                inCombat = false;
                runServerCmd('item replace entity Karlach weapon.mainhand with minecraft:wheat');
                bot.chat('Pronto! Alvo neutralizado. Retomando a patrulha.');
                setTimeout(moveToNextWaypoint, 1500);
            }
        }, 250);
    });

    bot.on('chat', (username, message) => {
        if (username === bot.username || username === 'Antigravity') return;
        const lower = message.toLowerCase().trim();
        console.log(`[CHAT-KARLACH] <${username}>: ${message}`);

        // Comandos de Controle
        if (lower.includes('vem') || lower.includes('segue') || lower.includes('follow') || lower.includes('aqui')) {
            followTarget = username;
            isFollowing = true;
            isPatrolling = false;
            isRestingAtWp = false;
            bot.setControlState('sneak', false);

            const leader = bot.players[username]?.entity;
            if (leader) {
                bot.pathfinder.setGoal(new goals.GoalFollow(leader, 2.5), true);
            }
            bot.swingArm('right');
            bot.chat(`Com você, soldado! Protegendo sua retaguarda.`);
            return;
        }

        if (lower.includes('patrulha') || lower.includes('santuario') || lower.includes('cuida') || lower.includes('bichos') || lower.includes('volta')) {
            isFollowing = false;
            isPatrolling = true;
            bot.swingArm('right');
            bot.chat(`Entendido, soldado! Retomando os cuidados com os animais no santuário!`);
            moveToNextWaypoint();
            return;
        }

        if (lower.includes('alimenta') || lower.includes('comida') || lower.includes('trigo') || lower.includes('maca')) {
            bot.swingArm('right');
            runServerCmd('execute at Karlach run particle minecraft:heart ~ ~1.2 ~ 1.5 0.5 1.5 0 5');
            bot.chat(`Bora encher o bucho desses fofuchos! Já tô com os petiscos na mão.`);
            return;
        }

        if (lower.includes('danca') || lower.includes('dança') || lower.includes('anima')) {
            bot.swingArm('right');
            bot.setControlState('jump', true);
            setTimeout(() => {
                bot.setControlState('jump', false);
                bot.setControlState('sneak', true);
                setTimeout(() => bot.setControlState('sneak', false), 600);
            }, 300);
            bot.chat(`Urrul! O motor tá fervendo, soldado! Solta a música!`);
            return;
        }

        // Resposta Inteligente via Gemini se mencionada
        if (lower.includes('karlach') || lower.includes('soldado') || lower.includes('capivara') || lower.includes('bicho') || lower.includes('motor')) {
            if (Date.now() - lastChatReplyTime < 3000) return;
            lastChatReplyTime = Date.now();
            askGeminiKarlach(username, message, (reply) => {
                bot.chat(reply);
            });
        }
    });

    bot.on('error', (err) => {
        console.error('[ERR] Karlach erro:', err);
    });

    bot.on('end', () => {
        console.log('[-] Karlach desconectou. Reconectando em 5 segundos...');
        setTimeout(createBot, 5000);
    });
}

createBot();
