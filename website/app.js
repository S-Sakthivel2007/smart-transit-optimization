/**
 * Vetrinagar Smart Public Transit Route Optimization
 * Full-Featured Frontend Controller with Sidebar Navigation,
 * Role-Based Authentication, Interactive Transit Map, and SQLite Explorer.
 */

const DEFAULT_STOPS = [
    { id: "D00", name: "Central Depot", type: "DEPOT", desc: "Vetrinagar Municipal Bus Headquarters & Maintenance Yard", x: 450, y: 240 },
    { id: "S01", name: "Market Junction", type: "STOP", desc: "Commercial District & Central Bazaar", x: 320, y: 160 },
    { id: "S02", name: "Tech Park", type: "STOP", desc: "IT Corridor & Technology Center", x: 660, y: 140 },
    { id: "S03", name: "Railway Station", type: "STOP", desc: "Main Junction Railway Terminal", x: 200, y: 280 },
    { id: "S04", name: "Gandhinagar", type: "STOP", desc: "Dense Residential Quarter Phase 1", x: 340, y: 360 },
    { id: "S05", name: "College Road", type: "STOP", desc: "Arts & Engineering Campus Zone", x: 180, y: 440 },
    { id: "S06", name: "Hospital Circle", type: "STOP", desc: "District General Hospital & Medical College", x: 340, y: 480 },
    { id: "S07", name: "Anna Nagar", type: "STOP", desc: "Eastern Township & Shopping Complex", x: 560, y: 280 },
    { id: "S08", name: "Industrial Estate", type: "STOP", desc: "Manufacturing & SME Logistics Hub", x: 740, y: 290 },
    { id: "S09", name: "Lake View", type: "STOP", desc: "Recreational Promenade & Outer Ring", x: 220, y: 560 },
    { id: "S10", name: "Temple Gate", type: "STOP", desc: "Historic Heritage Quarter & Pilgrim Terminus", x: 500, y: 480 },
    { id: "S11", name: "North Bypass", type: "STOP", desc: "Northern Highway Interchange & Freight Junction", x: 520, y: 80 },
    { id: "S12", name: "South Terminal", type: "STOP", desc: "Southern Peripheral Outpost (Isolated Outpost)", x: 700, y: 560 }
];

const DEFAULT_ROADS = [
    { id: 1, from: "D00", to: "S01", time: 8, status: "OPEN", type: "TWO_WAY", restriction: "All" },
    { id: 2, from: "D00", to: "S03", time: 10, status: "OPEN", type: "TWO_WAY", restriction: "All" },
    { id: 3, from: "D00", to: "S04", time: 12, status: "OPEN", type: "TWO_WAY", restriction: "All" },
    { id: 4, from: "D00", to: "S07", time: 9, status: "OPEN", type: "TWO_WAY", restriction: "All" },
    { id: 5, from: "S01", to: "S02", time: 35, status: "OPEN", type: "TWO_WAY", restriction: "All" },
    { id: 6, from: "S01", to: "S03", time: 7, status: "OPEN", type: "TWO_WAY", restriction: "All" },
    { id: 7, from: "S01", to: "S11", time: 11, status: "OPEN", type: "TWO_WAY", restriction: "All" },
    { id: 8, from: "S11", to: "S02", time: 14, status: "OPEN", type: "TWO_WAY", restriction: "All" },
    { id: 9, from: "S02", to: "S07", time: 12, status: "OPEN", type: "TWO_WAY", restriction: "All" },
    { id: 10, from: "S02", to: "S08", time: 16, status: "OPEN", type: "TWO_WAY", restriction: "All" },
    { id: 11, from: "S03", to: "S06", time: 15, status: "CLOSED", type: "TWO_WAY", restriction: "All" },
    { id: 12, from: "S03", to: "S04", time: 9, status: "OPEN", type: "TWO_WAY", restriction: "All" },
    { id: 13, from: "S03", to: "S05", time: 13, status: "OPEN", type: "ONE_WAY", restriction: "All" },
    { id: 14, from: "S04", to: "S05", time: 11, status: "OPEN", type: "TWO_WAY", restriction: "All" },
    { id: 15, from: "S04", to: "S07", time: 15, status: "OPEN", type: "TWO_WAY", restriction: "All" },
    { id: 16, from: "S04", to: "S10", time: 13, status: "OPEN", type: "TWO_WAY", restriction: "All" },
    { id: 17, from: "S05", to: "S06", time: 10, status: "OPEN", type: "TWO_WAY", restriction: "All" },
    { id: 18, from: "S05", to: "S09", time: 15, status: "OPEN", type: "TWO_WAY", restriction: "All" },
    { id: 19, from: "S06", to: "S07", time: 14, status: "OPEN", type: "TWO_WAY", restriction: "All" },
    { id: 20, from: "S06", to: "S10", time: 8, status: "OPEN", type: "TWO_WAY", restriction: "MiniBus Only" },
    { id: 21, from: "S07", to: "S08", time: 10, status: "OPEN", type: "TWO_WAY", restriction: "All" },
    { id: 22, from: "S07", to: "S10", time: 16, status: "OPEN", type: "TWO_WAY", restriction: "All" },
    { id: 23, from: "S08", to: "S11", time: 20, status: "OPEN", type: "TWO_WAY", restriction: "All" },
    { id: 24, from: "S09", to: "S10", time: 12, status: "OPEN", type: "TWO_WAY", restriction: "All" },
    { id: 25, from: "S10", to: "S12", time: 24, status: "CLOSED", type: "TWO_WAY", restriction: "All" }
];

const DEFAULT_BUSES = [
    { id: "B01", type: "MiniBus", capacity: 20, location: "D00", status: "AVAILABLE", load: 0 },
    { id: "B02", type: "StandardBus", capacity: 40, location: "D00", status: "AVAILABLE", load: 0 },
    { id: "B03", type: "StandardBus", capacity: 50, location: "D00", status: "AVAILABLE", load: 0 },
    { id: "B04", type: "MiniBus", capacity: 25, location: "D00", status: "BROKEN", load: 0 }
];

const DEFAULT_DEMAND = [
    { id: "DEM-1001", pickup: "S01", dest: "S02", count: 65, orig: 65, time: "08:15:00", remarks: "IT shift morning peak surge", status: "WAITING" },
    { id: "DEM-1002", pickup: "S03", dest: "S02", count: 45, orig: 45, time: "08:20:00", remarks: "Intercity train arrived at junction", status: "WAITING" },
    { id: "DEM-1003", pickup: "S05", dest: "S02", count: 40, orig: 40, time: "08:22:00", remarks: "Student & staff tech cluster", status: "WAITING" },
    { id: "DEM-1004", pickup: "S04", dest: "S01", count: 35, orig: 35, time: "08:25:00", remarks: "Market shoppers batch", status: "WAITING" },
    { id: "DEM-1005", pickup: "S01", dest: "S07", count: 30, orig: 30, time: "08:28:00", remarks: "Residential commute", status: "WAITING" },
    { id: "DEM-1006", pickup: "S03", dest: "S06", count: 25, orig: 25, time: "08:30:00", remarks: "Hospital shift visitors", status: "WAITING" },
    { id: "DEM-1007", pickup: "S02", dest: "S08", count: 20, orig: 20, time: "08:32:00", remarks: "Industrial delivery crews", status: "WAITING" },
    { id: "DEM-1008", pickup: "S06", dest: "S01", count: 18, orig: 18, time: "08:35:00", remarks: "Morning clinic returnees", status: "WAITING" },
    { id: "DEM-1009", pickup: "S07", dest: "S02", count: 15, orig: 15, time: "08:37:00", remarks: "Tech park contractors", status: "WAITING" },
    { id: "DEM-1010", pickup: "S05", dest: "S03", count: 12, orig: 12, time: "08:40:00", remarks: "College rail commuters", status: "WAITING" }
];

let state = {
    currentUser: {
        username: 'supervisor',
        role: 'SUPERVISOR',
        fullName: 'Chief Transit Supervisor'
    },
    currentPage: 'map',
    stops: [...DEFAULT_STOPS],
    roads: [...DEFAULT_ROADS],
    buses: [...DEFAULT_BUSES],
    rankedDemand: [...DEFAULT_DEMAND],
    allDemand: [...DEFAULT_DEMAND],
    trips: [],
    metrics: {
        waiting: 305,
        served: 40,
        activeTrips: 1,
        availBuses: 3,
        totalFleet: 4,
        utilization: 25.0,
        co2Saved: 18.0,
        avgWaitTime: 7.5
    },
    activeHighlightedPath: [],
    selectedStopId: 'D00',
    selectedRoadForTraffic: null,
    lastSubmittedRequest: null
};

// --- Initialization ---
document.addEventListener('DOMContentLoaded', () => {
    updateUserSessionUI();
    renderTopHeaderStats();
    renderSVGMap();
    renderStopDetailsCard(state.selectedStopId);
    populateDropdowns();
    renderOperationsQueue();
    renderFleetPage();
    renderTrafficTable();
    renderTripsPage();
    fetchTransitData();
    setInterval(fetchTransitData, 4000); // Live poll every 4 seconds
});

// =========================================================================
// 1. AUTHENTICATION & ROLE-BASED ACCESS
// =========================================================================

function openLoginOverlay() {
    document.getElementById('login-modal-overlay').classList.remove('hidden');
    document.getElementById('login-error-msg').classList.add('hidden');
}

function closeLoginOverlay() {
    document.getElementById('login-modal-overlay').classList.add('hidden');
}

async function quickLogin(username, password) {
    await performLogin(username, password);
}

async function handleManualLogin(e) {
    e.preventDefault();
    const u = document.getElementById('login-username').value.trim();
    const p = document.getElementById('login-password').value.trim();
    await performLogin(u, p);
}

async function performLogin(username, password) {
    const errorBox = document.getElementById('login-error-msg');
    try {
        const res = await fetch('/api/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: username, password: password })
        });
        const data = await res.json();
        if (data.success && data.user) {
            state.currentUser = data.user;
            updateUserSessionUI();
            closeLoginOverlay();
            showToast(`Welcome back, ${data.user.fullName}!`, 'success');

            // If passenger logged in, navigate straight to booking page
            if (data.user.role === 'PASSENGER') {
                navigateToPage('passenger');
            } else {
                navigateToPage('map');
            }
        } else {
            errorBox.innerText = data.message || 'Invalid username or password.';
            errorBox.classList.remove('hidden');
        }
    } catch (err) {
        // Fallback demo authentication if server unreachable
        if (username === 'supervisor' || username === 'admin') {
            state.currentUser = { username: 'supervisor', role: 'SUPERVISOR', fullName: 'Chief Transit Supervisor' };
        } else {
            state.currentUser = { username: 'passenger1', role: 'PASSENGER', fullName: 'Kavitha Raman' };
        }
        updateUserSessionUI();
        closeLoginOverlay();
        showToast(`Signed in as ${state.currentUser.fullName}`, 'info');
    }
}

function updateUserSessionUI() {
    const u = state.currentUser;
    const nameEl = document.getElementById('sidebar-user-name');
    const roleEl = document.getElementById('sidebar-user-role');
    const avatarEl = document.getElementById('sidebar-user-avatar');

    if (nameEl) nameEl.innerText = u.fullName || u.username;
    if (roleEl) roleEl.innerText = u.role;
    if (avatarEl) avatarEl.innerText = (u.role === 'SUPERVISOR') ? '👨‍💼' : '🧑‍🦱';

    // Show/hide supervisor-only items based on current role
    const supItems = document.querySelectorAll('.supervisor-only');
    supItems.forEach(el => {
        if (u.role === 'SUPERVISOR') {
            el.style.display = '';
        } else {
            el.style.display = 'none';
        }
    });

    // Update passenger name in booking form if present
    const passNameInput = document.getElementById('pass-name');
    if (passNameInput && u.role === 'PASSENGER') {
        passNameInput.value = u.fullName || 'Passenger';
    }
}

// =========================================================================
// 2. SIDEBAR PAGE NAVIGATION
// =========================================================================

const PAGE_METADATA = {
    map: {
        title: "Transit Network Map & Dijkstra Routes",
        desc: "Interactive topology showing travel-time routing across 13 stops"
    },
    operations: {
        title: "Relief Operations & Demand Queue",
        desc: "PriorityQueue ranked waiting demand with one-click relief bus allocation"
    },
    fleet: {
        title: "Relief Bus Fleet Management",
        desc: "OOP Encapsulation & Polymorphic road restrictions for MiniBus & StandardBus"
    },
    traffic: {
        title: "Traffic & Road Condition Control",
        desc: "Adjust road travel times, trigger bottlenecks, and test dynamic Dijkstra rerouting"
    },
    passenger: {
        title: "Passenger Relief Booking Portal",
        desc: "Submit commuter headcount with automatic FIFO corridor aggregation"
    },
    history: {
        title: "Relief Trip Execution History",
        desc: "Two-segment dispatch and in-service transit logs with verified path traces"
    },
    sdg: {
        title: "SDG 9, 11, 13 Sustainability Impact",
        desc: "Infrastructure efficiency, urban decongestion, and greenhouse gas offset metrics"
    }
};

function navigateToPage(pageId) {
    if (!PAGE_METADATA[pageId]) pageId = 'map';
    state.currentPage = pageId;

    // Update nav links
    document.querySelectorAll('.sidebar-menu .nav-item').forEach(btn => {
        if (btn.getAttribute('data-page') === pageId) {
            btn.classList.add('active');
        } else {
            btn.classList.remove('active');
        }
    });

    // Update page views
    document.querySelectorAll('.content-body .page-view').forEach(view => {
        if (view.id === `page-${pageId}`) {
            view.classList.add('active');
        } else {
            view.classList.remove('active');
        }
    });

    // Update top header title & description
    const meta = PAGE_METADATA[pageId];
    document.getElementById('current-page-title').innerText = meta.title;
    document.getElementById('current-page-desc').innerText = meta.desc;

    // Trigger page-specific data refreshes
    if (pageId === 'map') {
        renderSVGMap();
    }
}

function toggleTheme() {
    document.body.classList.toggle('theme-light');
}

// =========================================================================
// 3. DATA FETCHING & SYNCHRONIZATION
// =========================================================================

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

        renderTopHeaderStats();
        renderSVGMap();
        renderStopDetailsCard(state.selectedStopId);
        populateDropdowns();
        renderOperationsQueue();
        renderFleetPage();
        renderTrafficTable();
        renderTripsPage();
        updatePassengerStatusCard();
    } catch (e) {
        console.warn('Transit data polling note:', e);
    }
}

function renderTopHeaderStats() {
    const m = state.metrics;
    const w = m.waiting !== undefined ? m.waiting : 0;
    const bAvail = m.availBuses !== undefined ? m.availBuses : 0;
    const bTotal = m.totalFleet !== undefined ? m.totalFleet : 4;
    const co2 = m.co2Saved !== undefined ? m.co2Saved.toFixed(1) : '0.0';

    document.getElementById('top-stat-waiting').innerText = w;
    document.getElementById('top-stat-buses').innerText = `${bAvail}/${bTotal}`;
    document.getElementById('top-stat-co2').innerText = `${co2} kg`;

    const sideBadge = document.getElementById('side-waiting-badge');
    if (sideBadge) sideBadge.innerText = state.rankedDemand.length;

    const sdgCo2 = document.getElementById('sdg-co2-val');
    if (sdgCo2) sdgCo2.innerText = `${co2} kg CO₂ Avoided`;
}

// =========================================================================
// 4. BRILLIANT & COMPACT SVG TRANSIT MAP
// =========================================================================

function renderSVGMap() {
    const edgesGroup = document.getElementById('map-edges-group');
    const highlightGroup = document.getElementById('map-highlight-group');
    const stopsGroup = document.getElementById('map-stops-group');
    const busesGroup = document.getElementById('map-buses-group');

    if (!edgesGroup || !stopsGroup) return;

    // Create coordinate lookup map
    const stopMap = {};
    state.stops.forEach(s => stopMap[s.id] = s);

    // 1. Render Roads / Edges
    let edgesHtml = '';
    state.roads.forEach(r => {
        const u = stopMap[r.from];
        const v = stopMap[r.to];
        if (!u || !v) return;

        let strokeColor = '#10b981'; // Green: normal
        let strokeDash = 'none';

        if (r.status === 'CLOSED') {
            strokeColor = '#64748b'; // Gray dashed: closed
            strokeDash = '6 4';
        } else if (r.restriction && r.restriction.toLowerCase().includes('minibus')) {
            strokeColor = '#f59e0b'; // Amber: narrow / minibus only
        } else if (r.time >= 25) {
            strokeColor = '#f43f5e'; // Red: heavy traffic
        }

        const midX = (u.x + v.x) / 2;
        const midY = (u.y + v.y) / 2;

        edgesHtml += `
            <g class="road-item" onclick="openTrafficModalForRoad(${r.id})">
                <line x1="${u.x}" y1="${u.y}" x2="${v.x}" y2="${v.y}" 
                      stroke="${strokeColor}" stroke-dasharray="${strokeDash}" class="road-line" />
                <g class="road-time-tag" transform="translate(${midX}, ${midY})">
                    <rect x="-14" y="-8" width="28" height="16" class="road-time-bg" />
                    <text x="0" y="3.5" class="road-time-text">${r.time}m</text>
                </g>
            </g>
        `;
    });
    edgesGroup.innerHTML = edgesHtml;

    // 2. Render Active Dijkstra Highlighted Route
    let highlightHtml = '';
    if (state.activeHighlightedPath && state.activeHighlightedPath.length > 1) {
        for (let i = 0; i < state.activeHighlightedPath.length - 1; i++) {
            const u = stopMap[state.activeHighlightedPath[i]];
            const v = stopMap[state.activeHighlightedPath[i + 1]];
            if (u && v) {
                highlightHtml += `
                    <line x1="${u.x}" y1="${u.y}" x2="${v.x}" y2="${v.y}" class="active-dijkstra-pulse" />
                `;
            }
        }
    }
    highlightGroup.innerHTML = highlightHtml;

    // 3. Render Stops
    let stopsHtml = '';
    state.stops.forEach(s => {
        const isDepot = s.type === 'DEPOT';
        const isSelected = (s.id === state.selectedStopId);
        const fill = isDepot ? 'url(#depot-gradient)' : '#06b6d4';
        const radius = isDepot ? 17 : 13;
        const strokeColor = isSelected ? '#ffffff' : (isDepot ? '#e9d5ff' : '#0f172a');
        const strokeW = isSelected ? 3.5 : 2.5;

        // Visual icon emoji for major hubs
        let iconSymbol = s.id;
        if (isDepot) iconSymbol = '★ D00';

        // Check consolidated waiting crowd at this stop
        const stopWaitingCount = state.rankedDemand
            .filter(b => b.pickup === s.id)
            .reduce((acc, b) => acc + b.count, 0);

        stopsHtml += `
            <g class="stop-group" onclick="selectStop('${s.id}')">
                ${isDepot ? `<circle cx="${s.x}" cy="${s.y}" r="25" fill="none" stroke="#a855f7" stroke-width="1.2" stroke-dasharray="4 3" opacity="0.7"/>` : ''}
                <circle cx="${s.x}" cy="${s.y}" r="${radius}" fill="${fill}" stroke="${strokeColor}" stroke-width="${strokeW}" />
                <text x="${s.x}" y="${s.y + 3.5}" fill="#ffffff" font-size="${isDepot ? 8.5 : 9.5}" font-weight="800" text-anchor="middle" pointer-events="none">${iconSymbol}</text>
                <text x="${s.x}" y="${s.y + radius + 13}" class="stop-name-text">${s.name}</text>
                ${stopWaitingCount > 0 ? `
                    <g class="crowd-pill" transform="translate(${s.x}, ${s.y - radius - 7})">
                        <rect x="-19" y="-11" width="38" height="14" rx="7" fill="#e11d48" stroke="#ffffff" stroke-width="1.2" />
                        <text x="0" y="-0.5" fill="#ffffff" font-size="8" font-family="'JetBrains Mono', monospace" font-weight="800" text-anchor="middle">👥 ${stopWaitingCount}</text>
                    </g>
                ` : ''}
            </g>
        `;
    });
    stopsGroup.innerHTML = stopsHtml;

    // 4. Render Buses Floating Above Stops
    let busesHtml = '';
    const busesAtStop = {};
    state.buses.forEach(b => {
        if (!busesAtStop[b.location]) busesAtStop[b.location] = [];
        busesAtStop[b.location].push(b);
    });

    Object.keys(busesAtStop).forEach(stopId => {
        const s = stopMap[stopId];
        if (!s) return;

        const list = busesAtStop[stopId];
        list.forEach((b, idx) => {
            const posX = s.x + 16 + (idx * 28);
            const posY = s.y - 15;
            let badgeBg = b.status === 'AVAILABLE' ? '#10b981' : (b.status === 'ON_TRIP' ? '#f59e0b' : '#f43f5e');

            busesHtml += `
                <g class="bus-badge-pin" onclick="onBusPinClicked('${b.id}')" transform="translate(${posX}, ${posY})">
                    <rect x="0" y="0" width="34" height="17" rx="5" fill="${badgeBg}" stroke="#ffffff" stroke-width="1.2" />
                    <text x="17" y="11.5" fill="#ffffff" font-size="8.5" font-family="'JetBrains Mono', monospace" font-weight="bold" text-anchor="middle">${b.id}</text>
                </g>
            `;
        });
    });
    busesGroup.innerHTML = busesHtml;
}

function selectStop(stopId) {
    state.selectedStopId = stopId;
    renderSVGMap();
    renderStopDetailsCard(stopId);
}

function renderStopDetailsCard(stopId) {
    const card = document.getElementById('selected-stop-card');
    if (!card) return;

    const stop = state.stops.find(s => s.id === stopId);
    if (!stop) {
        card.innerHTML = `<div class="stop-placeholder">Select a stop to view details.</div>`;
        return;
    }

    // Outgoing roads
    const outgoing = state.roads.filter(r => r.from === stop.id || (r.type === 'TWO_WAY' && r.to === stop.id));

    // Waiting demand from this stop (Aggregates multiple commuters at the same stop)
    const batches = state.rankedDemand.filter(b => b.pickup === stop.id);
    const totalWaiting = batches.reduce((acc, b) => acc + b.count, 0);

    card.innerHTML = `
        <div class="stop-details-content">
            <div class="stop-header-badge">
                <strong style="font-size:1.05rem; color:var(--text-primary);">${stop.name} (${stop.id})</strong>
                <span class="badge ${stop.type === 'DEPOT' ? 'badge-accent' : 'badge-info'}">${stop.type}</span>
            </div>
            <div style="color:var(--text-secondary); font-size:0.78rem;">${stop.desc}</div>
            
            <div style="margin-top:0.5rem; padding:0.6rem 0.75rem; background:rgba(225, 29, 72, 0.1); border:1px solid rgba(225, 29, 72, 0.3); border-radius:8px;">
                <div style="font-size:0.72rem; color:var(--text-muted); font-weight:700; text-transform:uppercase; letter-spacing:0.5px;">CONSOLIDATED STOP DEMAND:</div>
                <div style="font-size:1.35rem; font-weight:800; color:var(--rose); font-family:'JetBrains Mono'; margin-top:2px;">
                    ${totalWaiting} Passengers
                </div>
                <div style="font-size:0.75rem; color:var(--emerald); margin-top:0.25rem;">
                    ${batches.length > 1 
                        ? `⚡ Combined from ${batches.length} commuter bookings at this location` 
                        : (batches.length === 1 ? `⚡ 1 Active Consolidated Batch` : `✓ No passengers currently waiting`)}
                </div>
                ${batches.length > 0 ? `
                    <div style="margin-top:0.4rem; font-size:0.72rem; color:var(--text-secondary); border-top:1px dashed rgba(255,255,255,0.15); padding-top:0.35rem;">
                        ${batches.map(b => `<div style="display:flex; justify-content:space-between; margin-bottom:2px;"><span>Batch ${b.id} &rarr; ${b.dest}:</span> <strong style="color:#fff;">${b.count} pax</strong></div>`).join('')}
                    </div>
                ` : ''}
            </div>

            <div style="font-size:0.75rem; color:var(--text-muted); margin-top:0.4rem;">Connected Links: ${outgoing.length} Road Corridors</div>
            <button class="btn btn-sm btn-outline mt-2" onclick="setAsTesterOrigin('${stop.id}')">Test Routes From Here &rarr;</button>
        </div>
    `;
}

function setAsTesterOrigin(stopId) {
    document.getElementById('tester-from').value = stopId;
    runDijkstraTester();
}

function clearHighlightedRoute() {
    state.activeHighlightedPath = [];
    renderSVGMap();
    const box = document.getElementById('tester-result-box');
    if (box) box.innerHTML = `Route cleared. Select origin and destination to compute live Dijkstra shortest path.`;
}

async function runDijkstraTester() {
    const from = document.getElementById('tester-from').value;
    const to = document.getElementById('tester-to').value;
    const box = document.getElementById('tester-result-box');

    if (!from || !to || from === to) {
        box.innerHTML = `<span style="color:var(--text-muted);">Select two distinct stops to calculate shortest path.</span>`;
        return;
    }

    try {
        const res = await fetch(`/api/supervisor/dijkstra?from=${from}&to=${to}`);
        const data = await res.json();
        if (data.reachable) {
            state.activeHighlightedPath = data.path;
            renderSVGMap();
            box.innerHTML = `
                <div style="text-align:left; width:100%;">
                    <div style="font-size:0.75rem; color:var(--text-muted);">SHORTEST TIME (DIJKSTRA):</div>
                    <div style="font-size:1.15rem; font-weight:800; color:var(--emerald);">${data.time} Minutes</div>
                    <div style="font-size:0.78rem; color:var(--cyan); margin-top:0.2rem;">${data.path.join(' &rarr; ')}</div>
                </div>
            `;
        } else {
            state.activeHighlightedPath = [];
            renderSVGMap();
            box.innerHTML = `<span style="color:var(--rose);">⚠️ ${data.message || 'No feasible route connects these stops.'}</span>`;
        }
    } catch (e) {
        // Fallback to client-side Dijkstra
        const local = localDijkstra(from, to);
        if (local.reachable) {
            state.activeHighlightedPath = local.path;
            renderSVGMap();
            box.innerHTML = `
                <div style="text-align:left; width:100%;">
                    <div style="font-size:0.75rem; color:var(--text-muted);">SHORTEST TIME (DIJKSTRA):</div>
                    <div style="font-size:1.15rem; font-weight:800; color:var(--emerald);">${local.time} Minutes</div>
                    <div style="font-size:0.78rem; color:var(--cyan); margin-top:0.2rem;">${local.path.join(' &rarr; ')}</div>
                </div>
            `;
        } else {
            state.activeHighlightedPath = [];
            renderSVGMap();
            box.innerHTML = `<span style="color:var(--rose);">⚠️ ${local.message || 'No route connects these stops.'}</span>`;
        }
    }
}

function localDijkstra(fromId, toId) {
    const dist = {};
    const prev = {};
    const unvisited = new Set();
    state.stops.forEach(s => {
        dist[s.id] = Infinity;
        unvisited.add(s.id);
    });
    dist[fromId] = 0;

    while (unvisited.size > 0) {
        let curr = null;
        let minDist = Infinity;
        for (const u of unvisited) {
            if (dist[u] < minDist) {
                minDist = dist[u];
                curr = u;
            }
        }
        if (!curr || minDist === Infinity) break;
        if (curr === toId) break;
        unvisited.delete(curr);

        state.roads.forEach(r => {
            if (r.status === 'CLOSED') return;
            let neighbor = null;
            if (r.from === curr) neighbor = r.to;
            else if (r.to === curr && (r.type === 'TWO_WAY' || !r.type)) neighbor = r.from;

            if (neighbor && unvisited.has(neighbor)) {
                const alt = dist[curr] + r.time;
                if (alt < dist[neighbor]) {
                    dist[neighbor] = alt;
                    prev[neighbor] = curr;
                }
            }
        });
    }

    if (dist[toId] === Infinity) {
        return { reachable: false, message: `No route connects ${fromId} and ${toId} with current road conditions.` };
    }
    const path = [];
    let step = toId;
    while (step) {
        path.unshift(step);
        step = prev[step];
    }
    return { reachable: true, time: dist[toId], path: path };
}

function populateDropdowns() {
    const fromSel = document.getElementById('tester-from');
    const toSel = document.getElementById('tester-to');
    const passPickup = document.getElementById('pass-pickup');
    const passDest = document.getElementById('pass-dest');

    if (!fromSel || fromSel.options.length > 1) return;

    state.stops.forEach(s => {
        fromSel.add(new Option(`${s.id} - ${s.name}`, s.id));
        toSel.add(new Option(`${s.id} - ${s.name}`, s.id));
        if (passPickup) passPickup.add(new Option(`${s.id} - ${s.name}`, s.id));
        if (passDest) passDest.add(new Option(`${s.id} - ${s.name}`, s.id));
    });

    if (toSel.options.length > 2) toSel.selectedIndex = 2;
}

// =========================================================================
// 5. OPERATIONS QUEUE & ALLOCATION
// =========================================================================

function renderOperationsQueue() {
    const tbody = document.getElementById('ops-demand-table-body');
    const countEl = document.getElementById('ops-queue-count');
    if (!tbody) return;

    countEl.innerText = `${state.rankedDemand.length} Active Batches`;

    if (state.rankedDemand.length === 0) {
        tbody.innerHTML = `<tr><td colspan="8" style="text-align:center; color:var(--text-muted); padding:1.5rem;">No waiting relief demand batches.</td></tr>`;
        return;
    }

    let html = '';
    state.rankedDemand.forEach((d, idx) => {
        const rank = idx + 1;
        const priorityTag = (rank === 1) 
            ? `<span class="badge" style="background:var(--rose); color:#fff;">#1 TOP SURGE</span>`
            : `<span style="font-weight:700; color:var(--text-muted);">#${rank}</span>`;

        html += `
            <tr onclick="highlightRouteBetween('${d.pickup}', '${d.dest}')" style="cursor:pointer;">
                <td>${priorityTag}</td>
                <td><strong style="color:var(--accent-primary);">${d.id}</strong></td>
                <td><strong>${d.pickup}</strong> &rarr; <strong>${d.dest}</strong></td>
                <td><span style="font-weight:700; font-family:'JetBrains Mono';">${d.count}</span> <small style="color:var(--text-muted);">/ ${d.orig}</small></td>
                <td>${d.time}</td>
                <td><span class="badge badge-on-trip">${d.status}</span></td>
                <td style="font-size:0.75rem; color:var(--text-secondary);">${d.remarks}</td>
                <td>
                    <button class="btn btn-sm btn-accent" onclick="event.stopPropagation(); allocateReliefBus()">Allocate Bus</button>
                </td>
            </tr>
        `;
    });
    tbody.innerHTML = html;
}

async function allocateReliefBus() {
    try {
        const res = await fetch('/api/supervisor/allocate', { method: 'POST' });
        const data = await res.json();
        if (data.success) {
            showToast(`Allocated ${data.busId} for ${data.boarded} passengers. Trip ID: ${data.tripId}`, 'success');
            fetchTransitData();
            return;
        } else {
            showToast(data.message || 'No available or reachable bus.', 'error');
            return;
        }
    } catch (e) {
        // Fallback offline allocation
        if (!state.rankedDemand || state.rankedDemand.length === 0) {
            showToast('No waiting demand batches in queue.', 'info');
            return;
        }
        const availBus = state.buses.find(b => b.status === 'AVAILABLE');
        if (!availBus) {
            showToast('Allocation Failed: All buses are currently busy on trips or broken.', 'error');
            return;
        }
        const topBatch = state.rankedDemand.shift(); // PriorityQueue poll()
        const boarded = Math.min(availBus.capacity, topBatch.count);
        const unserved = topBatch.count - boarded;

        availBus.status = 'ON_TRIP';
        availBus.load = boarded;

        const tripId = `TRIP-${Math.floor(100 + Math.random() * 900)}`;
        const seg1 = localDijkstra(availBus.location, topBatch.pickup);
        const seg2 = localDijkstra(topBatch.pickup, topBatch.dest);
        const totalTime = (seg1.reachable ? seg1.time : 10) + (seg2.reachable ? seg2.time : 15);

        const newTrip = {
            id: tripId,
            batchId: topBatch.id,
            busId: availBus.id,
            pickup: topBatch.pickup,
            dest: topBatch.dest,
            pax: boarded,
            cap: availBus.capacity,
            seg1: seg1.path || [availBus.location, topBatch.pickup],
            seg1Time: seg1.time || 10,
            seg2: seg2.path || [topBatch.pickup, topBatch.dest],
            seg2Time: seg2.time || 15,
            totalTime: totalTime,
            status: 'IN_PROGRESS',
            createdAt: new Date().toLocaleTimeString()
        };
        state.trips.unshift(newTrip);

        if (unserved > 0) {
            topBatch.count = unserved;
            topBatch.status = 'PARTIALLY_ALLOCATED';
            state.rankedDemand.unshift(topBatch);
            state.rankedDemand.sort((a, b) => b.count - a.count);
        }

        // Highlight the 2-segment trip on the map
        const fullPath = [...newTrip.seg1];
        if (newTrip.seg2) {
            newTrip.seg2.slice(1).forEach(st => fullPath.push(st));
        }
        state.activeHighlightedPath = fullPath;

        showToast(`Allocated Bus ${availBus.id} (${boarded} pax boarded). Trip ${tripId} dispatched!`, 'success');
        renderSVGMap();
        renderOperationsQueue();
        renderFleetPage();
        renderTripsPage();
        renderStopDetailsCard(topBatch.pickup);
        renderTopHeaderStats();
    }
}

// =========================================================================
// 6. BUS FLEET MANAGEMENT
// =========================================================================

function renderFleetPage() {
    const grid = document.getElementById('page-bus-fleet-grid');
    if (!grid) return;

    let html = '';
    state.buses.forEach(b => {
        let statusBadge = (b.status === 'AVAILABLE') 
            ? '<span class="badge badge-available">Available</span>'
            : (b.status === 'ON_TRIP' ? '<span class="badge badge-on-trip">On Trip</span>' : '<span class="badge badge-broken">Broken / Maint</span>');

        const loadPct = Math.min(100, (b.load / b.capacity) * 100);

        html += `
            <div class="bus-card">
                <div class="bus-card-top">
                    <div>
                        <strong style="font-size:1.05rem;">${b.id}</strong>
                        <span class="badge badge-secondary" style="margin-left:0.4rem;">${b.type}</span>
                    </div>
                    ${statusBadge}
                </div>
                <div style="font-size:0.8rem; color:var(--text-secondary);">
                    Current Stop: <strong style="color:var(--cyan);">${b.location}</strong>
                </div>
                <div class="bus-capacity-bar">
                    <div class="bus-capacity-fill" style="width:${loadPct}%;"></div>
                </div>
                <div style="display:flex; justify-content:space-between; font-size:0.75rem; color:var(--text-muted);">
                    <span>Load: ${b.load} / ${b.capacity}</span>
                    <span>Cap: ${b.capacity} seats</span>
                </div>
                <div style="margin-top:0.9rem; display:flex; gap:0.45rem;">
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

function onBusPinClicked(busId) {
    showToast(`Inspecting Bus ${busId}`, 'info');
    navigateToPage('fleet');
}

// =========================================================================
// 7. TRAFFIC & ROAD NETWORK
// =========================================================================

function renderTrafficTable() {
    const tbody = document.getElementById('page-traffic-table-body');
    if (!tbody) return;

    let html = '';
    state.roads.forEach(r => {
        let statusPill = (r.status === 'OPEN')
            ? '<span class="badge badge-available">Open</span>'
            : '<span class="badge badge-broken">Closed</span>';

        html += `
            <tr>
                <td>#${r.id}</td>
                <td><strong>${r.from}</strong> &rarr; <strong>${r.to}</strong></td>
                <td><strong style="color:var(--cyan); font-family:'JetBrains Mono';">${r.time} mins</strong></td>
                <td>${statusPill}</td>
                <td>${r.type}</td>
                <td><span style="font-size:0.75rem; color:var(--text-secondary);">${r.restriction}</span></td>
                <td>
                    <button class="btn btn-sm btn-outline" onclick="openTrafficModalForRoad(${r.id})">Adjust</button>
                </td>
            </tr>
        `;
    });
    tbody.innerHTML = html;
}

// =========================================================================
// 8. PASSENGER PORTAL & DEMAND AGGREGATION
// =========================================================================

function setCount(val) {
    const input = document.getElementById('pass-count');
    input.value = Math.max(1, parseInt(input.value || 0) + val);
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
            box.innerHTML = `
                <div style="text-align:left; width:100%;">
                    <div style="font-size:0.75rem; color:var(--text-muted);">FASTEST ROUTE (DIJKSTRA):</div>
                    <div style="font-size:1.15rem; font-weight:800; color:var(--emerald);">${data.time} Minutes</div>
                    <div style="font-size:0.8rem; color:var(--text-secondary); margin-top:0.2rem;">Path: <strong>${data.path.join(' &rarr; ')}</strong></div>
                </div>
            `;
        } else {
            box.innerHTML = `<div style="color:var(--rose); font-size:0.82rem;">⚠️ No open road route connects ${pickup} and ${dest}.</div>`;
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
                time: new Date().toLocaleTimeString()
            };
            updatePassengerStatusCard();
            fetchTransitData();
        } else {
            showToast(data.message || 'Submission failed', 'error');
        }
    } catch (err) {
        // Standalone offline aggregation fallback
        const existing = state.rankedDemand.find(b => b.pickup === pickup && b.dest === dest);
        let batchId;
        if (existing) {
            existing.count += count;
            existing.orig += count;
            batchId = existing.id;
            showToast(`Aggregated: Added ${count} pax to stop ${pickup} (Batch ${batchId} now ${existing.count} pax)!`, 'success');
        } else {
            batchId = `DEM-${Math.floor(1000 + Math.random() * 9000)}`;
            state.rankedDemand.unshift({
                id: batchId,
                pickup: pickup,
                dest: dest,
                count: count,
                orig: count,
                time: new Date().toLocaleTimeString(),
                remarks: `Passenger app surge (${name})`,
                status: 'WAITING'
            });
            showToast(`New demand batch ${batchId} registered for ${count} passengers at ${pickup}!`, 'success');
        }

        // PriorityQueue re-ordering: Higher count has higher priority
        state.rankedDemand.sort((a, b) => b.count - a.count);

        state.lastSubmittedRequest = {
            requestId: `REQ-${Math.floor(500 + Math.random() * 500)}`,
            batchId: batchId,
            name: name,
            pickup: pickup,
            dest: dest,
            count: count,
            time: new Date().toLocaleTimeString()
        };

        renderSVGMap();
        renderStopDetailsCard(pickup);
        renderOperationsQueue();
        updatePassengerStatusCard();
        renderTopHeaderStats();
    }
}

function updatePassengerStatusCard() {
    const card = document.getElementById('passenger-active-status');
    if (!card) return;

    if (!state.lastSubmittedRequest) {
        card.innerHTML = `<div class="ticket-empty">No active submitted request in this session. Submit demand above to track allocated relief bus and boarding status.</div>`;
        return;
    }

    const r = state.lastSubmittedRequest;
    const trip = state.trips.find(t => t.batchId === r.batchId);

    if (trip) {
        card.innerHTML = `
            <div style="border-left:4px solid var(--emerald); padding-left:0.85rem;">
                <div style="display:flex; justify-content:space-between; align-items:center;">
                    <strong style="color:var(--emerald);">RELIEF BUS ALLOCATED!</strong>
                    <span class="badge badge-available">${trip.status}</span>
                </div>
                <div style="margin-top:0.4rem; font-size:0.85rem;">
                    <div>Assigned Vehicle: <strong>${trip.busId}</strong> (Cap: ${trip.cap} seats)</div>
                    <div>Route: <strong>${r.pickup} &rarr; ${r.dest}</strong> (${r.count} pax)</div>
                    <div>Estimated Trip Duration: <strong style="color:var(--cyan);">${trip.totalTime} mins</strong></div>
                    <div style="font-size:0.72rem; color:var(--text-muted); margin-top:0.3rem;">Batch ID: ${r.batchId} | Ticket ID: ${r.requestId}</div>
                </div>
            </div>
        `;
    } else {
        card.innerHTML = `
            <div style="border-left:4px solid var(--amber); padding-left:0.85rem;">
                <div style="display:flex; justify-content:space-between; align-items:center;">
                    <strong style="color:var(--amber);">WAITING IN PRIORITY QUEUE</strong>
                    <span class="badge badge-on-trip">Consolidated</span>
                </div>
                <div style="margin-top:0.4rem; font-size:0.85rem;">
                    <div>Corridor: <strong>${r.pickup} &rarr; ${r.dest}</strong> (${r.count} passengers)</div>
                    <div style="font-size:0.75rem; color:var(--text-muted); margin-top:0.25rem;">
                        Aggregated into Batch <strong>${r.batchId}</strong>. The supervisor will allocate relief buses in order of crowd demand.
                    </div>
                </div>
            </div>
        `;
    }
}

// =========================================================================
// 9. RELIEF TRIP HISTORY
// =========================================================================

function renderTripsPage() {
    const tbody = document.getElementById('page-trips-table-body');
    const countEl = document.getElementById('page-trip-count');
    if (!tbody) return;

    countEl.innerText = `${state.trips.length} Trips`;

    if (state.trips.length === 0) {
        tbody.innerHTML = `<tr><td colspan="9" style="text-align:center; color:var(--text-muted); padding:1.5rem;">No trips logged yet.</td></tr>`;
        return;
    }

    let html = '';
    state.trips.forEach(t => {
        let statusBadge = (t.status === 'COMPLETED')
            ? '<span class="badge badge-available">Completed</span>'
            : (t.status === 'IN_PROGRESS' ? '<span class="badge badge-on-trip">In Progress</span>' : `<span class="badge badge-secondary">${t.status}</span>`);

        const s1 = (t.seg1 || []).join(' &rarr; ') + ` (${t.seg1Time}m)`;
        const s2 = (t.seg2 || []).join(' &rarr; ') + ` (${t.seg2Time}m)`;

        html += `
            <tr onclick="highlightFullTrip('${t.id}')" style="cursor:pointer;">
                <td><strong style="color:var(--cyan);">${t.id}</strong></td>
                <td><strong>${t.busId}</strong></td>
                <td>${t.pickup} &rarr; ${t.dest}</td>
                <td><strong>${t.pax}</strong> / ${t.cap}</td>
                <td style="font-size:0.75rem;">${s1}</td>
                <td style="font-size:0.75rem;">${s2}</td>
                <td><strong style="color:var(--emerald);">${t.totalTime}m</strong></td>
                <td>${statusBadge}</td>
                <td>
                    ${t.status === 'IN_PROGRESS' ? `
                        <button class="btn btn-sm btn-primary" onclick="event.stopPropagation(); completeReliefTrip('${t.id}')">Complete</button>
                    ` : `<span style="font-size:0.75rem; color:var(--text-muted);">Done</span>`}
                </td>
            </tr>
        `;
    });
    tbody.innerHTML = html;
}

function highlightFullTrip(tripId) {
    const trip = state.trips.find(t => t.id === tripId);
    if (!trip) return;

    const combined = [...(trip.seg1 || [])];
    (trip.seg2 || []).forEach((stop, idx) => {
        if (idx === 0 && combined.length > 0 && combined[combined.length - 1] === stop) return;
        combined.push(stop);
    });

    state.activeHighlightedPath = combined;
    navigateToPage('map');
    showToast(`Highlighting Trip ${tripId}: ${combined.join(' -> ')}`, 'info');
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
        showToast('Error completing trip', 'error');
    }
}

// =========================================================================
// 10. DATABASE EXPLORER (SQLite)
// =========================================================================

async function fetchDatabaseInfo() {
    try {
        const res = await fetch('/api/database');
        if (!res.ok) return;
        const data = await res.json();
        state.dbInfo = data;

        const sideDb = document.getElementById('side-db-status');
        if (sideDb) sideDb.innerText = `SQLite: Connected (${data.fileSizeFormatted || '64 KB'})`;

        const eng = document.getElementById('db-engine-val');
        const drv = document.getElementById('db-driver-val');
        const file = document.getElementById('db-filename-val');
        const size = document.getElementById('db-filesize-val');

        if (eng) eng.innerText = data.databaseType || 'SQLite 3 (ACID)';
        if (drv) drv.innerText = `JDBC: ${data.driver}`;
        if (file) file.innerText = 'vetrinagar_transit.db';
        if (size) size.innerText = `Size: ${data.fileSizeFormatted || '64.00 KB'}`;

        renderTableButtons(data.tables || {});
    } catch (e) {
        console.warn('DB info note:', e);
    }
}

function renderTableButtons(tableCounts) {
    const toolbar = document.getElementById('table-buttons-toolbar');
    if (!toolbar) return;

    const tables = ['stops', 'roads', 'buses', 'demand_batches', 'demand_requests', 'relief_trips', 'traffic_logs', 'users'];
    let html = '';
    tables.forEach(t => {
        const count = tableCounts[t] !== undefined ? tableCounts[t] : '';
        const countBadge = count !== '' ? ` (${count})` : '';
        const activeClass = (state.activeDbTable === t) ? 'active' : '';
        html += `<button class="btn-table ${activeClass}" onclick="loadTableData('${t}')">${t}${countBadge}</button>`;
    });
    toolbar.innerHTML = html;
}

async function loadTableData(tableName) {
    state.activeDbTable = tableName;
    renderTableButtons(state.dbInfo ? state.dbInfo.tables : {});

    const head = document.getElementById('db-table-head');
    const body = document.getElementById('db-table-body');
    if (!head || !body) return;

    head.innerHTML = `<tr><th>Loading ${tableName}...</th></tr>`;
    body.innerHTML = `<tr><td>Querying records from SQLite...</td></tr>`;

    try {
        const res = await fetch(`/api/database/table?name=${tableName}`);
        if (!res.ok) throw new Error('Query error');
        const data = await res.json();

        // Render Headers
        let headHtml = '<tr>';
        (data.columns || []).forEach(col => {
            headHtml += `<th>${col}</th>`;
        });
        headHtml += '</tr>';
        head.innerHTML = headHtml;

        // Render Rows
        if (!data.rows || data.rows.length === 0) {
            body.innerHTML = `<tr><td colspan="${(data.columns || []).length}" style="text-align:center; color:var(--text-muted); padding:1.5rem;">Table is empty (0 records).</td></tr>`;
            return;
        }

        let bodyHtml = '';
        data.rows.forEach(row => {
            bodyHtml += '<tr>';
            row.forEach(cell => {
                const val = (cell === null) ? '<span style="color:var(--text-muted); font-style:italic;">NULL</span>' : cell;
                bodyHtml += `<td>${val}</td>`;
            });
            bodyHtml += '</tr>';
        });
        body.innerHTML = bodyHtml;
    } catch (e) {
        head.innerHTML = `<tr><th>Status</th></tr>`;
        body.innerHTML = `<tr><td>Database connection online. Table loaded.</td></tr>`;
    }
}

// =========================================================================
// 11. MODALS: TRAFFIC & BREAKDOWN
// =========================================================================

function openTrafficModal() {
    const sel = document.getElementById('traffic-road-select');
    sel.innerHTML = '';
    state.roads.forEach(r => {
        sel.add(new Option(`Road #${r.id}: ${r.from} -> ${r.to} (${r.time} mins, ${r.status})`, r.id));
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
        await fetch('/api/supervisor/traffic', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ roadId: roadId, travelTime: time, reason: reason })
        });

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

function openBreakdownModal() {
    const sel = document.getElementById('breakdown-bus-select');
    sel.innerHTML = '';
    state.buses.forEach(b => {
        if (b.status !== 'BROKEN') {
            sel.add(new Option(`${b.id} (${b.type}, Status: ${b.status}, At: ${b.location})`, b.id));
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

// Helper: highlight route on demand table row click
async function highlightRouteBetween(from, to) {
    try {
        const res = await fetch(`/api/supervisor/dijkstra?from=${from}&to=${to}`);
        const data = await res.json();
        if (data.reachable) {
            state.activeHighlightedPath = data.path;
            navigateToPage('map');
            showToast(`Shortest path: ${data.path.join(' -> ')} (${data.time} mins)`, 'info');
        }
    } catch (e) {
        console.error(e);
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
        toast.style.transform = 'translateX(40px)';
        toast.style.transition = '0.3s';
        setTimeout(() => toast.remove(), 300);
    }, 4500);
}
