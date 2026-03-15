export type Role = 'admin' | 'teacher' | 'student';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  userId: number;
  username: string;
  fullName: string;
  role: Role;
  accessToken: string;
  refreshToken: string;
  tokenType: string;
}

export interface RegisterRequest {
  username: string;
  password: string;
  fullName: string;
  email?: string;
  desiredRole: 'teacher' | 'student';
}

export interface RegisterResponse {
  message: string;
  status: string;
}