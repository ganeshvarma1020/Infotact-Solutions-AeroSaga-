import api from './api';

export const droneService = {
  async getAllDrones() {
    const res = await api.get('/drones');
    return res.data;
  },

  async getAvailableDrones() {
    const res = await api.get('/drones/available');
    return res.data;
  },

  async getDroneById(id) {
    const res = await api.get(`/drones/${id}`);
    return res.data;
  },

  async createDrone(droneData) {
    const res = await api.post('/drones', droneData);
    return res.data;
  },

  async updateStatus(id, status) {
    const res = await api.patch(`/drones/${id}/status?status=${status}`);
    return res.data;
  }
};
