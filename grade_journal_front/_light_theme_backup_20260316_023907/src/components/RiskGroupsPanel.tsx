import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../lib/api';

type Props = {
  title: string;
  description: string;
};

type RiskGroupStudent = {
  riskBand: 'high' | 'medium' | 'low';
  studentId: number;
  fullName: string;
  groupCode: string;
  studentCard: string;
  averageGrade: number;
};

function RiskTable({
  caption,
  rows
}: {
  caption: string;
  rows: RiskGroupStudent[];
}) {
  return (
    <div className="overflow-x-auto rounded-3xl border border-slate-800 bg-slate-950/70">
      <div className="border-b border-slate-800 bg-slate-900 px-4 py-3 text-sm font-medium text-slate-300">
        {caption}
      </div>
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
          {rows.map((student) => (
            <tr key={student.studentId} className="border-t border-slate-800">
              <td className="px-4 py-3 font-medium text-white">{student.fullName}</td>
              <td className="px-4 py-3">{student.groupCode}</td>
              <td className="px-4 py-3">{student.studentCard}</td>
              <td className="px-4 py-3">{student.averageGrade}</td>
            </tr>
          ))}
          {!rows.length && (
            <tr className="border-t border-slate-800">
              <td colSpan={4} className="px-4 py-6 text-center text-slate-400">
                Нет студентов в этой категории.
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}

export function RiskGroupsPanel({ title, description }: Props) {
  const query = useQuery({
    queryKey: ['analytics', 'risk-groups'],
    queryFn: async () => (await api.get<RiskGroupStudent[]>('/api/analytics/risk-groups')).data
  });

  const grouped = useMemo(() => {
    const rows = query.data ?? [];

    return {
      high: rows.filter((item) => item.riskBand === 'high'),
      medium: rows.filter((item) => item.riskBand === 'medium'),
      low: rows.filter((item) => item.riskBand === 'low')
    };
  }, [query.data]);

  return (
    <section className="space-y-5 rounded-3xl border border-slate-800 bg-slate-900/70 p-6">
      <div>
        <h2 className="text-2xl font-semibold text-white">{title}</h2>
        <p className="mt-2 text-slate-400">{description}</p>
      </div>

      <RiskTable caption="Средняя оценка больше 7.5" rows={grouped.high} />
      <RiskTable caption="Средняя оценка от 6 до 7.5" rows={grouped.medium} />
      <RiskTable caption="Средняя оценка меньше 6" rows={grouped.low} />
    </section>
  );
}