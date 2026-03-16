import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../lib/api';
import { PerformancePanel } from '../components/PerformancePanel';
import { StudentProfileViewerPanel } from '../components/StudentProfileViewerPanel';
import { GroupViewerPanel } from '../components/GroupViewerPanel';
import { ExcelExportPanel } from '../components/ExcelExportPanel';
import { SchedulePanel } from '../components/SchedulePanel';
import { UserDirectoryPanel, type DirectoryControlMode } from '../components/UserDirectoryPanel';
import { TeacherSchedulePanel } from '../components/TeacherSchedulePanel';
import { GroupComparisonPanel } from '../components/GroupComparisonPanel';
import { StudentComparisonPanel } from '../components/StudentComparisonPanel';
import { RiskGroupsPanel } from '../components/RiskGroupsPanel';
import { TeacherGradingPanel } from '../components/TeacherGradingPanel';
import type { ProfileResponse as BaseProfileResponse } from '../types/profile';

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

type ProfileApiResponse = BaseProfileResponse & {
  studentInfo?: BaseProfileResponse['student'];
  teacherInfo?: BaseProfileResponse['teacher'];
};

function getDirectoryMode(panel: PanelKey): DirectoryControlMode | null {
  return panel === 'directory-explorer' ? 'explorer' : null;
}

function profileValue(value: unknown) {
  if (value === null || value === undefined || value === '') {
    return '—';
  }
  return String(value);
}

function normalizeProfile(raw: ProfileApiResponse): BaseProfileResponse {
  return {
    ...raw,
    student: raw.student ?? raw.studentInfo ?? null,
    teacher: raw.teacher ?? raw.teacherInfo ?? null
  };
}

export function TeacherPage() {
  const [activePanel, setActivePanel] = useState<PanelKey>('home');

  const profileQuery = useQuery({
    queryKey: ['profile', 'me', 'teacher-page'],
    queryFn: async () => normalizeProfile((await api.get<ProfileApiResponse>('/api/profile/me')).data)
  });

  const directoryMode = getDirectoryMode(activePanel);
  const data = profileQuery.data;

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
    { key: 'performance', title: 'Успеваемость', subtitle: 'Оценки, средние баллы и прогноз' },
    { key: 'student-profile', title: 'Профиль студента', subtitle: 'Просмотр студента по курсам' },
    { key: 'group-viewer', title: 'Просмотр групп', subtitle: 'Состав выбранной группы' },
    { key: 'group-comparison', title: 'Сравнение групп', subtitle: 'Средний балл по группам' },
    {
      key: 'student-comparison',
      title: 'Сравнение студентов',
      subtitle: 'Таблица со средними баллами'
    },
    { key: 'risk-groups', title: 'Группы риска', subtitle: 'Распределение студентов по рискам' },
    { key: 'excel-export', title: 'Экспорт в Excel', subtitle: 'Формирование отчётов' }
  ];

  return (
    <div className="space-y-6">
      <section className="rounded-3xl border border-slate-300 bg-white p-8 shadow-sm">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <h1 className="text-3xl font-bold text-slate-900">Панель преподавателя</h1>
            <p className="mt-2 max-w-4xl text-slate-600">
              Здесь собраны разделы преподавателя: профиль, расписание, оценки, успеваемость,
              просмотр студентов и аналитические панели.
            </p>
          </div>

          <button
            onClick={() => setActivePanel('home')}
            className="rounded-2xl border border-slate-300 bg-slate-50 px-4 py-3 font-medium text-slate-700 transition hover:bg-slate-100"
          >
            На главную
          </button>
        </div>

        <div className="mt-6 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
          {buttons.map((button) => (
            <button
              key={button.key}
              onClick={() => setActivePanel(button.key)}
              className={`rounded-2xl px-5 py-4 text-left transition ${
                activePanel === button.key
                  ? 'bg-blue-600 text-white'
                  : 'border border-slate-300 bg-slate-50 text-slate-800 hover:bg-slate-100'
              }`}
            >
              <div className="text-sm opacity-80">{button.subtitle}</div>
              <div className="mt-1 text-lg font-semibold">{button.title}</div>
            </button>
          ))}
        </div>
      </section>

      {activePanel === 'home' && (
        <section className="rounded-3xl border border-slate-300 bg-white p-6 shadow-sm">
          <h2 className="text-2xl font-semibold text-slate-900">Рабочее пространство преподавателя</h2>
          <p className="mt-2 text-slate-600">
            Выберите нужный раздел сверху. Основные проблемы с профилем, списками студентов и
            выставлением оценок вынесены в отдельные панели.
          </p>
        </section>
      )}

      {activePanel === 'profile' && (
        <section className="grid gap-4 md:grid-cols-2">
          <div className="rounded-3xl border border-slate-300 bg-white p-6 shadow-sm">
            <h2 className="mb-4 text-xl font-semibold text-slate-900">Основные данные</h2>

            {profileQuery.isLoading ? (
              <p className="text-slate-500">Загрузка профиля...</p>
            ) : profileQuery.isError ? (
              <p className="text-rose-600">
                Не удалось загрузить профиль преподавателя. Проверь backend /api/profile/me.
              </p>
            ) : (
              <div className="space-y-3 text-slate-700">
                <p>
                  <span className="text-slate-500">ФИО:</span> {profileValue(data?.fullName)}
                </p>
                <p>
                  <span className="text-slate-500">Логин:</span> {profileValue(data?.username)}
                </p>
                <p>
                  <span className="text-slate-500">Email:</span> {profileValue(data?.email)}
                </p>
                <p>
                  <span className="text-slate-500">Роль:</span> {profileValue(data?.role ?? 'teacher')}
                </p>
              </div>
            )}
          </div>

          <div className="rounded-3xl border border-slate-300 bg-white p-6 shadow-sm">
            <h2 className="mb-4 text-xl font-semibold text-slate-900">Данные преподавателя</h2>

            {profileQuery.isLoading ? (
              <p className="text-slate-500">Загрузка данных преподавателя...</p>
            ) : profileQuery.isError ? (
              <p className="text-rose-600">Профиль не удалось отобразить.</p>
            ) : (
              <div className="space-y-3 text-slate-700">
                <p>
                  <span className="text-slate-500">Кафедра:</span>{' '}
                  {profileValue(data?.teacher?.departmentName)}
                </p>
                <p>
                  <span className="text-slate-500">Код кафедры:</span>{' '}
                  {profileValue(data?.teacher?.departmentCode)}
                </p>
                <p>
                  <span className="text-slate-500">Должность:</span>{' '}
                  {profileValue(data?.teacher?.position)}
                </p>
                <p>
                  <span className="text-slate-500">Телефон:</span>{' '}
                  {profileValue(data?.teacher?.phone)}
                </p>
              </div>
            )}
          </div>
        </section>
      )}

      {directoryMode && (
        <UserDirectoryPanel
          endpoint="/api/users/directory"
          queryKey={['directory', 'teacher']}
          title="Поиск, фильтр и сортировка"
          description="Используйте поиск, фильтры и сортировку для просмотра пользователей системы."
          activeControl={directoryMode}
        />
      )}

      {activePanel === 'schedule' && (
        <SchedulePanel
          canManage={false}
          title="Расписание"
          description="Просмотр расписания групп."
        />
      )}

      {activePanel === 'teacher-schedule' && (
        <TeacherSchedulePanel
          title="Расписание преподавателей"
          description="Выберите преподавателя и просмотрите его пары."
        />
      )}

      {activePanel === 'teacher-grading' && (
        <TeacherGradingPanel
          title="Оценки студента"
          description="Выберите группу, затем пару, после чего отобразится таблица студентов с оценками и посещаемостью."
        />
      )}

      {activePanel === 'performance' && (
        <PerformancePanel
          title="Успеваемость"
          description="Просмотр оценок, средних показателей и прогноза по студенту."
        />
      )}

      {activePanel === 'student-profile' && (
        <StudentProfileViewerPanel
          title="Профиль студента"
          description="Выберите студента и курс, чтобы увидеть оценки, рекомендации и динамику."
        />
      )}

      {activePanel === 'group-viewer' && (
        <GroupViewerPanel
          title="Просмотр групп"
          description="Выберите группу и просмотрите список студентов."
        />
      )}

      {activePanel === 'group-comparison' && (
        <GroupComparisonPanel
          title="Сравнение групп"
          description="Список всех групп и средний балл по каждой."
        />
      )}

      {activePanel === 'student-comparison' && (
        <StudentComparisonPanel
          title="Сравнение студентов"
          description="Таблица студентов со средними баллами и сортировкой."
        />
      )}

      {activePanel === 'risk-groups' && (
        <RiskGroupsPanel
          title="Группы риска"
          description="Распределение студентов по группам риска."
        />
      )}

      {activePanel === 'excel-export' && (
        <ExcelExportPanel
          title="Экспорт в Excel"
          description="Сформируйте Excel-отчёт по выбранным разделам."
        />
      )}
    </div>
  );
}