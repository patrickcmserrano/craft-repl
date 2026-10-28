/* GPS Radar & Waypoints Renderer */
let zoomLevel = 1.0;
let centerTarget = 'me';

function changeZoom(delta) {
    zoomLevel = Math.max(0.4, Math.min(3.5, zoomLevel + delta));
    drawRadar();
}

function centerOnMe() {
    centerTarget = 'me';
    drawRadar();
}

function centerOnPartner() {
    centerTarget = 'partner';
    drawRadar();
}

function centerOnBase() {
    centerTarget = 'base';
    drawRadar();
}

function drawRadar() {
    const canvas = document.getElementById('radarCanvas');
    if (!canvas || !telemetryData) return;
    const ctx = canvas.getContext('2d');
    const width = canvas.width;
    const height = canvas.height;
    const cx = width / 2;
    const cy = height / 2;

    ctx.clearRect(0, 0, width, height);

    const myKey = myProfile;
    const partnerKey = myProfile === 'pace' ? 'lizzie' : 'pace';

    const me = (telemetryData.players || []).find(p => p.name.toLowerCase() === myKey) || {x: -146, z: -85, yaw: 0};
    const partner = (telemetryData.players || []).find(p => p.name.toLowerCase() === partnerKey);

    let ox = me.x;
    let oz = me.z;
    if (centerTarget === 'base') {
        ox = -146;
        oz = -85;
    } else if (centerTarget === 'partner' && partner) {
        ox = partner.x;
        oz = partner.z;
    }

    const scale = 1.6 * zoomLevel;

    // Concentric rings
    const rings = [25, 50, 100, 200];
    ctx.strokeStyle = 'rgba(59, 130, 246, 0.2)';
    ctx.lineWidth = 1;
    rings.forEach(r => {
        const pr = r * scale;
        if (pr < cx * 1.5) {
            ctx.beginPath();
            ctx.arc(cx, cy, pr, 0, Math.PI * 2);
            ctx.stroke();

            ctx.fillStyle = 'rgba(147, 197, 253, 0.4)';
            ctx.font = '9px JetBrains Mono';
            ctx.fillText(`${r}m`, cx + pr - 14, cy - 3);
        }
    });

    // Central crosshair
    ctx.strokeStyle = 'rgba(59, 130, 246, 0.25)';
    ctx.beginPath();
    ctx.moveTo(cx, 10); ctx.lineTo(cx, height - 10);
    ctx.moveTo(10, cy); ctx.lineTo(width - 10, cy);
    ctx.stroke();

    // Compass directions
    ctx.fillStyle = '#60a5fa';
    ctx.font = 'bold 11px Inter';
    ctx.fillText('N (-Z)', cx - 18, 22);
    ctx.fillText('S (+Z)', cx - 18, height - 12);
    ctx.fillText('L (+X)', width - 38, cy - 4);
    ctx.fillText('O (-X)', 12, cy - 4);

    // Waypoints
    if (telemetryData.waypoints) {
        telemetryData.waypoints.forEach(wp => {
            const wx = cx + (wp.x - ox) * scale;
            const wz = cy + (wp.z - oz) * scale;
            if (wx >= 10 && wx <= width - 10 && wz >= 10 && wz <= height - 10) {
                ctx.font = '14px sans-serif';
                ctx.fillText(wp.icon || '📍', wx - 7, wz + 5);
                ctx.fillStyle = '#fde047';
                ctx.font = '9px Inter';
                ctx.fillText(wp.name, wx - 15, wz + 16);
            }
        });
    }

    // Partner Marker
    if (partner) {
        const lx = cx + (partner.x - ox) * scale;
        const lz = cy + (partner.z - oz) * scale;
        const isPartnerLiz = partnerKey === 'lizzie';

        ctx.fillStyle = isPartnerLiz ? '#ec4899' : '#3b82f6';
        ctx.beginPath();
        ctx.arc(lx, lz, 7, 0, Math.PI * 2);
        ctx.fill();

        // Direction vector
        const radPartner = ((partner.yaw + 90) * Math.PI) / 180;
        ctx.strokeStyle = isPartnerLiz ? '#f472b6' : '#93c5fd';
        ctx.lineWidth = 2;
        ctx.beginPath();
        ctx.moveTo(lx, lz);
        ctx.lineTo(lx + Math.cos(radPartner) * 16, lz + Math.sin(radPartner) * 16);
        ctx.stroke();

        ctx.fillStyle = isPartnerLiz ? '#fbcfe8' : '#bfdbfe';
        ctx.font = 'bold 10px Inter';
        ctx.fillText(isPartnerLiz ? '🌸 Lizzie' : '⚔️ Pace', lx - 20, lz - 10);

        const dx = partner.x - me.x;
        const dz = partner.z - me.z;
        const dist = Math.sqrt(dx * dx + dz * dz).toFixed(1);
        const distEl = document.getElementById('gpsDistance');
        if (distEl) distEl.textContent = `${getPartnerShort()} a ${dist}m de você`;

        // Dotted connection line
        const px = cx + (me.x - ox) * scale;
        const pz = cy + (me.z - oz) * scale;
        ctx.strokeStyle = isPartnerLiz ? 'rgba(236, 72, 153, 0.4)' : 'rgba(59, 130, 246, 0.4)';
        ctx.setLineDash([4, 4]);
        ctx.beginPath();
        ctx.moveTo(px, pz);
        ctx.lineTo(lx, lz);
        ctx.stroke();
        ctx.setLineDash([]);
    } else {
        const distEl = document.getElementById('gpsDistance');
        if (distEl) distEl.textContent = `${getPartnerShort()} offline`;
    }

    // Active Player Marker (ME)
    const px = cx + (me.x - ox) * scale;
    const pz = cy + (me.z - oz) * scale;
    const isMeLiz = myKey === 'lizzie';

    ctx.fillStyle = isMeLiz ? 'rgba(236, 72, 153, 0.25)' : 'rgba(59, 130, 246, 0.25)';
    ctx.beginPath();
    ctx.arc(px, pz, 14, 0, Math.PI * 2);
    ctx.fill();

    ctx.fillStyle = isMeLiz ? '#ec4899' : '#3b82f6';
    ctx.beginPath();
    ctx.arc(px, pz, 8, 0, Math.PI * 2);
    ctx.fill();

    const radMe = ((me.yaw + 90) * Math.PI) / 180;
    ctx.strokeStyle = isMeLiz ? '#f472b6' : '#93c5fd';
    ctx.lineWidth = 3;
    ctx.beginPath();
    ctx.moveTo(px, pz);
    ctx.lineTo(px + Math.cos(radMe) * 20, pz + Math.sin(radMe) * 20);
    ctx.stroke();

    ctx.fillStyle = isMeLiz ? '#fbcfe8' : '#bfdbfe';
    ctx.font = 'bold 10px Inter';
    ctx.fillText(`${getMyDisplay()} (Você)`, px - 30, pz - 13);

    const bar = document.getElementById('radarTeleBar');
    if (bar) {
        const myPosText = `${getMyDisplay()}: [${me.x}, ${me.z}]`;
        const partnerPosText = `${getPartnerDisplay()}: [${partner ? partner.x : '?'}, ${partner ? partner.z : '?'}]`;
        bar.innerHTML = `<span>${myPosText}</span><span>${partnerPosText}</span>`;
    }
}

// Click to teleport on radar
function initRadarEvents() {
    const canvas = document.getElementById('radarCanvas');
    if (!canvas) return;
    canvas.addEventListener('click', (e) => {
        if (!telemetryData) return;
        const rect = canvas.getBoundingClientRect();
        const cx = canvas.width / 2;
        const cy = canvas.height / 2;
        const clickX = (e.clientX - rect.left) * (canvas.width / rect.width);
        const clickY = (e.clientY - rect.top) * (canvas.height / rect.height);

        const myKey = myProfile;
        const me = (telemetryData.players || []).find(p => p.name.toLowerCase() === myKey) || {x: -146, z: -85};
        const partner = (telemetryData.players || []).find(p => p.name.toLowerCase() === (myKey === 'pace' ? 'lizzie' : 'pace'));

        let ox = me.x;
        let oz = me.z;
        if (centerTarget === 'base') {
            ox = -146;
            oz = -85;
        } else if (centerTarget === 'partner' && partner) {
            ox = partner.x;
            oz = partner.z;
        }

        const scale = 1.6 * zoomLevel;
        const targetX = Math.round(ox + (clickX - cx) / scale);
        const targetZ = Math.round(oz + (clickY - cy) / scale);

        if (confirm(`Deseja teleportar ${getMyNick()} para as coordenadas [X: ${targetX}, Z: ${targetZ}]?`)) {
            runCmd(`tp ${getMyNick()} ${targetX} ~ ${targetZ}`);
        }
    });
}

function setMapView(mode) {
    const radarWrap = document.getElementById('radarViewWrap');
    const blueWrap = document.getElementById('blueMapViewWrap');
    const btnRadar = document.getElementById('viewBtnRadar');
    const btnBlue = document.getElementById('viewBtnBlueMap');
    const iframe = document.getElementById('blueMapIframe');

    if (mode === 'bluemap') {
        radarWrap.style.display = 'none';
        blueWrap.style.display = 'flex';
        btnRadar.style.background = 'var(--surface)';
        btnRadar.classList.remove('btn-cyan');
        btnBlue.style.background = 'rgba(6, 182, 212, 0.15)';
        btnBlue.classList.add('btn-cyan');
        if (!iframe.src || iframe.src === 'about:blank' || iframe.src === window.location.href) {
            iframe.src = '/map/';
        }
    } else {
        blueWrap.style.display = 'none';
        radarWrap.style.display = 'flex';
        btnBlue.style.background = 'var(--surface)';
        btnBlue.classList.remove('btn-cyan');
        btnRadar.style.background = 'rgba(6, 182, 212, 0.15)';
        btnRadar.classList.add('btn-cyan');
    }
}

function openBlueMapExternal() {
    window.open('/map/', '_blank');
}

function refreshBlueMap() {
    const iframe = document.getElementById('blueMapIframe');
    if (iframe) iframe.src = `/map/?t=${Date.now()}`;
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
