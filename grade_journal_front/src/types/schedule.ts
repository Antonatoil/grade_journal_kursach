export type ScheduleGroupOption = {
  groupId: number;
  groupCode: string;
  courseNo: number;
  admissionYear: number;
  facultyName: string;
  specializationName: string;
};

export type ScheduleTeacherOption = {
  teacherId: number;
  fullName: string;
  departmentName: string;
  position: string;
};

export type ScheduleCourseOption = {
  courseId: number;
  courseCode: string;
  courseName: string;
  studyYear: number;
  controlForm: string;
};

export type ScheduleEntry = {
  scheduleId: number;
  lessonDate: string;
  timeSlot: string;
  groupId: number;
  groupCode: string;
  courseName: string;
  teacherFullName: string;
  room?: string | null;
  lessonType: string;
  topic?: string | null;
};

export type ScheduleMetaResponse = {
  groups: ScheduleGroupOption[];
  teachers: ScheduleTeacherOption[];
  courses: ScheduleCourseOption[];
  timeSlots: string[];
  lessonTypes: string[];
};