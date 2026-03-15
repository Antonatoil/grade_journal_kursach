import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../lib/api';

type Props = {
  title: string;
  description: string;
};

type StudentComparison = {
  studentId: number;
  fullName: string;
  groupCode: string;
  studentCard: string;
  averageGrade: number;
};

export function StudentComparisonPanel({ title, description }: Props) {
  const [sort, setSort] = useState<'asc' | 'desc'>('desc');

  const query = useQuery({
    queryKey: ['analytics', 'student-comparison', sort],
    queryFn: async () =>
      (
        await api.get<StudentComparison[]>('/api/analytics/student-comparison', {
          params: { sort }
        })
      ).data
  });

  return (
    <section className="space-y-5 rounded-3xl border border-slate-800 bg-slate-900/70 p-6">
      <div className="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <h2 className="text-2xl font-semibold text-white">{title}</h2>
          <p className="mt-2 text-slate-400">{description}</p>
        </div>

        <label className="space-y-2">
          <span className="text-sm text-slate-400">Сортировка по среднему баллу</span>
          <select
            value={sort}
            onChange={(event) => setSort(event.target.value as 'asc' | 'desc')}
            className="rounded-2xl border border-slate-700 bg-slate-950 px-4 py-3 text-slate-100 outline-none focus:border-blue-400"
          >
            <option value="desc">По убыванию</option>
            <option value="asc">По возрастанию</option>
          </select>
        </label>
      </div>

      <div className="overflow-x-auto rounded-3xl border border-slate-800 bg-slate-950/70">
        <table className="min-w-full text-left text-sm text-slate-300">
          <thead className="bg-slate-900 text-slate-400">
            <tr>
              <th className="px-4 py-3 font-medium">ФИО</th>
              <th className="px-4 py-3 font-medium">Группа</th>
              <th className="px-4 py-3 font-medium">Студенческий билет</th>
              <th className="px-4 py-3 font-medium">Средний балл</th>
            </tr>
          </thead>
          <tbody>
            {(query.data ?? []).map((student) => (
              <tr key={student.studentId} className="border-t border-slate-800">
                <td className="px-4 py-3 font-medium text-white">{student.fullName}</td>
                <td className="px-4 py-3">{student.groupCode}</td>
                <td className="px-4 py-3">{student.studentCard}</td>
                <td className="px-4 py-3">{student.averageGrade}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}