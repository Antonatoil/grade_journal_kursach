import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import type { LoginResponse, Role } from '../types/auth';

interface AuthState {
  userId: number | null;
  username: string | null;
  fullName: string | null;
  role: Role | null;
  accessToken: string | null;
  refreshToken: string | null;
  setAuth: (payload: LoginResponse) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      userId: null,
      username: null,
      fullName: null,
      role: null,
      accessToken: null,
      refreshToken: null,
      setAuth: (payload) =>
        set({
          userId: payload.userId,
          username: payload.username,
          fullName: payload.fullName,
          role: payload.role,
          accessToken: payload.accessToken,
          refreshToken: payload.refreshToken
        }),
      logout: () =>
        set({
          userId: null,
          username: null,
          fullName: null,
          role: null,
          accessToken: null,
          refreshToken: null
        })
    }),
    {
      name: 'grade-journal-auth',
      storage: createJSONStorage(() => localStorage)
    }
  )
);