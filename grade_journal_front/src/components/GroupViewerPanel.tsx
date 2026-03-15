import { useEffect, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../lib/api';

type GroupOption = {
  groupId: number;
  groupCode: string;
  courseNo: number;
  facultyName: string;
  specializationName: string;
};

type GroupStudent = {
  studentId: number;
  username: string;
  fullName: string;
  email?: string | null;
  studentCard: string;
  groupCode: string;
  courseNo: number;
  active: boolean;
  approved: boolean;
};

type Props = {
  title: string;
  description: string;
};

export function GroupViewerPanel({ title, description }: Props) {
  const groupsQuery = useQuery({
    queryKey: ['group-viewer', 'groups'],
    queryFn: async () => (await api.get<GroupOption[]>('/api/group-viewer/groups')).data
  });

  const [selectedGroupId, setSelectedGroupId] = useState<string>('');

  useEffect(() => {
    if (!selectedGroupId && groupsQuery.data && groupsQuery.data.length > 0) {
      setSelectedGroupId(String(groupsQuery.data[0].groupId));
    }
  }, [groupsQuery.data, selectedGroupId]);

  const studentsQuery = useQuery({
    queryKey: ['group-viewer', 'students', selectedGroupId],
    queryFn: async () => (await api.get<GroupStudent[]>(`/api/group-viewer/groups/${selectedGroupId}/students`)).data,
    enabled: Boolean(selectedGroupId)
  });

  const selectedGroup = (groupsQuery.data ?? []).find((group) => String(group.groupId) === selectedGroupId) ?? null;

  return (
    <section className="space-y-5 rounded-3xl border border-slate-800 bg-slate-900/70 p-6">
      <div>
        <h2 className="text-2xl font-semibold text-white">{title}</h2>
        <p className="mt-2 max-w-4xl text-slate-400">{description}</p>
      </div>

      <div className="grid gap-4 lg:grid-cols-[360px,1fr] lg:items-end">
        <label className="space-y-2">
          <span className="text-sm text-slate-400">Группа</span>
          <select
            value={selectedGroupId}
            onChange={(event) => setSelectedGroupId(event.target.value)}
            className="w-full rounded-2xl border border-slate-700 bg-slate-950 px-4 py-3 text-slate-100 outline-none focus:border-blue-400"
          >
            {(groupsQuery.data ?? []).map((group) => (
              <option key={group.groupId} value={group.groupId}>
                {group.groupCode} — курс {group.courseNo}
              </option>
            ))}
          </select>
        </label>

        {selectedGroup && (
          <div className="rounded-2xl border border-slate-800 bg-slate-950/70 px-4 py-3 text-sm text-slate-300">
            <div className="font-medium text-white">{selectedGroup.groupCode}</div>
            <div className="mt-1">Курс: {selectedGroup.courseNo}</div>
            <div className="mt-1">Факультет: {selectedGroup.facultyName}</div>
            <div className="mt-1">Специальность: {selectedGroup.specializationName}</div>
          </div>
        )}
      </div>

      <div className="overflow-hidden rounded-3xl border border-slate-800 bg-slate-950/70">
        <div className="overflow-x-auto">
          <table className="min-w-full text-left text-sm text-slate-300">
            <thead className="bg-slate-900 text-slate-400">
              <tr>
                <th className="px-4 py-3 font-medium">ФИО</th>
                <th className="px-4 py-3 font-medium">Логин</th>
                <th className="px-4 py-3 font-medium">Email</th>
                <th className="px-4 py-3 font-medium">Группа</th>
                <th className="px-4 py-3 font-medium">Курс</th>
                <th className="px-4 py-3 font-medium">Студенческий билет</th>
                <th className="px-4 py-3 font-medium">Активен</th>
                <th className="px-4 py-3 font-medium">Одобрен</th>
              </tr>
            </thead>
            <tbody>
              {(studentsQuery.data ?? []).map((student) => (
                <tr key={student.studentId} className="border-t border-slate-800">
                  <td className="px-4 py-3 font-medium text-white">{student.fullName}</td>
                  <td className="px-4 py-3">{student.username}</td>
                  <td className="px-4 py-3">{student.email || '—'}</td>
                  <td className="px-4 py-3">{student.groupCode}</td>
                  <td className="px-4 py-3">{student.courseNo}</td>
                  <td className="px-4 py-3">{student.studentCard}</td>
                  <td className="px-4 py-3">{student.active ? 'Да' : 'Нет'}</td>
                  <td className="px-4 py-3">{student.approved ? 'Да' : 'Нет'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {studentsQuery.isLoading && <div className="text-sm text-slate-400">Загрузка студентов группы…</div>}
      {studentsQuery.isError && <div className="text-sm text-rose-300">Не удалось загрузить студентов выбранной группы.</div>}
      {studentsQuery.data && studentsQuery.data.length === 0 && <div className="text-sm text-slate-400">В выбранной группе пока нет студентов.</div>}
    </section>
  );
}