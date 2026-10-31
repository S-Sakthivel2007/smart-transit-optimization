$f = 'C:\Users\sakth\.gemini\antigravity\scratch\smart-transit-optimization\src\main\resources\web\index.html'
$content = Get-Content $f -Raw

$oldToolbar = @'
                    <div class="operations-toolbar">
                        <button class="btn btn-accent" onclick="allocateReliefBus()">
                            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <polygon points="5 3 19 12 5 21 5 3"></polygon>
                            </svg>
                            Allocate Relief Bus to #1 Surge Batch
                        </button>
                        <div class="policy-pill">
                            Policy: <strong>Passenger Count (DESC)</strong> &rarr; <strong>Arrival Time (ASC)</strong>
                        </div>
                    </div>
'@

$newToolbar = @'
                    <div class="operations-toolbar">
                        <div class="btn-group-left" style="display:flex; gap:0.5rem; align-items:center; flex-wrap:wrap;">
                            <button class="btn btn-accent" onclick="allocateReliefBus()">
                                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                    <polygon points="5 3 19 12 5 21 5 3"></polygon>
                                </svg>
                                Allocate Relief Bus to #1 Surge Batch
                            </button>
                            <button class="btn btn-outline" onclick="clearReliefQueue()" style="border:1px solid var(--rose); color:var(--rose);">
                                🗑️ Clear Relief Queue
                            </button>
                            <button class="btn btn-outline" onclick="resetAllTransit()" title="Reset all buses to D00 with full capacity and clear all demand">
                                🔄 Reset Fleet &amp; Queue
                            </button>
                        </div>
                        <div class="policy-pill">
                            Policy: <strong>Passenger Count (DESC)</strong> &rarr; <strong>Arrival Time (ASC)</strong>
                        </div>
                    </div>
'@

$oldPassengerCard = @'
                                <div id="passenger-route-estimate" class="route-estimate-box">
                                    <div class="estimate-placeholder">
                                        Select pickup and destination stops to view live Dijkstra shortest path and travel duration.
                                    </div>
                                </div>
'@

$newPassengerCard = @'
                                <div class="dynamic-capacity-banner" id="passenger-dynamic-capacity-banner" style="margin-bottom:1rem; padding:0.75rem 1rem; background:rgba(6,182,212,0.1); border:1px solid rgba(6,182,212,0.3); border-radius:8px;">
                                    <div style="font-size:0.75rem; color:var(--cyan); font-weight:700; text-transform:uppercase; letter-spacing:0.5px;">🚌 Dynamic Bus Capacity Status at Selected Stop:</div>
                                    <div id="passenger-live-capacity-details" style="font-size:0.92rem; margin-top:0.35rem; color:var(--text-primary);">
                                        Select a pickup stop above to inspect bus seat availability in real time.
                                    </div>
                                </div>

                                <div id="passenger-route-estimate" class="route-estimate-box">
                                    <div class="estimate-placeholder">
                                        Select pickup and destination stops to view live Dijkstra shortest path and travel duration.
                                    </div>
                                </div>
'@

$content = $content.Replace($oldToolbar.Replace("`r`n", "`n"), $newToolbar.Replace("`r`n", "`n"))
$content = $content.Replace($oldToolbar, $newToolbar)

$content = $content.Replace($oldPassengerCard.Replace("`r`n", "`n"), $newPassengerCard.Replace("`r`n", "`n"))
$content = $content.Replace($oldPassengerCard, $newPassengerCard)

$content | Set-Content $f -Encoding UTF8 -NoNewline
Write-Host "Updated index.html successfully"
