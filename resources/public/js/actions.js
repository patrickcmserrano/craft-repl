/* UI Actions, Builds, Inventory & Modals */

function updateDynamicUI() {
    const myNick = getMyNick();
    const partnerNick = getPartnerNick();
    const partnerShort = getPartnerShort();
    const partnerDisplay = getPartnerDisplay();
    const myDisplay = getMyDisplay();

    // Radar focus buttons
    const centerMeBtn = document.getElementById('radarBtnCenterMe');
    if (centerMeBtn) centerMeBtn.textContent = `🎯 Focar em Mim`;
    const centerPartnerBtn = document.getElementById('radarBtnCenterPartner');
    if (centerPartnerBtn) centerPartnerBtn.textContent = `🎯 Focar em ${partnerShort}`;

    // Radar Quick Teleports
    const btnGpsHome = document.getElementById('btnGpsHome');
    if (btnGpsHome) btnGpsHome.textContent = `🏠 Minha Base (-146, 107, -85)`;
    const btnGpsPull = document.getElementById('btnGpsPull');
    if (btnGpsPull) btnGpsPull.textContent = `🧲 Puxar ${partnerShort} até Mim`;

    // Quick Controls Tab
    const btnQuickHomeMe = document.getElementById('btnQuickHomeMe');
    if (btnQuickHomeMe) btnQuickHomeMe.textContent = `🏠 Ir para a Base (${myNick})`;
    const btnQuickPull = document.getElementById('btnQuickPull');
    if (btnQuickPull) btnQuickPull.textContent = `🧲 Puxar ${partnerShort} até Mim`;
    const btnQuickGoto = document.getElementById('btnQuickGoto');
    if (btnQuickGoto) btnQuickGoto.textContent = `🏃 Ir até ${partnerShort}`;
    const btnQuickGolem = document.getElementById('btnQuickGolem');
    if (btnQuickGolem) btnQuickGolem.textContent = `🤖 Puxar Guardião até Mim (${myNick})`;

    // Survival
    const btnHealMe = document.getElementById('btnHealMe');
    if (btnHealMe) btnHealMe.textContent = `💚 Curar a Mim (${myNick})`;
    const btnVisionMe = document.getElementById('btnVisionMe');
    if (btnVisionMe) btnVisionMe.textContent = `👁️ Visão Noturna (${myNick})`;
    const btnFlyKitMe = document.getElementById('btnFlyKitMe');
    if (btnFlyKitMe) btnFlyKitMe.textContent = `🪽 Kit Voo (${myNick})`;
    const btnSniperMe = document.getElementById('btnSniperMe');
    if (btnSniperMe) btnSniperMe.textContent = `🎯 Kit Sniper (${myNick})`;
    const btnQuickInstaTnt = document.getElementById('btnQuickInstaTnt');
    if (btnQuickInstaTnt) btnQuickInstaTnt.textContent = `💥 Lançar Insta-TNT (${myNick})`;
    const btnQuickNuke = document.getElementById('btnQuickNuke');
    if (btnQuickNuke) btnQuickNuke.textContent = `☢️ DISPARAR NUKE (${myNick})`;
    const btnAirdropSniper = document.getElementById('btnAirdropSniper');
    if (btnAirdropSniper) btnAirdropSniper.textContent = `🎯 AirDrop Kit Sniper FPS para ${partnerShort}`;

    // Skins
    const btnSkinMe = document.getElementById('btnSkinMe');
    if (btnSkinMe) btnSkinMe.textContent = `⚡ Minha Skin (${myNick})`;
    const btnSkinPartner = document.getElementById('btnSkinPartner');
    if (btnSkinPartner) btnSkinPartner.textContent = `⚡ Skin de ${partnerShort}`;

    // AirDrop section header
    const airdropHeader = document.getElementById('airdropHeaderTitle');
    if (airdropHeader) airdropHeader.textContent = `🎁 AirDrop Imediato para ${partnerDisplay}`;

    // Gear & Chests
    const btnQuickRepair = document.getElementById('btnQuickRepair');
    if (btnQuickRepair) btnQuickRepair.textContent = `🛠️ Fixar Equipamentos (${myNick})`;
    const btnQuickDeposit = document.getElementById('btnQuickDeposit');
    if (btnQuickDeposit) btnQuickDeposit.textContent = `📦 Enviar Loot para Baús (${myNick})`;
    const btnVaultDeposit = document.getElementById('btnVaultDeposit');
    if (btnVaultDeposit) btnVaultDeposit.textContent = `📦 Despejar Loot nos Baús (${myNick})`;
    const btnVaultRepair = document.getElementById('btnVaultRepair');
    if (btnVaultRepair) btnVaultRepair.textContent = `🛠️ Fixar Equipamentos (${myNick})`;
}

/* --- INVENTÁRIO --- */
function selectPlayer(name) {
    currentPlayer = name;
    const btnP = document.getElementById('btnPace');
    const btnL = document.getElementById('btnLizzie');
    if (btnP) btnP.classList.toggle('active', name === 'pace');
    if (btnL) btnL.classList.toggle('active', name === 'lizzie');
    loadInventory();
}

function renderItemSlotContent(item) {
    if (!item) return '';
    const cleanId = (item.id || '').split(':').pop();
    const texUrl = item.texture || `/textures/item/${cleanId}.png`;
    const fallback = item.icon || '📦';
    const countBadge = (item.count > 1) ? `<span class="inv-slot-count">${item.count}</span>` : '';
    
    return `
        <img src="${texUrl}" alt="${item.name}" class="mc-item-icon" onerror="if(!this._triedBlock){this._triedBlock=true; this.src='/textures/block/${cleanId}.png';} else { this.style.display='none'; if(this.nextElementSibling) this.nextElementSibling.style.display='inline'; }" />
        <span class="inv-slot-icon mc-fallback" style="display:none;">${fallback}</span>
        ${countBadge}
    `;
}

async function loadInventory() {
    try {
        const res = await fetch(`/api/inventory?player=${currentPlayer}`);
        const data = await res.json();
        if (data.error) {
            showToast(data.error, true);
            return;
        }

        const titleEl = document.getElementById('invPlayerTitle');
        const targetPlayer = (data.player || currentPlayer || 'pace').toLowerCase();
        const pObj = (typeof telemetryData !== 'undefined' && telemetryData && telemetryData.players) 
            ? telemetryData.players.find(p => (p.name || '').toLowerCase() === targetPlayer) 
            : null;
        const isOnline = Boolean(pObj && pObj.online);
        const badgeHtml = `<span class="player-status-badge ${isOnline ? 'online' : 'offline'}" style="margin-left:6px;">${isOnline ? 'ONLINE' : 'OFFLINE'}</span>`;
        if (titleEl) titleEl.innerHTML = `Inventário de ${targetPlayer.toUpperCase()} ${badgeHtml}`;
        const statusEl = document.getElementById('invPlayerStatus');
        if (statusEl) statusEl.innerHTML = `❤️ ${data.health} | 🍗 ${data.food}/20 | ⭐ Lv.${data.xp} | Total: ${data.total_items} itens`;

        // Render Armor
        const armorRow = document.getElementById('armorRow');
        if (armorRow) {
            armorRow.innerHTML = '';
            const armors = [
                {key: 'helmet', label: '🪖', item: data.armor ? data.armor.helmet : null},
                {key: 'chestplate', label: '🦺', item: data.armor ? data.armor.chestplate : null},
                {key: 'leggings', label: '👖', item: data.armor ? data.armor.leggings : null},
                {key: 'boots', label: '🥾', item: data.armor ? data.armor.boots : null},
                {key: 'offhand', label: '🛡️', item: data.offhand}
            ];
            armors.forEach(a => {
                const slot = document.createElement('div');
                slot.className = 'inv-slot' + (a.item ? '' : ' empty');
                if (a.item) {
                    slot.innerHTML = renderItemSlotContent(a.item);
                    slot.title = `${a.item.name}`;
                    slot.onclick = () => openItemModal(a.item);
                } else {
                    slot.innerHTML = `<span style="font-size:0.9rem; opacity:0.3;">${a.label}</span>`;
                }
                armorRow.appendChild(slot);
            });
        }

        // Render Main Backpack (27 slots)
        const mainGrid = document.getElementById('mainGrid');
        if (mainGrid && data.main) {
            mainGrid.innerHTML = '';
            data.main.forEach(item => {
                const slot = document.createElement('div');
                slot.className = 'inv-slot' + (item ? '' : ' empty');
                if (item) {
                    slot.innerHTML = renderItemSlotContent(item);
                    slot.title = `${item.name} (${item.count})`;
                    slot.onclick = () => openItemModal(item);
                }
                mainGrid.appendChild(slot);
            });
        }

        // Render Hotbar (9 slots)
        const hotbarGrid = document.getElementById('hotbarGrid');
        if (hotbarGrid && data.hotbar) {
            hotbarGrid.innerHTML = '';
            data.hotbar.forEach(item => {
                const slot = document.createElement('div');
                slot.className = 'inv-slot' + (item ? '' : ' empty');
                if (item) {
                    slot.innerHTML = renderItemSlotContent(item);
                    slot.title = `${item.name} (${item.count})`;
                    slot.onclick = () => openItemModal(item);
                }
                hotbarGrid.appendChild(slot);
            });
        }
    } catch (err) {
        console.error('Falha ao carregar inventário', err);
    }
}

let activeModalItem = null;

function openItemModal(item) {
    activeModalItem = item;
    const modal = document.getElementById('modalItemAction');
    const title = document.getElementById('modalItemTitle');
    const info = document.getElementById('modalItemInfo');
    const btnAirdrop = document.getElementById('modalItemBtnAirdrop');

    const cleanId = (item.id || '').split(':').pop();
    const texUrl = item.texture || `/textures/item/${cleanId}.png`;
    const iconImg = `<img src="${texUrl}" alt="" class="modal-item-icon" onerror="if(!this._triedBlock){this._triedBlock=true; this.src='/textures/block/${cleanId}.png';} else { this.style.display='none'; }" />`;

    if (title) title.innerHTML = `${iconImg}<span>${item.name}</span>`;
    if (info) info.textContent = `Quantidade: ${item.count} | ID: ${item.id}${item.damage > 0 ? ` | Dano: ${item.damage}` : ''}`;
    if (btnAirdrop) btnAirdrop.textContent = `🎁 AirDrop para ${getPartnerShort()}`;
    if (info) info.textContent = `Quantidade: ${item.count} | ID: ${item.id}${item.damage > 0 ? ` | Dano: ${item.damage}` : ''}`;
    if (btnAirdrop) btnAirdrop.textContent = `🎁 AirDrop para ${getPartnerShort()}`;

    if (modal && typeof modal.showModal === 'function') {
        modal.showModal();
    } else {
        // Fallback for older browsers
        const choice = prompt(`Item: ${item.name} (${item.count})\n1 - AirDrop para ${getPartnerShort()}\n2 - Guardar no Vault\n3 - Descartar`, "1");
        if (choice === "1") executeItemAirdrop();
        else if (choice === "2") executeItemVault();
        else if (choice === "3") executeItemDiscard();
    }
}

function closeItemModal() {
    const modal = document.getElementById('modalItemAction');
    if (modal) modal.close();
}

function executeItemAirdrop() {
    if (!activeModalItem) return;
    const partner = getPartnerNick();
    runCmd(`clear ${currentPlayer} ${activeModalItem.id} ${activeModalItem.count} && give ${partner} ${activeModalItem.id} ${activeModalItem.count}`);
    showToast(`🎁 ${activeModalItem.name} enviado para ${getPartnerShort()}!`);
    closeItemModal();
    setTimeout(loadInventory, 600);
}

function executeItemVault() {
    if (!activeModalItem) return;
    runCmd(`clear ${currentPlayer} ${activeModalItem.id} ${activeModalItem.count}`);
    showToast(`🎒 ${activeModalItem.name} guardado no Vault!`);
    closeItemModal();
    setTimeout(loadInventory, 600);
}

function executeItemDiscard() {
    if (!activeModalItem) return;
    runCmd(`clear ${currentPlayer} ${activeModalItem.id} ${activeModalItem.count}`);
    showToast(`🗑️ ${activeModalItem.name} descartado!`);
    closeItemModal();
    setTimeout(loadInventory, 600);
}

async function repairGearAction() {
    const player = currentTab === 'inventory' ? currentPlayer : getMyNick();
    showToast(`🛠️ Reparando equipamentos de ${player}...`);
    try {
        const res = await fetch(`/api/gear/repair?player=${player}`, { method: 'POST' });
        const data = await res.json();
        if (data.ok) {
            showToast(data.message || 'Equipamentos restaurados para 100%!');
            if (currentTab === 'inventory') setTimeout(loadInventory, 600);
        } else {
            showToast(data.error || data.message || 'Erro ao reparar equipamentos', true);
        }
    } catch (err) {
        showToast('Falha na requisição de reparo', true);
    }
}

async function depositLootAction() {
    const player = currentTab === 'inventory' ? currentPlayer : getMyNick();
    showToast(`📦 Organizando e enviando loot de ${player}...`);
    try {
        const res = await fetch(`/api/chests/deposit?player=${player}`, { method: 'POST' });
        const data = await res.json();
        if (data.ok) {
            showToast(data.message || 'Loot guardado com sucesso!');
            if (currentTab === 'inventory') setTimeout(loadInventory, 600);
        } else {
            showToast(data.error || data.message || 'Erro ao depositar nos baús', true);
        }
    } catch (err) {
        showToast('Falha na requisição de depósito', true);
    }
}

/* --- SHORTCUT ACTIONS --- */
function teleportToHome() {
    if (!confirm("Tem certeza que deseja teleportar para a Base?")) return;
    runCmd(`tp ${getMyNick()} -146 107 -85`);
}
function teleportAllHome() {
    if (!confirm("Tem certeza que deseja teleportar TODOS para a Base?")) return;
    runCmd(`tp @a -146 107 -85`);
}
function pullPartner() { runCmd(`tp ${getPartnerNick()} ${getMyNick()}`); }
function gotoPartner() { runCmd(`tp ${getMyNick()} ${getPartnerNick()}`); }
function rescueGolem() { runCmd(`tp @e[type=iron_golem,limit=1] ${getMyNick()}`); }

function healPlayer(target) {
    const nick = target === 'all' ? '@a' : (target === 'partner' ? getPartnerNick() : getMyNick());
    runCmd(`effect give ${nick} instant_health 1 255 && effect give ${nick} saturation 1 255`);
}

function visionPlayer(target) {
    const nick = target === 'all' ? '@a' : (target === 'partner' ? getPartnerNick() : getMyNick());
    runCmd(`effect give ${nick} night_vision infinite 1 true`);
}

function giveFlightKit(target) {
    const nick = target === 'all' ? '@a' : (target === 'partner' ? getPartnerNick() : getMyNick());
    runCmd(`give ${nick} elytra 1 && give ${nick} firework_rocket[fireworks={flight:3}] 64`);
    showToast(`🪽 Kit de Voo (100% Seguro) enviado para ${nick}!`);
}

function giveSniperKit(target) {
    const nick = target === 'all' ? '@a' : (target === 'partner' ? getPartnerNick() : getMyNick());
    runCmd(`give ${nick} crossbow[custom_name='"Sniper MK-I"',enchantments={levels:{quick_charge:5,piercing:4}},unbreakable={}] 1 && give ${nick} spyglass[custom_name='"Mira Telescopica 8x (FPS Scope)"'] 1 && give ${nick} spectral_arrow 64 && give ${nick} tipped_arrow[potion_contents={potion:"minecraft:strong_harming"}] 64`);
    showToast(`🎯 Kit Sniper FPS (.50 Cal Harming + Spectral) enviado para ${nick}!`);
}

/* --- ARSENAL DE ARMAS (JEG MOD & 1.21.1) --- */
let currentWeaponTarget = 'me';

function setWeaponTarget(target) {
    currentWeaponTarget = target;
    const btnMe = document.getElementById('btnWeaponTargetMe');
    const btnPartner = document.getElementById('btnWeaponTargetPartner');
    const btnAll = document.getElementById('btnWeaponTargetAll');
    if (btnMe) btnMe.classList.toggle('btn-cyan', target === 'me');
    if (btnPartner) btnPartner.classList.toggle('btn-cyan', target === 'partner');
    if (btnAll) btnAll.classList.toggle('btn-cyan', target === 'all');
}

function giveWeaponKit(weaponType, target) {
    const tgt = target || currentWeaponTarget || 'me';
    const nick = tgt === 'all' ? '@a' : (tgt === 'partner' ? getPartnerNick() : getMyNick());
    let cmds = [];
    let title = '';

    switch (weaponType) {
        case 'rifle':
            title = 'Fuzil de Assalto (Assault Rifle)';
            cmds = [
                `give ${nick} jeg:assault_rifle 1`,
                `give ${nick} jeg:rifle_magazine 3`,
                `give ${nick} jeg:magazine_loader 1`,
                `give ${nick} jeg:rifle_ammo 128`,
                `give ${nick} jeg:reflex_sight 1`
            ];
            break;
        case 'sniper_rifle':
            title = 'Sniper Bolt-Action (Precisão)';
            cmds = [
                `give ${nick} jeg:bolt_action_rifle 1`,
                `give ${nick} jeg:silencer 1`,
                `give ${nick} jeg:rifle_ammo 64`,
                `give ${nick} spyglass 1`
            ];
            break;
        case 'shotgun':
            title = 'Escopeta Tática (Pump Shotgun)';
            cmds = [
                `give ${nick} jeg:pump_shotgun 1`,
                `give ${nick} jeg:shotgun_shell 64`,
                `give ${nick} jeg:bulletproof_vest_vi 1`
            ];
            break;
        case 'smg':
            title = 'Submetralhadora Tática (Custom SMG)';
            cmds = [
                `give ${nick} jeg:custom_smg 1`,
                `give ${nick} jeg:smg_magazine 4`,
                `give ${nick} jeg:magazine_loader 1`,
                `give ${nick} jeg:pistol_ammo 128`,
                `give ${nick} jeg:silencer 1`
            ];
            break;
        case 'minigun':
            title = 'Minigun Pesada (Heavy Gunner)';
            cmds = [
                `give ${nick} jeg:minigun 1`,
                `give ${nick} jeg:drum_mag 2`,
                `give ${nick} jeg:rifle_ammo 256`
            ];
            break;
        case 'rpg':
            title = 'Lança-Foguetes & Explosivos (RPG)';
            cmds = [
                `give ${nick} jeg:rocket_launcher 1`,
                `give ${nick} jeg:rocket 16`,
                `give ${nick} jeg:grenade 8`
            ];
            break;
        case 'flamethrower':
            title = 'Lança-Chamas & Incendiários';
            cmds = [
                `give ${nick} jeg:flamethrower 1`,
                `give ${nick} jeg:molotov_cocktail 6`
            ];
            break;
        case 'armor_vest':
            title = 'Armadura Balística Militar (Tier VI)';
            cmds = [
                `give ${nick} jeg:bulletproof_helmet_vi 1`,
                `give ${nick} jeg:bulletproof_vest_vi 1`
            ];
            break;
        case 'ammo_pack':
            title = 'Caixa Geral de Munições';
            cmds = [
                `give ${nick} jeg:rifle_ammo 128`,
                `give ${nick} jeg:pistol_ammo 128`,
                `give ${nick} jeg:shotgun_shell 64`,
                `give ${nick} jeg:rocket 8`
            ];
            break;
        case 'mace_121':
            title = 'Martelo Titânico (Mace 1.21 OP)';
            cmds = [
                `give ${nick} mace[custom_name='"Martelo Titânico"',enchantments={levels:{density:5,breach:4,wind_burst:3,unbreaking:3}},unbreakable={}] 1`
            ];
            break;
        case 'god_sword':
            title = 'Lâmina Netherite Lendária';
            cmds = [
                `give ${nick} netherite_sword[custom_name='"Lâmina do Destino"',enchantments={levels:{sharpness:5,looting:3,fire_aspect:2,sweeping_edge:3,unbreaking:3}},unbreakable={}] 1`
            ];
            break;
        case 'god_bow':
            title = 'Arco dos Deuses (Infinity)';
            cmds = [
                `give ${nick} bow[custom_name='"Arco dos Deuses"',enchantments={levels:{power:5,flame:1,punch:2,infinity:1,unbreaking:3}},unbreakable={}] 1`,
                `give ${nick} arrow 1`
            ];
            break;
        case 'trident':
            title = 'Tridente de Poseidon (Channeling)';
            cmds = [
                `give ${nick} trident[custom_name='"Tridente de Poseidon"',enchantments={levels:{loyalty:3,channeling:1,impaling:5,unbreaking:3}},unbreakable={}] 1`
            ];
            break;
        case 'full_arsenal':
            title = 'Arsenal Militar Completo';
            cmds = [
                `give ${nick} jeg:assault_rifle 1`,
                `give ${nick} jeg:bolt_action_rifle 1`,
                `give ${nick} jeg:pump_shotgun 1`,
                `give ${nick} jeg:rocket_launcher 1`,
                `give ${nick} jeg:bulletproof_helmet_vi 1`,
                `give ${nick} jeg:bulletproof_vest_vi 1`,
                `give ${nick} jeg:rifle_magazine 3`,
                `give ${nick} jeg:magazine_loader 1`,
                `give ${nick} jeg:rifle_ammo 128`,
                `give ${nick} jeg:shotgun_shell 64`,
                `give ${nick} jeg:rocket 8`,
                `give ${nick} jeg:grenade 4`,
                `give ${nick} mace[custom_name='"Martelo Titânico"',enchantments={levels:{density:5,breach:4,wind_burst:3,unbreaking:3}},unbreakable={}] 1`
            ];
            break;
        default:
            return;
    }

    runCmd(cmds.join(' && '));
    showToast(`⚔️ ${title} enviado para ${nick}!`);
}

function triggerInstaTnt() {
    const nick = getMyNick();
    if (telemetryData && telemetryData.players) {
        const me = telemetryData.players.find(p => p.name.toLowerCase() === myProfile);
        if (me) {
            const distToBase = Math.hypot(me.x - (-146), me.z - (-85));
            if (distToBase < 20) {
                if (!confirm("⚠️ Atenção: Você está a menos de 20m da sua Base! Deseja mesmo detonar uma Insta-TNT aqui?")) return;
            }
        }
    }
    runCmd(`effect give ${nick} resistance 5 5 true && execute at ${nick} run summon tnt ^ ^1.6 ^11 {Fuse:45} && playsound minecraft:entity.firework_rocket.launch player ${nick}`);
    showToast(`💥 Insta-TNT disparada 11m à frente (com escudo anti-dano)!`);
}

async function triggerNukeStrike() {
    const nick = getMyNick();
    if (telemetryData && telemetryData.players) {
        const me = telemetryData.players.find(p => p.name.toLowerCase() === myProfile);
        if (me) {
            const distToBase = Math.hypot(me.x - (-146), me.z - (-85));
            if (distToBase < 40) {
                alert(`☢️ PERIGO CRÍTICO:\n\nVocê está a apenas ${Math.round(distToBase)}m da Base Principal (-146, 107, -85)!\n\nAfaste-se para pelo menos 40m para não vaporizar sua casa.`);
                return;
            }
        }
    }
    if (!confirm(`☢️ PROTOCOLO DE LANÇAMENTO NUCLEAR ☢️\n\nAutorizar míssil nuclear tático a 25m na direção da mira de ${nick}?\n\nA área será completamente vaporizada com cratera colossal!`)) return;
    showToast(`☢️ Míssil nuclear autorizado por ${nick}! Impacto em 3s...`);
    try {
        const res = await fetch(`/api/nuke/launch?player=${nick}`, { method: 'POST' });
        const data = await res.json();
        if (data.ok) {
            showToast(data.message || 'Míssil em aproximação!');
        } else {
            showToast(data.error || data.message || 'Falha no disparo do míssil', true);
        }
    } catch (err) {
        showToast('Falha na requisição de lançamento', true);
    }
}

async function purgeUndergroundMonstersAction() {
    const nick = getMyNick();
    showToast(`🧹 Iniciando varredura subterrânea sob ${nick}...`);
    try {
        const res = await fetch(`/api/monsters/purge-underground?player=${nick}`, { method: 'POST' });
        const data = await res.json();
        if (data.ok) {
            showToast(data.message || 'Cavernas subterrâneas limpas com sucesso!');
        } else {
            showToast(data.error || data.message || 'Erro ao realizar expurgo', true);
        }
    } catch (err) {
        showToast('Falha na comunicação com o servidor', true);
    }
}

function airdropItem(item, count) {
    const partner = getPartnerNick();
    runCmd(`give ${partner} ${item} ${count}`);
    showToast(`🎁 Enviado ${count}x ${item} para ${partner}!`);
}

function applySkin(target) {
    if (target === 'partner') {
        const pKey = myProfile === 'pace' ? 'lizzie' : 'pace';
        const pNick = getPartnerNick();
        runCmd(`skin-restore ${pNick} ${pKey}`);
    } else {
        runCmd(`skin-restore ${getMyNick()} ${myProfile}`);
    }
}

/* --- BLUEPRINTS 3D --- */
async function backupBlueprintAction() {
    showToast("🏛️ Capturando blueprint 3D da Casa Principal...");
    try {
        const res = await fetch('/api/blueprint/backup', { method: 'POST' });
        const data = await res.json();
        if (data.ok) {
            showToast(data.message || 'Blueprint 3D salvo com sucesso!');
        } else {
            showToast(data.error || data.message || 'Erro ao salvar blueprint', true);
        }
    } catch (err) {
        showToast('Falha na requisição de blueprint', true);
    }
}

async function restoreBlueprintAction() {
    if (!confirm("⚠️ ATENÇÃO: Deseja realmente restaurar a Casa Principal para o estado do último Blueprint salvo?")) return;
    showToast("🔄 Restaurando Casa Principal...");
    try {
        const res = await fetch('/api/blueprint/restore', { method: 'POST' });
        const data = await res.json();
        if (data.ok) {
            showToast(data.message || 'Casa Principal restaurada com sucesso!');
        } else {
            showToast(data.error || data.message || 'Erro ao restaurar', true);
        }
    } catch (err) {
        showToast('Falha na requisição de restauração', true);
    }
}

async function pasteStructureAction() {
    const nick = getMyNick();
    if (!confirm(`📋 Deseja colar uma cópia da Casa Principal na sua posição atual (${nick})?`)) return;
    showToast(`📋 Colando cópia da Casa Principal em ${nick}...`);
    try {
        const res = await fetch(`/api/blueprint/paste?player=${nick}`, { method: 'POST' });
        const data = await res.json();
        if (data.ok) {
            showToast(data.message || 'Casa colada com sucesso!');
            const btnUndo = document.getElementById('btnUndoPaste');
            if (btnUndo) btnUndo.style.display = 'block';
            setTimeout(fetchTelemetry, 600);
        } else {
            showToast(data.error || data.message || 'Erro ao colar casa', true);
        }
    } catch (err) {
        showToast('Falha na comunicação com o servidor', true);
    }
}

async function undoPasteAction() {
    if (!confirm("↩️ Deseja realmente remover a cópia colada da casa e restaurar o terreno?")) return;
    showToast("↩️ Desfazendo colagem da casa...");
    try {
        const res = await fetch('/api/blueprint/undo-paste', { method: 'POST' });
        const data = await res.json();
        if (data.ok) {
            showToast(data.message || 'Colagem desfeita com sucesso!');
            const btnUndo = document.getElementById('btnUndoPaste');
            if (btnUndo) btnUndo.style.display = 'none';
            setTimeout(fetchTelemetry, 600);
        } else {
            showToast(data.error || data.message || 'Erro ao desfazer colagem', true);
        }
    } catch (err) {
        showToast('Falha na comunicação com o servidor', true);
    }
}

/* --- CONSTRUTOR VISUAL DE CASAS --- */
let selectedHouseWood = 'cherry';
let selectedHouseDir = 'auto';

function promptCustomHouse() {
    const modal = document.getElementById('modalCustomHouse');
    if (modal && typeof modal.showModal === 'function') {
        modal.showModal();
    } else {
        // Fallback for older browsers
        const wood = prompt("Tipo de Madeira (cherry, spruce, oak, birch, bamboo):", "cherry") || "cherry";
        const w = prompt("Largura da fachada [ex: 9]:", "9") || "9";
        const l = prompt("Comprimento [ex: 11]:", "11") || "11";
        const h = prompt("Altura [ex: 4]:", "4") || "4";
        const dir = prompt("Orientação (auto, north, south, east, west):", "auto") || "auto";
        buildPresetHouse(wood, parseInt(w), parseInt(l), parseInt(h), dir);
    }
}

function selectHouseWood(wood, btn) {
    selectedHouseWood = wood;
    document.querySelectorAll('.wood-choice-btn').forEach(b => b.classList.remove('btn-cyan'));
    if (btn) btn.classList.add('btn-cyan');
}

function selectHouseDir(dir, btn) {
    selectedHouseDir = dir;
    document.querySelectorAll('.dir-choice-btn').forEach(b => b.classList.remove('btn-cyan'));
    if (btn) btn.classList.add('btn-cyan');
}

function submitCustomHouseModal() {
    const w = parseInt(document.getElementById('inputHouseWidth').value) || 9;
    const l = parseInt(document.getElementById('inputHouseLength').value) || 11;
    const h = parseInt(document.getElementById('inputHouseHeight').value) || 4;
    const modal = document.getElementById('modalCustomHouse');
    if (modal) modal.close();
    buildPresetHouse(selectedHouseWood, w, l, h, selectedHouseDir);
}

function closeCustomHouseModal() {
    const modal = document.getElementById('modalCustomHouse');
    if (modal) modal.close();
}

async function buildPresetHouse(wood, w, l, h, dir="auto") {
    const nick = getMyNick();
    showToast(`🏠 Construindo casa de ${wood} (${w}x${l}x${h}) voltada para ${dir}...`);
    try {
        let url = `/api/build/custom-house?player=${nick}&wood=${wood}&w=${w}&l=${l}&h=${h}`;
        if (dir && dir !== "auto") url += `&dir=${encodeURIComponent(dir)}`;
        const res = await fetch(url, { method: 'POST' });
        const data = await res.json();
        if (data.ok) {
            showToast(data.message || 'Casa construída com sucesso!');
            const undoBtn = document.getElementById('btnUndoHouse');
            if (undoBtn) undoBtn.style.display = 'block';
            setTimeout(fetchTelemetry, 600);
        } else {
            showToast(data.error || data.message || 'Erro ao construir casa', true);
        }
    } catch (err) {
        showToast('Falha na requisição de construção', true);
    }
}

async function undoCustomHouseAction() {
    if (!confirm("↩️ Deseja realmente remover a última casa e restaurar o terreno?")) return;
    showToast("↩️ Desfazendo casa...");
    try {
        const res = await fetch('/api/build/undo-custom-house', { method: 'POST' });
        const data = await res.json();
        if (data.ok) {
            showToast(data.message || 'Casa desfeita!');
            const undoBtn = document.getElementById('btnUndoHouse');
            if (undoBtn) undoBtn.style.display = 'none';
            setTimeout(fetchTelemetry, 600);
        } else {
            showToast(data.error || data.message || 'Erro ao desfazer', true);
        }
    } catch (err) {
        showToast('Falha na requisição de desfazer', true);
    }
}

/* --- MONUMENTOS COM UNDO --- */
async function buildThermalPoolAction() {
    const nick = getMyNick();
    if (!confirm(`♨️ Erigir uma Piscina Termal Curativa nas coordenadas atuais de ${nick}?`)) return;
    showToast(`♨️ Criando Piscina Termal em ${nick}...`);
    try {
        const res = await fetch(`/api/build/thermal-pool?player=${nick}`, { method: 'POST' });
        const data = await res.json();
        if (data.ok) {
            showToast(data.message || 'Piscina termal erguida!');
            const undoBtn = document.getElementById('btnUndoPool');
            if (undoBtn) undoBtn.style.display = 'block';
            setTimeout(fetchTelemetry, 600);
        } else {
            showToast(data.error || data.message || 'Erro ao erguer piscina', true);
        }
    } catch (err) {
        showToast('Falha na requisição da piscina termal', true);
    }
}

async function undoThermalPoolAction() {
    if (!confirm("↩️ Deseja realmente remover a última piscina termal e restaurar o terreno?")) return;
    showToast("↩️ Desfazendo piscina termal...");
    try {
        const res = await fetch('/api/build/undo-thermal-pool', { method: 'POST' });
        const data = await res.json();
        if (data.ok) {
            showToast(data.message || 'Piscina desfeita!');
            const undoBtn = document.getElementById('btnUndoPool');
            if (undoBtn) undoBtn.style.display = 'none';
            setTimeout(fetchTelemetry, 600);
        } else {
            showToast(data.error || data.message || 'Erro ao desfazer', true);
        }
    } catch (err) {
        showToast('Falha na requisição de desfazer', true);
    }
}

async function buildEndermanTotemAction() {
    const nick = getMyNick();
    if (!confirm(`🔮 Erigir um Obelisco Místico Anti-Enderman nas coordenadas atuais de ${nick}?`)) return;
    showToast(`🔮 Erguendo Obelisco Anti-Enderman em ${nick}...`);
    try {
        const res = await fetch(`/api/build/enderman-totem?player=${nick}`, { method: 'POST' });
        const data = await res.json();
        if (data.ok) {
            showToast(data.message || 'Obelisco anti-enderman erguido!');
            const undoBtn = document.getElementById('btnUndoTotem');
            if (undoBtn) undoBtn.style.display = 'block';
            setTimeout(fetchTelemetry, 600);
        } else {
            showToast(data.error || data.message || 'Erro ao erguer obelisco', true);
        }
    } catch (err) {
        showToast('Falha na requisição do obelisco', true);
    }
}

async function undoEndermanTotemAction() {
    if (!confirm("↩️ Deseja realmente remover o último obelisco anti-enderman e restaurar o terreno?")) return;
    showToast("↩️ Desfazendo obelisco...");
    try {
        const res = await fetch('/api/build/undo-enderman-totem', { method: 'POST' });
        const data = await res.json();
        if (data.ok) {
            showToast(data.message || 'Obelisco desfeito!');
            const undoBtn = document.getElementById('btnUndoTotem');
            if (undoBtn) undoBtn.style.display = 'none';
            setTimeout(fetchTelemetry, 600);
        } else {
            showToast(data.error || data.message || 'Erro ao desfazer', true);
        }
    } catch (err) {
        showToast('Falha na requisição de desfazer', true);
    }
}

/* --- PREENCHIMENTO DE LAVA & DRENAGEM --- */
async function fillLavaAction() {
    const nick = getMyNick();
    if (!confirm(`🌋 Deseja encher o fosso/cavidade com LAVA ao redor de ${nick}?\n(Raio: 4 blocos | Profundidade: 4 blocos abaixo dos seus pés. Terreno e construções sólidas são preservados!)`)) return;
    showToast(`🌋 Preenchendo área com lava em ${nick}...`);
    try {
        const res = await fetch(`/api/build/lava?player=${nick}&radius=4&depth=4`, { method: 'POST' });
        const data = await res.json();
        if (data.ok) {
            showToast(data.message || 'Cavidade preenchida com lava!');
            const undoBtns = document.querySelectorAll('.btn-undo-lava');
            undoBtns.forEach(b => b.style.display = 'block');
            setTimeout(fetchTelemetry, 600);
        } else {
            showToast(data.error || data.message || 'Erro ao preencher com lava', true);
        }
    } catch (err) {
        showToast('Falha na requisição de lava', true);
    }
}

async function undoLavaAction() {
    if (!confirm("↩️ Deseja realmente DRENAR e remover a última lava colocada?")) return;
    showToast("↩️ Drenando lava...");
    try {
        const res = await fetch('/api/build/undo-lava', { method: 'POST' });
        const data = await res.json();
        if (data.ok) {
            showToast(data.message || 'Lava drenada com sucesso!');
            const undoBtns = document.querySelectorAll('.btn-undo-lava');
            undoBtns.forEach(b => b.style.display = 'none');
            setTimeout(fetchTelemetry, 600);
        } else {
            showToast(data.error || data.message || 'Erro ao drenar lava', true);
        }
    } catch (err) {
        showToast('Falha na requisição de drenagem', true);
    }
}

/* --- MAR DE LAVA PROFUNDO (MEGABURACO) - ZERO RESTART --- */
function fillLavaSeaAction() {
    const nick = getMyNick();
    if (!confirm(`🌋 Inundar o buracão com um MAR DE LAVA ao redor de ${nick}?\n\n- Aplica 10 min de Resistência ao Fogo a todos\n- Inunda cavidades profundas até a camada Y=-30 (raio 15m)\n- Preenche até 1 bloco abaixo dos seus pés\n- Terrenos e construções sólidas são preservados!`)) return;
    showToast(`🌋 Inundando com Mar de Lava em ${nick}...`);

    const cmds = [
        // 1. Proteção de fogo para todos os jogadores (10 minutos)
        `effect give @a minecraft:fire_resistance 600 1 true`,
        // 2. Fatia profunda (-30 até 0)
        `execute at ${nick} run fill ~-15 -30 ~-15 ~15 0 ~15 lava replace air`,
        `execute at ${nick} run fill ~-15 -30 ~-15 ~15 0 ~15 lava replace water`,
        // 3. Fatia intermediária (1 até 30)
        `execute at ${nick} run fill ~-15 1 ~-15 ~15 30 ~15 lava replace air`,
        `execute at ${nick} run fill ~-15 1 ~-15 ~15 30 ~15 lava replace water`,
        // 4. Fatia superior (31 até 1 bloco abaixo dos pés)
        `execute at ${nick} run fill ~-15 31 ~-15 ~15 ~-1 ~15 lava replace air`,
        `execute at ${nick} run fill ~-15 31 ~-15 ~15 ~-1 ~15 lava replace water`,
        // 5. Efeito sonoro
        `execute at ${nick} run playsound minecraft:item.bucket.empty_lava master @a ~ ~ ~ 2 0.8`
    ];

    cmds.forEach(c => runCmd(c));
    const undoBtns = document.querySelectorAll('.btn-undo-lava-sea');
    undoBtns.forEach(b => b.style.display = 'block');
}

function undoLavaSeaAction() {
    const nick = getMyNick();
    if (!confirm(`↩️ Deseja realmente DRENAR todo o mar de lava na região profunda ao redor de ${nick}?`)) return;
    showToast("↩️ Drenando mar de lava...");

    const cmds = [
        `execute at ${nick} run fill ~-15 -30 ~-15 ~15 ~-1 ~15 air replace lava`,
        `execute at ${nick} run fill ~-15 -30 ~-15 ~15 ~-1 ~15 air replace flowing_lava`,
        `execute at ${nick} run playsound minecraft:item.bucket.fill_lava master @a ~ ~ ~ 2 1`
    ];

    cmds.forEach(c => runCmd(c));
    const undoBtns = document.querySelectorAll('.btn-undo-lava-sea');
    undoBtns.forEach(b => b.style.display = 'none');
}

/* --- CASTELO MEDIEVAL DE PEDRA (ZERO RESTART VIA API /api/run) --- */
function buildCastleAction() {
    const nick = getMyNick();
    if (!confirm(`🏰 Erguer um Castelo de Pedra Medieval (15x15) ao seu redor (${nick})?\n(Sem necessidade de reiniciar o servidor!)`)) return;
    showToast(`🏰 Erguendo Castelo de Pedra em ${nick}...`);

    const cmds = [
        // 1. Limpeza do envelope 15x15x12
        `execute at ${nick} run fill ~-7 ~ ~-7 ~7 ~12 ~7 air replace`,
        // 2. Fundação e chão do pátio
        `execute at ${nick} run fill ~-8 ~-1 ~-8 ~8 ~-1 ~8 cobblestone replace`,
        `execute at ${nick} run fill ~-7 ~-1 ~-7 ~7 ~-1 ~7 stone_bricks replace`,
        `execute at ${nick} run fill ~-6 ~-1 ~-6 ~6 ~-1 ~6 polished_andesite replace`,
        // 3. Muralhas externas e passadiço de ronda
        `execute at ${nick} run fill ~-7 ~ ~-7 ~7 ~5 ~7 stone_bricks replace`,
        `execute at ${nick} run fill ~-6 ~ ~-6 ~6 ~5 ~6 air replace`,
        `execute at ${nick} run fill ~-6 ~4 ~-6 ~6 ~4 ~6 stone_brick_slab[type=bottom] replace`,
        `execute at ${nick} run fill ~-5 ~4 ~-5 ~5 ~4 ~5 air replace`,
        // 4. Ameias defensivas nas muralhas
        `execute at ${nick} run fill ~-7 ~6 ~-7 ~7 ~6 ~-7 stone_brick_wall replace`,
        `execute at ${nick} run fill ~-7 ~6 ~7 ~7 ~6 ~7 stone_brick_wall replace`,
        `execute at ${nick} run fill ~-7 ~6 ~-7 ~-7 ~6 ~7 stone_brick_wall replace`,
        `execute at ${nick} run fill ~7 ~6 ~-7 ~7 ~6 ~7 stone_brick_wall replace`,
        // 5. Quatro Torres de Canto (4x4, 8m com parapeito e lanternas)
        `execute at ${nick} run fill ~-7 ~ ~-7 ~-4 ~8 ~-4 stone_bricks replace`,
        `execute at ${nick} run fill ~-6 ~ ~-6 ~-5 ~7 ~-5 air replace`,
        `execute at ${nick} run fill ~-7 ~9 ~-7 ~-4 ~9 ~-4 stone_brick_wall replace`,
        `execute at ${nick} run setblock ~-7 ~10 ~-7 lantern[hanging=false] replace`,
        `execute at ${nick} run setblock ~-4 ~10 ~-7 lantern[hanging=false] replace`,
        `execute at ${nick} run setblock ~-7 ~10 ~-4 lantern[hanging=false] replace`,
        `execute at ${nick} run setblock ~-4 ~10 ~-4 lantern[hanging=false] replace`,

        `execute at ${nick} run fill ~4 ~ ~-7 ~7 ~8 ~-4 stone_bricks replace`,
        `execute at ${nick} run fill ~5 ~ ~-6 ~6 ~7 ~-5 air replace`,
        `execute at ${nick} run fill ~4 ~9 ~-7 ~7 ~9 ~-4 stone_brick_wall replace`,
        `execute at ${nick} run setblock ~4 ~10 ~-7 lantern[hanging=false] replace`,
        `execute at ${nick} run setblock ~7 ~10 ~-7 lantern[hanging=false] replace`,
        `execute at ${nick} run setblock ~4 ~10 ~-4 lantern[hanging=false] replace`,
        `execute at ${nick} run setblock ~7 ~10 ~-4 lantern[hanging=false] replace`,

        `execute at ${nick} run fill ~-7 ~ ~4 ~-4 ~8 ~7 stone_bricks replace`,
        `execute at ${nick} run fill ~-6 ~ ~5 ~-5 ~7 ~6 air replace`,
        `execute at ${nick} run fill ~-7 ~9 ~4 ~-4 ~9 ~7 stone_brick_wall replace`,
        `execute at ${nick} run setblock ~-7 ~10 ~4 lantern[hanging=false] replace`,
        `execute at ${nick} run setblock ~-4 ~10 ~4 lantern[hanging=false] replace`,
        `execute at ${nick} run setblock ~-7 ~10 ~7 lantern[hanging=false] replace`,
        `execute at ${nick} run setblock ~-4 ~10 ~7 lantern[hanging=false] replace`,

        `execute at ${nick} run fill ~4 ~ ~4 ~7 ~8 ~7 stone_bricks replace`,
        `execute at ${nick} run fill ~5 ~ ~5 ~6 ~7 ~6 air replace`,
        `execute at ${nick} run fill ~4 ~9 ~4 ~7 ~9 ~7 stone_brick_wall replace`,
        `execute at ${nick} run setblock ~4 ~10 ~4 lantern[hanging=false] replace`,
        `execute at ${nick} run setblock ~7 ~10 ~4 lantern[hanging=false] replace`,
        `execute at ${nick} run setblock ~4 ~10 ~7 lantern[hanging=false] replace`,
        `execute at ${nick} run setblock ~7 ~10 ~7 lantern[hanging=false] replace`,

        // Escada interna na torre noroeste
        `execute at ${nick} run fill ~-5 ~ ~-5 ~-5 ~8 ~-5 ladder[facing=west] replace`,
        `execute at ${nick} run fill ~-4 ~ ~-6 ~-4 ~2 ~-6 air replace`,

        // Portão frontal com grade de ferro
        `execute at ${nick} run fill ~-1 ~ ~7 ~1 ~3 ~7 air replace`,
        `execute at ${nick} run fill ~-1 ~3 ~7 ~1 ~3 ~7 iron_bars replace`,

        // Salão nobre: trono, tapete vermelho e oficinas de trabalho
        `execute at ${nick} run setblock ~ ~ ~-3 chiseled_stone_bricks replace`,
        `execute at ${nick} run setblock ~ ~1 ~-3 stone_brick_stairs[facing=south,half=bottom] replace`,
        `execute at ${nick} run setblock ~ ~2 ~-3 red_banner replace`,
        `execute at ${nick} run setblock ~-1 ~ ~-3 stone_brick_stairs[facing=east,half=bottom] replace`,
        `execute at ${nick} run setblock ~1 ~ ~-3 stone_brick_stairs[facing=west,half=bottom] replace`,
        `execute at ${nick} run fill ~ ~ ~-2 ~ ~ ~4 red_carpet replace`,
        `execute at ${nick} run setblock ~-3 ~ ~-1 anvil replace`,
        `execute at ${nick} run setblock ~-3 ~ ~ crafting_table replace`,
        `execute at ${nick} run setblock ~-3 ~ ~1 blast_furnace replace`,
        `execute at ${nick} run setblock ~3 ~ ~-1 stonecutter replace`,
        `execute at ${nick} run setblock ~3 ~ ~ chest replace`,
        `execute at ${nick} run setblock ~3 ~ ~1 barrel replace`,
        `execute at ${nick} run setblock ~-2 ~2 ~-2 lantern[hanging=false] replace`,
        `execute at ${nick} run setblock ~2 ~2 ~-2 lantern[hanging=false] replace`,
        `execute at ${nick} run setblock ~-2 ~2 ~2 lantern[hanging=false] replace`,
        `execute at ${nick} run setblock ~2 ~2 ~2 lantern[hanging=false] replace`,

        // Efeito sonoro de conquista
        `execute at ${nick} run playsound minecraft:ui.toast.challenge_complete master @a ~ ~ ~ 1 1`
    ];

    cmds.forEach(c => runCmd(c));
    const undoBtns = document.querySelectorAll('.btn-undo-castle');
    undoBtns.forEach(b => b.style.display = 'block');
}

function undoCastleAction() {
    const nick = getMyNick();
    if (!confirm(`↩️ Deseja realmente remover o castelo ao redor de ${nick} e restaurar o terreno com grama?`)) return;
    showToast("↩️ Desfazendo castelo...");
    const cmds = [
        `execute at ${nick} run fill ~-8 ~-1 ~-8 ~8 ~12 ~8 air replace`,
        `execute at ${nick} run fill ~-8 ~-1 ~-8 ~8 ~-1 ~8 grass_block replace`
    ];
    cmds.forEach(c => runCmd(c));
    const undoBtns = document.querySelectorAll('.btn-undo-castle');
    undoBtns.forEach(b => b.style.display = 'none');
}

/* --- CASTELO TITÂNICO DO VAZIO (A CIDADELA QWEN) --- */
async function buildTitanicCastleAction() {
    const nick = getMyNick();
    if (!confirm(`🌌 Erguer o Castelo Titânico do Vazio (41x41) nas coordenadas de ${nick}?\n\nUma cidadela colossal 100% oca por dentro: muralha DUPLA com passadiço, fosso com água e ponte levadiça, 4 torres gigantes de canto com escadas de mão e mirantes com beacons, um Torreão Central (Donjon) de 22 blocos com Salão do Trono, 2º andar e pináculo místico, além de Capela do Vazio com vitrais e Jardins Reais!\n\n⚠️ Ocupa 41x41 blocos e até 30 blocos de altura.`)) return;
    showToast(`🌌 Erguendo o Castelo Titânico do Vazio em ${nick}...`);

    const cmds = [
        // 0. Garante limite de blocos e segurança do jogador
        `gamerule commandModificationBlockLimit 1000000`,
        `effect give ${nick} minecraft:resistance 30 255 true`,
        `effect give ${nick} minecraft:fire_resistance 30 255 true`,

        // 1. Limpeza do espaço aéreo (49x49x35) e nivelamento do terreno
        `execute at ${nick} run fill ~-24 ~ ~-24 ~24 ~35 ~24 air replace`,
        `execute at ${nick} run fill ~-26 ~-1 ~-26 ~26 ~-1 ~26 grass_block replace`,
        `execute at ${nick} run fill ~-22 ~-1 ~-22 ~22 ~-1 ~22 polished_andesite replace`,

        // 2. Fosso profundo de água em volta do perímetro
        `execute at ${nick} run fill ~-23 ~-3 ~-23 ~23 ~-1 ~23 water replace`,
        `execute at ${nick} run fill ~-20 ~-3 ~-20 ~20 ~-1 ~20 air replace`,
        `execute at ${nick} run fill ~-20 ~-1 ~-20 ~20 ~-1 ~20 stone_bricks replace`,

        // 3. Muralha Exterior (5 blocos de altura, 4 paredes independentes - pátio nunca é fechado)
        `execute at ${nick} run fill ~-20 ~ ~-20 ~20 ~5 ~-20 stone_bricks replace`,
        `execute at ${nick} run fill ~-20 ~ ~20 ~20 ~5 ~20 stone_bricks replace`,
        `execute at ${nick} run fill ~-20 ~ ~-20 ~-20 ~5 ~20 stone_bricks replace`,
        `execute at ${nick} run fill ~20 ~ ~-20 ~20 ~5 ~20 stone_bricks replace`,

        // Ameias da muralha exterior
        `execute at ${nick} run fill ~-20 ~6 ~-20 ~20 ~6 ~-20 stone_brick_wall replace`,
        `execute at ${nick} run fill ~-20 ~6 ~20 ~20 ~6 ~20 stone_brick_wall replace`,
        `execute at ${nick} run fill ~-20 ~6 ~-20 ~-20 ~6 ~20 stone_brick_wall replace`,
        `execute at ${nick} run fill ~20 ~6 ~-20 ~20 ~6 ~20 stone_brick_wall replace`,

        // 4. Muralha Interior (7 blocos de altura, deepslate bricks)
        `execute at ${nick} run fill ~-16 ~ ~-16 ~16 ~7 ~-16 deepslate_bricks replace`,
        `execute at ${nick} run fill ~-16 ~ ~16 ~16 ~7 ~16 deepslate_bricks replace`,
        `execute at ${nick} run fill ~-16 ~ ~-16 ~-16 ~7 ~16 deepslate_bricks replace`,
        `execute at ${nick} run fill ~16 ~ ~-16 ~16 ~7 ~16 deepslate_bricks replace`,

        // Ameias da muralha interior
        `execute at ${nick} run fill ~-16 ~8 ~-16 ~16 ~8 ~-16 deepslate_tile_wall replace`,
        `execute at ${nick} run fill ~-16 ~8 ~16 ~16 ~8 ~16 deepslate_tile_wall replace`,
        `execute at ${nick} run fill ~-16 ~8 ~-16 ~-16 ~8 ~16 deepslate_tile_wall replace`,
        `execute at ${nick} run fill ~16 ~8 ~-16 ~16 ~8 ~16 deepslate_tile_wall replace`,

        // 5. Passadiço / corredor militar entre as muralhas (a ~4)
        `execute at ${nick} run fill ~-19 ~4 ~-20 ~19 ~4 ~-17 stone_brick_slab[type=top] replace`,
        `execute at ${nick} run fill ~-19 ~4 ~17 ~19 ~4 ~20 stone_brick_slab[type=top] replace`,
        `execute at ${nick} run fill ~-20 ~4 ~-19 ~-17 ~4 ~19 stone_brick_slab[type=top] replace`,
        `execute at ${nick} run fill ~17 ~4 ~-19 ~20 ~4 ~19 stone_brick_slab[type=top] replace`,

        // 6. As Quatro Torres Titânicas de Canto (7x7, 18 blocos de altura, 100% HOLLOW com escadas de mão)
        // Torre NW (~-20 a ~-14, ~-20 a ~-14)
        `execute at ${nick} run fill ~-20 ~ ~-20 ~-14 ~18 ~-14 stone_bricks hollow`,
        `execute at ${nick} run fill ~-14 ~ ~-17 ~-14 ~2 ~-17 air replace`,
        `execute at ${nick} run fill ~-19 ~15 ~-19 ~-15 ~15 ~-15 stone_brick_slab[type=top] replace`,
        `execute at ${nick} run setblock ~-18 ~15 ~-18 air replace`,
        `execute at ${nick} run fill ~-18 ~ ~-18 ~-18 ~15 ~-18 ladder[facing=south] replace`,
        `execute at ${nick} run fill ~-20 ~19 ~-20 ~-14 ~19 ~-14 stone_brick_wall outline`,
        `execute at ${nick} run setblock ~-17 ~20 ~-17 crying_obsidian replace`,
        `execute at ${nick} run setblock ~-17 ~21 ~-17 gold_block replace`,
        `execute at ${nick} run setblock ~-17 ~22 ~-17 beacon replace`,
        `execute at ${nick} run setblock ~-20 ~3 ~-17 iron_bars replace`,
        `execute at ${nick} run setblock ~-17 ~3 ~-20 iron_bars replace`,

        // Torre SE (~14 a ~20, ~14 a ~20)
        `execute at ${nick} run fill ~14 ~ ~14 ~20 ~18 ~20 stone_bricks hollow`,
        `execute at ${nick} run fill ~14 ~ ~17 ~14 ~2 ~17 air replace`,
        `execute at ${nick} run fill ~15 ~15 ~15 ~19 ~15 ~19 stone_brick_slab[type=top] replace`,
        `execute at ${nick} run setblock ~18 ~15 ~18 air replace`,
        `execute at ${nick} run fill ~18 ~ ~18 ~18 ~15 ~18 ladder[facing=north] replace`,
        `execute at ${nick} run fill ~14 ~19 ~14 ~20 ~19 ~20 stone_brick_wall outline`,
        `execute at ${nick} run setblock ~17 ~20 ~17 crying_obsidian replace`,
        `execute at ${nick} run setblock ~17 ~21 ~17 gold_block replace`,
        `execute at ${nick} run setblock ~17 ~22 ~17 beacon replace`,
        `execute at ${nick} run setblock ~20 ~3 ~17 iron_bars replace`,
        `execute at ${nick} run setblock ~17 ~3 ~20 iron_bars replace`,

        // Torre NE (~14 a ~20, ~-20 a ~-14)
        `execute at ${nick} run fill ~14 ~ ~-20 ~20 ~18 ~-14 stone_bricks hollow`,
        `execute at ${nick} run fill ~14 ~ ~-17 ~14 ~2 ~-17 air replace`,
        `execute at ${nick} run fill ~15 ~15 ~-19 ~19 ~15 ~-15 stone_brick_slab[type=top] replace`,
        `execute at ${nick} run setblock ~18 ~15 ~-18 air replace`,
        `execute at ${nick} run fill ~18 ~ ~-18 ~18 ~15 ~-18 ladder[facing=south] replace`,
        `execute at ${nick} run fill ~14 ~19 ~-20 ~20 ~19 ~-14 stone_brick_wall outline`,
        `execute at ${nick} run setblock ~17 ~20 ~-17 crying_obsidian replace`,
        `execute at ${nick} run setblock ~17 ~21 ~-17 gold_block replace`,
        `execute at ${nick} run setblock ~17 ~22 ~-17 beacon replace`,
        `execute at ${nick} run setblock ~20 ~3 ~-17 iron_bars replace`,
        `execute at ${nick} run setblock ~17 ~3 ~-20 iron_bars replace`,

        // Torre SW (~-20 a ~-14, ~14 a ~20)
        `execute at ${nick} run fill ~-20 ~ ~14 ~-14 ~18 ~20 stone_bricks hollow`,
        `execute at ${nick} run fill ~-14 ~ ~17 ~-14 ~2 ~17 air replace`,
        `execute at ${nick} run fill ~-19 ~15 ~15 ~-15 ~15 ~19 stone_brick_slab[type=top] replace`,
        `execute at ${nick} run setblock ~-18 ~15 ~18 air replace`,
        `execute at ${nick} run fill ~-18 ~ ~18 ~-18 ~15 ~18 ladder[facing=north] replace`,
        `execute at ${nick} run fill ~-20 ~19 ~14 ~-14 ~19 ~20 stone_brick_wall outline`,
        `execute at ${nick} run setblock ~-17 ~20 ~17 crying_obsidian replace`,
        `execute at ${nick} run setblock ~-17 ~21 ~17 gold_block replace`,
        `execute at ${nick} run setblock ~-17 ~22 ~17 beacon replace`,
        `execute at ${nick} run setblock ~-20 ~3 ~17 iron_bars replace`,
        `execute at ${nick} run setblock ~-17 ~3 ~20 iron_bars replace`,

        // 7. Torreão Central (Donjon) Colossal (11x11, 22 blocos, 100% HOLLOW com múltiplos andares)
        `execute at ${nick} run fill ~-5 ~ ~-5 ~5 ~22 ~5 stone_bricks hollow`,
        `execute at ${nick} run fill ~-4 ~-1 ~-4 ~4 ~-1 ~4 polished_deepslate replace`,

        // Portas de entrada frontal e traseira no Donjon
        `execute at ${nick} run fill ~-1 ~ ~5 ~1 ~3 ~5 air replace`,
        `execute at ${nick} run fill ~-1 ~ ~-5 ~1 ~3 ~-5 air replace`,

        // 2º Andar do Donjon (Sala de Guerra / Quarto Imperial)
        `execute at ${nick} run fill ~-4 ~6 ~-4 ~4 ~6 ~4 deepslate_tile_slab[type=bottom] replace`,
        `execute at ${nick} run setblock ~-4 ~6 ~-4 air replace`,
        `execute at ${nick} run fill ~-4 ~ ~-4 ~-4 ~6 ~-4 ladder[facing=east] replace`,
        `execute at ${nick} run fill ~-5 ~8 ~-1 ~-5 ~9 ~1 glass_pane replace`,
        `execute at ${nick} run fill ~5 ~8 ~-1 ~5 ~9 ~1 glass_pane replace`,

        // 3º Andar / Mirante Ameado (a ~14)
        `execute at ${nick} run fill ~-4 ~14 ~-4 ~4 ~14 ~4 stone_brick_slab[type=bottom] replace`,
        `execute at ${nick} run setblock ~-4 ~14 ~-4 air replace`,
        `execute at ${nick} run fill ~-4 ~7 ~-4 ~-4 ~14 ~-4 ladder[facing=east] replace`,

        // Cobertura e Pináculo Piramidal Místico do Donjon (a ~22)
        `execute at ${nick} run fill ~-5 ~22 ~-5 ~5 ~22 ~5 stone_brick_slab[type=top] replace`,
        `execute at ${nick} run fill ~-5 ~23 ~-5 ~5 ~23 ~5 stone_brick_wall outline`,
        `execute at ${nick} run fill ~-3 ~23 ~-3 ~3 ~23 ~3 deepslate_tiles replace`,
        `execute at ${nick} run fill ~-2 ~24 ~-2 ~2 ~24 ~2 deepslate_tiles replace`,
        `execute at ${nick} run fill ~-1 ~25 ~-1 ~1 ~25 ~1 deepslate_tiles replace`,
        `execute at ${nick} run setblock ~ ~26 ~ crying_obsidian replace`,
        `execute at ${nick} run setblock ~ ~27 ~ gold_block replace`,
        `execute at ${nick} run setblock ~ ~28 ~ beacon replace`,

        // 8. Mobília Imperial do Salão do Trono (Térreo do Donjon)
        `execute at ${nick} run fill ~ ~ ~-4 ~ ~ ~4 purple_carpet replace`,
        `execute at ${nick} run setblock ~ ~ ~-4 crying_obsidian replace`,
        `execute at ${nick} run setblock ~ ~1 ~-4 deepslate_brick_stairs[facing=south,half=bottom] replace`,
        `execute at ${nick} run setblock ~ ~2 ~-4 purple_banner replace`,
        `execute at ${nick} run setblock ~-1 ~ ~-4 deepslate_brick_stairs[facing=east,half=bottom] replace`,
        `execute at ${nick} run setblock ~1 ~ ~-4 deepslate_brick_stairs[facing=west,half=bottom] replace`,
        `execute at ${nick} run setblock ~-4 ~3 ~-4 purple_banner replace`,
        `execute at ${nick} run setblock ~4 ~3 ~-4 purple_banner replace`,
        `execute at ${nick} run setblock ~-3 ~ ~-2 anvil replace`,
        `execute at ${nick} run setblock ~-3 ~ ~-1 crafting_table replace`,
        `execute at ${nick} run setblock ~-3 ~ ~ blast_furnace replace`,
        `execute at ${nick} run setblock ~-3 ~ ~1 smithing_table replace`,
        `execute at ${nick} run setblock ~3 ~ ~-2 chest replace`,
        `execute at ${nick} run setblock ~3 ~ ~-1 barrel replace`,
        `execute at ${nick} run setblock ~3 ~ ~ stonecutter replace`,
        `execute at ${nick} run setblock ~3 ~ ~1 ender_chest replace`,
        `execute at ${nick} run setblock ~-3 ~3 ~ soul_lantern[hanging=true] replace`,
        `execute at ${nick} run setblock ~3 ~3 ~ soul_lantern[hanging=true] replace`,
        `execute at ${nick} run setblock ~ ~3 ~-1 soul_lantern[hanging=true] replace`,
        `execute at ${nick} run setblock ~ ~3 ~1 soul_lantern[hanging=true] replace`,

        // 9. Capela do Vazio com Vitrais Místicos no pátio oeste (100% HOLLOW)
        `execute at ${nick} run fill ~-13 ~ ~-3 ~-8 ~6 ~3 deepslate_bricks hollow`,
        `execute at ${nick} run fill ~-12 ~-1 ~-2 ~-9 ~-1 ~2 smooth_quartz replace`,
        `execute at ${nick} run fill ~-8 ~ ~-1 ~-8 ~2 ~1 air replace`,
        `execute at ${nick} run fill ~-13 ~1 ~-2 ~-13 ~4 ~-2 cyan_stained_glass replace`,
        `execute at ${nick} run fill ~-13 ~1 ~ ~-13 ~4 ~ magenta_stained_glass replace`,
        `execute at ${nick} run fill ~-13 ~1 ~2 ~-13 ~4 ~2 cyan_stained_glass replace`,
        `execute at ${nick} run setblock ~-12 ~ ~ crying_obsidian replace`,
        `execute at ${nick} run setblock ~-12 ~1 ~ amethyst_cluster[facing=up] replace`,
        `execute at ${nick} run setblock ~-10 ~4 ~ soul_lantern[hanging=true] replace`,

        // 10. Jardins e Lago no pátio leste
        `execute at ${nick} run fill ~8 ~-1 ~-3 ~13 ~-1 ~3 grass_block replace`,
        `execute at ${nick} run fill ~9 ~-1 ~-2 ~12 ~-1 ~2 water replace`,
        `execute at ${nick} run setblock ~10 ~ ~ lily_pad replace`,
        `execute at ${nick} run setblock ~11 ~ ~-1 lily_pad replace`,
        `execute at ${nick} run setblock ~8 ~ ~ oak_log replace`,
        `execute at ${nick} run setblock ~8 ~1 ~ oak_leaves[persistent=true] replace`,
        `execute at ${nick} run setblock ~9 ~ ~3 sweet_berry_bush replace`,
        `execute at ${nick} run setblock ~11 ~ ~3 sweet_berry_bush replace`,
        `execute at ${nick} run setblock ~10 ~ ~-3 azure_bluet replace`,

        // 11. Portão Principal com Grade de Ferro e Ponte Levadiça sobre o fosso
        `execute at ${nick} run fill ~-2 ~ ~16 ~2 ~4 ~16 air replace`,
        `execute at ${nick} run fill ~-2 ~ ~20 ~2 ~4 ~20 air replace`,
        `execute at ${nick} run fill ~-2 ~4 ~20 ~2 ~4 ~20 iron_bars replace`,
        `execute at ${nick} run fill ~-2 ~-1 ~21 ~2 ~-1 ~24 oak_planks replace`,

        // 12. Lanternas das muralhas e escadaria de acesso
        `execute at ${nick} run setblock ~-14 ~5 ~-16 soul_lantern[hanging=false] replace`,
        `execute at ${nick} run setblock ~14 ~5 ~-16 soul_lantern[hanging=false] replace`,
        `execute at ${nick} run setblock ~-14 ~5 ~16 soul_lantern[hanging=false] replace`,
        `execute at ${nick} run setblock ~14 ~5 ~16 soul_lantern[hanging=false] replace`,
        `execute at ${nick} run fill ~2 ~ ~9 ~4 ~ ~9 polished_andesite replace`,
        `execute at ${nick} run setblock ~2 ~ ~9 stone_brick_stairs[facing=north,half=bottom] replace`,
        `execute at ${nick} run setblock ~3 ~1 ~9 stone_brick_stairs[facing=north,half=bottom] replace`,
        `execute at ${nick} run setblock ~4 ~2 ~9 stone_brick_stairs[facing=north,half=bottom] replace`,
        `execute at ${nick} run setblock ~4 ~3 ~9 stone_brick_stairs[facing=north,half=bottom] replace`,
        `execute at ${nick} run fill ~2 ~4 ~9 ~4 ~4 ~9 stone_brick_slab[type=bottom] replace`,

        // 13. Teletransporte seguro para o Salão do Trono e celebração
        `execute at ${nick} run tp ${nick} ~ ~1 ~`,
        `execute at ${nick} run playsound minecraft:ui.toast.challenge_complete master @a ~ ~ ~ 1 1`,
        `execute at ${nick} run playsound minecraft:block.beacon.activate master @a ~ ~28 ~ 1 1`
    ];

    for (const c of cmds) {
        await runCmd(c);
    }

    showToast(`🌌 Castelo Titânico do Vazio (41x41) erguido com sucesso em ${nick}!`);
    const undoBtns = document.querySelectorAll('.btn-undo-titanic-castle');
    undoBtns.forEach(b => b.style.display = 'block');
}

async function undoTitanicCastleAction() {
    const nick = getMyNick();
    if (!confirm(`↩️ Deseja realmente demolir o Castelo Titânico do Vazio ao redor de ${nick}, drenar o fosso e restaurar o terreno com grama?`)) return;
    showToast("↩️ Demolindo o Castelo Titânico do Vazio...");
    const cmds = [
        `gamerule commandModificationBlockLimit 1000000`,
        `execute at ${nick} run fill ~-28 ~ ~-28 ~28 ~35 ~28 air replace`,
        `execute at ${nick} run fill ~-28 ~-3 ~-28 ~28 ~-1 ~28 dirt replace`,
        `execute at ${nick} run fill ~-28 ~-1 ~-28 ~28 ~-1 ~28 grass_block replace`
    ];
    for (const c of cmds) {
        await runCmd(c);
    }
    const undoBtns = document.querySelectorAll('.btn-undo-titanic-castle');
    undoBtns.forEach(b => b.style.display = 'none');
}

/* --- CIDADELA GÓTICA MONUMENTAL (100x100x90) --- */
function buildGothicMegacastleAction() {
    const nick = getMyNick();
    if (!confirm(`🏛️ Erguer a Cidadela Gótica Monumental (100x100x90) em ${nick}?\n\n- Salão Catedralício (40x30x40) com 3 andares e escadaria em caracol\n- Salão do Trono, Banquete e Aposentos Reais\n- Pináculo Central de 75m com telhado vermelho até 90m e Farol\n- Ponte de Colunata de 50m de altura com pilares\n- Torre de Vigia independente (9x9)\n\n⚠️ Ocupa 100x100 blocos e até 90 blocos de altura.`)) return;
    showToast(`🏛️ Erguendo Cidadela Gótica Monumental em ${nick}...`);
    runCmd(`gothic-castle ${nick}`);
}

/* --- USS ENTERPRISE NCC-1701 --- */
function buildEnterpriseAction() {
    const nick = getMyNick();
    if (!confirm(`🛸 Construir a USS Enterprise NCC-1701 flutuante em ${nick}?\n\n- Disco Principal (32m de diâmetro com Ponte de Comando)\n- Ponte com Cadeira do Capitão, Visor espacial e Bancadas\n- Casco Secundário de Engenharia com Defletor luminoso\n- 2 Nacelles de Dobra de 38 blocos com Coletores Bussard vermelhos e Plasma azul\n- Sala de Teletransporte com efeitos sonoros`)) return;
    showToast(`🛸 Construindo USS Enterprise NCC-1701 nos céus de ${nick}...`);
    runCmd(`enterprise ${nick}`);
}

/* --- OUTRAS ESTRUTURAS --- */
function buildBunker() {
    const nick = getMyNick();
    runCmd(`execute at ${nick} run fill ~-3 ~-1 ~-3 ~3 ~-1 ~3 smooth_stone && execute at ${nick} run fill ~-3 ~ ~-3 ~3 ~3 ~3 tinted_glass replace air && execute at ${nick} run fill ~-2 ~ ~-2 ~2 ~2 ~2 air && execute at ${nick} run setblock ~ ~ ~ torch && execute at ${nick} run setblock ~1 ~ ~ red_bed`);
    showToast(`🏰 Bunker blindado criado ao redor de ${nick}!`);
}

function buildStonePlatform() {
    const nick = getMyNick();
    runCmd(`execute at ${nick} run fill ^-5 ^-1 ^1 ^5 ^-1 ^10 stone`);
    showToast(`🧱 Plataforma de pedra gerada na frente de ${nick}!`);
}

function buildGrassPlatform() {
    const nick = getMyNick();
    runCmd(`execute at ${nick} run fill ^-5 ^-2 ^1 ^5 ^-2 ^10 dirt && execute at ${nick} run fill ^-5 ^-1 ^1 ^5 ^-1 ^10 grass_block`);
    showToast(`🌱 Plataforma de Terra + Grama (10x10) gerada na frente de ${nick}!`);
}

function buildRamp() {
    const nick = getMyNick();
    let cmds = [];
    for (let i = 1; i <= 15; i++) {
        cmds.push(`fill ^-1 ^${i-1} ^${i} ^1 ^${i-1} ^${i} stone_bricks`);
        cmds.push(`fill ^-1 ^${i} ^${i} ^1 ^${i+2} ^${i} air replace`);
    }
    cmds.push(`fill ^-1 ^14 ^15 ^1 ^14 ^18 stone_bricks`);
    runCmd(`execute at ${nick} run ${cmds.join(' && execute at ' + nick + ' run ')}`);
    showToast(`🪜 Escadaria de 15m erguida na frente de ${nick}!`);
}

function buildMineShaft() {
    const nick = getMyNick();
    runCmd(`execute at ${nick} run fill ~-2 ~-22 ~-2 ~2 ~-1 ~2 stone_bricks && execute at ${nick} run fill ~-1 ~-22 ~-1 ~1 ~-1 ~1 air && execute at ${nick} run setblock ~-1 ~-22 ~-1 soul_sand && execute at ${nick} run fill ~-1 ~-21 ~-1 ~-1 ~-1 ~-1 water && execute at ${nick} run fill ~1 ~-22 ~-1 ~1 ~-1 ~-1 water && execute at ${nick} run fill ~ ~-22 ~1 ~ ~-1 ~1 ladder[facing=north] && execute at ${nick} run fill ~-3 ~-22 ~-3 ~3 ~-20 ~3 air && execute at ${nick} run fill ~-3 ~-23 ~-3 ~3 ~-23 ~3 smooth_stone && execute at ${nick} run setblock ~ ~-21 ~ torch && execute at ${nick} run setblock ~2 ~-22 ~ chest && execute at ${nick} run setblock ~-2 ~-22 ~ crafting_table`);
    showToast(`⛏️ Poço de mina seguro escavado sob ${nick}!`);
}

function buildLaunchTower() {
    const nick = getMyNick();
    runCmd(`execute at ${nick} run fill ~-1 ~ ~-1 ~1 ~35 ~1 glass && execute at ${nick} run fill ~ ~ ~ ~ ~35 ~ air && execute at ${nick} run setblock ~ ~-1 ~ soul_sand && execute at ${nick} run fill ~ ~ ~ ~ ~35 ~ water && execute at ${nick} run fill ~-2 ~35 ~-2 ~2 ~35 ~2 smooth_quartz && execute at ${nick} run fill ~-2 ~36 ~-2 ~2 ~36 ~2 glass_pane && execute at ${nick} run setblock ~ ~36 ~2 air && execute at ${nick} run setblock ~1 ~36 ~1 barrel && execute at ${nick} run item replace block ~1 ~36 ~1 container.0 with firework_rocket 64`);
    showToast(`🪽 Torre lançadora de Elytra erguida em ${nick}!`);
}

function buildEnchantingSanctuary() {
    const nick = getMyNick();
    runCmd(`execute at ${nick} run fill ~-3 ~ ~-3 ~3 ~3 ~3 air && execute at ${nick} run fill ~-3 ~-1 ~-3 ~3 ~-1 ~3 polished_deepslate && execute at ${nick} run fill ~-2 ~ ~-2 ~2 ~1 ~-2 bookshelf && execute at ${nick} run fill ~-2 ~ ~2 ~2 ~1 ~2 bookshelf && execute at ${nick} run fill ~-2 ~ ~-2 ~-2 ~1 ~2 bookshelf && execute at ${nick} run fill ~2 ~ ~-2 ~2 ~1 ~2 bookshelf && execute at ${nick} run setblock ~ ~ ~2 air && execute at ${nick} run setblock ~ ~1 ~2 air && execute at ${nick} run setblock ~ ~ ~ enchanting_table && execute at ${nick} run setblock ~1 ~ ~1 anvil && execute at ${nick} run setblock ~-1 ~ ~1 grindstone && execute at ${nick} run setblock ~-1 ~ ~-1 barrel && execute at ${nick} run item replace block ~-1 ~ ~-1 container.0 with lapis_lazuli 64 && execute at ${nick} run setblock ~1 ~ ~-1 barrel && execute at ${nick} run item replace block ~1 ~ ~-1 container.0 with book 16`);
    showToast(`✨ Altar de Encantamentos Nível 30 pronto em ${nick}!`);
}

function buildWorkshop() {
    const nick = getMyNick();
    runCmd(`execute at ${nick} run fill ~-2 ~-1 ~-2 ~2 ~-1 ~2 spruce_planks && execute at ${nick} run fill ~-2 ~3 ~-2 ~2 ~3 ~2 spruce_slab && execute at ${nick} run setblock ~-2 ~ ~-2 blast_furnace[facing=south] && execute at ${nick} run setblock ~-1 ~ ~-2 blast_furnace[facing=south] && execute at ${nick} run setblock ~ ~ ~-2 crafting_table && execute at ${nick} run setblock ~1 ~ ~-2 smoker[facing=south] && execute at ${nick} run setblock ~2 ~ ~-2 smoker[facing=south] && execute at ${nick} run setblock ~-2 ~ ~ smithing_table && execute at ${nick} run setblock ~-2 ~ ~1 stonecutter && execute at ${nick} run setblock ~-2 ~ ~2 anvil && execute at ${nick} run setblock ~2 ~ ~ chest[facing=west] && execute at ${nick} run setblock ~2 ~ ~1 chest[facing=west] && execute at ${nick} run setblock ~ ~2 ~ lantern[hanging=true]`);
    showToast(`⚒️ Oficina completa criada em ${nick}!`);
}

function buildPlateau() {
    const nick = getMyNick();
    runCmd(`execute at ${nick} run fill ~-10 ~ ~-10 ~10 ~15 ~10 air && execute at ${nick} run fill ~-10 ~-1 ~-10 ~10 ~-1 ~10 grass_block && execute at ${nick} run fill ~-10 ~-3 ~-10 ~10 ~-2 ~10 dirt`);
    showToast(`🧹 Terreno 21x21 nivelado em ${nick}!`);
}

function buildLightingGrid() {
    const nick = getMyNick();
    const offsets = [-12, 0, 12];
    let cmds = [];
    for (let x of offsets) {
        for (let z of offsets) {
            cmds.push(`setblock ~${x} ~ ~${z} polished_deepslate_wall`);
            cmds.push(`setblock ~${x} ~1 ~${z} polished_deepslate_wall`);
            cmds.push(`setblock ~${x} ~2 ~${z} lantern`);
        }
    }
    runCmd(`execute at ${nick} run ${cmds.join(' && execute at ' + nick + ' run ')}`);
    showToast(`💡 Rede de postes instalada (área 31x31 livre de monstros)!`);
}

function buildCamp() {
    const nick = getMyNick();
    runCmd(`execute at ${nick} run fill ~-2 ~-1 ~-2 ~2 ~-1 ~2 oak_planks && execute at ${nick} run setblock ~ ~ ~ red_bed && execute at ${nick} run setblock ~2 ~ ~ campfire && execute at ${nick} run setblock ~-2 ~ ~ torch`);
    showToast(`⛺ Acampamento criado em ${nick}!`);
}

function buildBridge() {
    const nick = getMyNick();
    runCmd(`execute at ${nick} run fill ~ ~-1 ~ ~ ~-1 ~12 oak_planks && execute at ${nick} run fill ~ ~-1 ~ ~12 ~-1 ~ oak_planks`);
    showToast(`🌉 Ponte criada a partir de ${nick}!`);
}

function buildFarm() {
    const nick = getMyNick();
    runCmd(`execute at ${nick} run fill ~-2 ~-1 ~-2 ~2 ~-1 ~2 farmland && execute at ${nick} run setblock ~ ~-1 ~ water && execute at ${nick} run fill ~-2 ~ ~-2 ~2 ~ ~2 wheat[age=7] replace air`);
    showToast(`🌾 Mini-horta criada em ${nick}!`);
}

function buildTower() {
    const nick = getMyNick();
    runCmd(`execute at ${nick} run fill ~1 ~ ~ ~1 ~8 ~ cobblestone && execute at ${nick} run fill ~1 ~ ~-1 ~1 ~8 ~-1 ladder[facing=north] && execute at ${nick} run fill ~ ~8 ~-1 ~2 ~8 ~1 oak_planks`);
    showToast(`🗼 Torre de vigia criada em ${nick}!`);
}

function buildPyramid() {
    if (confirm("🏛️ Deseja erguer a Grande Pirâmide Gigante no Deserto (-160, 70, -160)?")) {
        showToast('🏛️ Erguendo a Grande Pirâmide...');
        runCmd('pyramid -160 70 -160 25');
    }
}

/* --- WAYPOINT MODAL --- */
let selectedWaypointIcon = '📍';

function saveCurrentAsWaypoint() {
    let defaultCoords = "-146 107 -85";
    if (telemetryData && telemetryData.players) {
        const me = telemetryData.players.find(p => p.name.toLowerCase() === myProfile);
        if (me) defaultCoords = `${Math.round(me.x)} ${Math.round(me.y)} ${Math.round(me.z)}`;
    }
    const modal = document.getElementById('modalWaypoint');
    const inputCoords = document.getElementById('inputWaypointCoords');
    const inputName = document.getElementById('inputWaypointName');
    if (inputCoords) inputCoords.value = defaultCoords;
    if (inputName) inputName.value = "Novo Ponto";
    if (modal && typeof modal.showModal === 'function') {
        modal.showModal();
    } else {
        const name = prompt("Nome do ponto:", "Novo Ponto");
        if (!name) return;
        const coords = prompt("Coordenadas X Y Z:", defaultCoords);
        if (!coords) return;
        const p = coords.trim().split(/\s+/);
        fetch('/api/waypoints', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({name: name, x: parseInt(p[0]), y: parseInt(p[1]), z: parseInt(p[2]), icon: '📍'})
        }).then(() => {
            showToast('Ponto salvo!');
            loadWaypoints();
            fetchTelemetry();
        });
    }
}

function selectWaypointIcon(icon, btn) {
    selectedWaypointIcon = icon;
    document.querySelectorAll('.wp-icon-btn').forEach(b => b.classList.remove('btn-cyan'));
    if (btn) btn.classList.add('btn-cyan');
}

function submitWaypointModal() {
    const name = document.getElementById('inputWaypointName').value.trim() || "Ponto";
    const coords = (document.getElementById('inputWaypointCoords').value || "-146 107 -85").trim().split(/\s+/);
    const modal = document.getElementById('modalWaypoint');
    if (modal) modal.close();

    fetch('/api/waypoints', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({
            name: name,
            x: parseInt(coords[0]) || 0,
            y: parseInt(coords[1]) || 100,
            z: parseInt(coords[2]) || 0,
            icon: selectedWaypointIcon
        })
    }).then(() => {
        showToast('Ponto salvo!');
        loadWaypoints();
        fetchTelemetry();
    });
}

function closeWaypointModal() {
    const modal = document.getElementById('modalWaypoint');
    if (modal) modal.close();
}

/* --- SKINS --- */
function customSkinPrompt() {
    const modal = document.getElementById('modalSkin');
    if (modal && typeof modal.showModal === 'function') {
        modal.showModal();
    } else {
        const player = prompt("Qual jogador? (pace ou Lizzie):", getMyNick());
        if (!player) return;
        const nick = prompt("Nome do jogador com a skin original (ex: M0r7eM):", "M0r7eM");
        if (!nick) return;
        runCmd(`skin ${player} ${nick}`);
        showToast(`🎨 Solicitando skin '${nick}' para ${player}...`);
    }
}

function submitSkinModal() {
    const player = document.getElementById('inputSkinPlayer').value || getMyNick();
    const nick = document.getElementById('inputSkinNick').value.trim();
    const modal = document.getElementById('modalSkin');
    if (modal) modal.close();
    if (nick) {
        runCmd(`skin ${player} ${nick}`);
        showToast(`🎨 Solicitando skin '${nick}' para ${player}...`);
    }
}

function closeSkinModal() {
    const modal = document.getElementById('modalSkin');
    if (modal) modal.close();
}

/* --- BACKUPS & CONSOLE --- */
async function triggerBackup() {
    showToast("💾 Gerando backup do mundo...");
    try {
        const res = await fetch('/api/backup', { method: 'POST' });
        const data = await res.json();
        if (data.ok) {
            showToast("✅ Backup salvo com sucesso!");
        } else {
            showToast("Erro no backup: " + data.error, true);
        }
    } catch (err) {
        showToast("Falha de rede ao acionar backup", true);
    }
}

function runCustomCmd() {
    const input = document.getElementById('customCmd');
    if (!input) return;
    let val = input.value.trim();
    if (val) {
        if (val.startsWith('/')) val = val.substring(1);
        val = val.replace(/@s\b/g, getMyNick());
        runCmd(val);
        input.value = '';
    }
}
