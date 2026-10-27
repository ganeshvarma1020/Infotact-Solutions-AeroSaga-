// AeroSaga dashboard. Plain JavaScript, no build step.
// Talks only to our own API (same origin). The browser handles the login prompt (HTTP Basic).
'use strict';

const POLL_MS = 1500;
const state = { csrf: null, config: null, draft: [], selected: null, layers: {} };

// ---------- helpers ----------
const $ = (id) => document.getElementById(id);

function esc(s) {
  return String(s ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

async function api(path, options = {}) {
  const opts = { credentials: 'same-origin', ...options, headers: { ...(options.headers || {}) } };
  if (!opts.headers['Authorization']) {
    opts.headers['Authorization'] = 'Basic ' + btoa('operator:local-dev-only');
  }
  if (opts.method && opts.method !== 'GET' && state.csrf) {
    opts.headers[state.csrf.headerName] = state.csrf.token;
  }
  if (opts.body && typeof opts.body !== 'string') {
    opts.headers['Content-Type'] = 'application/json';
    opts.body = JSON.stringify(opts.body);
  }
  const res = await fetch(path, opts);
  if (res.status === 204) return null;
  const text = await res.text();
  const data = text ? JSON.parse(text) : null;
  if (!res.ok) {
    const err = new Error((data && data.detail) || res.statusText);
    err.errors = (data && data.errors) || [];
    err.status = res.status;
    throw err;
  }
  return data;
}

function setConn(ok, text) {
  const c = $('conn');
  c.textContent = text;
  c.className = 'chip ' + (ok ? 'chip-ok' : 'chip-bad');
}

const time = (ms) => new Date(ms).toLocaleTimeString();

// ---------- map ----------
if (L && L.Icon && L.Icon.Default) {
  delete L.Icon.Default.prototype._getIconUrl;
  L.Icon.Default.mergeOptions({
    iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
    iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
    shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
  });
}
const map = L.map('map');
L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
  maxZoom: 18,
  attribution: '&copy; OpenStreetMap contributors',
}).addTo(map);
state.layers.draft = L.layerGroup().addTo(map);
state.layers.mission = L.layerGroup().addTo(map);
state.layers.zones = L.layerGroup().addTo(map);

function drawStatic(cfg) {
  map.setView([cfg.base.lat, cfg.base.lon], 12);
  L.circle([cfg.operatingCenter.lat, cfg.operatingCenter.lon], {
    radius: cfg.operatingRadiusKm * 1000, color: '#3fa7ff', weight: 1, fill: false, dashArray: '6 6',
  }).addTo(map).bindTooltip('Operating area');
  cfg.noFlyZones.forEach((z) => L.circle([z.center.lat, z.center.lon], {
    radius: z.radiusKm * 1000, color: '#f85149', fillOpacity: 0.15,
  }).addTo(map).bindTooltip('No-fly: ' + esc(z.name)));
  L.marker([cfg.base.lat, cfg.base.lon]).addTo(map).bindTooltip('Base');
}

map.on('click', (e) => {
  if (state.draft.length >= 10) return;
  state.draft.push({ name: 'WP' + (state.draft.length + 1), lat: +e.latlng.lat.toFixed(5), lon: +e.latlng.lng.toFixed(5) });
  renderDraft();
});

function renderDraft() {
  const list = $('draft');
  list.innerHTML = '';
  state.layers.draft.clearLayers();
  const pts = [[state.config.base.lat, state.config.base.lon]];
  state.draft.forEach((w, i) => {
    const li = document.createElement('li');
    li.innerHTML = `${esc(w.name)} <small class="muted">${w.lat}, ${w.lon}</small>`;
    const rm = document.createElement('button');
    rm.textContent = 'x';
    rm.title = 'Remove';
    rm.onclick = () => { state.draft.splice(i, 1); renderDraft(); };
    li.appendChild(rm);
    list.appendChild(li);
    pts.push([w.lat, w.lon]);
    L.circleMarker([w.lat, w.lon], { radius: 6, color: '#3fa7ff' }).addTo(state.layers.draft).bindTooltip(w.name);
  });
  if (state.draft.length) {
    pts.push(pts[0]);
    L.polyline(pts, { color: '#3fa7ff', dashArray: '4 6', weight: 2 }).addTo(state.layers.draft);
  }
}

// ---------- planner ----------
$('demo').onclick = () => {
  state.draft = [
    { name: 'Bommasandra', lat: 12.8169, lon: 77.6950 },
    { name: 'Hosur Road Hub', lat: 12.8700, lon: 77.6400 },
  ];
  renderDraft();
};
$('clear').onclick = () => { state.draft = []; renderDraft(); };

$('launch').onclick = async () => {
  const errors = $('formErrors');
  errors.innerHTML = '';
  $('launch').disabled = true;
  try {
    const view = await api('/api/missions', {
      method: 'POST',
      body: {
        operator: $('operator').value,
        payloadKg: parseFloat($('payload').value),
        scenario: $('scenario').value,
        waypoints: state.draft,
      },
    });
    state.selected = view.missionId;
    state.draft = [];
    renderDraft();
    await refresh();
  } catch (e) {
    [e.message, ...e.errors].forEach((m) => {
      const li = document.createElement('li');
      li.textContent = m;
      errors.appendChild(li);
    });
  } finally {
    $('launch').disabled = false;
  }
};

$('abort').onclick = async () => {
  if (!state.selected || !confirm('Abort mission ' + state.selected + '? Finished steps will be undone.')) return;
  try {
    await api('/api/missions/' + encodeURIComponent(state.selected) + '/abort', { method: 'POST', body: { reason: 'Aborted from dashboard' } });
  } catch (e) {
    alert(e.message);
  }
};

// ---------- rendering ----------
function renderMissions(list) {
  const ul = $('missions');
  ul.innerHTML = list.length ? '' : '<li class="muted">No missions yet</li>';
  list.slice(0, 15).forEach((m) => {
    const li = document.createElement('li');
    if (m.id === state.selected) li.classList.add('selected');
    li.innerHTML = `<span class="id">${esc(m.id)}</span><span class="pill ${esc(m.phase)}">${esc(m.phase)}</span>`;
    li.onclick = () => { state.selected = m.id; refresh(); };
    ul.appendChild(li);
  });
}

function renderDetail(v) {
  $('detail').classList.remove('hidden');
  $('dTitle').textContent = v.missionId;
  $('dPhase').textContent = v.phase;
  $('dPhase').className = 'pill ' + v.phase;
  $('dDrone').textContent = v.droneId || '-';
  $('dLegs').textContent = `${v.legsCompleted}/${v.legsTotal}`;
  $('dKm').textContent = (v.distanceFlownKm || 0).toFixed(2) + ' km';
  const b = v.batteryPct;
  $('dBattery').style.width = (b == null ? 0 : b) + '%';
  $('dBattery').style.background = b == null ? 'transparent' : b < 25 ? 'var(--bad)' : b < 50 ? 'var(--warn)' : 'var(--ok)';
  $('dBatteryText').textContent = b == null ? 'Battery -' : 'Battery ' + b.toFixed(1) + '%';
  const fail = $('dFailure');
  if (v.failureCode) {
    fail.textContent = v.failureCode + ': ' + (v.failureReason || '');
    fail.classList.remove('hidden');
  } else {
    fail.classList.add('hidden');
  }
  const finalPhases = ['COMPLETED', 'ROLLED_BACK', 'ABORTED', 'REJECTED', 'COMPENSATION_FAILED', 'FAILED_TO_START'];
  $('abort').disabled = finalPhases.includes(v.phase) || ['DELIVERING', 'RETURNING', 'COMPENSATING'].includes(v.phase) || v.abortRequested;

  const tl = $('timeline');
  tl.innerHTML = '';
  (v.timeline || []).forEach((t) => {
    const li = document.createElement('li');
    if (t.message.startsWith('UNDO')) li.classList.add('undo');
    li.innerHTML = `<span class="t">${time(t.atEpochMillis)}</span>${esc(t.message)}`;
    tl.appendChild(li);
  });
  tl.scrollTop = tl.scrollHeight;

  state.layers.mission.clearLayers();
  if (v.route && v.route.length) {
    const pts = v.route.map((w) => [w.position.lat, w.position.lon]);
    pts.push(pts[0]);
    L.polyline(pts, { color: '#8b98a5', weight: 2 }).addTo(state.layers.mission);
    v.route.slice(1).forEach((w) => L.circleMarker([w.position.lat, w.position.lon], { radius: 5, color: '#8b98a5' })
      .addTo(state.layers.mission).bindTooltip(w.name));
  }
  if (v.position) {
    L.circleMarker([v.position.lat, v.position.lon], { radius: 9, color: '#3fb950', fillOpacity: 0.9 })
      .addTo(state.layers.mission).bindTooltip((v.droneId || 'drone') + ' (' + v.phase + ')');
  }
}

function renderFleet(f) {
  $('slots').textContent = `Slots: ${f.slotsInUse}/${f.slotCapacity}`;
  const ul = $('fleet');
  ul.innerHTML = '';
  f.drones.forEach((d) => {
    const li = document.createElement('li');
    li.innerHTML = `<span><b>${esc(d.droneId)}</b> <small class="muted">${esc(d.model)}</small><br>
      <small>${esc(d.status)} - ${d.batteryPct.toFixed(1)}%${d.missionId ? ' - ' + esc(d.missionId) : ''}</small></span>`;
    const btn = document.createElement('button');
    btn.textContent = 'Recharge';
    btn.disabled = d.status !== 'AVAILABLE' || d.batteryPct >= 100;
    btn.onclick = async () => {
      try { await api('/api/fleet/' + encodeURIComponent(d.droneId) + '/recharge', { method: 'POST' }); refresh(); } catch (e) { alert(e.message); }
    };
    li.appendChild(btn);
    ul.appendChild(li);
  });

  const zl = $('zones');
  zl.innerHTML = f.temporaryZones.length ? '' : '<li class="muted">None</li>';
  state.layers.zones.clearLayers();
  f.temporaryZones.forEach((z) => {
    const li = document.createElement('li');
    li.innerHTML = `<span>${esc(z.name)} <small class="muted">${z.radiusKm} km</small></span>`;
    const btn = document.createElement('button');
    btn.textContent = 'Lift';
    btn.onclick = async () => {
      try { await api('/api/airspace/zones/' + encodeURIComponent(z.name), { method: 'DELETE' }); refresh(); } catch (e) { alert(e.message); }
    };
    li.appendChild(btn);
    zl.appendChild(li);
    L.circle([z.center.lat, z.center.lon], { radius: z.radiusKm * 1000, color: '#d29922', dashArray: '4 4', fillOpacity: 0.15 })
      .addTo(state.layers.zones).bindTooltip('Temporary: ' + esc(z.name));
  });
}

// ---------- polling ----------
let busy = false;
async function refresh() {
  if (busy) return;
  busy = true;
  try {
    const [missions, fleet] = await Promise.all([api('/api/missions'), api('/api/fleet')]);
    renderMissions(missions);
    renderFleet(fleet);
    if (state.selected) renderDetail(await api('/api/missions/' + encodeURIComponent(state.selected)));
    setConn(true, 'Online');
  } catch (e) {
    setConn(false, 'Offline: ' + e.message);
  } finally {
    busy = false;
  }
}

(async function start() {
  try {
    state.csrf = await api('/api/csrf');
    state.config = await api('/api/config');
    drawStatic(state.config);
    renderDraft();
    await refresh();
    setInterval(refresh, POLL_MS);
  } catch (e) {
    setConn(false, 'Cannot reach API: ' + e.message);
  }
})();
