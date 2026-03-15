import axios from 'axios';
import { useAuthStore } from '../store/auth';

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json'
  }
});

api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = (error.config ?? {}) as any;
    const refreshToken = useAuthStore.getState().refreshToken;

    if (
      error.response?.status === 401 &&
      refreshToken &&
      !originalRequest._retry &&
      !String(originalRequest?.url ?? '').includes('/api/auth/login') &&
      !String(originalRequest?.url ?? '').includes('/api/auth/refresh')
    ) {
      originalRequest._retry = true;

      try {
        const refreshResponse = await axios.post(
          `${import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'}/api/auth/refresh`,
          { refreshToken }
        );

        useAuthStore.getState().setAuth(refreshResponse.data);
        originalRequest.headers = originalRequest.headers ?? {};
        originalRequest.headers.Authorization = `Bearer ${refreshResponse.data.accessToken}`;

        return api(originalRequest);
      } catch {
        useAuthStore.getState().logout();
        window.location.href = '/login';
      }
    }

    return Promise.reject(error);
  }
);