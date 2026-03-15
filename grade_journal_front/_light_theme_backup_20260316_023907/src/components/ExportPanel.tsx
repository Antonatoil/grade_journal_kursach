import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { api } from '../lib/api';

type ExportPayload = {
  students: boolean;
  teachers: boolean;
  courses: boolean;
  schedule: boolean;
  performance: boolean;
  attendance: boolean;
};

type Props = {
  title: string;
  description: string;
};

const initialPayload: ExportPayload = {
  students: true,
  teachers: true,
  courses: false,
  schedule: true,
  performance: true,
  attendance: true
};

const checkboxItems: Array<{ key: keyof ExportPayload; label: string; hint: string }> = [
  { key: 'students', label: 'Студенты', hint: 'Список студентов с базовыми данными и группами.' },
  { key: 'teachers', label: 'Преподаватели', hint: 'Список преподавателей, кафедр и должностей.' },
  { key: 'courses', label: 'Справочник курсов', hint: 'Коды, названия, курс, часы и форма контроля.' },
  { key: 'schedule', label: 'Расписание занятий', hint: 'Группы, предметы, преподаватели, даты и время пар.' },
  { key: 'performance', label: 'Успеваемость', hint: 'Оценки, средние баллы, прогнозы и уровни риска.' },
  { key: 'attendance', label: 'Посещаемость', hint: 'Посещения, пропуски и статусы по каждому занятию.' }
];

export function ExportPanel({ title, description }: Props) {
  const [payload, setPayload] = useState<ExportPayload>(initialPayload);
  const [message, setMessage] = useState<string | null>(null);

  const exportMutation = useMutation({
    mutationFn: async () => {
      const response = await api.post('/api/reports/export', payload, { responseType: 'blob' });
      const blob = new Blob([response.data], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
      });

      const disposition = response.headers['content-disposition'] as string | undefined;
      const fileNameMatch = disposition?.match(/filename="([^"]+)"/);
      const fileName = fileNameMatch?.[1] ?? 'grade-journal-report.xlsx';

      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = fileName;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    },
    onSuccess: () => setMessage('Excel-отчет успешно сформирован и скачан.'),
    onError: (error: any) => setMessage(error?.response?.data?.message || 'Не удалось сформировать Excel-отчет.')
  });

  const selectedCount = Object.values(payload).filter(Boolean).length;

  return (
    <section className="space-y-5 rounded-3xl border border-slate-800 bg-slate-900/70 p-6">
      <div>
        <h2 className="text-2xl font-semibold text-white">{title}</h2>
        <p className="mt-2 max-w-4xl text-slate-400">{description}</p>
      </div>

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        {checkboxItems.map((item) => (
          <label key={item.key} className="flex items-start gap-3 rounded-2xl border border-slate-800 bg-slate-950/70 px-4 py-4">
            <input
              type="checkbox"
              checked={payload[item.key]}
              onChange={(event) => setPayload((prev) => ({ ...prev, [item.key]: event.target.checked }))}
              className="mt-1 h-4 w-4 rounded border-slate-600 bg-slate-900 text-blue-500"
            />
            <div>
              <div className="font-medium text-white">{item.label}</div>
              <div className="mt-1 text-sm text-slate-400">{item.hint}</div>
            </div>
          </label>
        ))}
      </div>

      <div className="flex flex-wrap items-center gap-3">
        <button
          onClick={() => exportMutation.mutate()}
          disabled={exportMutation.isPending || selectedCount === 0}
          className="rounded-2xl bg-blue-500 px-4 py-3 font-medium text-white hover:bg-blue-400 disabled:opacity-60"
        >
          {exportMutation.isPending ? 'Формирование Excel…' : 'Сформировать Excel-отчет'}
        </button>
        <div className="text-sm text-slate-400">Выбрано разделов: {selectedCount}</div>
      </div>

      {message && <div className="rounded-2xl border border-blue-500/30 bg-blue-500/10 px-4 py-3 text-blue-100">{message}</div>}
    </section>
  );
}