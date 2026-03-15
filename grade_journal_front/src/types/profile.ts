import type { Role } from './auth';

export interface ProfileResponse {
  userId: number;
  username: string;
  fullName: string;
  email?: string | null;
  role: Role;
  active: boolean;
  approved: boolean;
  student?: {
    studentId: number;
    studentCard: string;
    groupCode: string;
    courseNo: number;
    facultyName: string;
    specializationName: string;
  } | null;
  teacher?: {
    teacherId: number;
    departmentCode: string;
    departmentName: string;
    position: string;
    phone?: string | null;
  } | null;
}