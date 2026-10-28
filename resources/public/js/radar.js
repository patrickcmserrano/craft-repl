/* BlueMap 3D & Waypoints Management */

function openBlueMapExternal() {
    window.open('/map/', '_blank');
}

function refreshBlueMap() {
    const iframe = document.getElementById('blueMapIframe');
    if (iframe) iframe.src = `/map/?t=${Date.now()}`;
}

function drawRadar() {
    if (!telemetryData) return;
    const myKey = myProfile;
    const partnerKey = myProfile === 'pace' ? 'lizzie' : 'pace';
    const me = (telemetryData.players || []).find(p => p.name.toLowerCase() === myKey);
    const partner = (telemetryData.players || []).find(p => p.name.toLowerCase() === partnerKey);

    const bar = document.getElementById('radarTeleBar');
    if (bar) {
        const meOnline = Boolean(me && me.online);
        const partnerOnline = Boolean(partner && partner.online);

        const myCoords = me ? `[${Math.round(me.x)}, ${Math.round(me.y || 107)}, ${Math.round(me.z)}]` : '[-146, 107, -85]';
        const partnerCoords = partner ? `[${Math.round(partner.x)}, ${Math.round(partner.y || 107)}, ${Math.round(partner.z)}]` : '[-146, 107, -85]';

        const myPosText = meOnline
            ? `🟢 ${getMyDisplay()}: ${myCoords}`
            : `⚪ ${getMyDisplay()}: Offline (${myCoords})`;

        const partnerPosText = partnerOnline
            ? `🟢 ${getPartnerDisplay()}: ${partnerCoords}`
            : `⚪ ${getPartnerDisplay()}: Offline (${partnerCoords})`;

        bar.innerHTML = `<span>${myPosText}</span><span>${partnerPosText}</span>`;
    }
}

function initRadarEvents() {
    // No-op for BlueMap
}

async function loadWaypoints() {
    try {
        const res = await fetch('/api/waypoints');
        const pts = await res.json();
        const container = document.getElementById('waypointList');
        if (!container) return;
        container.innerHTML = '';
        const myNick = getMyNick();
        pts.forEach(pt => {
            const el = document.createElement('div');
            el.className = 'card-row';
            el.innerHTML = `
                <div>
                    <div style="font-weight:700; font-size:0.88rem;">${pt.icon || '📍'} ${pt.name}</div>
                    <div style="font-family:'JetBrains Mono'; font-size:0.72rem; color:var(--text-muted);">X: ${pt.x} | Y: ${pt.y} | Z: ${pt.z}</div>
                </div>
                <div style="display:flex; gap:6px;">
                    <button class="btn btn-sm btn-teleport" onclick="runCmd('tp ${myNick} ${pt.x} ${pt.y} ${pt.z}')">Ir (${myNick})</button>
                    <button class="btn btn-sm btn-purple" onclick="runCmd('tp @a ${pt.x} ${pt.y} ${pt.z}')">Todos</button>
                </div>
            `;
            container.appendChild(el);
        });
    } catch (err) {
        console.error('Falha ao carregar waypoints', err);
    }
}
