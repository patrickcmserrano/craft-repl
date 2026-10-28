/* API Client & State Management */
let currentTab = 'gps';
let currentPlayer = 'pace';
let myProfile = 'pace';
let telemetryData = null;

function getMyNick() {
    return myProfile === 'lizzie' ? 'Lizzie' : 'pace';
}

function getPartnerNick() {
    return myProfile === 'lizzie' ? 'pace' : 'Lizzie';
}

function getPartnerShort() {
    return myProfile === 'lizzie' ? 'Pace' : 'Liz';
}

function getMyDisplay() {
    return myProfile === 'lizzie' ? '🌸 Lizzie' : '⚔️ Pace';
}

function getPartnerDisplay() {
    return myProfile === 'lizzie' ? '⚔️ Pace' : '🌸 Lizzie';
}

function showToast(text, isError = false) {
    const toast = document.getElementById('toast');
    if (!toast) return;
    toast.textContent = text;
    toast.style.borderColor = isError ? '#ef4444' : '#10b981';
    toast.classList.add('show');
    clearTimeout(toast._timeout);
    toast._timeout = setTimeout(() => toast.classList.remove('show'), 2200);
}

async function runCmd(cmd) {
    try {
        const res = await fetch('/api/run', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ cmd: cmd })
        });
        const data = await res.json();
        if (data.ok) {
            showToast(data.output || `⚡ /${cmd}`);
            setTimeout(fetchTelemetry, 300);
        } else {
            showToast(`Erro: ${data.output || data.error}`, true);
        }
    } catch (err) {
        showToast(`Falha de rede ao conectar`, true);
    }
}

async function fetchTelemetry() {
    try {
        const res = await fetch('/api/telemetry');
        telemetryData = await res.json();
        
        const badge = document.getElementById('serverBadge');
        if (badge) {
            if (telemetryData.server_online) {
                badge.textContent = "🟢 MINECRAFT ONLINE";
                badge.style.background = "rgba(16, 185, 129, 0.15)";
                badge.style.color = "#34d399";
                badge.style.borderColor = "rgba(16, 185, 129, 0.3)";
            } else {
                badge.textContent = "🔴 MINECRAFT OFFLINE";
                badge.style.background = "rgba(239, 68, 68, 0.15)";
                badge.style.color = "#f87171";
                badge.style.borderColor = "rgba(239, 68, 68, 0.3)";
            }
        }

        if (typeof drawRadar === 'function') drawRadar();
        updateNukeDistanceBadge();
    } catch (err) {
        console.error('Falha de telemetria', err);
    }
}

function updateNukeDistanceBadge() {
    const badge = document.getElementById('nukeSafetyBadge');
    if (!badge || !telemetryData || !telemetryData.players) return;
    const me = telemetryData.players.find(p => p.name.toLowerCase() === myProfile);
    if (!me) return;
    const distToBase = Math.hypot(me.x - (-146), me.z - (-85));
    if (distToBase < 40) {
        badge.textContent = `🔴 BLOQUEADO: ${Math.round(distToBase)}m da Base (Mínimo 40m)`;
        badge.style.color = '#f87171';
    } else {
        badge.textContent = `🟢 SEGURO: ${Math.round(distToBase)}m da Base`;
        badge.style.color = '#34d399';
    }
}

function switchTab(tabId, el) {
    document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
    document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
    const target = document.getElementById('tab-' + tabId);
    if (target) target.classList.add('active');
    if (el) el.classList.add('active');
    currentTab = tabId;
    if (tabId === 'inventory') selectPlayer(myProfile);
    if (tabId === 'gps') {
        fetchTelemetry();
        if (typeof loadWaypoints === 'function') loadWaypoints();
    }
    if (tabId === 'vault') {
        if (typeof loadVault === 'function') loadVault();
    }
}

function setProfile(profile) {
    myProfile = (profile || 'pace').toLowerCase();
    localStorage.setItem('mc_profile', myProfile);

    const btnP = document.getElementById('profileBtnPace');
    const btnL = document.getElementById('profileBtnLizzie');
    if (btnP && btnL) {
        btnP.className = 'profile-btn' + (myProfile === 'pace' ? ' active-pace' : '');
        btnL.className = 'profile-btn' + (myProfile === 'lizzie' ? ' active-lizzie' : '');
    }

    if (typeof centerTarget !== 'undefined' && centerTarget !== 'base') {
        centerTarget = 'me';
    }

    if (typeof updateDynamicUI === 'function') updateDynamicUI();
    if (currentTab === 'inventory') selectPlayer(myProfile);
    if (typeof drawRadar === 'function') drawRadar();
    showToast(`Perfil ativo: ${getMyDisplay()}`);
}

function initProfile() {
    const urlParams = new URLSearchParams(window.location.search);
    const paramP = urlParams.get('p') || urlParams.get('player');
    if (paramP && (paramP.toLowerCase() === 'lizzie' || paramP.toLowerCase() === 'pace')) {
        myProfile = paramP.toLowerCase();
        localStorage.setItem('mc_profile', myProfile);
    } else {
        const saved = localStorage.getItem('mc_profile');
        if (saved && (saved.toLowerCase() === 'lizzie' || saved.toLowerCase() === 'pace')) {
            myProfile = saved.toLowerCase();
        } else {
            myProfile = 'pace';
        }
    }
    setProfile(myProfile);
}
