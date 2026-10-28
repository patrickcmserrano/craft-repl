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

async function loadInventory() {
    try {
        const res = await fetch(`/api/inventory?player=${currentPlayer}`);
        const data = await res.json();
        if (data.error) {
            showToast(data.error, true);
            return;
        }

        const titleEl = document.getElementById('invPlayerTitle');
        if (titleEl) titleEl.textContent = `Inventário de ${data.player.toUpperCase()}`;
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
                    slot.innerHTML = `<span class="inv-slot-icon">${a.item.icon}</span>`;
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
                    slot.innerHTML = `
                        <span class="inv-slot-icon">${item.icon}</span>
                        ${item.count > 1 ? `<span class="inv-slot-count">${item.count}</span>` : ''}
                    `;
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
                    slot.innerHTML = `
                        <span class="inv-slot-icon">${item.icon}</span>
                        ${item.count > 1 ? `<span class="inv-slot-count">${item.count}</span>` : ''}
                    `;
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

    if (title) title.innerHTML = `${item.icon} ${item.name}`;
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
function teleportToHome() { runCmd(`tp ${getMyNick()} -146 107 -85`); }
function teleportAllHome() { runCmd(`tp @a -146 107 -85`); }
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
