import { useEffect, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../lib/api';
import type { StudentProfileOption, StudentProfileResponse } from '../types/studentProfile';

type Props = {
  title: string;
  description: string;
};

function formatDate(value: string) {
  if (!value) {
    return '—';
  }
  const [year, month, day] = value.slice(0, 10).split('-');
  return `${day}.${month}.${year}`;
}

function riskLabel(value: string) {
  if (value === 'high') return 'Высокий';
  if (value === 'medium') return 'Средний';
  return 'Низкий';
}

export function StudentProfileViewerPanel({ title, description }: Props) {
  const [search, setSearch] = useState('');
  const [studentId, setStudentId] = useState('');

  const optionsQuery = useQuery({
    queryKey: ['student-profile-options', search],
    queryFn: async () => {
      const response = await api.get<StudentProfileOption[]>('/api/student-profiles/options', {
        params: {
          query: search
        }
      });
      return response.data;
    }
  });

  useEffect(() => {
    const options = optionsQuery.data ?? [];
    if (!options.length) {
      setStudentId('');
      return;
    }

    const exists = options.some((option) => String(option.studentId) === studentId);
    if (!studentId || !exists) {
      setStudentId(String(options[0].studentId));
    }
  }, [optionsQuery.data, studentId]);

  const profileQuery = useQuery({
    queryKey: ['student-profile', studentId],
    enabled: Boolean(studentId),
    queryFn: async () => {
      const response = await api.get<StudentProfileResponse>(`/api/student-profiles/${studentId}`);
      return response.data;
    }
  });

  const errorMessage =
    (profileQuery.error as any)?.response?.data?.message ??
    'Не удалось загрузить профиль студента.';

  return (
    <section className="space-y-5 rounded-3xl border border-slate-800 bg-slate-900/70 p-6">
      <div>
        <h2 className="text-2xl font-semibold text-white">{title}</h2>
        <p className="mt-2 text-slate-400">{description}</p>
      </div>

      <div className="grid gap-4 lg:grid-cols-[1.2fr_1fr]">
        <label className="space-y-2">
          <span className="text-sm text-slate-400">Поиск студента</span>
          <input
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="ФИО, email, группа или студенческий билет"
            className="w-full rounded-2xl border border-slate-700 bg-slate-950 px-4 py-3 text-slate-100 outline-none focus:border-blue-400"
          />
        </label>

        <label className="space-y-2">
          <span className="text-sm text-slate-400">Студент</span>
          <select
            value={studentId}
            onChange={(event) => setStudentId(event.target.value)}
            className="w-full rounded-2xl border border-slate-700 bg-slate-950 px-4 py-3 text-slate-100 outline-none focus:border-blue-400"
          >
            {(optionsQuery.data ?? []).map((option) => (
              <option key={option.studentId} value={option.studentId}>
                {option.fullName} · {option.groupCode}
              </option>
            ))}
          </select>
        </label>
      </div>

      {optionsQuery.isLoading && <p className="text-slate-400">Загрузка списка студентов...</p>}

      {!optionsQuery.isLoading && (optionsQuery.data ?? []).length === 0 && (
        <div className="rounded-2xl border border-amber-500/30 bg-amber-500/10 px-4 py-3 text-amber-200">
          По заданному запросу студенты не найдены.
        </div>
      )}

      {profileQuery.isLoading && <p className="text-slate-400">Загрузка профиля студента...</p>}

      {profileQuery.isError && (
        <div className="rounded-2xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-rose-200">
          {errorMessage}
        </div>
      )}

      {profileQuery.data && (
        <>
          <div className="grid gap-4 xl:grid-cols-3">
            <div className="rounded-3xl border border-slate-800 bg-slate-950/80 p-5">
              <div className="text-sm text-slate-500">ФИО</div>
              <div className="mt-2 text-xl font-semibold text-white">{profileQuery.data.fullName}</div>
              <div className="mt-3 text-sm text-slate-400">Логин: {profileQuery.data.username}</div>
              <div className="text-sm text-slate-400">Email: {profileQuery.data.email || '—'}</div>
            </div>
            <div className="rounded-3xl border border-slate-800 bg-slate-950/80 p-5">
              <div className="text-sm text-slate-500">Учебные данные</div>
              <div className="mt-2 space-y-2 text-sm text-slate-300">
                <div>Группа: <span className="text-white">{profileQuery.data.groupCode}</span></div>
                <div>Курс: <span className="text-white">{profileQuery.data.courseNo}</span></div>
                <div>Студенческий билет: <span className="text-white">{profileQuery.data.studentCard || '—'}</span></div>
              </div>
            </div>
            <div className="rounded-3xl border border-slate-800 bg-slate-950/80 p-5">
              <div className="text-sm text-slate-500">Факультет и специальность</div>
              <div className="mt-2 text-sm text-slate-300">{profileQuery.data.facultyName}</div>
              <div className="mt-2 text-sm text-white">{profileQuery.data.specializationName}</div>
              <div className="mt-4 text-sm text-slate-400">
                Статус: {profileQuery.data.active ? 'активен' : 'не активен'} · {profileQuery.data.approved ? 'одобрен' : 'не одобрен'}
              </div>
            </div>
          </div>

          <div className="space-y-4">
            {profileQuery.data.subjects.length === 0 && (
              <div className="rounded-2xl border border-slate-800 bg-slate-950/80 px-4 py-6 text-center text-slate-500">
                У этого студента пока нет учебных записей и оценок.
              </div>
            )}

            {profileQuery.data.subjects.map((subject) => (
              <div key={subject.courseId} className="rounded-3xl border border-slate-800 bg-slate-950/70 p-5">
                <div className="flex flex-col gap-4 xl:flex-row xl:items-start xl:justify-between">
                  <div>
                    <h3 className="text-xl font-semibold text-white">{subject.courseName}</h3>
                    <p className="mt-1 text-sm text-slate-400">{subject.courseCode}</p>
                  </div>
                  <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
                    <div className="rounded-2xl border border-slate-800 bg-slate-900/80 px-4 py-3">
                      <div className="text-xs uppercase tracking-wide text-slate-500">Средний балл</div>
                      <div className="mt-1 text-lg font-semibold text-white">{subject.averageGrade.toFixed(2)}</div>
                    </div>
                    <div className="rounded-2xl border border-blue-500/30 bg-blue-500/10 px-4 py-3">
                      <div className="text-xs uppercase tracking-wide text-blue-200">Прогнозный балл</div>
                      <div className="mt-1 text-lg font-semibold text-white">{subject.predictedGrade.toFixed(2)}</div>
                    </div>
                    <div className="rounded-2xl border border-slate-800 bg-slate-900/80 px-4 py-3">
                      <div className="text-xs uppercase tracking-wide text-slate-500">Посещаемость</div>
                      <div className="mt-1 text-lg font-semibold text-white">{subject.attendanceRate.toFixed(2)}%</div>
                    </div>
                    <div className="rounded-2xl border border-slate-800 bg-slate-900/80 px-4 py-3">
                      <div className="text-xs uppercase tracking-wide text-slate-500">Риск / пропуски</div>
                      <div className="mt-1 text-lg font-semibold text-white">{riskLabel(subject.riskLevel)} · {subject.missedHours} ч</div>
                    </div>
                  </div>
                </div>

                <div className="mt-5 overflow-x-auto rounded-2xl border border-slate-800">
                  <table className="min-w-full text-left text-sm text-slate-300">
                    <thead className="bg-slate-900 text-slate-400">
                      <tr>
                        <th className="px-4 py-3 font-medium">Дата</th>
                        <th className="px-4 py-3 font-medium">Оценка</th>
                        <th className="px-4 py-3 font-medium">Тип оценки</th>
                        <th className="px-4 py-3 font-medium">Контрольная точка</th>
                      </tr>
                    </thead>
                    <tbody>
                      {subject.grades.length === 0 && (
                        <tr>
                          <td colSpan={4} className="px-4 py-6 text-center text-slate-500">
                            По этому предмету пока нет выставленных оценок.
                          </td>
                        </tr>
                      )}
                      {subject.grades.map((grade, index) => (
                        <tr key={`${subject.courseId}-${index}`} className="border-t border-slate-800">
                          <td className="px-4 py-3">{formatDate(grade.gradedDate)}</td>
                          <td className="px-4 py-3 font-semibold text-white">{grade.gradeValue.toFixed(2)}</td>
                          <td className="px-4 py-3">{grade.assessmentType}</td>
                          <td className="px-4 py-3">{grade.assessmentTitle}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>

                <div className="mt-4">
                  <div className="mb-2 text-sm font-medium text-slate-300">Рекомендации</div>
                  {subject.recommendations.length === 0 ? (
                    <div className="rounded-2xl border border-slate-800 bg-slate-900/80 px-4 py-3 text-sm text-slate-500">
                      Для этого предмета пока нет рекомендаций.
                    </div>
                  ) : (
                    <div className="space-y-2">
                      {subject.recommendations.map((item, index) => (
                        <div key={`${subject.courseId}-rec-${index}`} className="rounded-2xl border border-slate-800 bg-slate-900/80 px-4 py-3 text-sm text-slate-300">
                          {item}
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        </>
      )}
    </section>
  );
}