import { useEffect, useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../lib/api';

type Props = {
  title: string;
  description: string;
};

type StudentOption = {
  studentId: number;
  fullName: string;
  groupCode: string;
};

type CourseOption = {
  courseId: number;
  courseName: string;
};

type DetailPoint = {
  orderNo: number;
  value: number;
  gradeType: string;
  gradedAt: string;
};

type DetailResponse = {
  studentName: string;
  groupCode: string;
  courseName: string;
  averageAllCourses: number;
  averageSelectedCourse: number;
  predictedFinalGrade: number;
  recommendationSummary: string;
  recommendations: string[];
  points: DetailPoint[];
  gradeCount?: number;
  attendanceRate?: number;
  missedHours?: number;
  lastThreeGrades?: string;
  trend?: string;
  modelVersion?: string;
};

function unwrapArray(value: any): any[] {
  if (Array.isArray(value)) return value;
  if (Array.isArray(value?.items)) return value.items;
  if (Array.isArray(value?.content)) return value.content;
  if (Array.isArray(value?.data)) return value.data;
  return [];
}

async function fetchFirst(urls: string[]) {
  let lastError: any = null;

  for (const url of urls) {
    try {
      const response = await api.get(url);
      return response.data;
    } catch (error) {
      lastError = error;
    }
  }

  throw lastError ?? new Error('Не удалось загрузить данные.');
}

function normalizeStudents(raw: any): StudentOption[] {
  return unwrapArray(raw)
    .map((item: any) => ({
      studentId: Number(item.studentId ?? item.id ?? item.student?.studentId ?? 0),
      fullName: String(item.fullName ?? item.studentName ?? item.name ?? item.user?.fullName ?? ''),
      groupCode: String(item.groupCode ?? item.group?.groupCode ?? item.groupName ?? '')
    }))
    .filter((item: StudentOption) => item.studentId > 0 && item.fullName.trim().length > 0)
    .sort((a: StudentOption, b: StudentOption) => a.fullName.localeCompare(b.fullName, 'ru'));
}

function normalizeCourses(raw: any): CourseOption[] {
  return unwrapArray(raw)
    .map((item: any) => ({
      courseId: Number(item.courseId ?? item.id ?? item.course?.courseId ?? 0),
      courseName: String(item.courseName ?? item.name ?? item.title ?? item.course?.courseName ?? '')
    }))
    .filter((item: CourseOption) => item.courseId > 0 && item.courseName.trim().length > 0)
    .sort((a: CourseOption, b: CourseOption) => a.courseName.localeCompare(b.courseName, 'ru'));
}

function normalizeDetails(raw: any): DetailResponse {
  const pointsRaw = unwrapArray(raw?.points ?? raw?.grades ?? raw?.items);

  return {
    studentName: String(raw?.studentName ?? raw?.fullName ?? '—'),
    groupCode: String(raw?.groupCode ?? '—'),
    courseName: String(raw?.courseName ?? '—'),
    averageAllCourses: Number(raw?.averageAllCourses ?? 0),
    averageSelectedCourse: Number(raw?.averageSelectedCourse ?? raw?.averageGrade ?? 0),
    predictedFinalGrade: Number(raw?.predictedFinalGrade ?? 0),
    recommendationSummary: String(raw?.recommendationSummary ?? '—'),
    recommendations: Array.isArray(raw?.recommendations) ? raw.recommendations.map(String) : [],
    points: pointsRaw.map((point: any, index: number) => ({
      orderNo: Number(point.orderNo ?? index + 1),
      value: Number(point.value ?? point.gradeValue ?? point.grade ?? 0),
      gradeType: String(point.gradeType ?? point.typeName ?? point.title ?? 'Оценка'),
      gradedAt: String(point.gradedAt ?? point.date ?? point.dueDate ?? '')
    })),
    gradeCount: Number(raw?.gradeCount ?? pointsRaw.length ?? 0),
    attendanceRate:
      raw?.attendanceRate === null || raw?.attendanceRate === undefined
        ? undefined
        : Number(raw.attendanceRate),
    missedHours:
      raw?.missedHours === null || raw?.missedHours === undefined
        ? undefined
        : Number(raw.missedHours),
    lastThreeGrades: raw?.lastThreeGrades ? String(raw.lastThreeGrades) : undefined,
    trend: raw?.trend ? String(raw.trend) : undefined,
    modelVersion: raw?.modelVersion ? String(raw.modelVersion) : undefined
  };
}

function formatNumber(value: number | undefined) {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return '—';
  }
  return String(value);
}

function formatDate(value: string) {
  if (!value) {
    return '—';
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return date.toLocaleString();
}

export function StudentProfileViewerPanel({ title, description }: Props) {
  const [search, setSearch] = useState('');
  const [selectedStudentId, setSelectedStudentId] = useState<string>('');
  const [selectedCourseId, setSelectedCourseId] = useState<string>('');

  const studentsQuery = useQuery({
    queryKey: ['student-profile-viewer', 'students'],
    queryFn: async () =>
      normalizeStudents(
        await fetchFirst([
          '/api/performance-panel/students',
          '/api/student-profile/students',
          '/api/students',
          '/api/users/students'
        ])
      )
  });

  useEffect(() => {
    if (!selectedStudentId && studentsQuery.data?.length) {
      setSelectedStudentId(String(studentsQuery.data[0].studentId));
    }
  }, [selectedStudentId, studentsQuery.data]);

  useEffect(() => {
    setSelectedCourseId('');
  }, [selectedStudentId]);

  const coursesQuery = useQuery({
    enabled: Boolean(selectedStudentId),
    queryKey: ['student-profile-viewer', 'courses', selectedStudentId],
    queryFn: async () =>
      normalizeCourses(
        await fetchFirst([
          `/api/performance-panel/courses?studentId=${selectedStudentId}`,
          `/api/performance/student-courses?studentId=${selectedStudentId}`,
          `/api/student-profile/courses?studentId=${selectedStudentId}`,
          `/api/courses?studentId=${selectedStudentId}`
        ])
      )
  });

  useEffect(() => {
    if (!selectedCourseId && coursesQuery.data?.length) {
      setSelectedCourseId(String(coursesQuery.data[0].courseId));
    }
  }, [selectedCourseId, coursesQuery.data]);

  const filteredStudents = useMemo(() => {
    const source = studentsQuery.data ?? [];
    const query = search.trim().toLowerCase();

    if (!query) {
      return source;
    }

    return source.filter(
      (student) =>
        student.fullName.toLowerCase().includes(query) ||
        student.groupCode.toLowerCase().includes(query) ||
        String(student.studentId).includes(query)
    );
  }, [studentsQuery.data, search]);

  const detailsQuery = useQuery({
    enabled: Boolean(selectedStudentId) && Boolean(selectedCourseId),
    queryKey: ['student-profile-viewer', 'details', selectedStudentId, selectedCourseId],
    queryFn: async () =>
      normalizeDetails(
        await fetchFirst([
          `/api/performance-panel/details?studentId=${selectedStudentId}&courseId=${selectedCourseId}`,
          `/api/performance?studentId=${selectedStudentId}&courseId=${selectedCourseId}`,
          `/api/student-profile/details?studentId=${selectedStudentId}&courseId=${selectedCourseId}`
        ])
      )
  });

  return (
    <section className="rounded-3xl border border-slate-300 bg-white p-6 shadow-sm">
      <div className="mb-5">
        <h2 className="text-2xl font-semibold text-slate-900">{title}</h2>
        <p className="mt-2 text-slate-600">{description}</p>
      </div>

      <div className="grid gap-4 xl:grid-cols-3">
        <label className="space-y-2">
          <span className="text-sm font-medium text-slate-700">Поиск студента</span>
          <input
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="ФИО, группа или id"
            className="w-full rounded-2xl border border-slate-300 bg-white px-4 py-3 text-slate-900 outline-none transition focus:border-blue-500"
          />
        </label>

        <label className="space-y-2">
          <span className="text-sm font-medium text-slate-700">Студент</span>
          <select
            value={selectedStudentId}
            onChange={(event) => setSelectedStudentId(event.target.value)}
            className="w-full rounded-2xl border border-slate-300 bg-white px-4 py-3 text-slate-900 outline-none transition focus:border-blue-500"
          >
            {!filteredStudents.length && <option value="">Нет студентов</option>}
            {filteredStudents.map((student) => (
              <option key={student.studentId} value={student.studentId}>
                {student.fullName} — {student.groupCode}
              </option>
            ))}
          </select>
        </label>

        <label className="space-y-2">
          <span className="text-sm font-medium text-slate-700">Курс</span>
          <select
            value={selectedCourseId}
            onChange={(event) => setSelectedCourseId(event.target.value)}
            className="w-full rounded-2xl border border-slate-300 bg-white px-4 py-3 text-slate-900 outline-none transition focus:border-blue-500"
          >
            {!coursesQuery.data?.length && <option value="">Нет курсов</option>}
            {(coursesQuery.data ?? []).map((course) => (
              <option key={course.courseId} value={course.courseId}>
                {course.courseName}
              </option>
            ))}
          </select>
        </label>
      </div>

      {studentsQuery.isLoading && (
        <div className="mt-5 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-slate-600">
          Загрузка списка студентов...
        </div>
      )}

      {studentsQuery.isError && (
        <div className="mt-5 rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-rose-700">
          Не удалось загрузить список студентов.
        </div>
      )}

      {coursesQuery.isLoading && selectedStudentId && (
        <div className="mt-5 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-slate-600">
          Загрузка списка курсов...
        </div>
      )}

      {coursesQuery.isError && selectedStudentId && (
        <div className="mt-5 rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-rose-700">
          Не удалось загрузить курсы выбранного студента.
        </div>
      )}

      {detailsQuery.isLoading && selectedStudentId && selectedCourseId && (
        <div className="mt-5 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-slate-600">
          Загрузка профиля студента...
        </div>
      )}

      {detailsQuery.isError && selectedStudentId && selectedCourseId && (
        <div className="mt-5 rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-rose-700">
          Не удалось загрузить детали по студенту и курсу.
        </div>
      )}

      {detailsQuery.data && (
        <div className="mt-6 space-y-6">
          <div className="grid gap-4 lg:grid-cols-4">
            <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
              <div className="text-sm text-slate-500">Студент</div>
              <div className="mt-1 font-semibold text-slate-900">
                {detailsQuery.data.studentName}
              </div>
            </div>

            <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
              <div className="text-sm text-slate-500">Группа</div>
              <div className="mt-1 font-semibold text-slate-900">
                {detailsQuery.data.groupCode}
              </div>
            </div>

            <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
              <div className="text-sm text-slate-500">Средний по предмету</div>
              <div className="mt-1 font-semibold text-slate-900">
                {formatNumber(detailsQuery.data.averageSelectedCourse)}
              </div>
            </div>

            <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
              <div className="text-sm text-slate-500">Прогноз</div>
              <div className="mt-1 font-semibold text-slate-900">
                {formatNumber(detailsQuery.data.predictedFinalGrade)}
              </div>
            </div>
          </div>

          <div className="grid gap-4 lg:grid-cols-4">
            <div className="rounded-2xl border border-slate-200 bg-white p-4">
              <div className="text-sm text-slate-500">Средний по всем курсам</div>
              <div className="mt-1 font-semibold text-slate-900">
                {formatNumber(detailsQuery.data.averageAllCourses)}
              </div>
            </div>

            <div className="rounded-2xl border border-slate-200 bg-white p-4">
              <div className="text-sm text-slate-500">Количество оценок</div>
              <div className="mt-1 font-semibold text-slate-900">
                {formatNumber(detailsQuery.data.gradeCount)}
              </div>
            </div>

            <div className="rounded-2xl border border-slate-200 bg-white p-4">
              <div className="text-sm text-slate-500">Посещаемость</div>
              <div className="mt-1 font-semibold text-slate-900">
                {detailsQuery.data.attendanceRate === undefined
                  ? '—'
                  : `${detailsQuery.data.attendanceRate}%`}
              </div>
            </div>

            <div className="rounded-2xl border border-slate-200 bg-white p-4">
              <div className="text-sm text-slate-500">Пропущено часов</div>
              <div className="mt-1 font-semibold text-slate-900">
                {detailsQuery.data.missedHours === undefined ? '—' : detailsQuery.data.missedHours}
              </div>
            </div>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-slate-50 p-5">
            <h3 className="text-lg font-semibold text-slate-900">Сводка</h3>
            <p className="mt-2 text-slate-700">{detailsQuery.data.recommendationSummary}</p>

            {(detailsQuery.data.lastThreeGrades || detailsQuery.data.trend || detailsQuery.data.modelVersion) && (
              <div className="mt-4 grid gap-3 md:grid-cols-3">
                <div className="rounded-2xl border border-slate-200 bg-white p-4">
                  <div className="text-sm text-slate-500">Последние оценки</div>
                  <div className="mt-1 font-medium text-slate-900">
                    {detailsQuery.data.lastThreeGrades ?? '—'}
                  </div>
                </div>

                <div className="rounded-2xl border border-slate-200 bg-white p-4">
                  <div className="text-sm text-slate-500">Динамика</div>
                  <div className="mt-1 font-medium text-slate-900">
                    {detailsQuery.data.trend ?? '—'}
                  </div>
                </div>

                <div className="rounded-2xl border border-slate-200 bg-white p-4">
                  <div className="text-sm text-slate-500">Версия модели</div>
                  <div className="mt-1 font-medium text-slate-900">
                    {detailsQuery.data.modelVersion ?? '—'}
                  </div>
                </div>
              </div>
            )}
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white p-5">
            <h3 className="text-lg font-semibold text-slate-900">Рекомендации</h3>

            {detailsQuery.data.recommendations.length ? (
              <ul className="mt-3 space-y-3">
                {detailsQuery.data.recommendations.map((recommendation, index) => (
                  <li
                    key={`${recommendation}-${index}`}
                    className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-slate-700"
                  >
                    {recommendation}
                  </li>
                ))}
              </ul>
            ) : (
              <p className="mt-3 text-slate-600">Рекомендации отсутствуют.</p>
            )}
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white p-5">
            <h3 className="text-lg font-semibold text-slate-900">Оценки по выбранному курсу</h3>

            <div className="mt-4 overflow-x-auto">
              <table className="min-w-full border-separate border-spacing-0 text-sm">
                <thead>
                  <tr className="bg-slate-100 text-slate-700">
                    <th className="rounded-l-2xl px-4 py-3 text-left font-semibold">№</th>
                    <th className="px-4 py-3 text-left font-semibold">Тип</th>
                    <th className="px-4 py-3 text-left font-semibold">Оценка</th>
                    <th className="rounded-r-2xl px-4 py-3 text-left font-semibold">Дата</th>
                  </tr>
                </thead>
                <tbody>
                  {detailsQuery.data.points.length ? (
                    detailsQuery.data.points.map((point) => (
                      <tr key={`${point.orderNo}-${point.gradedAt}`} className="border-b border-slate-200">
                        <td className="px-4 py-3 text-slate-700">{point.orderNo}</td>
                        <td className="px-4 py-3 text-slate-700">{point.gradeType}</td>
                        <td className="px-4 py-3 font-medium text-slate-900">{point.value}</td>
                        <td className="px-4 py-3 text-slate-700">{formatDate(point.gradedAt)}</td>
                      </tr>
                    ))
                  ) : (
                    <tr>
                      <td colSpan={4} className="px-4 py-6 text-center text-slate-500">
                        По выбранному курсу оценок пока нет.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}
    </section>
  );
}