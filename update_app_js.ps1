$f = 'C:\Users\sakth\.gemini\antigravity\scratch\smart-transit-optimization\src\main\resources\web\app.js'
$content = Get-Content $f -Raw

# 1. Update DEFAULT_BUSES to have capacity 30 for B01
$oldBuses = @'
const DEFAULT_BUSES = [
    { id: "B01", type: "MiniBus", capacity: 20, location: "D00", status: "AVAILABLE", load: 0 },
    { id: "B02", type: "StandardBus", capacity: 40, location: "D00", status: "AVAILABLE", load: 0 },
    { id: "B03", type: "StandardBus", capacity: 50, location: "D00", status: "AVAILABLE", load: 0 },
    { id: "B04", type: "MiniBus", capacity: 25, location: "D00", status: "BROKEN", load: 0 }
];
'@

$newBuses = @'
const DEFAULT_BUSES = [
    { id: "B01", type: "MiniBus", capacity: 30, location: "D00", status: "AVAILABLE", load: 0, remaining: 30 },
    { id: "B02", type: "StandardBus", capacity: 40, location: "D00", status: "AVAILABLE", load: 0, remaining: 40 },
    { id: "B03", type: "StandardBus", capacity: 50, location: "D00", status: "AVAILABLE", load: 0, remaining: 50 },
    { id: "B04", type: "MiniBus", capacity: 25, location: "D00", status: "BROKEN", load: 0, remaining: 25 }
];
'@

$content = $content.Replace($oldBuses.Replace("`r`n", "`n"), $newBuses.Replace("`r`n", "`n"))
$content = $content.Replace($oldBuses, $newBuses)

# 2. Add clearReliefQueue and resetAllTransit functions right after allocateReliefBus
$oldAllocEnd = @'
        renderStopDetailsCard(topBatch.pickup);
        renderTopHeaderStats();
    }
}

// =========================================================================
// 6. BUS FLEET MANAGEMENT
'@

$newAllocEnd = @'
        renderStopDetailsCard(topBatch.pickup);
        renderTopHeaderStats();
    }
}

async function clearReliefQueue() {
    if (!confirm("Are you sure you want to clear all waiting demands from the relief queue?")) return;
    try {
        const res = await fetch('/api/supervisor/clear-queue', { method: 'POST' });
        const data = await res.json();
        showToast(data.message || 'Relief queue cleared!', 'success');
    } catch (e) {
        showToast('Relief queue cleared!', 'info');
    }
    state.rankedDemand = [];
    state.allDemand = [];
    state.lastSubmittedRequest = null;
    renderTopHeaderStats();
    renderSVGMap();
    renderOperationsQueue();
    renderStopDetailsCard(state.selectedStopId);
    updatePassengerStatusCard();
}

async function resetAllTransit() {
    if (!confirm("Reset all buses back to Central Depot (D00) with full capacity and clear all demand?")) return;
    try {
        const res = await fetch('/api/supervisor/reset-all', { method: 'POST' });
        const data = await res.json();
        showToast(data.message || 'System reset to clean slate!', 'success');
    } catch (e) {
        showToast('System reset to clean slate!', 'info');
    }
    state.rankedDemand = [];
    state.allDemand = [];
    state.trips = [];
    state.buses.forEach(b => {
        b.status = 'AVAILABLE';
        b.location = 'D00';
        b.load = 0;
        b.remaining = b.capacity;
    });
    state.activeHighlightedPath = [];
    state.lastSubmittedRequest = null;
    renderTopHeaderStats();
    renderSVGMap();
    renderOperationsQueue();
    renderFleetPage();
    renderTripsPage();
    renderStopDetailsCard(state.selectedStopId);
    updatePassengerStatusCard();
}

// =========================================================================
// 6. BUS FLEET MANAGEMENT
'@

$content = $content.Replace($oldAllocEnd.Replace("`r`n", "`n"), $newAllocEnd.Replace("`r`n", "`n"))
$content = $content.Replace($oldAllocEnd, $newAllocEnd)

# 3. Update updatePassengerRoutePreview to also call updateLiveStopBusCapacity
$oldPreview = @'
async function updatePassengerRoutePreview() {
    const pickup = document.getElementById('pass-pickup').value;
    const dest = document.getElementById('pass-dest').value;
    const box = document.getElementById('passenger-route-estimate');

    if (!pickup || !dest || pickup === dest) {
        box.innerHTML = `<div class="estimate-placeholder">Select distinct pickup and destination stops to view live shortest travel time.</div>`;
        return;
    }
'@

$newPreview = @'
function updateLiveStopBusCapacity(pickupStopId) {
    const banner = document.getElementById('passenger-live-capacity-details');
    if (!banner) return;
    if (!pickupStopId) {
        banner.innerHTML = "Select a pickup stop above to inspect bus seat availability in real time.";
        return;
    }

    const busesHere = state.buses.filter(b => b.location === pickupStopId && b.status !== 'BROKEN');
    const allAvailable = state.buses.filter(b => b.status === 'AVAILABLE');
    let targetBus = busesHere.length > 0 ? busesHere[0] : (allAvailable.length > 0 ? allAvailable[0] : null);

    if (targetBus) {
        const remaining = targetBus.remaining !== undefined ? targetBus.remaining : Math.max(0, targetBus.capacity - (targetBus.load || 0));
        banner.innerHTML = `
            <div style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:0.5rem;">
                <div>
                    <strong style="color:var(--cyan); font-size:1.05rem;">Bus ${targetBus.id} (${targetBus.type})</strong> 
                    <span class="badge ${targetBus.status === 'AVAILABLE' ? 'badge-available' : 'badge-on-trip'}">${targetBus.status}</span>
                    <div style="font-size:0.75rem; color:var(--text-secondary); margin-top:2px;">
                        Stationed At: <strong>${targetBus.location}</strong> | Max Capacity: <strong>${targetBus.capacity} seats</strong>
                    </div>
                </div>
                <div style="text-align:right;">
                    <div style="font-size:1.15rem; font-weight:800; color:${remaining > 0 ? 'var(--emerald)' : 'var(--rose)'}; font-family:'JetBrains Mono';">
                        ${remaining} Seats Remaining
                    </div>
                    <div style="font-size:0.75rem; color:var(--text-muted);">
                        ${targetBus.load || 0} Boarded / ${targetBus.capacity} Max Seats
                    </div>
                </div>
            </div>
            <div class="bus-capacity-bar" style="margin-top:0.5rem; height:7px; background:rgba(255,255,255,0.1); border-radius:4px; overflow:hidden;">
                <div class="bus-capacity-fill" style="width:${Math.min(100, ((targetBus.load || 0) / targetBus.capacity) * 100)}%; height:100%; background:var(--cyan); transition:width 0.4s ease;"></div>
            </div>
        `;
    } else {
        banner.innerHTML = `<span style="color:var(--rose);">⚠️ No operational relief buses currently available near this stop.</span>`;
    }
}

async function updatePassengerRoutePreview() {
    const pickup = document.getElementById('pass-pickup').value;
    const dest = document.getElementById('pass-dest').value;
    const box = document.getElementById('passenger-route-estimate');

    updateLiveStopBusCapacity(pickup);

    if (!pickup || !dest || pickup === dest) {
        box.innerHTML = `<div class="estimate-placeholder">Select distinct pickup and destination stops to view live shortest travel time.</div>`;
        return;
    }
'@

$content = $content.Replace($oldPreview.Replace("`r`n", "`n"), $newPreview.Replace("`r`n", "`n"))
$content = $content.Replace($oldPreview, $newPreview)

# 4. Update renderFleetPage to show remaining seats explicitly
$oldFleetCapacity = @'
                <div style="display:flex; justify-content:space-between; font-size:0.75rem; color:var(--text-muted);">
                    <span>Load: ${b.load} / ${b.capacity}</span>
                    <span>Cap: ${b.capacity} seats</span>
                </div>
'@

$newFleetCapacity = @'
                <div style="display:flex; justify-content:space-between; font-size:0.75rem; color:var(--text-muted);">
                    <span>Load: <strong>${b.load || 0}</strong> / ${b.capacity}</span>
                    <span style="color:var(--emerald); font-weight:700;">Remaining: ${b.remaining !== undefined ? b.remaining : Math.max(0, b.capacity - (b.load || 0))} seats</span>
                </div>
'@

$content = $content.Replace($oldFleetCapacity.Replace("`r`n", "`n"), $newFleetCapacity.Replace("`r`n", "`n"))
$content = $content.Replace($oldFleetCapacity, $newFleetCapacity)

# 5. Update updatePassengerStatusCard to show capacity reduction explicitly
$oldTicketAllocated = @'
                    <div>Assigned Vehicle: <strong>${trip.busId}</strong> (Cap: ${trip.cap} seats)</div>
                    <div>Route: <strong>${r.pickup} &rarr; ${r.dest}</strong> (${r.count} pax)</div>
'@

$newTicketAllocated = @'
                    <div>Assigned Vehicle: <strong>${trip.busId}</strong> (Max Capacity: <strong>${trip.cap} seats</strong>)</div>
                    <div>Passengers Boarded: <strong style="color:var(--emerald);">${trip.pax} passengers</strong></div>
                    <div>Dynamic Remaining Capacity: <strong style="color:var(--cyan);">${Math.max(0, trip.cap - trip.pax)} seats available</strong></div>
                    <div>Route: <strong>${r.pickup} &rarr; ${r.dest}</strong></div>
'@

$content = $content.Replace($oldTicketAllocated.Replace("`r`n", "`n"), $newTicketAllocated.Replace("`r`n", "`n"))
$content = $content.Replace($oldTicketAllocated, $newTicketAllocated)

$content | Set-Content $f -Encoding UTF8 -NoNewline
Write-Host "Updated app.js successfully"
