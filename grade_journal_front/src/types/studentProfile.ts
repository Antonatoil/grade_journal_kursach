export type StudentProfileOption = {
  studentId: number;
  fullName: string;
  groupCode: string;
  courseNo: number;
};

export type StudentProfileGrade = {
  gradedDate: string;
  gradeValue: number;
  assessmentType: string;
  assessmentTitle: string;
};

export type StudentProfileCourse = {
  courseId: number;
  courseCode: string;
  courseName: string;
  averageGrade: number;
  predictedGrade: number;
  attendanceRate: number;
  missedHours: number;
  riskLevel: string;
  recommendations: string[];
  grades: StudentProfileGrade[];
};

export type StudentProfileResponse = {
  userId: number;
  username: string;
  fullName: string;
  email?: string | null;
  active: boolean;
  approved: boolean;
  studentId: number;
  studentCard: string;
  groupCode: string;
  courseNo: number;
  facultyName: string;
  specializationName: string;
  subjects: StudentProfileCourse[];
};