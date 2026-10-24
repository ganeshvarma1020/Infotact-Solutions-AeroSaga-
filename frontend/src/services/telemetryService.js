import api from './api';

export const telemetryService = {
  async getTelemetryByMission(missionId) {
    const res = await api.get(`/telemetry/mission/${missionId}`);
    return res.data;
  },

  async getLatestTelemetry(missionId) {
    const res = await api.get(`/telemetry/mission/${missionId}/latest`);
    return res.data;
  },

  async logTelemetry(telemetryData) {
    const res = await api.post('/telemetry', telemetryData);
    return res.data;
  }
};
