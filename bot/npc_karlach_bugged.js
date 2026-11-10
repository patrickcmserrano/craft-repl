/**
 * npc_karlach_bugged.js - Versão Legada Bugada (Mineflayer / Node.js)
 * 
 * Reproduz com 100% de precisão os bugs estruturais do Mineflayer descritos no vídeo:
 * 1. Conflito do God-Object: Estado 'sneak' preso durante o pathfinding (Karlach andando agachada).
 * 2. Armadilha do Portão Duplo: Timer cego (setTimeout) fecha o portão antes da travessia.
 *    Sem teleporte artificial (tp Karlach), ela fica trancada dentro do cercado das capivaras [-171, 65, 171].
 * 3. Falha do A*: canOpenDoors = false + cerca dupla tratada como parede intransponível.
 * 4. Desperdício de memória no Heap V8 (~210 MB RSS para 1 bot).
 */

const mineflayer = require('mineflayer');
const { pathfinder, Movements, goals } = require('mineflayer-pathfinder');
const { Vec3 } = require('vec3');
const http = require('http');

const CONFIG = {
    host: '127.0.0.1',
    port: 25565,
    username: 'Karlach',
    version: '1.21.1'
};

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
    req.on('error', (e) => console.error('[HTTP-ERR]', e.message));
    req.write(postData);
    req.end();
}

let bot = null;
let trapped = false;

async function toggleGate(gateX, gateY, gateZ, facing, gateType, shouldOpen) {
    const state = shouldOpen ? 'open=true' : 'open=false';
    const sound = shouldOpen ? 'minecraft:block.fence_gate.open' : 'minecraft:block.fence_gate.close';

    runServerCmd(`setblock ${gateX} ${gateY} ${gateZ} ${gateType}[facing=${facing},${state}] replace`);
    runServerCmd(`setblock ${gateX} ${gateY + 1} ${gateZ} ${gateType}[facing=${facing},${state}] replace`);
    runServerCmd(`playsound ${sound} block @a ${gateX} ${gateY} ${gateZ} 1.0 1.0`);
    if (bot) bot.swingArm('right');
    await new Promise(r => setTimeout(r, 200));
}

async function stepThroughGate(targetX, targetZ, durationMs) {
    if (!bot || !bot.entity) return;
    await bot.lookAt(new Vec3(targetX, bot.entity.position.y + 1.6, targetZ), true);
    await new Promise(r => setTimeout(r, 100));
    bot.setControlState('forward', true);
    await new Promise(r => setTimeout(r, durationMs));
    bot.setControlState('forward', false);
}

function startBugDemonstration() {
    console.log('\n======================================================');
    console.log(' [DEMO MINEFLAYER BUGADO] Iniciando rotina da Karlach...');
    console.log('======================================================\n');

    // Teleporta para o início da calçada
    runServerCmd('tp Karlach -175 65 175');
    runServerCmd('skin set Karlach Karlach');
    runServerCmd('item replace entity Karlach weapon.mainhand with minecraft:apple');
    runServerCmd('item replace entity Karlach weapon.offhand with minecraft:netherite_axe');
    runServerCmd('item replace entity Karlach armor.chest with minecraft:leather_chestplate[dyed_color=11546150]');
    runServerCmd('item replace entity Karlach armor.legs with minecraft:leather_leggings[dyed_color=3944747]');
    runServerCmd('item replace entity Karlach armor.feet with minecraft:leather_boots[dyed_color=1908001]');

    bot.chat('Bora ver como estão as capivarinhas, soldado!');

    setTimeout(async () => {
        console.log('[KARLACH] Caminhando em direção ao Bosque das Capivaras [-173, 65, 171]...');

        // Bug 1: Estado 'sneak' ativado sem isolamento pelo God-Object
        console.log('\n>>> [BUG 1: GOD-OBJECT COLLISION] Conflito de estado de controle!');
        console.log('>>> bot.setControlState("sneak", true) foi ativado e trava a caminhada.\n');
        bot.setControlState('sneak', true);

        // Define objetivo no portão das capivaras
        bot.pathfinder.setGoal(new goals.GoalNear(-173, 65, 171, 0.5));

        // Espera chegar próximo ao portão
        while (bot.entity.position.distanceTo(new Vec3(-173, 65, 171)) > 1.2) {
            await new Promise(r => setTimeout(r, 400));
        }

        console.log('[KARLACH] Chegou no portão externo das capivaras [-173, 65, 171].');
        bot.chat('Oi fofuras! Entrando pra alimentar vocês!');

        // 1. Abre portão duplo para entrar
        console.log('[KARLACH] Abrindo portão duplo em [-171, 65, 171]...');
        await toggleGate(-171, 65, 171, 'west', 'dark_oak_fence_gate', true);

        // 2. Entra no cercado (tempo suficiente para cruzar para dentro: X > -168)
        console.log('[KARLACH] Cruzando para dentro do cercado...');
        await stepThroughGate(-166, 171, 2200);

        // 3. Fecha portão atrás de si
        console.log('[KARLACH] Fechando portão...');
        await toggleGate(-171, 65, 171, 'west', 'dark_oak_fence_gate', false);
        console.log(`[STATUS] Karlach dentro do cercado: X=${bot.entity.position.x.toFixed(2)}, Z=${bot.entity.position.z.toFixed(2)}`);

        // Bug 1: Conflito do God-Object - evento de descanso/interação ativa sneak sem reset
        console.log('\n>>> [BUG 1: GOD-OBJECT COLLISION] Conflito de estado de controle!');
        console.log('>>> bot.setControlState("sneak", true) foi travado pelo loop de interação.\n');
        bot.setControlState('sneak', true);

        // 4. Alimenta capivaras (efeito visual)
        console.log('[KARLACH] Alimentando as capivaras...');
        bot.swingArm('right');
        runServerCmd('execute at Karlach as @e[type=!player,type=!item,type=!armor_stand,distance=..8,sort=nearest,limit=3] run data merge entity @s {InLove:600}');
        runServerCmd('execute at Karlach as @e[type=!player,type=!item,type=!armor_stand,distance=..8,sort=nearest,limit=3] at @s run particle minecraft:heart ~ ~0.9 ~ 0.35 0.35 0.35 0 6 force');
        runServerCmd('execute at Karlach run playsound minecraft:entity.generic.eat ambient @a ~ ~ ~ 0.85 1.15');
        bot.chat('Capivarinha linda! Come tudo, coisa fofa!');

        await new Promise(r => setTimeout(r, 2000));

        // 5. Tenta SAIR do cercado (O BUG FATAL)
        console.log('\n>>> [BUG 2: A ARMADILHA DO TIMER E DA CERCA DUPLA]');
        console.log('>>> Karlach tenta voltar para o portão e sair para o corredor...');
        
        // Caminha até próximo ao portão (lado de dentro)
        await bot.lookAt(new Vec3(-171.0, 66.2, 171.0), true);
        await new Promise(r => setTimeout(r, 600));

        console.log('[KARLACH] Abrindo o portão de saída...');
        await toggleGate(-171, 65, 171, 'west', 'dark_oak_fence_gate', true);

        console.log('[KARLACH] Dando passo cego para fora (timer = 900ms em sneak speed)...');
        // Em velocidade de sneak (1.3 m/s), 900ms não alcança a calçada externa em -173!
        await stepThroughGate(-173, 171, 900);

        // O timer venceu! O código fecha o portão cegamente:
        console.log('>>> [TIMER EXPIROU] O portão bateu e trancou antes que ela pudesse cruzar!');
        await toggleGate(-171, 65, 171, 'west', 'dark_oak_fence_gate', false);

        // A posição atual dela está 100% PRESA dentro do cercado (X > -171):
        const trappedPos = bot.entity.position;
        console.log(`\n======================================================`);
        console.log(` 🔒 [PRESA NA JAULA!] Karlach trancada dentro do cercado!`);
        console.log(` Posição: X=${trappedPos.x.toFixed(2)}, Y=${trappedPos.y.toFixed(2)}, Z=${trappedPos.z.toFixed(2)}`);
        console.log(` (O portão duplo fechado está em X=-171, calçada externa em X=-173)`);
        console.log(`======================================================\n`);

        console.log('>>> [BUG 3: FALHA TOTAL DO PATHFINDER A*]');
        console.log('>>> Tentando navegar para a próxima praça [-173, 65, 180] fora da jaula...');
        console.log('>>> movements.canOpenDoors == false (linha 101)');
        console.log('>>> Cerca dupla tratada como obstáculo infinito (linha 389)\n');

        // Ela tenta ir para o próximo waypoint fora do cercado
        bot.pathfinder.setGoal(new goals.GoalNear(-173, 65, 180, 1.0));
        bot.chat('Ei... o portão fechou na minha cara? Socorro soldado, tô presa com as capivaras!');

        trapped = true;

        // Loop contínuo: fica batendo contra a cerca do portão agachada tentando sair
        setInterval(() => {
            if (!trapped || !bot || !bot.entity) return;
            const pos = bot.entity.position;
            console.log(`[PRESA NA JAULA] Karlach em [${pos.x.toFixed(1)}, ${pos.y.toFixed(1)}, ${pos.z.toFixed(1)}] | Sneak: ${bot.getControlState('sneak')} | Batendo na cerca...`);
            bot.lookAt(new Vec3(-171.0, 66.2, 171.0), true);
            bot.swingArm('right');
            bot.setControlState('forward', true);
            bot.setControlState('sneak', true); // Mantém o agachamento bugado visível
            setTimeout(() => bot.setControlState('forward', false), 800);
        }, 2000);

    }, 3000);
}

function createBot() {
    console.log('[*] Conectando Karlach Legada via Mineflayer (Node.js)...');
    bot = mineflayer.createBot(CONFIG);
    bot.loadPlugin(pathfinder);

    bot.on('login', () => {
        console.log('[+] [MINEFLAYER] Login TCP estabelecido.');
    });

    bot.on('spawn', () => {
        console.log('[+] [MINEFLAYER] Karlach spawnou no mundo.');

        const move = new Movements(bot);
        move.canDig = false;
        move.allowParkour = false;
        move.allowSprinting = false;
        move.canOpenDoors = false; // LINHA 101 oficial do mineflayer-pathfinder
        bot.pathfinder.setMovements(move);

        setTimeout(startBugDemonstration, 2000);
    });

    bot.on('error', (err) => {
        console.error('[ERR] ProtoDef / Mineflayer erro:', err);
    });

    bot.on('end', () => {
        console.log('[-] Karlach desconectou.');
    });
}

createBot();
