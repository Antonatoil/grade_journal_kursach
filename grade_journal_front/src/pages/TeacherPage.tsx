import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../lib/api';
import { PerformancePanel } from '../components/PerformancePanel';
import { StudentProfileViewerPanel } from '../components/StudentProfileViewerPanel';
import { GroupViewerPanel } from '../components/GroupViewerPanel';
import { ExcelExportPanel } from '../components/ExcelExportPanel';
import { SchedulePanel } from '../components/SchedulePanel';
import { UserDirectoryPanel, type DirectoryControlMode } from '../components/UserDirectoryPanel';
import type { ProfileResponse } from '../types/profile';
import { TeacherSchedulePanel } from '../components/TeacherSchedulePanel';
import { GroupComparisonPanel } from '../components/GroupComparisonPanel';
import { StudentComparisonPanel } from '../components/StudentComparisonPanel';
import { RiskGroupsPanel } from '../components/RiskGroupsPanel';
import { TeacherGradingPanel } from '../components/TeacherGradingPanel';

type PanelKey =
  | 'home'
  | 'profile'
  | 'directory-explorer'
  | 'schedule'
  | 'teacher-schedule'
  | 'teacher-grading'
  | 'performance'
  | 'student-profile'
  | 'group-viewer'
  | 'group-comparison'
  | 'student-comparison'
  | 'risk-groups'
  | 'excel-export';

function getDirectoryMode(panel: PanelKey): DirectoryControlMode | null {
  return panel === 'directory-explorer' ? 'explorer' : null;
}

export function TeacherPage() {
  const [activePanel, setActivePanel] = useState<PanelKey>('home');

  const { data, isLoading } = useQuery({
    queryKey: ['profile', 'me'],
    queryFn: async () => (await api.get<ProfileResponse>('/api/profile/me')).data
  });

  if (isLoading) {
    return <p className="text-slate-400">Загрузка профиля...</p>;
  }

  const directoryMode = getDirectoryMode(activePanel);

  const buttons: Array<{ key: PanelKey; title: string; subtitle: string }> = [
    { key: 'profile', title: 'Мой профиль', subtitle: 'Личные данные преподавателя' },
    {
      key: 'directory-explorer',
      title: 'Поиск, фильтр и сортировка',
      subtitle: 'Общий справочник пользователей'
    },
    { key: 'schedule', title: 'Расписание', subtitle: 'Просмотр расписания групп' },
    {
      key: 'teacher-schedule',
      title: 'Расписание преподавателей',
      subtitle: 'Пары выбранного преподавателя'
    },
    {
      key: 'teacher-grading',
      title: 'Оценки студента',
      subtitle: 'Выставление оценок и пропусков'
    },
    {
      key: 'performance',
      title: 'Успеваемость',
      subtitle: 'Оценки, средние баллы и прогноз'
    },
    {
      key: 'student-profile',
      title: 'Профиль студента',
      subtitle: 'Полный просмотр данных'
    },
    { key: 'group-viewer', title: 'Просмотр групп', subtitle: 'Состав выбранной группы' },
    {
      key: 'group-comparison',
      title: 'Сравнение групп',
      subtitle: 'Средний балл по группам'
    },
    {
      key: 'student-comparison',
      title: 'Сравнение студентов',
      subtitle: 'Таблица со средними баллами'
    },
    {
      key: 'risk-groups',
      title: 'Группы риска',
      subtitle: 'Распределение студентов по рискам'
    },
    {
      key: 'excel-export',
      title: 'Экспорт в Excel',
      subtitle: 'Формирование отчетов'
    }
  ];

  return (
    <div className="w-full max-w-none min-w-0 space-y-6">
      <section className="w-full max-w-none min-w-0 rounded-3xl border border-slate-800 bg-slate-900/70 p-8">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <h1 className="text-3xl font-bold text-white">Главная панель преподавателя</h1>
            <p className="mt-2 max-w-4xl text-slate-400">
              Здесь собраны все основные разделы для работы преподавателя: просмотр профиля,
              расписание, выставление оценок, работа с успеваемостью, просмотр студентов,
              аналитика по группам и экспорт отчетов.
            </p>
          </div>

          <button
            onClick={() => setActivePanel('home')}
            className="rounded-2xl border border-slate-700 bg-slate-950 px-4 py-3 font-medium text-slate-200 hover:bg-slate-800"
          >
            На главную панель
          </button>
        </div>

        <div className="mt-6 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
          {buttons.map((button) => (
            <button
              key={button.key}
              onClick={() => setActivePanel(button.key)}
              className={`rounded-2xl px-5 py-4 text-left transition ${
                activePanel === button.key
                  ? 'bg-blue-500 text-white'
                  : 'bg-slate-950 text-slate-200 hover:bg-slate-800'
              }`}
            >
              <div className="text-sm opacity-80">{button.subtitle}</div>
              <div className="mt-1 text-lg font-semibold">{button.title}</div>
            </button>
          ))}
        </div>
      </section>

      {activePanel === 'home' && (
        <section className="w-full max-w-none min-w-0 rounded-3xl border border-slate-800 bg-slate-900/70 p-6 text-slate-300">
          <h2 className="text-2xl font-semibold text-white">Рабочее пространство преподавателя</h2>
          <p className="mt-2 text-slate-400">
            Выберите нужный раздел с помощью кнопок выше. Преподаватель может просматривать
            расписание, выставлять оценки и посещаемость, анализировать результаты студентов,
            сравнивать группы и формировать Excel-отчеты.
          </p>
        </section>
      )}

      {activePanel === 'profile' && (
        <section className="grid gap-4 md:grid-cols-2">
          <div className="rounded-3xl border border-slate-800 bg-slate-900/70 p-6">
            <h2 className="mb-4 text-xl font-semibold text-white">Основная информация</h2>
            <div className="space-y-3 text-slate-300">
              <p>
                <span className="text-slate-500">ФИО:</span> {data?.fullName}
              </p>
              <p>
                <span className="text-slate-500">Логин:</span> {data?.username}
              </p>
              <p>
                <span className="text-slate-500">Email:</span> {data?.email || '—'}
              </p>
              <p>
                <span className="text-slate-500">Роль:</span> Преподаватель
              </p>
            </div>
          </div>

          <div className="rounded-3xl border border-slate-800 bg-slate-900/70 p-6">
            <h2 className="mb-4 text-xl font-semibold text-white">Информация преподавателя</h2>
            <div className="space-y-3 text-slate-300">
              <p>
                <span className="text-slate-500">Кафедра:</span>{' '}
                {data?.teacher?.departmentName || '—'}
              </p>
              <p>
                <span className="text-slate-500">Код кафедры:</span>{' '}
                {data?.teacher?.departmentCode || '—'}
              </p>
              <p>
                <span className="text-slate-500">Должность:</span>{' '}
                {data?.teacher?.position || '—'}
              </p>
              <p>
                <span className="text-slate-500">Телефон:</span>{' '}
                {data?.teacher?.phone || '—'}
              </p>
            </div>
          </div>
        </section>
      )}

      {directoryMode && (
        <div className="directory-panel-shell w-full max-w-none min-w-0">
          <UserDirectoryPanel
          endpoint="/api/users/directory"
          queryKey={['directory', 'teacher']}
          title="Поиск, фильтр и сортировка"
          description="Используйте единое окно поиска, фильтров и сортировки для просмотра справочника пользователей."
          activeControl={directoryMode}
         />
        </div>
      )}

      {activePanel === 'schedule' && (
        <div className="schedule-panel-shell w-full max-w-none min-w-0">
          <SchedulePanel
          canManage={false}
          title="Расписание"
          description="Преподаватель может просматривать расписание выбранной группы."
         />
        </div>
      )}

      {activePanel === 'teacher-schedule' && (
        <TeacherSchedulePanel
          title="Расписание преподавателей"
          description="Выберите преподавателя, чтобы увидеть список его пар."
        />
      )}

      {activePanel === 'teacher-grading' && (
        <TeacherGradingPanel
          title="Оценки студента"
          description="Выберите группу, затем пару, после чего отредактируйте присутствие, оценки и комментарии по этой паре."
        />
      )}

      {activePanel === 'performance' && (
        <PerformancePanel
          title="Успеваемость"
          description="Выберите студента и предмет, чтобы увидеть оценки, средние показатели и прогнозную следующую оценку."
        />
      )}

      {activePanel === 'student-profile' && (
        <StudentProfileViewerPanel
          title="Профиль студента"
          description="Преподаватель может найти любого студента и просмотреть его профиль, оценки по всем предметам, прогнозы и рекомендации."
        />
      )}

      {activePanel === 'group-viewer' && (
        <GroupViewerPanel
          title="Просмотр групп"
          description="Преподаватель может выбрать любую группу и посмотреть состав группы с базовой информацией о студентах."
        />
      )}

      {activePanel === 'group-comparison' && (
        <GroupComparisonPanel
          title="Сравнение групп"
          description="Список всех групп и их средний балл."
        />
      )}

      {activePanel === 'student-comparison' && (
        <StudentComparisonPanel
          title="Сравнение студентов"
          description="Таблица всех студентов со средним баллом по всем оценкам."
        />
      )}

      {activePanel === 'risk-groups' && (
        <RiskGroupsPanel
          title="Группы риска"
          description="Распределение студентов на три группы по среднему баллу."
        />
      )}

      {activePanel === 'excel-export' && (
        <ExcelExportPanel
          title="Экспорт в Excel"
          description="Преподаватель может выбрать нужные разделы и скачать Excel-отчет по журналу успеваемости."
        />
      )}
    </div>
  );
}