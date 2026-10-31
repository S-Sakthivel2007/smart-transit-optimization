/**
 * Vetrinagar Smart Public Transit Route Optimization
 * Frontend Application Controller
 */

let state = {
    currentRole: 'SUPERVISOR',
    stops: [],
    roads: [],
    buses: [],
    rankedDemand: [],
    allDemand: [],
    trips: [],
    metrics: {},
    activeHighlightedPath: [],
    lastSubmittedRequest: null,
    selectedRoadForTraffic: null
};

// --- Initialization ---
document.addEventListener('DOMContentLoaded', () => {
    fetchTransitData();
    setInterval(fetchTransitData, 4000); // Poll status every 4 seconds
});

// --- Role Switching ---
function switchRole(role) {
    state.currentRole = role;
    const supBtn = document.getElementById('role-supervisor-btn');
    const passBtn = document.getElementById('role-passenger-btn');
    const supSec = document.getElementById('supervisor-section');
    const passSec = document.getElementById('passenger-section');

    if (role === 'SUPERVISOR') {
        supBtn.classList.add('active');
        passBtn.classList.remove('active');
        supSec.classList.remove('hidden');
        passSec.classList.add('hidden');
    } else {
        passBtn.classList.add('active');
        supBtn.classList.remove('active');
        passSec.classList.remove('hidden');
        supSec.classList.add('hidden');
    }
}

// --- Theme Toggle ---
function toggleTheme() {
    document.body.classList.toggle('theme-light');
}

// --- Fetch Data ---
async function fetchTransitData() {
    try {
        const res = await fetch('/api/status');
        if (!res.ok) return;
        const data = await res.json();

        state.stops = data.stops || [];
        state.roads = data.roads || [];
        state.buses = data.buses || [];
        state.rankedDemand = data.rankedDemand || [];
        state.allDemand = data.allDemand || [];
        state.trips = data.trips || [];
        state.metrics = data.metrics || {};

        renderKPIs();
        renderSVGMap();
        renderDemandTable();
        renderBusFleet();
        renderTripHistory();
        populateStopDropdowns();
        updatePassengerStatusCard();
    } catch (e) {
        console.error('Fetch error:', e);
    }
}

// --- Render KPI Banner ---
function renderKPIs() {
    const m = state.metrics;
    document.getElementById('kpi-waiting').innerText = m.waiting || 0;
    document.getElementById('kpi-served').innerText = m.served || 0;
    document.getElementById('kpi-avail-buses').innerText = `${m.availBuses || 0} / ${m.totalFleet || 4}`;
    document.getElementById('kpi-active-trips').innerText = m.activeTrips || 0;
    document.getElementById('kpi-co2').innerText = `${(m.co2Saved || 0).toFixed(1)} kg`;
}

// --- Render SVG Transit Map ---
function renderSVGMap() {
    const edgesGroup = document.getElementById('map-edges-group');
    const highlightGroup = document.getElementById('map-highlight-group');
    const stopsGroup = document.getElementById('map-stops-group');
    const busesGroup = document.getElementById('map-buses-group');

    if (!edgesGroup || !stopsGroup) return;

    // Build stop lookup map
    const stopMap = {};
    state.stops.forEach(s => stopMap[s.id] = s);

    // 1. Render Roads / Edges
    let edgesHtml = '';
    state.roads.forEach(r => {
        const u = stopMap[r.from];
        const v = stopMap[r.to];
        if (!u || !v) return;

        let strokeColor = '#10b981'; // normal
        let strokeDash = 'none';

        if (r.status === 'CLOSED') {
            strokeColor = '#6b7280';
            strokeDash = '6 4';
        } else if (r.restriction && r.restriction.toLowerCase().includes('minibus')) {
            strokeColor = '#f59e0b'; // amber for narrow/minibus
        } else if (r.time >= 25) {
            strokeColor = '#f43f5e'; // red for heavy traffic
        }

        const midX = (u.x + v.x) / 2;
        const midY = (u.y + v.y) / 2;

        edgesHtml += `
            <g class="road-item" onclick="openTrafficModalForRoad(${r.id})">
                <line x1="${u.x}" y1="${u.y}" x2="${v.x}" y2="${v.y}" 
                      stroke="${strokeColor}" stroke-dasharray="${strokeDash}" class="road-line" />
                <rect x="${midX - 14}" y="${midY - 9}" width="28" height="16" rx="4" fill="#111827" stroke="rgba(255,255,255,0.1)"/>
                <text x="${midX}" y="${midY + 3}" class="road-label">${r.time}m</text>
            </g>
        `;
    });
    edgesGroup.innerHTML = edgesHtml;

    // 2. Render Active Highlighted Route
    let highlightHtml = '';
    if (state.activeHighlightedPath && state.activeHighlightedPath.length > 1) {
        for (let i = 0; i < state.activeHighlightedPath.length - 1; i++) {
            const u = stopMap[state.activeHighlightedPath[i]];
            const v = stopMap[state.activeHighlightedPath[i + 1]];
            if (u && v) {
                highlightHtml += `
                    <line x1="${u.x}" y1="${u.y}" x2="${v.x}" y2="${v.y}" class="active-route-path" />
                `;
            }
        }
    }
    highlightGroup.innerHTML = highlightHtml;

    // 3. Render Stops
    let stopsHtml = '';
    state.stops.forEach(s => {
        const isDepot = s.type === 'DEPOT';
        const fill = isDepot ? '#a855f7' : '#06b6d4';
        const radius = isDepot ? 16 : 13;

        stopsHtml += `
            <g class="stop-node" onclick="onStopClicked('${s.id}')">
                <circle cx="${s.x}" cy="${s.y}" r="${radius}" fill="${fill}" stroke="#1e293b" stroke-width="2.5" />
                <text x="${s.x}" y="${s.y + 4}" fill="#ffffff" font-size="9" font-weight="bold" text-anchor="middle">${s.id}</text>
                <text x="${s.x}" y="${s.y + radius + 13}" class="stop-text">${s.name}</text>
            </g>
        `;
    });
    stopsGroup.innerHTML = stopsHtml;

    // 4. Render Buses on Stops
    let busesHtml = '';
    const busLocCount = {};
    state.buses.forEach(b => {
        const s = stopMap[b.location];
        if (!s) return;

        const offsetIndex = busLocCount[b.location] || 0;
        busLocCount[b.location] = offsetIndex + 1;
        const busX = s.x + 18 + (offsetIndex * 24);
        const busY = s.y - 14;

        let badgeColor = b.status === 'AVAILABLE' ? '#10b981' : (b.status === 'ON_TRIP' ? '#f59e0b' : '#f43f5e');

        busesHtml += `
            <g class="bus-pin" onclick="onBusClicked('${b.id}')" transform="translate(${busX}, ${busY})">
                <rect x="0" y="0" width="34" height="18" rx="5" fill="${badgeColor}" stroke="#ffffff" stroke-width="1.2" />
                <text x="17" y="12" fill="#ffffff" font-size="9" font-weight="bold" text-anchor="middle">${b.id}</text>
            </g>
        `;
    });
    busesGroup.innerHTML = busesHtml;
}

// --- Render Demand Table (PriorityQueue) ---
function renderDemandTable() {
    const tbody = document.getElementById('demand-table-body');
    const badge = document.getElementById('badge-queue-count');
    if (!tbody) return;

    badge.innerText = `${state.rankedDemand.length} Active Batches`;

    if (state.rankedDemand.length === 0) {
        tbody.innerHTML = `<tr><td colspan="6" style="text-align: center; color: var(--text-muted); padding: 1.5rem;">No waiting relief demand in queue.</td></tr>`;
        return;
    }

    let html = '';
    state.rankedDemand.forEach((d, idx) => {
        const rank = idx + 1;
        const rankBadge = rank === 1 
            ? `<span class="badge" style="background:#f43f5e; color:#fff;">#1 TOP</span>` 
            : `<span style="font-weight:700; color:var(--text-muted);">#${rank}</span>`;

        html += `
            <tr onclick="highlightRouteBetween('${d.pickup}', '${d.dest}')" style="cursor: pointer;">
                <td>${rankBadge}</td>
                <td><strong style="color:var(--accent-primary);">${d.id}</strong></td>
                <td><strong>${d.pickup}</strong> &rarr; <strong>${d.dest}</strong></td>
                <td><span style="font-weight:700; font-family:'JetBrains Mono';">${d.count}</span> <span style="font-size:0.75rem; color:var(--text-muted);">/ ${d.orig}</span></td>
                <td>${d.time}</td>
                <td style="font-size:0.75rem; color:var(--text-secondary);">${d.remarks}</td>
            </tr>
        `;
    });
    tbody.innerHTML = html;
}

// --- Render Bus Fleet Management Grid ---
function renderBusFleet() {
    const grid = document.getElementById('bus-fleet-grid');
    if (!grid) return;

    let html = '';
    state.buses.forEach(b => {
        let statusBadge = '';
        if (b.status === 'AVAILABLE') statusBadge = '<span class="badge badge-available">Available</span>';
        else if (b.status === 'ON_TRIP') statusBadge = '<span class="badge badge-on-trip">On Trip</span>';
        else statusBadge = '<span class="badge badge-broken">Broken / Maint</span>';

        const loadPct = Math.min(100, (b.load / b.capacity) * 100);

        html += `
            <div class="bus-card">
                <div class="bus-card-top">
                    <div>
                        <div class="bus-title">${b.id} - ${b.type}</div>
                        <span class="bus-type-tag">${b.type === 'MiniBus' ? 'Agile (All Roads)' : 'Standard (No Narrow Lanes)'}</span>
                    </div>
                    ${statusBadge}
                </div>

                <div style="font-size: 0.82rem; color: var(--text-secondary); margin-top: 0.5rem;">
                    Location: <strong style="color:var(--cyan);">${b.location}</strong>
                </div>

                <div class="bus-capacity-bar">
                    <div class="bus-capacity-fill" style="width: ${loadPct}%;"></div>
                </div>
                <div style="display:flex; justify-content:space-between; font-size:0.75rem; color:var(--text-muted);">
                    <span>Occupancy: ${b.load} / ${b.capacity}</span>
                    <span>Cap: ${b.capacity} seats</span>
                </div>

                <div class="bus-actions">
                    ${b.status === 'BROKEN' ? `
                        <button class="btn btn-sm btn-primary" onclick="repairBus('${b.id}')">Repair & Release</button>
                    ` : `
                        <button class="btn btn-sm btn-outline" onclick="triggerBreakdown('${b.id}')">Simulate Breakdown</button>
                    `}
                </div>
            </div>
        `;
    });
    grid.innerHTML = html;
}

// --- Render Trip History Table ---
function renderTripHistory() {
    const tbody = document.getElementById('trips-table-body');
    const countBadge = document.getElementById('trip-history-count');
    if (!tbody) return;

    countBadge.innerText = `${state.trips.length} Trips`;

    if (state.trips.length === 0) {
        tbody.innerHTML = `<tr><td colspan="9" style="text-align: center; color: var(--text-muted); padding: 1.5rem;">No trips logged yet.</td></tr>`;
        return;
    }

    let html = '';
    state.trips.forEach(t => {
        let statusBadge = '';
        if (t.status === 'COMPLETED') statusBadge = '<span class="badge badge-available">Completed</span>';
        else if (t.status === 'IN_PROGRESS') statusBadge = '<span class="badge badge-on-trip">In Progress</span>';
        else if (t.status === 'REQUIRES_ATTENTION') statusBadge = '<span class="badge badge-broken">Attention Needed</span>';
        else statusBadge = '<span class="badge badge-secondary">' + t.status + '</span>';

        const seg1Str = (t.seg1 || []).join(' &rarr; ') + ` (${t.seg1Time}m)`;
        const seg2Str = (t.seg2 || []).join(' &rarr; ') + ` (${t.seg2Time}m)`;

        html += `
            <tr onclick="highlightFullTripRoute('${t.id}')" style="cursor: pointer;">
                <td><strong style="color:var(--cyan);">${t.id}</strong></td>
                <td><strong>${t.busId}</strong></td>
                <td>${t.pickup} &rarr; ${t.dest}</td>
                <td><strong>${t.pax}</strong> / ${t.cap}</td>
                <td style="font-size:0.75rem;">${seg1Str}</td>
                <td style="font-size:0.75rem;">${seg2Str}</td>
                <td><strong style="color:var(--emerald);">${t.totalTime} mins</strong></td>
                <td>${statusBadge}</td>
                <td>
                    ${t.status === 'IN_PROGRESS' ? `
                        <button class="btn btn-sm btn-primary" onclick="event.stopPropagation(); completeReliefTrip('${t.id}')">Complete</button>
                    ` : `<span style="color:var(--text-muted); font-size:0.75rem;">Done</span>`}
                </td>
            </tr>
        `;
    });
    tbody.innerHTML = html;
}

// --- Route Highlighting Helpers ---
async function highlightRouteBetween(from, to, busId = null) {
    try {
        let url = `/api/supervisor/dijkstra?from=${from}&to=${to}`;
        if (busId) url += `&busId=${busId}`;
        const res = await fetch(url);
        const data = await res.json();
        if (data.reachable) {
            state.activeHighlightedPath = data.path;
            renderSVGMap();
            showToast(`Shortest path: ${data.path.join(' -> ')} (${data.time} mins)`, 'info');
        } else {
            showToast(data.message || 'No feasible route found', 'error');
        }
    } catch (e) {
        console.error(e);
    }
}

function highlightFullTripRoute(tripId) {
    const trip = state.trips.find(t => t.id === tripId);
    if (!trip) return;

    // Combine segment 1 and segment 2
    const combined = [...(trip.seg1 || [])];
    (trip.seg2 || []).forEach((stop, idx) => {
        if (idx === 0 && combined.length > 0 && combined[combined.length - 1] === stop) {
            return;
        }
        combined.push(stop);
    });

    state.activeHighlightedPath = combined;
    renderSVGMap();
    showToast(`Displaying Trip ${tripId}: ${combined.join(' -> ')}`, 'info');
}

// --- Dropdowns ---
function populateStopDropdowns() {
    const pickupSelect = document.getElementById('pass-pickup');
    const destSelect = document.getElementById('pass-dest');
    if (!pickupSelect || pickupSelect.options.length > 1) return;

    state.stops.forEach(s => {
        const opt1 = new Option(`${s.id} - ${s.name}`, s.id);
        const opt2 = new Option(`${s.id} - ${s.name}`, s.id);
        pickupSelect.add(opt1);
        destSelect.add(opt2);
    });
}

// --- Passenger Actions ---
function setCount(val) {
    const input = document.getElementById('pass-count');
    input.value = parseInt(input.value || 0) + val;
}

async function updatePassengerRoutePreview() {
    const pickup = document.getElementById('pass-pickup').value;
    const dest = document.getElementById('pass-dest').value;
    const box = document.getElementById('passenger-route-estimate');

    if (!pickup || !dest || pickup === dest) {
        box.innerHTML = `<div class="estimate-placeholder">Select distinct pickup and destination stops to view live shortest travel time.</div>`;
        return;
    }

    try {
        const res = await fetch(`/api/supervisor/dijkstra?from=${pickup}&to=${dest}`);
        const data = await res.json();
        if (data.reachable) {
            state.activeHighlightedPath = data.path;
            renderSVGMap();
            box.innerHTML = `
                <div class="estimate-active">
                    <div style="font-size:0.8rem; color:var(--text-muted);">FASTEST ROUTE (DIJKSTRA)</div>
                    <div style="font-size:1.1rem; font-weight:700; color:var(--emerald); margin:0.2rem 0;">
                        ${data.time} Minutes
                    </div>
                    <div style="font-size:0.82rem; color:var(--text-secondary);">
                        Path: <strong>${data.path.join(' &rarr; ')}</strong>
                    </div>
                </div>
            `;
        } else {
            box.innerHTML = `<div style="color:var(--rose); font-size:0.85rem;">⚠️ No open route connects ${pickup} and ${dest}.</div>`;
        }
    } catch (e) {
        console.error(e);
    }
}

async function handlePassengerSubmit(e) {
    e.preventDefault();
    const name = document.getElementById('pass-name').value;
    const pickup = document.getElementById('pass-pickup').value;
    const dest = document.getElementById('pass-dest').value;
    const count = parseInt(document.getElementById('pass-count').value);

    try {
        const res = await fetch('/api/passenger/request', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ passengerName: name, pickupStopId: pickup, destinationStopId: dest, passengerCount: count })
        });
        const data = await res.json();
        if (data.success) {
            showToast(`Request ${data.requestId} submitted & aggregated into Batch ${data.batchId}!`, 'success');
            state.lastSubmittedRequest = {
                requestId: data.requestId,
                batchId: data.batchId,
                name: name,
                pickup: pickup,
                dest: dest,
                count: count,
                submittedAt: new Date().toLocaleTimeString()
            };
            updatePassengerStatusCard();
            fetchTransitData();
        } else {
            showToast(data.message || 'Submission failed', 'error');
        }
    } catch (err) {
        showToast('Server error submitting demand', 'error');
    }
}

function updatePassengerStatusCard() {
    const card = document.getElementById('passenger-active-status');
    if (!card) return;

    if (!state.lastSubmittedRequest) {
        card.innerHTML = `<div class="ticket-empty">No active submitted request in this session. Submit demand to track allocated relief bus and boarding status.</div>`;
        return;
    }

    const r = state.lastSubmittedRequest;
    // Check if batch is allocated in trips
    const trip = state.trips.find(t => t.batchId === r.batchId);

    if (trip) {
        card.innerHTML = `
            <div class="ticket-allocated">
                <div style="display:flex; justify-content:space-between; align-items:center;">
                    <strong style="color:var(--emerald);">RELIEF BUS DISPATCHED!</strong>
                    <span class="badge badge-available">${trip.status}</span>
                </div>
                <div style="margin-top:0.5rem; font-size:0.85rem;">
                    <div>Assigned Bus: <strong>${trip.busId}</strong> (Capacity: ${trip.cap})</div>
                    <div>Your Group: <strong>${r.count} passengers</strong> on corridor <strong>${r.pickup} &rarr; ${r.dest}</strong></div>
                    <div>Estimated Total Transit Time: <strong style="color:var(--cyan);">${trip.totalTime} mins</strong></div>
                    <div style="font-size:0.75rem; color:var(--text-muted); margin-top:0.3rem;">Request ID: ${r.requestId} | Batch ID: ${r.batchId}</div>
                </div>
            </div>
        `;
    } else {
        card.innerHTML = `
            <div style="border-left: 4px solid var(--amber); padding-left: 0.8rem;">
                <div style="display:flex; justify-content:space-between; align-items:center;">
                    <strong style="color:var(--amber);">WAITING IN PRIORITY QUEUE</strong>
                    <span class="badge badge-on-trip">Aggregated</span>
                </div>
                <div style="margin-top:0.5rem; font-size:0.85rem;">
                    <div>Corridor: <strong>${r.pickup} &rarr; ${r.dest}</strong> (${r.count} passengers)</div>
                    <div style="color:var(--text-secondary); font-size:0.8rem; margin-top:0.2rem;">
                        Aggregated into Batch <strong>${r.batchId}</strong>. The supervisor allocates buses based on passenger surge ranking.
                    </div>
                </div>
            </div>
        `;
    }
}

// --- Supervisor Actions ---
async function allocateReliefBus() {
    try {
        const res = await fetch('/api/supervisor/allocate', { method: 'POST' });
        const data = await res.json();
        if (data.success) {
            showToast(`Success! Allocated Bus ${data.busId} for ${data.boarded} passengers. Trip ID: ${data.tripId}`, 'success');
            fetchTransitData();
        } else {
            showToast(data.message || 'Allocation failed', 'error');
        }
    } catch (e) {
        showToast('Network error on allocation', 'error');
    }
}

async function completeReliefTrip(tripId) {
    try {
        const res = await fetch('/api/supervisor/complete', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ tripId: tripId })
        });
        const data = await res.json();
        if (data.success) {
            showToast(`Trip ${tripId} marked COMPLETED. Bus released!`, 'success');
            fetchTransitData();
        } else {
            showToast(data.message || 'Completion failed', 'error');
        }
    } catch (e) {
        showToast('Network error completing trip', 'error');
    }
}

// --- Traffic Modal & Updates ---
function openTrafficModal() {
    const sel = document.getElementById('traffic-road-select');
    sel.innerHTML = '';
    state.roads.forEach(r => {
        const opt = new Option(`Road #${r.id}: ${r.from} -> ${r.to} (${r.time} mins, ${r.status})`, r.id);
        sel.add(opt);
    });
    onTrafficRoadChange();
    document.getElementById('traffic-modal').classList.remove('hidden');
}

function openTrafficModalForRoad(roadId) {
    openTrafficModal();
    document.getElementById('traffic-road-select').value = roadId;
    onTrafficRoadChange();
}

function closeTrafficModal() {
    document.getElementById('traffic-modal').classList.add('hidden');
}

function onTrafficRoadChange() {
    const roadId = parseInt(document.getElementById('traffic-road-select').value);
    const road = state.roads.find(r => r.id === roadId);
    if (!road) return;

    document.getElementById('traffic-travel-time').value = road.time;
    document.getElementById('time-val-display').innerText = road.time;
    setRoadStatusChoice(road.status);
}

let chosenRoadStatus = 'OPEN';
function setRoadStatusChoice(status) {
    chosenRoadStatus = status;
    const btnOpen = document.getElementById('btn-road-open');
    const btnClosed = document.getElementById('btn-road-closed');
    if (status === 'OPEN') {
        btnOpen.classList.add('active');
        btnClosed.classList.remove('active');
    } else {
        btnClosed.classList.add('active');
        btnOpen.classList.remove('active');
    }
}

async function submitTrafficUpdate() {
    const roadId = parseInt(document.getElementById('traffic-road-select').value);
    const time = parseInt(document.getElementById('traffic-travel-time').value);
    const reason = document.getElementById('traffic-reason').value || 'Supervisor road condition adjustment';

    try {
        // Update time
        await fetch('/api/supervisor/traffic', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ roadId: roadId, travelTime: time, reason: reason })
        });

        // Update status if changed
        const currentRoad = state.roads.find(r => r.id === roadId);
        if (currentRoad && currentRoad.status !== chosenRoadStatus) {
            await fetch('/api/supervisor/toggle-road', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ roadId: roadId, status: chosenRoadStatus })
            });
        }

        showToast(`Road #${roadId} updated! Dijkstra routes dynamically re-evaluated.`, 'success');
        closeTrafficModal();
        fetchTransitData();
    } catch (e) {
        showToast('Error updating traffic', 'error');
    }
}

// --- Breakdown Modal & Actions ---
function openBreakdownModal() {
    const sel = document.getElementById('breakdown-bus-select');
    sel.innerHTML = '';
    state.buses.forEach(b => {
        if (b.status !== 'BROKEN') {
            const opt = new Option(`${b.id} (${b.type}, Status: ${b.status}, At: ${b.location})`, b.id);
            sel.add(opt);
        }
    });
    document.getElementById('breakdown-modal').classList.remove('hidden');
}

function closeBreakdownModal() {
    document.getElementById('breakdown-modal').classList.add('hidden');
}

function triggerBreakdown(busId) {
    openBreakdownModal();
    document.getElementById('breakdown-bus-select').value = busId;
}

async function submitBusBreakdown() {
    const busId = document.getElementById('breakdown-bus-select').value;
    if (!busId) return;

    try {
        const res = await fetch('/api/supervisor/breakdown', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ busId: busId })
        });
        const data = await res.json();
        showToast(data.message, data.success ? 'success' : 'error');
        closeBreakdownModal();
        fetchTransitData();
    } catch (e) {
        showToast('Error reporting breakdown', 'error');
    }
}

async function repairBus(busId) {
    try {
        const res = await fetch('/api/supervisor/repair', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ busId: busId })
        });
        const data = await res.json();
        showToast(data.message, 'success');
        fetchTransitData();
    } catch (e) {
        showToast('Error repairing bus', 'error');
    }
}

// --- Toast Alerts ---
function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.innerText = message;
    container.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateX(50px)';
        toast.style.transition = '0.3s';
        setTimeout(() => toast.remove(), 300);
    }, 4500);
}
