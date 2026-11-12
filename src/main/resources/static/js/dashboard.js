// AeroSaga Mission Control JavaScript Engine with Role-Based Authentication & Fleet Reporting

let map;
let droneMarker;
let baseMarker;
let flightPathPolyline;
let activeMissionId = null;
let currentDroneId = "DRONE-ALPHA-01";
let pollingTimer = null;
let lastKnownPhase = "";

// User Authentication State (Default: ADMIN)
let currentUser = {
    username: "admin",
    role: "ADMIN",
    displayName: "Commander / Flight Director"
};

const BASE_COORDS = [12.9716, 80.2446];

document.addEventListener("DOMContentLoaded", () => {
    try { initMap(); } catch(e) { console.warn("Map initialization warning:", e); }
    try { initClock(); } catch(e) { console.warn("Clock error:", e); }
    try { fetchSummary(); } catch(e) { console.warn("Summary fetch error:", e); }
    try { fetchScenarios(); } catch(e) { console.warn("Scenarios fetch error:", e); }
    try { applyRolePermissions(); } catch(e) { console.warn("Role permissions error:", e); }
    
    // Poll summary and telemetry every 3 seconds
    setInterval(fetchSummary, 3000);
});

function initClock() {
    setInterval(() => {
        const now = new Date();
        const clockEl = document.getElementById("liveClock");
        if (clockEl) {
            clockEl.innerText = now.toUTCString().slice(17, 25) + " UTC";
        }
    }, 1000);
}

function initMap() {
    if (typeof L === 'undefined') {
        console.warn("Leaflet library not yet loaded. Rendering tactical fallback display.");
        const mapEl = document.getElementById('mapContainer');
        if (mapEl) {
            mapEl.innerHTML = '<div style="display:flex;height:100%;align-items:center;justify-content:center;color:#38bdf8;background:#0f172a;flex-direction:column;gap:10px;"><h3>🛰️ Radar Sector Active</h3><p style="color:#94a3b8;font-size:0.85rem;">Base Coordinates: 12.9716° N, 80.2446° E</p></div>';
        }
        return;
    }

    // Dark mode tile layer
    map = L.map('mapContainer').setView(BASE_COORDS, 14);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; OpenStreetMap contributors',
        maxZoom: 18
    }).addTo(map);

    // Custom Base Station Icon
    const baseIcon = L.divIcon({
        className: 'base-marker',
        html: '<div style="background:#2563eb; color:white; border-radius:50%; width:32px; height:32px; display:flex; align-items:center; justify-content:center; font-size:16px; border:2px solid white; box-shadow:0 0 10px rgba(37,99,235,0.8);">🏠</div>',
        iconSize: [32, 32],
        iconAnchor: [16, 16]
    });
    baseMarker = L.marker(BASE_COORDS, { icon: baseIcon }).addTo(map)
        .bindPopup("<b>AeroSaga Base Station Alpha</b><br>Primary Launch/Recovery Helipad");

    // Custom Drone Icon
    const droneIcon = L.divIcon({
        className: 'drone-marker',
        html: '<div id="droneVisualIcon" style="background:#06b6d4; color:white; border-radius:50%; width:36px; height:36px; display:flex; align-items:center; justify-content:center; font-size:20px; border:2px solid #ffffff; box-shadow:0 0 14px rgba(6,182,212,0.9); transition: all 0.5s ease;">🚁</div>',
        iconSize: [36, 36],
        iconAnchor: [18, 18]
    });
    droneMarker = L.marker(BASE_COORDS, { icon: droneIcon }).addTo(map)
        .bindPopup("<b>Drone Active Unit</b><br>Status: Standby");

    flightPathPolyline = L.polyline([BASE_COORDS], {
        color: '#38bdf8',
        weight: 3,
        opacity: 0.85,
        dashArray: '6, 6'
    }).addTo(map);
}

function renderRestrictedZones(zones) {
    if (!zones || !map) return;
    zones.forEach(zone => {
        L.circle([zone.centerLat, zone.centerLon], {
            color: '#f43f5e',
            fillColor: '#f43f5e',
            fillOpacity: 0.2,
            radius: zone.radiusKm * 1000
        }).addTo(map).bindPopup("<b>RESTRICTED AIRSPACE:</b> " + zone.name + "<br>Radius: " + zone.radiusKm + " km");
    });
}

function fetchSummary() {
    fetch('/api/dashboard/summary')
        .then(res => res.json())
        .then(data => {
            const total = data.totalDrones || 0;
            const active = data.activeDrones || 0;
            const idle = data.idleDrones || 0;
            const maint = data.maintenanceDrones || 0;

            document.getElementById("kpiFleet").innerText = total + " Units";
            document.getElementById("kpiActive").innerText = active + " Airborne";
            document.getElementById("kpiInactive").innerText = idle + " Standby";
            document.getElementById("kpiCompleted").innerText = data.completedMissions || 0;
            document.getElementById("kpiCompensated").innerText = (data.compensatedMissions || 0) + " Recovered";

            // Update top header status pill
            const statusPill = document.getElementById("headerDroneStatusPill");
            if (statusPill) {
                statusPill.innerHTML = `Drones: <b style="color:#34d399">${active} Active</b> | <b>${idle} Standby</b>${maint > 0 ? ` | <span style="color:#f87171">${maint} Maint</span>` : ''}`;
            }

            if (data.restrictedZones && data.restrictedZones.length > 0 && !window.zonesRendered && map) {
                renderRestrictedZones(data.restrictedZones);
                window.zonesRendered = true;
            }
        })
        .catch(err => console.error("Error fetching summary:", err));
}

let loadedScenarios = [];
function fetchScenarios() {
    fetch('/api/dashboard/scenarios')
        .then(res => res.json())
        .then(scenarios => {
            loadedScenarios = scenarios;
            const container = document.getElementById("scenarioButtons");
            if (!container) return;
            container.innerHTML = "";
            scenarios.forEach((sc, idx) => {
                const btn = document.createElement("button");
                btn.className = (idx === 2 ? "btn btn-warning" : (idx === 3 ? "btn btn-danger" : "btn btn-primary")) + " admin-action";
                btn.innerText = sc.title;
                btn.title = sc.description;
                btn.onclick = () => launchPresetScenario(idx);
                container.appendChild(btn);
            });
            applyRolePermissions();
        })
        .catch(err => console.error("Error fetching scenarios:", err));
}

// =========================================================
// ROLE-BASED AUTHENTICATION & ACCESS CONTROL
// =========================================================

function applyRolePermissions() {
    const roleBadge = document.getElementById("userRoleBadge");
    const restrictedBanner = document.getElementById("roleRestrictedBanner");
    const adminActionButtons = document.querySelectorAll(".admin-action");

    if (currentUser.role === "ADMIN") {
        if (roleBadge) {
            roleBadge.className = "badge-role badge-admin";
            roleBadge.innerText = "👑 ADMIN: " + currentUser.username.toUpperCase();
        }
        if (restrictedBanner) restrictedBanner.style.display = "none";
        adminActionButtons.forEach(btn => {
            btn.style.opacity = "1";
            btn.style.cursor = "pointer";
        });
    } else {
        if (roleBadge) {
            roleBadge.className = "badge-role badge-user";
            roleBadge.innerText = "👤 USER (Observer): " + currentUser.username.toUpperCase();
        }
        if (restrictedBanner) restrictedBanner.style.display = "flex";
        adminActionButtons.forEach(btn => {
            btn.style.opacity = "0.45";
            btn.style.cursor = "not-allowed";
        });
    }
}

function verifyAdminAccess(actionName) {
    if (currentUser.role !== "ADMIN") {
        alert(`🔒 Permission Denied: You are currently signed in as "${currentUser.username}" (Observer Role).\n\nAction "${actionName}" requires Administrator (Flight Director) permissions.\n\nPlease click "Switch / Login" in the top bar and sign in as Administrator.`);
        openLoginModal();
        return false;
    }
    return true;
}

function openLoginModal() {
    document.getElementById("loginModal").classList.add("active");
    const feedback = document.getElementById("loginFeedback");
    if (feedback) feedback.style.display = "none";
}

function closeLoginModal() {
    document.getElementById("loginModal").classList.remove("active");
}

function quickLogin(username, password) {
    document.getElementById("loginUsername").value = username;
    document.getElementById("loginPassword").value = password;
    submitLogin(username, password);
}

function handleLoginFormSubmit(e) {
    e.preventDefault();
    const u = document.getElementById("loginUsername").value.trim();
    const p = document.getElementById("loginPassword").value.trim();
    submitLogin(u, p);
}

function submitLogin(username, password) {
    fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: username, password: password })
    })
    .then(async res => {
        const data = await res.json();
        const feedback = document.getElementById("loginFeedback");
        if (res.ok && data.success) {
            currentUser = {
                username: data.username,
                role: data.role,
                displayName: data.displayName
            };
            applyRolePermissions();
            logEvent(`User signed in: ${data.displayName} (${data.role})`, "success");
            closeLoginModal();
            alert(`✅ Successfully authenticated as ${data.displayName} (${data.role})!`);
        } else {
            feedback.style.display = "block";
            feedback.style.color = "var(--accent-rose)";
            feedback.innerText = "❌ " + (data.message || "Invalid credentials.");
        }
    })
    .catch(err => {
        alert("Authentication error: " + err.message);
    });
}

// =========================================================
// FLEET STATUS & ACTIVE VS INACTIVE DRONES REPORT
// =========================================================

function openFleetReportModal() {
    document.getElementById("fleetReportModal").classList.add("active");
    fetchFleetReport();
}

function closeFleetReportModal() {
    document.getElementById("fleetReportModal").classList.remove("active");
}

function fetchFleetReport() {
    fetch('/api/dashboard/fleet-report')
        .then(res => res.json())
        .then(data => {
            renderFleetReport(data);
        })
        .catch(err => console.error("Error fetching fleet report:", err));
}

function renderFleetReport(data) {
    document.getElementById("modalTotalUnits").innerText = (data.totalDrones || 0) + " Units";
    document.getElementById("modalActiveUnits").innerText = (data.activeDronesCount || 0) + " Airborne";
    document.getElementById("modalInactiveUnits").innerText = (data.inactiveDronesCount || 0) + " Standby";
    document.getElementById("modalMaintUnits").innerText = (data.maintenanceDronesCount || 0) + " Maintenance";

    const readiness = data.operationalRatePercent !== undefined ? data.operationalRatePercent : 100;
    const avgBat = data.averageBatteryPercentage !== undefined ? data.averageBatteryPercentage : 100;

    document.getElementById("modalReadinessPercent").innerText = readiness + "% Ready";
    document.getElementById("modalAvgBattery").innerText = avgBat + "%";
    document.getElementById("modalHealthBar").style.width = readiness + "%";

    const tbody = document.getElementById("droneReportTableBody");
    tbody.innerHTML = "";

    const allDrones = [
        ...(data.activeDrones || []),
        ...(data.inactiveDrones || []),
        ...(data.maintenanceDrones || [])
    ];

    if (allDrones.length === 0) {
        tbody.innerHTML = `<tr><td colspan="6" style="text-align:center;color:var(--text-muted);">No drones currently registered in fleet.</td></tr>`;
        return;
    }

    allDrones.forEach(d => {
        const row = document.createElement("tr");

        let statusBadge = `<span class="status-indicator"><span class="dot-idle"></span> Standby</span>`;
        if (d.status === "AIRBORNE" || d.status === "HOVERING" || d.status === "PREFLIGHT" || d.status === "RETURNING_HOME") {
            statusBadge = `<span class="status-indicator"><span class="dot-active"></span> <b style="color:var(--accent-green);">${d.status}</b></span>`;
        } else if (d.status === "MAINTENANCE_REQUIRED" || d.status === "EMERGENCY_LANDING") {
            statusBadge = `<span class="status-indicator"><span class="dot-maint"></span> <b style="color:var(--accent-rose);">${d.status}</b></span>`;
        }

        const batColor = d.batteryPercentage < 30 ? "var(--accent-rose)" : (d.batteryPercentage < 60 ? "var(--accent-amber)" : "var(--accent-green)");

        row.innerHTML = `
            <td><b>${d.id}</b></td>
            <td>${d.model}</td>
            <td>${statusBadge}</td>
            <td><b style="color:${batColor}">${d.batteryPercentage}%</b></td>
            <td>${d.maxPayloadKg} kg</td>
            <td>
                <button class="btn btn-secondary" onclick="rechargeSingleDrone('${d.id}')" style="padding:3px 8px;font-size:0.75rem;">⚡ Recharge</button>
            </td>
        `;
        tbody.appendChild(row);
    });
}

function rechargeSingleDrone(droneId) {
    if (!verifyAdminAccess("Recharge Drone")) return;
    fetch(`/api/drones/${droneId}/recharge`, { method: 'POST' })
        .then(() => {
            logEvent(`Drone ${droneId} recharged and status reset to IDLE.`, "success");
            fetchFleetReport();
            fetchSummary();
        })
        .catch(err => alert("Recharge failed: " + err.message));
}

// =========================================================
// MISSION DISPATCH & TEMPORAL EXECUTION
// =========================================================

async function launchPresetScenario(index) {
    if (!verifyAdminAccess("Launch Drone Mission")) return;

    const sc = loadedScenarios[index];
    if (!sc) return;

    logEvent("Preparing " + sc.title + " on " + sc.droneId + "...", "info");
    currentDroneId = sc.droneId;

    // Proactively recharge/clear drone state so preset launches smoothly
    try {
        await fetch('/api/drones/' + sc.droneId + '/recharge', { method: 'POST' });
    } catch (e) {
        // non-blocking
    }

    const requestPayload = {
        droneId: sc.droneId,
        missionType: sc.missionType,
        payloadWeightKg: sc.payloadWeightKg,
        simulateSevereWeather: sc.simulateSevereWeather,
        simulateMotorFailure: sc.simulateMotorFailure,
        waypoints: sc.waypoints
    };

    fetch('/api/missions/launch', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(requestPayload)
    })
    .then(async res => {
        if (!res.ok) {
            const err = await res.text();
            throw new Error(err);
        }
        return res.json();
    })
    .then(mission => {
        activeMissionId = mission.missionId;
        const badgeEl = document.getElementById("activeMissionBadge");
        if (badgeEl) badgeEl.innerText = "MISSION: " + activeMissionId;
        logEvent("Temporal workflow instance started: " + mission.workflowId, "success");

        // Clear flight path polyline and reset if map is active
        if (flightPathPolyline) {
            flightPathPolyline.setLatLngs([BASE_COORDS]);
        }

        // Draw intended flight plan
        if (map && sc.waypoints) {
            const planCoords = sc.waypoints.map(w => [w.latitude, w.longitude]);
            L.polyline(planCoords, { color: '#94a3b8', weight: 2, dashArray: '4, 4', opacity: 0.5 }).addTo(map);
        }

        if (pollingTimer) clearInterval(pollingTimer);
        pollingTimer = setInterval(pollMissionState, 900);
        fetchSummary();
    })
    .catch(err => {
        logEvent("Launch failed: " + err.message, "error");
        alert("Mission launch error: " + err.message);
    });
}

function pollMissionState() {
    if (!activeMissionId) return;

    fetch('/api/missions/' + activeMissionId)
        .then(res => res.json())
        .then(data => {
            const phase = data.currentPhase || "In Progress";
            const liveStatus = data.liveStatus || (data.mission ? data.mission.status : "PENDING");
            document.getElementById("liveWorkflowPhase").innerText = phase;

            if (phase !== lastKnownPhase) {
                lastKnownPhase = phase;
                logEvent("Workflow Phase: " + phase, "info");
            }

            if (data.telemetry) {
                updateTelemetryUI(data.telemetry);
            }

            // Check completion or compensation
            if (liveStatus === "COMPLETED") {
                logEvent("Mission " + activeMissionId + " completed successfully! Drone recovered.", "success");
                clearInterval(pollingTimer);
                pollingTimer = null;
                fetchSummary();
            } else if (liveStatus === "ABORTED_SAFE_LANDING" || liveStatus === "FAILED") {
                logEvent("Mission aborted. Temporal Saga compensation executed successfully.", "warn");
                clearInterval(pollingTimer);
                pollingTimer = null;
                fetchSummary();
            }
        })
        .catch(err => console.error("Error polling state:", err));
}

function updateTelemetryUI(t) {
    document.getElementById("telAltitude").innerText = (t.altitudeMeters || 0).toFixed(1) + " m";
    document.getElementById("telSpeed").innerText = (t.speedMps || 0).toFixed(1) + " m/s";
    document.getElementById("telHeading").innerText = (t.headingDegrees || 0).toFixed(0) + "°";
    document.getElementById("telSatellites").innerText = (t.satelliteCount || 16) + " Locked";
    document.getElementById("telWaypoint").innerText = t.currentWaypoint || "In Transit";

    const bat = t.batteryPercentage !== undefined ? t.batteryPercentage : 100;
    const batEl = document.getElementById("telBattery");
    batEl.innerText = bat.toFixed(1) + "%";
    document.getElementById("batteryBar").style.width = bat + "%";

    if (bat < 25) {
        batEl.style.color = "var(--accent-rose)";
        document.getElementById("batteryBar").style.background = "var(--accent-rose)";
    } else if (bat < 50) {
        batEl.style.color = "var(--accent-amber)";
        document.getElementById("batteryBar").style.background = "var(--accent-amber)";
    } else {
        batEl.style.color = "var(--accent-green)";
        document.getElementById("batteryBar").style.background = "var(--accent-green)";
    }

    if (t.latitude && t.longitude && map && droneMarker) {
        const newLatLng = [t.latitude, t.longitude];
        droneMarker.setLatLng(newLatLng);
        if (flightPathPolyline) flightPathPolyline.addLatLng(newLatLng);
    }
}

function sendPauseSignal(seconds) {
    if (!verifyAdminAccess("Hover In-Place Signal")) return;
    if (!activeMissionId) {
        alert("No active mission running to pause.");
        return;
    }
    logEvent("Dispatching Temporal Signal: Pause/Hover (" + seconds + "s)...", "warn");
    fetch('/api/missions/' + activeMissionId + '/signal/pause?seconds=' + seconds, { method: 'POST' })
        .then(res => res.json())
        .then(d => logEvent("Signal Acknowledged: " + d.message, "info"))
        .catch(err => logEvent("Error signaling: " + err, "error"));
}

function sendResumeSignal() {
    if (!verifyAdminAccess("Resume Flight Signal")) return;
    if (!activeMissionId) {
        alert("No active mission running to resume.");
        return;
    }
    logEvent("Dispatching Temporal Signal: Resume Flight...", "info");
    fetch('/api/missions/' + activeMissionId + '/signal/resume', { method: 'POST' })
        .then(res => res.json())
        .then(d => logEvent("Signal Acknowledged: " + d.message, "info"))
        .catch(err => logEvent("Error signaling: " + err, "error"));
}

function sendAbortSignal() {
    if (!verifyAdminAccess("Emergency Abort Signal")) return;
    if (!activeMissionId) {
        alert("No active mission running to abort.");
        return;
    }
    if (!confirm("Confirm immediate fail-safe abort and Temporal Saga compensation rollback?")) return;

    logEvent("Dispatching Temporal Signal: EMERGENCY ABORT...", "error");
    fetch('/api/missions/' + activeMissionId + '/signal/abort?reason=Manual%20abort%20from%20Flight%20Director', { method: 'POST' })
        .then(res => res.json())
        .then(d => logEvent("Signal Acknowledged: " + d.message, "warn"))
        .catch(err => logEvent("Error signaling abort: " + err, "error"));
}

function rechargeAllDrones() {
    if (!verifyAdminAccess("Recharge Fleet")) return;
    logEvent("Recharging all fleet assets to 100%...", "info");
    fetch('/api/drones')
        .then(res => res.json())
        .then(drones => {
            Promise.all(drones.map(d => fetch('/api/drones/' + d.id + '/recharge', { method: 'POST' })))
                .then(() => {
                    logEvent("All drone batteries topped to 100%. Ready for flight.", "success");
                    fetchSummary();
                });
        });
}

function logEvent(msg, type = "info") {
    const container = document.getElementById("eventLogs");
    if (!container) return;
    const now = new Date();
    const timeStr = now.toTimeString().slice(0, 8);

    const entry = document.createElement("div");
    entry.className = "log-entry";

    let cls = "log-msg-info";
    if (type === "warn") cls = "log-msg-warn";
    if (type === "error") cls = "log-msg-error";
    if (type === "success") cls = "log-msg-success";

    entry.innerHTML = `<span class="log-time">[${timeStr}]</span> <span class="${cls}">${msg}</span>`;
    container.appendChild(entry);
    container.scrollTop = container.scrollHeight;
}

function clearLogs() {
    const container = document.getElementById("eventLogs");
    if (container) container.innerHTML = "";
}
