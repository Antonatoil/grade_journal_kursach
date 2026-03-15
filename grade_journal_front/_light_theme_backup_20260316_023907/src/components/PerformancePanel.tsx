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

type PerformancePoint = {
  orderNo: number;
  value: number;
  gradeType: string;
  gradedAt: string;
};

type PerformanceDetails = {
  studentName: string;
  groupCode: string;
  courseName: string;
  averageAllCourses: number;
  averageSelectedCourse: number;
  predictedFinalGrade: number;
  recommendationSummary: string;
  recommendations: string[];
  points: PerformancePoint[];
};

function buildChartPoints(points: Array<{ x: number; y: number }>, width: number, height: number) {
  if (!points.length) return '';

  const maxX = Math.max(...points.map((point) => point.x));
  const minX = Math.min(...points.map((point) => point.x));
  const maxY = 10;
  const minY = 0;

  return points
    .map((point) => {
      const px =
        maxX === minX
          ? width / 2
          : ((point.x - minX) / (maxX - minX)) * (width - 40) + 20;
      const py = height - (((point.y - minY) / (maxY - minY)) * (height - 40) + 20);
      return `${px},${py}`;
    })
    .join(' ');
}

export function PerformancePanel({ title, description }: Props) {
  const [studentId, setStudentId] = useState('');
  const [courseId, setCourseId] = useState('');

  const studentsQuery = useQuery({
    queryKey: ['performance-panel', 'students'],
    queryFn: async () =>
      (await api.get<StudentOption[]>('/api/performance-panel/students')).data
  });

  useEffect(() => {
    if (!studentId && studentsQuery.data?.length) {
      setStudentId(String(studentsQuery.data[0].studentId));
    }
  }, [studentId, studentsQuery.data]);

  const coursesQuery = useQuery({
    queryKey: ['performance-panel', 'courses', studentId],
    enabled: Boolean(studentId),
    queryFn: async () =>
      (
        await api.get<CourseOption[]>('/api/performance-panel/courses', {
          params: { studentId }
        })
      ).data
  });

  useEffect(() => {
    if (coursesQuery.data?.length) {
      const exists = coursesQuery.data.some((course) => String(course.courseId) === courseId);
      if (!courseId || !exists) {
        setCourseId(String(coursesQuery.data[0].courseId));
      }
    } else {
      setCourseId('');
    }
  }, [courseId, coursesQuery.data]);

  const detailsQuery = useQuery({
    queryKey: ['performance-panel', 'details', studentId, courseId],
    enabled: Boolean(studentId && courseId),
    queryFn: async () =>
      (
        await api.get<PerformanceDetails>('/api/performance-panel/details', {
          params: { studentId, courseId }
        })
      ).data
  });

  const chartData = useMemo(() => {
    const actual = (detailsQuery.data?.points ?? []).map((point) => ({
      x: point.orderNo,
      y: point.value,
      isForecast: false
    }));

    const forecast =
      detailsQuery.data && actual.length
        ? [
            {
              x: actual.length + 1,
              y: detailsQuery.data.predictedFinalGrade,
              isForecast: true
            }
          ]
        : [];

    return [...actual, ...forecast];
  }, [detailsQuery.data]);

  const polylinePoints = useMemo(
    () => buildChartPoints(chartData, 760, 320),
    [chartData]
  );

  return (
    <section className="space-y-5 rounded-3xl border border-slate-800 bg-slate-900/70 p-6">
      <div>
        <h2 className="text-2xl font-semibold text-white">{title}</h2>
        <p className="mt-2 text-slate-400">{description}</p>
      </div>

      <div className="grid gap-4 xl:grid-cols-2">
        <label className="space-y-2">
          <span className="text-sm text-slate-400">Студент</span>
          <select
            value={studentId}
            onChange={(event) => setStudentId(event.target.value)}
            className="w-full rounded-2xl border border-slate-700 bg-slate-950 px-4 py-3 text-slate-100 outline-none focus:border-blue-400"
          >
            <option value="">Выберите студента</option>
            {(studentsQuery.data ?? []).map((student) => (
              <option key={student.studentId} value={student.studentId}>
                {student.fullName} ({student.groupCode})
              </option>
            ))}
          </select>
        </label>

        <label className="space-y-2">
          <span className="text-sm text-slate-400">Предмет</span>
          <select
            value={courseId}
            onChange={(event) => setCourseId(event.target.value)}
            className="w-full rounded-2xl border border-slate-700 bg-slate-950 px-4 py-3 text-slate-100 outline-none focus:border-blue-400"
          >
            <option value="">Выберите предмет</option>
            {(coursesQuery.data ?? []).map((course) => (
              <option key={course.courseId} value={course.courseId}>
                {course.courseName}
              </option>
            ))}
          </select>
        </label>
      </div>

      {detailsQuery.data && (
        <>
          <div className="grid gap-4 md:grid-cols-3">
            <div className="rounded-3xl border border-slate-800 bg-slate-950/70 p-4">
              <div className="text-sm text-slate-400">Средний балл по всем предметам</div>
              <div className="mt-2 text-3xl font-bold text-white">
                {detailsQuery.data.averageAllCourses.toFixed(2)}
              </div>
            </div>

            <div className="rounded-3xl border border-slate-800 bg-slate-950/70 p-4">
              <div className="text-sm text-slate-400">Средний балл по выбранному предмету</div>
              <div className="mt-2 text-3xl font-bold text-white">
                {detailsQuery.data.averageSelectedCourse.toFixed(2)}
              </div>
            </div>

            <div className="rounded-3xl border border-slate-800 bg-slate-950/70 p-4">
              <div className="text-sm text-slate-400">Прогнозная следующая оценка</div>
              <div className="mt-2 text-3xl font-bold text-white">
                {detailsQuery.data.predictedFinalGrade}
              </div>
            </div>
          </div>

          <div className="rounded-3xl border border-slate-800 bg-slate-950/70 p-5">
            <h3 className="text-lg font-semibold text-white">Рекомендации</h3>
            <p className="mt-2 text-slate-300">{detailsQuery.data.recommendationSummary}</p>

            <ul className="mt-4 space-y-2 text-slate-300">
              {(detailsQuery.data.recommendations ?? []).length > 0 ? (
                detailsQuery.data.recommendations.map((recommendation, index) => (
                  <li
                    key={`${recommendation}-${index}`}
                    className="rounded-2xl border border-slate-800 bg-slate-900 px-4 py-3"
                  >
                    {recommendation}
                  </li>
                ))
              ) : (
                <li className="rounded-2xl border border-slate-800 bg-slate-900 px-4 py-3">
                  Рекомендации не требуются: результаты по предмету хорошие.
                </li>
              )}
            </ul>
          </div>

          <div className="rounded-3xl border border-slate-800 bg-slate-950/70 p-5">
            <h3 className="text-lg font-semibold text-white">График оценок</h3>
            <p className="mt-2 text-sm text-slate-400">
              По оси X — номер оценки, по оси Y — балл. Последняя точка выделена отдельно как прогноз.
            </p>

            <div className="mt-4 overflow-x-auto">
              <svg width="760" height="320" viewBox="0 0 760 320" className="min-w-[760px]">
                <line x1="20" y1="300" x2="740" y2="300" stroke="#475569" strokeWidth="1" />
                <line x1="20" y1="20" x2="20" y2="300" stroke="#475569" strokeWidth="1" />

                {[0, 2, 4, 6, 8, 10].map((value) => {
                  const y = 300 - (value / 10) * 280;
                  return (
                    <g key={value}>
                      <line x1="20" y1={y} x2="740" y2={y} stroke="#1e293b" strokeWidth="1" />
                      <text x="4" y={y + 4} fontSize="12" fill="#94a3b8">
                        {value}
                      </text>
                    </g>
                  );
                })}

                {chartData.length > 1 && (
                  <polyline
                    fill="none"
                    stroke="#60a5fa"
                    strokeWidth="3"
                    points={polylinePoints}
                  />
                )}

                {chartData.map((point, index) => {
                  const maxX = Math.max(...chartData.map((item) => item.x));
                  const minX = Math.min(...chartData.map((item) => item.x));
                  const px =
                    maxX === minX
                      ? 380
                      : ((point.x - minX) / (maxX - minX)) * (760 - 40) + 20;
                  const py = 320 - (((point.y - 0) / (10 - 0)) * (320 - 40) + 20);

                  return (
                    <g key={`${point.x}-${index}`}>
                      <circle
                        cx={px}
                        cy={py}
                        r={point.isForecast ? 7 : 5}
                        fill={point.isForecast ? '#f43f5e' : '#60a5fa'}
                      />
                      <text x={px - 4} y={315} fontSize="12" fill="#94a3b8">
                        {point.x}
                      </text>
                    </g>
                  );
                })}
              </svg>
            </div>
          </div>

          <div className="overflow-x-auto rounded-3xl border border-slate-800 bg-slate-950/70">
            <table className="min-w-full text-left text-sm text-slate-300">
              <thead className="bg-slate-900 text-slate-400">
                <tr>
                  <th className="px-4 py-3 font-medium">№</th>
                  <th className="px-4 py-3 font-medium">Дата выставления</th>
                  <th className="px-4 py-3 font-medium">Тип оценки</th>
                  <th className="px-4 py-3 font-medium">Оценка</th>
                </tr>
              </thead>
              <tbody>
                {(detailsQuery.data.points ?? []).map((point) => (
                  <tr key={`${point.orderNo}-${point.gradedAt}`} className="border-t border-slate-800">
                    <td className="px-4 py-3">{point.orderNo}</td>
                    <td className="px-4 py-3">{point.gradedAt}</td>
                    <td className="px-4 py-3">{point.gradeType}</td>
                    <td className="px-4 py-3 font-semibold text-white">{point.value}</td>
                  </tr>
                ))}

                <tr className="border-t border-rose-500/30 bg-rose-500/5">
                  <td className="px-4 py-3">{(detailsQuery.data.points?.length ?? 0) + 1}</td>
                  <td className="px-4 py-3">Прогноз</td>
                  <td className="px-4 py-3">Следующая оценка</td>
                  <td className="px-4 py-3 font-semibold text-rose-300">
                    {detailsQuery.data.predictedFinalGrade}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </>
      )}
    </section>
  );
}