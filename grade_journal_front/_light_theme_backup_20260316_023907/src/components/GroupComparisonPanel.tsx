import { useQuery } from '@tanstack/react-query';
import { api } from '../lib/api';

type Props = {
  title: string;
  description: string;
};

type GroupComparison = {
  groupId: number;
  groupCode: string;
  courseNo: number;
  studentCount: number;
  averageGrade: number;
};

export function GroupComparisonPanel({ title, description }: Props) {
  const query = useQuery({
    queryKey: ['analytics', 'group-comparison'],
    queryFn: async () => (await api.get<GroupComparison[]>('/api/analytics/group-comparison')).data
  });

  return (
    <section className="space-y-5 rounded-3xl border border-slate-800 bg-slate-900/70 p-6">
      <div>
        <h2 className="text-2xl font-semibold text-white">{title}</h2>
        <p className="mt-2 text-slate-400">{description}</p>
      </div>

      <div className="overflow-x-auto rounded-3xl border border-slate-800 bg-slate-950/70">
        <table className="min-w-full text-left text-sm text-slate-300">
          <thead className="bg-slate-900 text-slate-400">
            <tr>
              <th className="px-4 py-3 font-medium">Группа</th>
              <th className="px-4 py-3 font-medium">Курс</th>
              <th className="px-4 py-3 font-medium">Количество студентов</th>
              <th className="px-4 py-3 font-medium">Средний балл</th>
            </tr>
          </thead>
          <tbody>
            {(query.data ?? []).map((group) => (
              <tr key={group.groupId} className="border-t border-slate-800">
                <td className="px-4 py-3 font-medium text-white">{group.groupCode}</td>
                <td className="px-4 py-3">{group.courseNo}</td>
                <td className="px-4 py-3">{group.studentCount}</td>
                <td className="px-4 py-3">{group.averageGrade}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}