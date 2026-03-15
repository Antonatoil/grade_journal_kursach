import { useEffect, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../lib/api';

type Props = {
  title: string;
  description: string;
};

type TeacherOption = {
  teacherId: number;
  fullName: string;
  departmentName: string;
  position: string;
};

type TeacherLesson = {
  lessonDate: string;
  timeSlot: string;
  courseName: string;
  groupCode: string;
  room: string;
  lessonType: string;
  topic: string;
};

export function TeacherSchedulePanel({ title, description }: Props) {
  const [teacherId, setTeacherId] = useState<string>('');

  const teachersQuery = useQuery({
    queryKey: ['analytics', 'teachers'],
    queryFn: async () => (await api.get<TeacherOption[]>('/api/analytics/teachers')).data
  });

  useEffect(() => {
    if (!teacherId && teachersQuery.data?.length) {
      setTeacherId(String(teachersQuery.data[0].teacherId));
    }
  }, [teacherId, teachersQuery.data]);

  const scheduleQuery = useQuery({
    queryKey: ['analytics', 'teacher-schedule', teacherId],
    enabled: Boolean(teacherId),
    queryFn: async () =>
      (
        await api.get<TeacherLesson[]>('/api/analytics/teacher-schedule', {
          params: { teacherId }
        })
      ).data
  });

  return (
    <section className="space-y-5 rounded-3xl border border-slate-800 bg-slate-900/70 p-6">
      <div>
        <h2 className="text-2xl font-semibold text-white">{title}</h2>
        <p className="mt-2 text-slate-400">{description}</p>
      </div>

      <label className="block space-y-2">
        <span className="text-sm text-slate-400">Преподаватель</span>
        <select
          value={teacherId}
          onChange={(event) => setTeacherId(event.target.value)}
          className="w-full rounded-2xl border border-slate-700 bg-slate-950 px-4 py-3 text-slate-100 outline-none focus:border-blue-400"
        >
          <option value="">Выберите преподавателя</option>
          {(teachersQuery.data ?? []).map((teacher) => (
            <option key={teacher.teacherId} value={teacher.teacherId}>
              {teacher.fullName} — {teacher.departmentName}
            </option>
          ))}
        </select>
      </label>

      <div className="overflow-x-auto rounded-3xl border border-slate-800 bg-slate-950/70">
        <table className="min-w-full text-left text-sm text-slate-300">
          <thead className="bg-slate-900 text-slate-400">
            <tr>
              <th className="px-4 py-3 font-medium">Дата</th>
              <th className="px-4 py-3 font-medium">Время</th>
              <th className="px-4 py-3 font-medium">Предмет</th>
              <th className="px-4 py-3 font-medium">Группа</th>
              <th className="px-4 py-3 font-medium">Аудитория</th>
              <th className="px-4 py-3 font-medium">Тип</th>
              <th className="px-4 py-3 font-medium">Тема</th>
            </tr>
          </thead>
          <tbody>
            {(scheduleQuery.data ?? []).map((lesson, index) => (
              <tr key={`${lesson.lessonDate}-${lesson.timeSlot}-${lesson.groupCode}-${index}`} className="border-t border-slate-800">
                <td className="px-4 py-3">{lesson.lessonDate}</td>
                <td className="px-4 py-3">{lesson.timeSlot}</td>
                <td className="px-4 py-3 font-medium text-white">{lesson.courseName}</td>
                <td className="px-4 py-3">{lesson.groupCode}</td>
                <td className="px-4 py-3">{lesson.room || '—'}</td>
                <td className="px-4 py-3">{lesson.lessonType}</td>
                <td className="px-4 py-3">{lesson.topic || '—'}</td>
              </tr>
            ))}
            {teacherId && !scheduleQuery.data?.length && (
              <tr className="border-t border-slate-800">
                <td colSpan={7} className="px-4 py-6 text-center text-slate-400">
                  Для выбранного преподавателя пары не найдены.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </section>
  );
}