import { useMemo, useState } from 'react';
import axios from 'axios';
import { api } from '../lib/api';

type Props = {
  title: string;
  description: string;
};

type ExportOptionKey =
  | 'students'
  | 'teachers'
  | 'courses'
  | 'schedule'
  | 'performance'
  | 'attendance';

const options: Array<{ key: ExportOptionKey; label: string }> = [
  { key: 'students', label: 'Студенты' },
  { key: 'teachers', label: 'Преподаватели' },
  { key: 'courses', label: 'Справочник курсов' },
  { key: 'schedule', label: 'Расписание занятий' },
  { key: 'performance', label: 'Успеваемость' },
  { key: 'attendance', label: 'Посещаемость' }
];

function buildParams(selected: Record<ExportOptionKey, boolean>) {
  return {
    students: selected.students,
    teachers: selected.teachers,
    courses: selected.courses,
    schedule: selected.schedule,
    performance: selected.performance,
    attendance: selected.attendance
  };
}

export function ExcelExportPanel({ title, description }: Props) {
  const [selected, setSelected] = useState<Record<ExportOptionKey, boolean>>({
    students: true,
    teachers: true,
    courses: false,
    schedule: true,
    performance: true,
    attendance: true
  });

  const [isLoading, setIsLoading] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  const selectedCount = useMemo(
    () => Object.values(selected).filter(Boolean).length,
    [selected]
  );

  const toggle = (key: ExportOptionKey) => {
    setSelected((prev) => ({ ...prev, [key]: !prev[key] }));
  };

  const exportExcel = async () => {
    if (selectedCount === 0) {
      setMessage('Выберите хотя бы один раздел для экспорта.');
      return;
    }

    setIsLoading(true);
    setMessage(null);

    const params = buildParams(selected);

    const candidates = [
      '/api/export/excel',
      '/api/reports/excel',
      '/api/admin/export/excel',
      '/api/teacher/export/excel'
    ];

    try {
      let response: any = null;
      let lastError: unknown = null;

      for (const url of candidates) {
        try {
          response = await api.get(url, {
            params,
            responseType: 'blob'
          });
          break;
        } catch (error) {
          lastError = error;
        }
      }

      if (!response) {
        throw lastError ?? new Error('Не удалось получить файл экспорта.');
      }

      const blob = new Blob(
        [response.data],
        {
          type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
        }
      );

      const href = URL.createObjectURL(blob);
      const link = document.createElement('a');
      const timestamp = new Date().toISOString().slice(0, 19).replace(/[:T]/g, '-');

      link.href = href;
      link.download = `grade-journal-report-${timestamp}.xlsx`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(href);

      setMessage('Excel-отчет успешно сформирован.');
    } catch (error: unknown) {
      if (axios.isAxiosError(error)) {
        setMessage(
          error.response?.data?.message ||
            'Не удалось сформировать Excel-отчет. Проверьте backend endpoint экспорта.'
        );
      } else {
        setMessage('Не удалось сформировать Excel-отчет.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <section className="space-y-5 rounded-3xl border border-slate-800 bg-slate-900/70 p-6">
      <div>
        <h2 className="text-2xl font-semibold text-white">{title}</h2>
        <p className="mt-2 max-w-4xl text-slate-400">{description}</p>
      </div>

      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
        {options.map((option) => (
          <label
            key={option.key}
            className="flex items-center gap-3 rounded-2xl border border-slate-800 bg-slate-950/70 px-4 py-3 text-slate-200"
          >
            <input
              type="checkbox"
              checked={selected[option.key]}
              onChange={() => toggle(option.key)}
              className="h-4 w-4 rounded border-slate-600 bg-slate-900"
            />
            <span>{option.label}</span>
          </label>
        ))}
      </div>

      <div className="flex flex-wrap items-center gap-3">
        <button
          type="button"
          onClick={exportExcel}
          disabled={isLoading}
          className="rounded-2xl bg-emerald-500 px-4 py-3 font-medium text-white hover:bg-emerald-400 disabled:opacity-70"
        >
          {isLoading ? 'Формирование Excel...' : 'Скачать Excel-отчет'}
        </button>

        <div className="text-sm text-slate-400">
          Выбрано разделов: {selectedCount}
        </div>
      </div>

      {message && (
        <div className="rounded-2xl border border-slate-800 bg-slate-950/70 px-4 py-3 text-sm text-slate-300">
          {message}
        </div>
      )}
    </section>
  );
}