import React, { useState, useEffect } from 'react';
import { authService } from './services/authService';
import { droneService } from './services/droneService';
import { missionService } from './services/missionService';
import { telemetryService } from './services/telemetryService';
import {
  LineChart, Line, XAxis, YAxis, Tooltip, CartesianGrid, ResponsiveContainer, Legend
} from 'recharts';
import { Plane, Battery, Shield, LogOut, Radio, Plus, RefreshCw, Play, AlertTriangle } from 'lucide-react';
import './App.css';

export default function App() {
  const [currentUser, setCurrentUser] = useState(authService.getCurrentUser());
  const [authTab, setAuthTab] = useState('login');
  const [authForm, setAuthForm] = useState({ username: '', password: '', role: 'OPERATOR' });
  const [authError, setAuthError] = useState('');

  // Dashboard Data
  const [drones, setDrones] = useState([]);
  const [missions, setMissions] = useState([]);
  const [selectedMission, setSelectedMission] = useState(null);
  const [telemetryHistory, setTelemetryHistory] = useState([]);
  const [temporalState, setTemporalState] = useState(null);
  const [pollingActive, setPollingActive] = useState(true);

  // New Mission Modal
  const [showMissionModal, setShowMissionModal] = useState(false);
  const [newMission, setNewMission] = useState({
    droneId: '',
    name: '',
    destinationLat: 12.9850,
    destinationLng: 77.6100,
  });

  useEffect(() => {
    const handler = () => setCurrentUser(authService.getCurrentUser());
    window.addEventListener('auth-change', handler);
    return () => window.removeEventListener('auth-change', handler);
  }, []);

  useEffect(() => {
    if (currentUser) {
      loadDashboardData();
    }
  }, [currentUser]);

  // Polling loop for Telemetry and Temporal Workflow State
  useEffect(() => {
    if (!currentUser || !selectedMission || !pollingActive) return;

    const intervalId = setInterval(async () => {
      try {
        const [latest, state] = await Promise.all([
          telemetryService.getLatestTelemetry(selectedMission.id),
          missionService.getTemporalState(selectedMission.id)
        ]);

        if (latest) {
          setTelemetryHistory((prev) => {
            const exists = prev.some((p) => p.id === latest.id);
            if (!exists) {
              return [...prev, latest].slice(-25);
            }
            return prev;
          });
        }

        if (state) {
          setTemporalState(state);
        }
      } catch (err) {
        console.error('Polling error:', err);
      }
    }, 2000);

    return () => clearInterval(intervalId);
  }, [currentUser, selectedMission, pollingActive]);

  async function loadDashboardData() {
    try {
      const [droneList, missionList] = await Promise.all([
        droneService.getAllDrones(),
        missionService.getAllMissions()
      ]);
      setDrones(droneList);
      setMissions(missionList);

      if (missionList.length > 0 && !selectedMission) {
        selectMission(missionList[0]);
      } else if (selectedMission) {
        const updated = missionList.find(m => m.id === selectedMission.id);
        if (updated) setSelectedMission(updated);
      }
    } catch (err) {
      console.error('Failed to load dashboard data:', err);
    }
  }

  async function selectMission(mission) {
    setSelectedMission(mission);
    try {
      const [history, state] = await Promise.all([
        telemetryService.getTelemetryByMission(mission.id),
        missionService.getTemporalState(mission.id)
      ]);
      setTelemetryHistory(history.slice(-25));
      setTemporalState(state);
    } catch (err) {
      console.error('Failed to load telemetry or temporal state:', err);
      setTelemetryHistory([]);
      setTemporalState(null);
    }
  }

  async function handleLaunchWorkflow(missionId) {
    try {
      await missionService.launchMission(missionId);
      loadDashboardData();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to dispatch workflow to Temporal');
    }
  }

  async function handleEmergencyAbort(missionId) {
    if (!confirm('Are you sure you want to trigger emergency abort? This will execute the Temporal Saga rollback compensation.')) {
      return;
    }
    try {
      await missionService.abortMission(missionId, 'Operator manual emergency abort triggered');
      loadDashboardData();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to dispatch abort signal');
    }
  }

  async function handleAuthSubmit(e) {
    e.preventDefault();
    setAuthError('');
    try {
      if (authTab === 'login') {
        await authService.login(authForm.username, authForm.password);
      } else {
        await authService.register(authForm.username, authForm.password, authForm.role);
      }
    } catch (err) {
      const msg = err.response?.data?.message || 'Authentication failed. Please verify credentials.';
      setAuthError(msg);
    }
  }

  async function handleCreateMission(e) {
    e.preventDefault();
    try {
      const payload = {
        name: newMission.name,
        destinationLat: parseFloat(newMission.destinationLat),
        destinationLng: parseFloat(newMission.destinationLng),
        droneId: newMission.droneId ? parseInt(newMission.droneId) : null,
      };
      await missionService.createMission(payload);
      setShowMissionModal(false);
      setNewMission({ droneId: '', name: '', destinationLat: 12.9850, destinationLng: 77.6100 });
      loadDashboardData();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to create mission');
    }
  }

  if (!currentUser) {
    return (
      <div className="auth-wrapper">
        <div className="auth-card">
          <div style={{ textAlign: 'center', marginBottom: '1.5rem' }}>
            <Plane size={36} color="#00f2fe" style={{ margin: '0 auto 0.5rem' }} />
            <h2 style={{ color: '#00f2fe' }}>AeroSaga Mission Control</h2>
            <p style={{ color: '#94a3b8', fontSize: '0.9rem' }}>Temporal.io Autonomous Drone Orchestration</p>
          </div>

          <div className="auth-tabs">
            <button
              className={`auth-tab ${authTab === 'login' ? 'active' : ''}`}
              onClick={() => { setAuthTab('login'); setAuthError(''); }}
            >
              Login
            </button>
            <button
              className={`auth-tab ${authTab === 'register' ? 'active' : ''}`}
              onClick={() => { setAuthTab('register'); setAuthError(''); }}
            >
              Register
            </button>
          </div>

          {authError && <div className="alert-box alert-error">{authError}</div>}

          <div className="alert-box alert-info">
            Default Admin: <strong>admin</strong> / <strong>admin123</strong><br/>
            Default Operator: <strong>operator</strong> / <strong>operator123</strong>
          </div>

          <form onSubmit={handleAuthSubmit}>
            <div className="form-group">
              <label>Username</label>
              <input
                type="text"
                className="form-control"
                required
                value={authForm.username}
                onChange={(e) => setAuthForm({ ...authForm, username: e.target.value })}
                placeholder="Enter username"
              />
            </div>
            <div className="form-group">
              <label>Password</label>
              <input
                type="password"
                className="form-control"
                required
                value={authForm.password}
                onChange={(e) => setAuthForm({ ...authForm, password: e.target.value })}
                placeholder="Enter password"
              />
            </div>

            {authTab === 'register' && (
              <div className="form-group">
                <label>Role</label>
                <select
                  className="form-control"
                  value={authForm.role}
                  onChange={(e) => setAuthForm({ ...authForm, role: e.target.value })}
                >
                  <option value="OPERATOR">OPERATOR</option>
                  <option value="ADMIN">ADMIN</option>
                </select>
              </div>
            )}

            <button type="submit" className="btn-primary" style={{ width: '100%', marginTop: '0.5rem' }}>
              {authTab === 'login' ? 'Sign In with JWT' : 'Create Account'}
            </button>
          </form>
        </div>
      </div>
    );
  }

  const availableCount = drones.filter(d => d.status === 'AVAILABLE').length;
  const inMissionCount = drones.filter(d => d.status === 'IN_MISSION').length;
  const maintenanceCount = drones.filter(d => d.status === 'MAINTENANCE').length;

  return (
    <div className="app-container">
      {/* Top Navigation */}
      <header className="app-header">
        <div className="brand">
          <Plane size={24} color="#00f2fe" />
          <h1>AEROSAGA MISSION CONTROL</h1>
          <span style={{ fontSize: '0.8rem', color: '#10b981', display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
            <Radio size={14} /> Temporal Saga Engine Active
          </span>
        </div>

        <div className="user-nav">
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <Shield size={16} color="#00f2fe" />
            <span style={{ fontWeight: 600 }}>{currentUser.username}</span>
            <span className="badge-role">{currentUser.role}</span>
          </div>
          <button className="btn-logout" onClick={() => authService.logout()} title="Sign Out">
            <LogOut size={16} />
          </button>
        </div>
      </header>

      {/* Main Content */}
      <main className="main-content">
        {/* Metrics Overview */}
        <div className="metrics-grid">
          <div className="metric-card">
            <h4>Total Fleet Assets</h4>
            <div className="metric-value">{drones.length}</div>
          </div>
          <div className="metric-card">
            <h4>Available Assets</h4>
            <div className="metric-value" style={{ color: '#10b981' }}>{availableCount}</div>
          </div>
          <div className="metric-card">
            <h4>Active In-Flight</h4>
            <div className="metric-value" style={{ color: '#4facfe' }}>{inMissionCount}</div>
          </div>
          <div className="metric-card">
            <h4>Maintenance</h4>
            <div className="metric-value" style={{ color: '#ef4444' }}>{maintenanceCount}</div>
          </div>
        </div>

        {/* Dashboard Grid */}
        <div className="dashboard-grid">
          {/* Left Column: Drone Fleet & Missions */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
            {/* Drone Fleet */}
            <div className="card">
              <div className="card-header">
                <h3>Drone Fleet Status</h3>
                <button className="btn-logout" onClick={loadDashboardData} title="Refresh">
                  <RefreshCw size={14} />
                </button>
              </div>
              <div style={{ overflowX: 'auto' }}>
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Name / Model</th>
                      <th>Battery</th>
                      <th>Coordinates</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {drones.map(d => (
                      <tr key={d.id}>
                        <td>
                          <strong>{d.name}</strong>
                          <div style={{ fontSize: '0.75rem', color: '#94a3b8' }}>{d.model}</div>
                        </td>
                        <td>
                          <span style={{ display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
                            <Battery size={14} color={d.batteryLevel > 50 ? '#10b981' : '#f59e0b'} />
                            {d.batteryLevel}%
                          </span>
                        </td>
                        <td style={{ fontSize: '0.8rem', color: '#94a3b8' }}>
                          {d.currentLat.toFixed(4)}, {d.currentLng.toFixed(4)}
                        </td>
                        <td>
                          <span className={`badge-status badge-${d.status}`}>{d.status}</span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>

            {/* Missions Dispatch */}
            <div className="card">
              <div className="card-header">
                <h3>Missions Dispatch</h3>
                <button className="btn-primary" onClick={() => setShowMissionModal(true)} style={{ display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
                  <Plus size={16} /> Schedule Mission
                </button>
              </div>
              <div style={{ overflowX: 'auto' }}>
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Mission</th>
                      <th>Drone</th>
                      <th>Status</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {missions.map(m => (
                      <tr
                        key={m.id}
                        className={selectedMission?.id === m.id ? 'selected' : ''}
                        onClick={() => selectMission(m)}
                      >
                        <td><strong>{m.name}</strong></td>
                        <td>{m.droneName || 'Auto-assign'}</td>
                        <td>
                          <span className={`badge-status badge-${m.status}`}>{m.status}</span>
                        </td>
                        <td>
                          {m.status === 'SCHEDULED' && (
                            <button
                              className="btn-primary"
                              style={{ padding: '0.3rem 0.6rem', fontSize: '0.75rem', display: 'flex', alignItems: 'center', gap: '0.2rem' }}
                              onClick={(e) => { e.stopPropagation(); handleLaunchWorkflow(m.id); }}
                            >
                              <Play size={12} /> Launch
                            </button>
                          )}
                          {m.status === 'RUNNING' && (
                            <button
                              className="btn-logout"
                              style={{ padding: '0.3rem 0.6rem', fontSize: '0.75rem', color: '#ef4444', borderColor: '#ef4444', display: 'flex', alignItems: 'center', gap: '0.2rem' }}
                              onClick={(e) => { e.stopPropagation(); handleEmergencyAbort(m.id); }}
                            >
                              <AlertTriangle size={12} /> Abort
                            </button>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          {/* Right Column: Live Telemetry & Temporal Workflow State */}
          <div className="card" style={{ display: 'flex', flexDirection: 'column' }}>
            <div className="card-header">
              <div>
                <h3>Live Avionics Telemetry</h3>
                <p style={{ fontSize: '0.8rem', color: '#94a3b8' }}>
                  {selectedMission ? `Monitoring: ${selectedMission.name}` : 'Select a mission to monitor'}
                </p>
              </div>

              {selectedMission && (
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  {temporalState?.active && (
                    <span style={{
                      fontSize: '0.75rem', padding: '0.2rem 0.6rem', borderRadius: 4,
                      backgroundColor: 'rgba(0, 242, 254, 0.15)', color: '#00f2fe', border: '1px solid #00f2fe'
                    }}>
                      Phase: {temporalState.currentPhase}
                    </span>
                  )}
                  <button
                    className="btn-logout"
                    onClick={() => setPollingActive(!pollingActive)}
                    style={{ color: pollingActive ? '#10b981' : '#94a3b8' }}
                  >
                    Polling: {pollingActive ? 'ON' : 'OFF'}
                  </button>
                </div>
              )}
            </div>

            {selectedMission ? (
              <div>
                {/* Telemetry Line Chart */}
                <div style={{ height: 320, width: '100%', marginTop: '1rem' }}>
                  <ResponsiveContainer width="100%" height="100%">
                    <LineChart data={telemetryHistory}>
                      <CartesianGrid strokeDasharray="3 3" stroke="#273553" />
                      <XAxis dataKey="timestamp" stroke="#94a3b8" tickFormatter={(t) => t ? t.substring(11, 19) : ''} />
                      <YAxis stroke="#94a3b8" />
                      <Tooltip contentStyle={{ backgroundColor: '#131b2e', border: '1px solid #273553', borderRadius: 6 }} />
                      <Legend />
                      <Line type="monotone" dataKey="altitude" name="Altitude (m)" stroke="#00f2fe" strokeWidth={2} dot={{ r: 3 }} />
                      <Line type="monotone" dataKey="speed" name="Speed (km/h)" stroke="#4facfe" strokeWidth={2} dot={{ r: 3 }} />
                      <Line type="monotone" dataKey="batteryPercentage" name="Battery (%)" stroke="#10b981" strokeWidth={2} dot={{ r: 3 }} />
                    </LineChart>
                  </ResponsiveContainer>
                </div>

                {/* Telemetry Snapshot Cards */}
                {telemetryHistory.length > 0 && (
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '1rem', marginTop: '1.5rem' }}>
                    <div style={{ background: '#131b2e', padding: '0.75rem', borderRadius: 6, textAlign: 'center' }}>
                      <div style={{ fontSize: '0.75rem', color: '#94a3b8' }}>Altitude</div>
                      <div style={{ fontSize: '1.25rem', fontWeight: 700, color: '#00f2fe' }}>
                        {telemetryHistory[telemetryHistory.length - 1].altitude} m
                      </div>
                    </div>
                    <div style={{ background: '#131b2e', padding: '0.75rem', borderRadius: 6, textAlign: 'center' }}>
                      <div style={{ fontSize: '0.75rem', color: '#94a3b8' }}>Speed</div>
                      <div style={{ fontSize: '1.25rem', fontWeight: 700, color: '#4facfe' }}>
                        {telemetryHistory[telemetryHistory.length - 1].speed} km/h
                      </div>
                    </div>
                    <div style={{ background: '#131b2e', padding: '0.75rem', borderRadius: 6, textAlign: 'center' }}>
                      <div style={{ fontSize: '0.75rem', color: '#94a3b8' }}>Battery</div>
                      <div style={{ fontSize: '1.25rem', fontWeight: 700, color: '#10b981' }}>
                        {telemetryHistory[telemetryHistory.length - 1].batteryPercentage}%
                      </div>
                    </div>
                  </div>
                )}
              </div>
            ) : (
              <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#94a3b8', minHeight: 250 }}>
                Please select or create a mission to stream real-time avionics telemetry.
              </div>
            )}
          </div>
        </div>
      </main>

      {/* Schedule Mission Modal */}
      {showMissionModal && (
        <div style={{
          position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
          backgroundColor: 'rgba(0,0,0,0.7)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000
        }}>
          <div className="card" style={{ maxWidth: 450, width: '100%' }}>
            <div className="card-header">
              <h3>Schedule Drone Mission</h3>
              <button className="btn-logout" onClick={() => setShowMissionModal(false)}>✕</button>
            </div>
            <form onSubmit={handleCreateMission}>
              <div className="form-group">
                <label>Mission Name</label>
                <input
                  type="text"
                  className="form-control"
                  required
                  placeholder="e.g. Surveillance Recon Sector 4"
                  value={newMission.name}
                  onChange={(e) => setNewMission({ ...newMission, name: e.target.value })}
                />
              </div>

              <div className="form-group">
                <label>Assign Drone (Optional)</label>
                <select
                  className="form-control"
                  value={newMission.droneId}
                  onChange={(e) => setNewMission({ ...newMission, droneId: e.target.value })}
                >
                  <option value="">Auto-assign available drone</option>
                  {drones.filter(d => d.status === 'AVAILABLE').map(d => (
                    <option key={d.id} value={d.id}>{d.name} ({d.model} - {d.batteryLevel}%)</option>
                  ))}
                </select>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                <div className="form-group">
                  <label>Destination Lat</label>
                  <input
                    type="number"
                    step="0.0001"
                    className="form-control"
                    required
                    value={newMission.destinationLat}
                    onChange={(e) => setNewMission({ ...newMission, destinationLat: e.target.value })}
                  />
                </div>
                <div className="form-group">
                  <label>Destination Lng</label>
                  <input
                    type="number"
                    step="0.0001"
                    className="form-control"
                    required
                    value={newMission.destinationLng}
                    onChange={(e) => setNewMission({ ...newMission, destinationLng: e.target.value })}
                  />
                </div>
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '1rem', marginTop: '1.5rem' }}>
                <button type="button" className="btn-logout" onClick={() => setShowMissionModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn-primary">
                  Dispatch Mission
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
