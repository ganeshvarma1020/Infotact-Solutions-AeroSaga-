import api from './api';

export const authService = {
  async login(username, password) {
    const res = await api.post('/auth/login', { username, password });
    if (res.data && res.data.token) {
      localStorage.setItem('aerosaga_token', res.data.token);
      localStorage.setItem('aerosaga_user', JSON.stringify({
        username: res.data.username,
        role: res.data.role,
      }));
      window.dispatchEvent(new Event('auth-change'));
    }
    return res.data;
  },

  async register(username, password, role = 'OPERATOR') {
    const res = await api.post('/auth/register', { username, password, role });
    if (res.data && res.data.token) {
      localStorage.setItem('aerosaga_token', res.data.token);
      localStorage.setItem('aerosaga_user', JSON.stringify({
        username: res.data.username,
        role: res.data.role,
      }));
      window.dispatchEvent(new Event('auth-change'));
    }
    return res.data;
  },

  logout() {
    localStorage.removeItem('aerosaga_token');
    localStorage.removeItem('aerosaga_user');
    window.dispatchEvent(new Event('auth-change'));
  },

  getCurrentUser() {
    const userStr = localStorage.getItem('aerosaga_user');
    return userStr ? JSON.parse(userStr) : null;
  },

  getToken() {
    return localStorage.getItem('aerosaga_token');
  }
};
