import api from './api';

export const missionService = {
  async getAllMissions(status) {
    const url = status ? `/missions?status=${status}` : '/missions';
    const res = await api.get(url);
    return res.data;
  },

  async getMissionById(id) {
    const res = await api.get(`/missions/${id}`);
    return res.data;
  },

  async createMission(missionData) {
    const res = await api.post('/missions', missionData);
    return res.data;
  },

  async updateStatus(id, status) {
    const res = await api.patch(`/missions/${id}/status?status=${status}`);
    return res.data;
  },

  async launchMission(id) {
    const res = await api.post(`/missions/${id}/launch`);
    return res.data;
  },

  async abortMission(id, reason = 'Operator emergency abort from UI') {
    const res = await api.post(`/missions/${id}/abort?reason=${encodeURIComponent(reason)}`);
    return res.data;
  },

  async getTemporalState(id) {
    const res = await api.get(`/missions/${id}/temporal-state`);
    return res.data;
  }
};
