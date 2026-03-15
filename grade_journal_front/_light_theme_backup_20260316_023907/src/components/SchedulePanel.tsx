import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { api } from '../lib/api';
import { queryClient } from '../lib/queryClient';
import type {
  ScheduleCourseOption,
  ScheduleEntry,
  ScheduleGroupOption,
  ScheduleMetaResponse,
  ScheduleTeacherOption
} from '../types/schedule';

const lessonTypeLabel: Record<string, string> = {
  lecture: 'Лекция',
  practice: 'Практика',
  lab: 'Лабораторная',
  seminar: 'Семинар',
  exam: 'Экзамен',
  consultation: 'Консультация'
};

type Props = {
  canManage: boolean;
  title: string;
  description: string;
};

type ScheduleFormState = {
  groupId: string;
  teacherId: string;
  courseId: string;
  lessonDate: string;
  timeSlot: string;
  lessonType: string;
  room: string;
  topic: string;
};

const emptyForm: ScheduleFormState = {
  groupId: '',
  teacherId: '',
  courseId: '',
  lessonDate: '',
  timeSlot: '',
  lessonType: 'lecture',
  room: '',
  topic: ''
};

export function SchedulePanel({ canManage, title, description }: Props) {
  const [selectedGroupId, setSelectedGroupId] = useState('');
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [feedbackMessage, setFeedbackMessage] = useState<string | null>(null);
  const [form, setForm] = useState<ScheduleFormState>(emptyForm);

  const metaQuery = useQuery({
    queryKey: ['schedule', 'meta'],
    queryFn: async () => {
      const response = await api.get<ScheduleMetaResponse>('/api/schedule/meta');
      return response.data;
    }
  });

  useEffect(() => {
    if (!selectedGroupId && metaQuery.data?.groups.length) {
      const firstGroupId = String(metaQuery.data.groups[0].groupId);
      setSelectedGroupId(firstGroupId);
      setForm((prev) => ({ ...prev, groupId: firstGroupId }));
    }
  }, [metaQuery.data, selectedGroupId]);

  const selectedGroup = useMemo<ScheduleGroupOption | undefined>(() => {
    return metaQuery.data?.groups.find((group) => String(group.groupId) === selectedGroupId);
  }, [metaQuery.data, selectedGroupId]);

  const availableCourses = useMemo<ScheduleCourseOption[]>(() => {
    const courses = metaQuery.data?.courses ?? [];
    if (!selectedGroup) {
      return courses;
    }
    return courses.filter((course) => course.studyYear === selectedGroup.courseNo);
  }, [metaQuery.data, selectedGroup]);

  const scheduleQuery = useQuery({
    queryKey: ['schedule', 'group', selectedGroupId],
    enabled: Boolean(selectedGroupId),
    queryFn: async () => {
      const response = await api.get<ScheduleEntry[]>(`/api/schedule/groups/${selectedGroupId}`);
      return response.data;
    }
  });

  const createScheduleMutation = useMutation({
    mutationFn: async () => {
      const response = await api.post<{ message: string }>('/api/schedule', {
        groupId: Number(form.groupId),
        teacherId: Number(form.teacherId),
        courseId: Number(form.courseId),
        lessonDate: form.lessonDate,
        timeSlot: form.timeSlot,
        lessonType: form.lessonType,
        room: form.room,
        topic: form.topic
      });
      return response.data;
    },
    onSuccess: async (data) => {
      setFeedbackMessage(data.message || 'Расписание успешно добавлено');
      setShowCreateForm(false);
      setForm((prev) => ({
        ...emptyForm,
        groupId: prev.groupId,
        lessonType: 'lecture'
      }));
      await queryClient.invalidateQueries({ queryKey: ['schedule', 'group', selectedGroupId] });
    },
    onError: (error: any) => {
      setFeedbackMessage(error?.response?.data?.message || 'Не удалось добавить запись в расписание');
    }
  });

  const handleGroupChange = (groupId: string) => {
    setSelectedGroupId(groupId);
    setForm((prev) => ({
      ...prev,
      groupId,
      courseId: ''
    }));
  };

  const submitSchedule = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setFeedbackMessage(null);
    createScheduleMutation.mutate();
  };

  const now = new Date();
  const todayString = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;

  const formatLocalDate = (value: string) => {
    const [year, month, day] = value.split('-');
    return day && month && year ? `${day}.${month}.${year}` : value;
  };

  return (
    <section className="space-y-6 rounded-3xl border border-slate-800 bg-slate-900/70 p-6 w-full max-w-none min-w-0 overflow-hidden">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between min-w-0">
        <div>
          <h2 className="text-2xl font-semibold text-white">{title}</h2>
          <p className="mt-2 max-w-4xl text-slate-400">{description}</p>
        </div>
        {canManage && (
          <button
            onClick={() => {
              setFeedbackMessage(null);
              setShowCreateForm((prev) => !prev);
              setForm((prev) => ({ ...prev, groupId: selectedGroupId || prev.groupId }));
            }}
            className="rounded-2xl bg-emerald-500 px-4 py-3 font-medium text-white transition hover:bg-emerald-400"
          >
            {showCreateForm ? 'Скрыть форму добавления' : 'Добавить расписание'}
          </button>
        )}
      </div>

      {feedbackMessage && (
        <div className="rounded-2xl border border-blue-500/20 bg-blue-500/10 px-4 py-3 text-sm text-blue-100">
          {feedbackMessage}
        </div>
      )}

      <div className="grid gap-4 lg:grid-cols-[320px,1fr]">
        <div className="rounded-3xl border border-slate-800 bg-slate-950/80 p-5">
          <label className="space-y-2">
            <span className="text-sm text-slate-400">Выберите группу</span>
            <select
              value={selectedGroupId}
              onChange={(event) => handleGroupChange(event.target.value)}
              className="w-full rounded-2xl border border-slate-700 bg-slate-900 px-4 py-3 text-slate-100 outline-none focus:border-blue-400"
            >
              {(metaQuery.data?.groups ?? []).map((group) => (
                <option key={group.groupId} value={group.groupId}>
                  {group.groupCode} · курс {group.courseNo}
                </option>
              ))}
            </select>
          </label>

          {selectedGroup && (
            <div className="mt-4 space-y-2 rounded-2xl border border-slate-800 bg-slate-900/80 p-4 text-sm text-slate-300">
              <p><span className="text-slate-500">Группа:</span> {selectedGroup.groupCode}</p>
              <p><span className="text-slate-500">Курс:</span> {selectedGroup.courseNo}</p>
              <p><span className="text-slate-500">Факультет:</span> {selectedGroup.facultyName}</p>
              <p><span className="text-slate-500">Специальность:</span> {selectedGroup.specializationName}</p>
            </div>
          )}
        </div>

        <div className="rounded-3xl border border-slate-800 bg-slate-950/80 p-5">
          <h3 className="text-lg font-semibold text-white">Таблица расписания</h3>
          <p className="mt-1 text-sm text-slate-400">После выбора группы отображается расписание текущего семестра.</p>

          {scheduleQuery.isLoading ? (
            <p className="mt-6 text-slate-400">Загрузка расписания...</p>
          ) : (
            <div className="mt-4 overflow-x-auto rounded-2xl border border-slate-800">
              <table className="min-w-[1100px] table-auto text-left text-sm text-slate-200">
                <thead className="bg-slate-900 text-slate-300">
                  <tr>
                    <th className="px-3 py-3 whitespace-nowrap align-top">Дата</th>
                    <th className="px-3 py-3 whitespace-nowrap align-top">Время</th>
                    <th className="px-3 py-3 whitespace-nowrap align-top">Дисциплина</th>
                    <th className="px-3 py-3 whitespace-nowrap align-top">Преподаватель</th>
                    <th className="px-3 py-3 whitespace-nowrap align-top">Аудитория</th>
                    <th className="px-3 py-3 whitespace-nowrap align-top">Тип</th>
                    <th className="px-3 py-3 whitespace-nowrap align-top">Тема</th>
                  </tr>
                </thead>
                <tbody>
                  {(scheduleQuery.data ?? []).length === 0 ? (
                    <tr>
                      <td colSpan={7} className="px-4 py-10 text-center text-slate-400">
                        Для выбранной группы пока нет записей в расписании.
                      </td>
                    </tr>
                  ) : (
                    (scheduleQuery.data ?? []).map((entry) => (
                      <tr key={entry.scheduleId} className="border-t border-slate-800 align-top hover:bg-slate-900/70">
                        <td className="px-3 py-4 whitespace-nowrap align-top">{formatLocalDate(entry.lessonDate)}</td>
                        <td className="px-3 py-4 whitespace-nowrap align-top">{entry.timeSlot}</td>
                        <td className="px-3 py-4 font-medium text-white whitespace-nowrap align-top">{entry.courseName}</td>
                        <td className="px-3 py-4 whitespace-nowrap align-top">{entry.teacherFullName}</td>
                        <td className="px-3 py-4 whitespace-nowrap align-top">{entry.room || '—'}</td>
                        <td className="px-3 py-4 whitespace-nowrap align-top">{lessonTypeLabel[entry.lessonType] || entry.lessonType}</td>
                        <td className="px-3 py-4 whitespace-nowrap align-top">{entry.topic || '—'}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {canManage && showCreateForm && (
        <div className="rounded-3xl border border-emerald-500/20 bg-slate-950/80 p-6">
          <div className="mb-4">
            <h3 className="text-lg font-semibold text-white">Добавление записи в расписание</h3>
            <p className="mt-1 text-sm text-slate-400">
              Администратор может добавить новую пару. Проверки на прошлую дату, пересечение времени у группы и занятость преподавателя выполняются на сервере.
            </p>
          </div>

          <form onSubmit={submitSchedule} className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            <label className="space-y-2">
              <span className="text-sm text-slate-400">Группа</span>
              <select
                value={form.groupId}
                onChange={(event) => {
                  handleGroupChange(event.target.value);
                  setForm((prev) => ({ ...prev, groupId: event.target.value, courseId: '' }));
                }}
                className="w-full rounded-2xl border border-slate-700 bg-slate-900 px-4 py-3 text-slate-100 outline-none focus:border-emerald-400"
                required
              >
                <option value="">Выберите группу</option>
                {(metaQuery.data?.groups ?? []).map((group) => (
                  <option key={group.groupId} value={group.groupId}>
                    {group.groupCode} · курс {group.courseNo}
                  </option>
                ))}
              </select>
            </label>

            <label className="space-y-2">
              <span className="text-sm text-slate-400">Дата</span>
              <input
                type="date"
                min={todayString}
                value={form.lessonDate}
                onChange={(event) => setForm((prev) => ({ ...prev, lessonDate: event.target.value }))}
                className="w-full rounded-2xl border border-slate-700 bg-slate-900 px-4 py-3 text-slate-100 outline-none focus:border-emerald-400"
                required
              />
            </label>

            <label className="space-y-2">
              <span className="text-sm text-slate-400">Время пары</span>
              <select
                value={form.timeSlot}
                onChange={(event) => setForm((prev) => ({ ...prev, timeSlot: event.target.value }))}
                className="w-full rounded-2xl border border-slate-700 bg-slate-900 px-4 py-3 text-slate-100 outline-none focus:border-emerald-400"
                required
              >
                <option value="">Выберите время</option>
                {(metaQuery.data?.timeSlots ?? []).map((slot) => (
                  <option key={slot} value={slot}>{slot}</option>
                ))}
              </select>
            </label>

            <label className="space-y-2">
              <span className="text-sm text-slate-400">Тип занятия</span>
              <select
                value={form.lessonType}
                onChange={(event) => setForm((prev) => ({ ...prev, lessonType: event.target.value }))}
                className="w-full rounded-2xl border border-slate-700 bg-slate-900 px-4 py-3 text-slate-100 outline-none focus:border-emerald-400"
                required
              >
                {(metaQuery.data?.lessonTypes ?? []).map((type) => (
                  <option key={type} value={type}>{lessonTypeLabel[type] || type}</option>
                ))}
              </select>
            </label>

            <label className="space-y-2 xl:col-span-2">
              <span className="text-sm text-slate-400">Дисциплина</span>
              <select
                value={form.courseId}
                onChange={(event) => setForm((prev) => ({ ...prev, courseId: event.target.value }))}
                className="w-full rounded-2xl border border-slate-700 bg-slate-900 px-4 py-3 text-slate-100 outline-none focus:border-emerald-400"
                required
              >
                <option value="">Выберите дисциплину</option>
                {availableCourses.map((course) => (
                  <option key={course.courseId} value={course.courseId}>
                    {course.courseName} ({course.courseCode})
                  </option>
                ))}
              </select>
            </label>

            <label className="space-y-2 xl:col-span-2">
              <span className="text-sm text-slate-400">Преподаватель</span>
              <select
                value={form.teacherId}
                onChange={(event) => setForm((prev) => ({ ...prev, teacherId: event.target.value }))}
                className="w-full rounded-2xl border border-slate-700 bg-slate-900 px-4 py-3 text-slate-100 outline-none focus:border-emerald-400"
                required
              >
                <option value="">Выберите преподавателя</option>
                {(metaQuery.data?.teachers ?? []).map((teacher: ScheduleTeacherOption) => (
                  <option key={teacher.teacherId} value={teacher.teacherId}>
                    {teacher.fullName} · {teacher.departmentName}
                  </option>
                ))}
              </select>
            </label>

            <label className="space-y-2">
              <span className="text-sm text-slate-400">Аудитория</span>
              <input
                value={form.room}
                onChange={(event) => setForm((prev) => ({ ...prev, room: event.target.value }))}
                className="w-full rounded-2xl border border-slate-700 bg-slate-900 px-4 py-3 text-slate-100 outline-none focus:border-emerald-400"
                placeholder="Например: А-301"
              />
            </label>

            <label className="space-y-2 xl:col-span-3">
              <span className="text-sm text-slate-400">Тема</span>
              <input
                value={form.topic}
                onChange={(event) => setForm((prev) => ({ ...prev, topic: event.target.value }))}
                className="w-full rounded-2xl border border-slate-700 bg-slate-900 px-4 py-3 text-slate-100 outline-none focus:border-emerald-400"
                placeholder="Например: Введение в тему занятия"
              />
            </label>

            <div className="xl:col-span-4 flex flex-wrap gap-3 pt-2">
              <button
                type="submit"
                disabled={createScheduleMutation.isPending}
                className="rounded-2xl bg-emerald-500 px-4 py-3 font-medium text-white transition hover:bg-emerald-400 disabled:cursor-not-allowed disabled:opacity-70"
              >
                {createScheduleMutation.isPending ? 'Сохранение...' : 'Сохранить расписание'}
              </button>
              <button
                type="button"
                onClick={() => setShowCreateForm(false)}
                className="rounded-2xl border border-slate-700 bg-slate-900 px-4 py-3 font-medium text-slate-200 transition hover:bg-slate-800"
              >
                Отмена
              </button>
            </div>
          </form>
        </div>
      )}
    </section>
  );
}