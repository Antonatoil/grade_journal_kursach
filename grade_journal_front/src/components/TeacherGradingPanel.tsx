import { useEffect, useMemo, useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { api } from '../lib/api';
import { queryClient } from '../lib/queryClient';

type Props = {
  title: string;
  description: string;
};

type TeacherGroupOption = {
  groupId: number;
  groupCode: string;
  courseNo: number;
};

type TeacherLessonOption = {
  scheduleId: number;
  lessonDate: string;
  timeSlot: string;
  courseName: string;
  groupCode: string;
  topic: string;
};

type TeacherLessonStudent = {
  studentId: number;
  enrollmentId: number;
  fullName: string;
  studentCard: string;
  present: boolean;
  missedHours: number;
  gradeValue: number | null;
  teacherComment: string | null;
};

type EditableRow = {
  studentId: number;
  enrollmentId: number;
  fullName: string;
  studentCard: string;
  present: boolean;
  missedHours: number;
  gradeValue: string;
  teacherComment: string;
};

export function TeacherGradingPanel({ title, description }: Props) {
  const [groupId, setGroupId] = useState('');
  const [scheduleId, setScheduleId] = useState('');
  const [rows, setRows] = useState<EditableRow[]>([]);
  const [message, setMessage] = useState<string | null>(null);

  const groupsQuery = useQuery({
    queryKey: ['teacher-grading', 'groups'],
    queryFn: async () =>
      (await api.get<TeacherGroupOption[]>('/api/teacher-grading/groups')).data
  });

  useEffect(() => {
    if (!groupId && groupsQuery.data?.length) {
      setGroupId(String(groupsQuery.data[0].groupId));
    }
  }, [groupId, groupsQuery.data]);

  const lessonsQuery = useQuery({
    queryKey: ['teacher-grading', 'lessons', groupId],
    enabled: Boolean(groupId),
    queryFn: async () =>
      (
        await api.get<TeacherLessonOption[]>('/api/teacher-grading/lessons', {
          params: { groupId }
        })
      ).data
  });

  useEffect(() => {
    if (!scheduleId && lessonsQuery.data?.length) {
      setScheduleId(String(lessonsQuery.data[0].scheduleId));
    }
  }, [scheduleId, lessonsQuery.data]);

  const studentsQuery = useQuery({
    queryKey: ['teacher-grading', 'lesson-students', scheduleId],
    enabled: Boolean(scheduleId),
    queryFn: async () =>
      (
        await api.get<TeacherLessonStudent[]>('/api/teacher-grading/lesson-students', {
          params: { scheduleId }
        })
      ).data
  });

  useEffect(() => {
    if (studentsQuery.data) {
      setRows(
        studentsQuery.data.map((student) => ({
          studentId: student.studentId,
          enrollmentId: student.enrollmentId,
          fullName: student.fullName,
          studentCard: student.studentCard,
          present: student.present,
          missedHours: student.missedHours,
          gradeValue: student.gradeValue == null ? '' : String(Math.round(student.gradeValue)),
          teacherComment: student.teacherComment ?? ''
        }))
      );
    } else {
      setRows([]);
    }
  }, [studentsQuery.data]);

  const selectedLesson = useMemo(
    () => (lessonsQuery.data ?? []).find((lesson) => String(lesson.scheduleId) === scheduleId) ?? null,
    [lessonsQuery.data, scheduleId]
  );

  const saveMutation = useMutation({
    mutationFn: async () => {
      if (!scheduleId) {
        return;
      }

      await api.post(`/api/teacher-grading/lesson-students/${scheduleId}/save`, {
        students: rows.map((row) => ({
          studentId: row.studentId,
          enrollmentId: row.enrollmentId,
          present: row.present,
          gradeValue: row.gradeValue === '' ? null : Number(row.gradeValue),
          teacherComment: row.teacherComment || null
        }))
      });
    },
    onSuccess: async () => {
      setMessage('Оценки и посещаемость успешно сохранены.');
      await queryClient.invalidateQueries({
        queryKey: ['teacher-grading', 'lesson-students', scheduleId]
      });
      await queryClient.invalidateQueries({ queryKey: ['performance'] });
      await queryClient.invalidateQueries({ queryKey: ['student-profile'] });
    },
    onError: (error: any) => {
      setMessage(error?.response?.data?.message || 'Не удалось сохранить изменения.');
    }
  });

  const updateRow = (index: number, patch: Partial<EditableRow>) => {
    setRows((prev) => prev.map((row, rowIndex) => (rowIndex === index ? { ...row, ...patch } : row)));
  };

  return (
    <section className="space-y-5 rounded-3xl border border-slate-800 bg-slate-900/70 p-6">
      <div>
        <h2 className="text-2xl font-semibold text-white">{title}</h2>
        <p className="mt-2 text-slate-400">{description}</p>
      </div>

      {message && (
        <div className="rounded-2xl border border-blue-500/30 bg-blue-500/10 px-4 py-3 text-blue-100">
          {message}
        </div>
      )}

      <div className="grid gap-4 xl:grid-cols-2">
        <label className="space-y-2">
          <span className="text-sm text-slate-400">Группа</span>
          <select
            value={groupId}
            onChange={(event) => {
              setGroupId(event.target.value);
              setScheduleId('');
            }}
            className="w-full rounded-2xl border border-slate-700 bg-slate-950 px-4 py-3 text-slate-100 outline-none focus:border-blue-400"
          >
            <option value="">Выберите группу</option>
            {(groupsQuery.data ?? []).map((group) => (
              <option key={group.groupId} value={group.groupId}>
                {group.groupCode} — курс {group.courseNo}
              </option>
            ))}
          </select>
        </label>

        <label className="space-y-2">
          <span className="text-sm text-slate-400">Пара</span>
          <select
            value={scheduleId}
            onChange={(event) => setScheduleId(event.target.value)}
            className="w-full rounded-2xl border border-slate-700 bg-slate-950 px-4 py-3 text-slate-100 outline-none focus:border-blue-400"
          >
            <option value="">Выберите пару</option>
            {(lessonsQuery.data ?? []).map((lesson) => (
              <option key={lesson.scheduleId} value={lesson.scheduleId}>
                {lesson.lessonDate} | {lesson.timeSlot} | {lesson.courseName}
              </option>
            ))}
          </select>
        </label>
      </div>

      {selectedLesson && (
        <div className="rounded-3xl border border-slate-800 bg-slate-950/70 p-4 text-sm text-slate-300">
          <div><span className="text-slate-400">Предмет:</span> {selectedLesson.courseName}</div>
          <div><span className="text-slate-400">Группа:</span> {selectedLesson.groupCode}</div>
          <div><span className="text-slate-400">Дата и время:</span> {selectedLesson.lessonDate} | {selectedLesson.timeSlot}</div>
          <div><span className="text-slate-400">Тема:</span> {selectedLesson.topic || '—'}</div>
        </div>
      )}

      <div className="overflow-x-auto rounded-3xl border border-slate-800 bg-slate-950/70">
        <table className="min-w-full text-left text-sm text-slate-300">
          <thead className="bg-slate-900 text-slate-400">
            <tr>
              <th className="px-4 py-3 font-medium">ФИО</th>
              <th className="px-4 py-3 font-medium">Студенческий билет</th>
              <th className="px-4 py-3 font-medium">Присутствовал</th>
              <th className="px-4 py-3 font-medium">Пропущено часов</th>
              <th className="px-4 py-3 font-medium">Оценка</th>
              <th className="px-4 py-3 font-medium">Комментарий</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row, index) => (
              <tr key={row.enrollmentId} className="border-t border-slate-800">
                <td className="px-4 py-3 font-medium text-white">{row.fullName}</td>
                <td className="px-4 py-3">{row.studentCard}</td>
                <td className="px-4 py-3">
                  <input
                    type="checkbox"
                    checked={row.present}
                    onChange={(event) => updateRow(index, { present: event.target.checked })}
                    className="h-4 w-4"
                  />
                </td>
                <td className="px-4 py-3">{row.missedHours}</td>
                <td className="px-4 py-3">
                  <input
                    type="number"
                    min="0"
                    max="10"
                    step="1"
                    value={row.gradeValue}
                    onChange={(event) => updateRow(index, { gradeValue: event.target.value })}
                    className="w-28 rounded-2xl border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100 outline-none focus:border-blue-400"
                  />
                </td>
                <td className="px-4 py-3">
                  <input
                    value={row.teacherComment}
                    onChange={(event) => updateRow(index, { teacherComment: event.target.value })}
                    className="w-full min-w-[220px] rounded-2xl border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100 outline-none focus:border-blue-400"
                  />
                </td>
              </tr>
            ))}
            {scheduleId && !rows.length && (
              <tr className="border-t border-slate-800">
                <td colSpan={6} className="px-4 py-6 text-center text-slate-400">
                  Для выбранной пары студенты не найдены.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <div className="flex justify-end">
        <button
          type="button"
          onClick={() => saveMutation.mutate()}
          disabled={saveMutation.isPending || !scheduleId}
          className="rounded-2xl bg-blue-500 px-5 py-3 font-medium text-white hover:bg-blue-400 disabled:opacity-70"
        >
          {saveMutation.isPending ? 'Сохранение...' : 'Сохранить'}
        </button>
      </div>
    </section>
  );
}