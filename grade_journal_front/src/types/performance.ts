export type PerformanceStudentOption = {
  studentId: number;
  fullName: string;
  groupCode: string;
  courseNo: number;
};

export type PerformanceCourseOption = {
  courseId: number;
  courseCode: string;
  courseName: string;
  studyYear: number;
};

export type PerformanceMetaResponse = {
  students: PerformanceStudentOption[];
  courses: PerformanceCourseOption[];
};

export type PerformanceGradePoint = {
  gradeId: number;
  gradedDate: string;
  gradeValue: number;
  assessmentType: string;
  assessmentTitle: string;
  sequenceNo: number;
  predicted: boolean;
};

export type PerformanceSummary = {
  studentId: number;
  studentFullName: string;
  groupCode: string;
  courseId: number;
  courseName: string;
  averageAllSubjects: number;
  averageSelectedSubject: number;
  predictedNextGrade: number;
};

export type PerformanceResponse = {
  summary: PerformanceSummary;
  grades: PerformanceGradePoint[];
  chartPoints: PerformanceGradePoint[];
};