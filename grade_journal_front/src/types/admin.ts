export interface PendingRequest {
  requestId: number;
  desiredRole: 'teacher' | 'student';
  username: string;
  fullName: string;
  email?: string | null;
  status: string;
  createdAt: string;
}

export interface IncompleteProfile {
  userId: number;
  username: string;
  fullName: string;
  email?: string | null;
  role: 'teacher' | 'student';
  approved: boolean;
  createdAt: string;
}

export interface GroupOption {
  groupId: number;
  groupCode: string;
  courseNo: number;
  admissionYear: number;
  facultyName: string;
  specializationName: string;
}

export interface DepartmentOption {
  departmentId: number;
  departmentCode: string;
  departmentName: string;
}

export interface AdminProfileOptions {
  groups: GroupOption[];
  departments: DepartmentOption[];
}