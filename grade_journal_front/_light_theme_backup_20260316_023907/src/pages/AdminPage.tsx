import { useMemo, useState, type FormEvent } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { api } from '../lib/api';
import { queryClient } from '../lib/queryClient';
import { PerformancePanel } from '../components/PerformancePanel';
import { StudentProfileViewerPanel } from '../components/StudentProfileViewerPanel';
import { GroupViewerPanel } from '../components/GroupViewerPanel';
import { ExcelExportPanel } from '../components/ExcelExportPanel';
import { SchedulePanel } from '../components/SchedulePanel';
import { UserDirectoryPanel, type DirectoryControlMode } from '../components/UserDirectoryPanel';
import type { AdminUser } from '../types/adminUser';
import { TeacherSchedulePanel } from '../components/TeacherSchedulePanel';
import { GroupComparisonPanel } from '../components/GroupComparisonPanel';
import { StudentComparisonPanel } from '../components/StudentComparisonPanel';
import { RiskGroupsPanel } from '../components/RiskGroupsPanel';

type PanelKey =
  | 'home'
  | 'requests'
  | 'profiles'
  | 'directory-explorer'
  | 'directory-management'
  | 'schedule'
  | 'teacher-schedule'
  | 'performance'
  | 'student-profile'
  | 'group-viewer'
  | 'group-comparison'
  | 'student-comparison'
  | 'risk-groups'
  | 'excel-export';

type PendingRequest = {
  requestId: number;
  desiredRole: 'teacher' | 'student';
  username: string;
  fullName: string;
  email?: string | null;
  status: string;
  createdAt: string;
};

type IncompleteProfile = {
  userId: number;
  username: string;
  fullName: string;
  email?: string | null;
  role: 'teacher' | 'student';
  approved: boolean;
  createdAt: string;
};

type GroupOption = {
  groupId: number;
  groupCode: string;
  courseNo: number;
};

type DepartmentOption = {
  departmentId: number;
  departmentCode: string;
  departmentName: string;
};

type ProfileOptions = {
  groups: GroupOption[];
  departments: DepartmentOption[];
};

type UpsertPayload = {
  username: string;
  password: string;
  fullName: string;
  email: string;
  role: 'admin' | 'teacher' | 'student';
  active: boolean;
  approved: boolean;
  groupId: string;
  studentCard: string;
  departmentId: string;
  position: string;
  phone: string;
};

const emptyPayload: UpsertPayload = {
  username: '',
  password: '',
  fullName: '',
  email: '',
  role: 'student',
  active: true,
  approved: true,
  groupId: '',
  studentCard: '',
  departmentId: '',
  position: '',
  phone: ''
};

function roleLabel(role: string) {
  if (role === 'admin') return 'Администратор';
  if (role === 'teacher') return 'Преподаватель';
  return 'Студент';
}

function yesNoLabel(value: boolean) {
  return value ? 'Да' : 'Нет';
}

function normalizeUpsertPayload(payload: UpsertPayload) {
  return {
    username: payload.username,
    password: payload.password,
    fullName: payload.fullName,
    email: payload.email,
    role: payload.role,
    active: payload.active,
    approved: payload.approved,
    groupId: payload.groupId ? Number(payload.groupId) : null,
    studentCard: payload.studentCard || null,
    departmentId: payload.departmentId ? Number(payload.departmentId) : null,
    position: payload.position || null,
    phone: payload.phone || null
  };
}

function mapUserToPayload(user: AdminUser): UpsertPayload {
  return {
    username: user.username ?? '',
    password: '',
    fullName: user.fullName ?? '',
    email: user.email ?? '',
    role: user.role,
    active: user.active,
    approved: user.approved,
    groupId: user.groupId ? String(user.groupId) : '',
    studentCard: user.studentCard ?? '',
    departmentId: user.departmentId ? String(user.departmentId) : '',
    position: user.position ?? '',
    phone: user.phone ?? ''
  };
}

function getDirectoryMode(panel: PanelKey): DirectoryControlMode | null {
  switch (panel) {
    case 'directory-explorer':
      return 'explorer';
    case 'directory-management':
      return 'management';
    default:
      return null;
  }
}

export function AdminPage() {
  const [activePanel, setActivePanel] = useState<PanelKey>('home');
  const [profileTarget, setProfileTarget] = useState<IncompleteProfile | null>(null);
  const [profileForm, setProfileForm] = useState({
    groupId: '',
    studentCard: '',
    departmentId: '',
    position: '',
    phone: ''
  });
  const [upsertMode, setUpsertMode] = useState<'create' | 'edit' | null>(null);
  const [editingUser, setEditingUser] = useState<AdminUser | null>(null);
  const [upsertPayload, setUpsertPayload] = useState<UpsertPayload>(emptyPayload);
  const [feedbackMessage, setFeedbackMessage] = useState<string | null>(null);

  const requestsQuery = useQuery({
    queryKey: ['admin', 'registration-requests'],
    queryFn: async () => (await api.get<PendingRequest[]>('/api/admin/registration-requests')).data
  });

  const incompleteProfilesQuery = useQuery({
    queryKey: ['admin', 'incomplete-profiles'],
    queryFn: async () => (await api.get<IncompleteProfile[]>('/api/admin/incomplete-profiles')).data
  });

  const optionsQuery = useQuery({
    queryKey: ['admin', 'profile-options'],
    queryFn: async () => (await api.get<ProfileOptions>('/api/admin/profile-options')).data
  });

  const approveMutation = useMutation({
    mutationFn: async ({ requestId, action }: { requestId: number; action: 'approve' | 'reject' }) =>
      (await api.post<{ message: string }>(`/api/admin/registration-requests/${requestId}/${action}`)).data,
    onSuccess: async (data) => {
      setFeedbackMessage(data.message || 'Статус заявки обновлён.');
      await queryClient.invalidateQueries({ queryKey: ['admin', 'registration-requests'] });
      await queryClient.invalidateQueries({ queryKey: ['admin', 'incomplete-profiles'] });
      await queryClient.invalidateQueries({ queryKey: ['admin', 'users'] });
    },
    onError: (error: any) =>
      setFeedbackMessage(error?.response?.data?.message || 'Не удалось обработать заявку.')
  });

  const fillProfileMutation = useMutation({
    mutationFn: async () => {
      if (!profileTarget) {
        return { message: 'Не выбран пользователь для заполнения профиля.' };
      }

      if (profileTarget.role === 'student') {
        return (
          await api.post<{ message: string }>(
            `/api/admin/users/${profileTarget.userId}/fill-student-profile`,
            {
              groupId: Number(profileForm.groupId),
              studentCard: profileForm.studentCard
            }
          )
        ).data;
      }

      return (
        await api.post<{ message: string }>(
          `/api/admin/users/${profileTarget.userId}/fill-teacher-profile`,
          {
            departmentId: Number(profileForm.departmentId),
            position: profileForm.position,
            phone: profileForm.phone
          }
        )
      ).data;
    },
    onSuccess: async (data) => {
      setFeedbackMessage(data.message || 'Профиль успешно заполнен.');
      setProfileTarget(null);
      setProfileForm({
        groupId: '',
        studentCard: '',
        departmentId: '',
        position: '',
        phone: ''
      });
      await queryClient.invalidateQueries({ queryKey: ['admin', 'incomplete-profiles'] });
      await queryClient.invalidateQueries({ queryKey: ['admin', 'users'] });
    },
    onError: (error: any) =>
      setFeedbackMessage(error?.response?.data?.message || 'Не удалось заполнить профиль.')
  });

  const upsertMutation = useMutation({
    mutationFn: async () => {
      const payload = normalizeUpsertPayload(upsertPayload);

      if (upsertMode === 'create') {
        return (await api.post<{ message: string }>('/api/admin/users', payload)).data;
      }

      if (!editingUser) {
        return { message: 'Не выбран пользователь для редактирования.' };
      }

      return (await api.put<{ message: string }>(`/api/admin/users/${editingUser.userId}`, payload))
        .data;
    },
    onSuccess: async (data) => {
      setFeedbackMessage(data.message || 'Данные пользователя сохранены.');
      closeUpsert();
      await queryClient.invalidateQueries({ queryKey: ['admin', 'users'] });
      await queryClient.invalidateQueries({ queryKey: ['admin', 'incomplete-profiles'] });
    },
    onError: (error: any) =>
      setFeedbackMessage(error?.response?.data?.message || 'Не удалось сохранить пользователя.')
  });

  const directoryMode = getDirectoryMode(activePanel);

  const selectedRoleDescription = useMemo(() => {
    if (upsertPayload.role === 'admin') {
      return 'Администратор получает доступ к панели управления, заявкам, аккаунтам и расписанию.';
    }

    if (upsertPayload.role === 'teacher') {
      return 'Для преподавателя дополнительно заполняются кафедра, должность и телефон.';
    }

    return 'Для студента дополнительно заполняются группа и номер студенческого билета.';
  }, [upsertPayload.role]);

  const openCreate = () => {
    setEditingUser(null);
    setUpsertPayload(emptyPayload);
    setUpsertMode('create');
  };

  const openEdit = (user: AdminUser) => {
    setEditingUser(user);
    setUpsertPayload(mapUserToPayload(user));
    setUpsertMode('edit');
  };

  const closeUpsert = () => {
    setEditingUser(null);
    setUpsertMode(null);
    setUpsertPayload(emptyPayload);
  };

  const submitUpsert = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    upsertMutation.mutate();
  };

  const buttons: Array<{ key: PanelKey; title: string; subtitle: string }> = [
    { key: 'requests', title: 'Одобрить заявки', subtitle: 'Обработка регистраций' },
    { key: 'profiles', title: 'Заполнить профиль', subtitle: 'Студенты и преподаватели без профиля' },
    { key: 'directory-explorer', title: 'Поиск, фильтр и сортировка', subtitle: 'Общий справочник пользователей' },
    { key: 'directory-management', title: 'Управление аккаунтами', subtitle: 'Создание и редактирование пользователей' },
    { key: 'schedule', title: 'Расписание', subtitle: 'Просмотр и добавление пар' },
    { key: 'teacher-schedule', title: 'Расписание преподавателей', subtitle: 'Пары выбранного преподавателя' },
    { key: 'performance', title: 'Успеваемость', subtitle: 'Оценки, средние баллы и прогноз' },
    { key: 'student-profile', title: 'Профиль студента', subtitle: 'Полный просмотр данных' },
    { key: 'group-viewer', title: 'Просмотр групп', subtitle: 'Состав выбранной группы' },
    { key: 'group-comparison', title: 'Сравнение групп', subtitle: 'Средний балл по группам' },
    { key: 'student-comparison', title: 'Сравнение студентов', subtitle: 'Таблица со средними баллами' },
    { key: 'risk-groups', title: 'Группы риска', subtitle: 'Распределение студентов по рискам' },
    { key: 'excel-export', title: 'Экспорт в Excel', subtitle: 'Формирование отчетов' }
  ];

  return (
    <div className="w-full max-w-none min-w-0 space-y-6">
      <section className="w-full max-w-none min-w-0 rounded-3xl border border-slate-800 bg-slate-900/70 p-8">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <h1 className="text-3xl font-bold text-white">Главная панель администратора</h1>
            <p className="mt-2 max-w-4xl text-slate-400">
              Здесь собраны основные разделы управления электронным журналом: заявки,
              профили, аккаунты, расписание, успеваемость и просмотр данных студентов.
            </p>
          </div>
          <button
            onClick={() => setActivePanel('home')}
            className="rounded-2xl border border-slate-700 bg-slate-950 px-4 py-3 font-medium text-slate-200 hover:bg-slate-800"
          >
            На главную панель
          </button>
        </div>

        <div className="mt-6 grid gap-3 sm:grid-cols-2 xl:grid-cols-6">
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

      {feedbackMessage && (
        <div className="rounded-2xl border border-blue-500/30 bg-blue-500/10 px-4 py-3 text-blue-100">
          {feedbackMessage}
        </div>
      )}

      {activePanel === 'home' && (
        <section className="w-full max-w-none min-w-0 rounded-3xl border border-slate-800 bg-slate-900/70 p-6 text-slate-300">
          <h2 className="text-2xl font-semibold text-white">Рабочее пространство администратора</h2>
          <p className="mt-2 text-slate-400">
            Выберите нужный раздел с помощью кнопок выше. Администратор может обрабатывать
            заявки, заполнять профили, управлять аккаунтами, вести расписание и просматривать
            успеваемость.
          </p>
        </section>
      )}

      {activePanel === 'requests' && (
        <section className="space-y-5 rounded-3xl border border-slate-800 bg-slate-900/70 p-6">
          <div>
            <h2 className="text-2xl font-semibold text-white">Одобрение заявок</h2>
            <p className="mt-2 text-slate-400">
              Здесь отображаются заявки на регистрацию. Администратор может одобрить или
              отклонить каждую заявку.
            </p>
          </div>

          <div className="overflow-x-auto rounded-3xl border border-slate-800 bg-slate-950/70">
            <table className="min-w-full text-left text-sm text-slate-300">
              <thead className="bg-slate-900 text-slate-400">
                <tr>
                  <th className="px-4 py-3 font-medium">Логин</th>
                  <th className="px-4 py-3 font-medium">ФИО</th>
                  <th className="px-4 py-3 font-medium">Email</th>
                  <th className="px-4 py-3 font-medium">Роль</th>
                  <th className="px-4 py-3 font-medium">Статус</th>
                  <th className="px-4 py-3 font-medium">Дата создания</th>
                  <th className="px-4 py-3 font-medium">Действия</th>
                </tr>
              </thead>
              <tbody>
                {(requestsQuery.data ?? []).map((request) => (
                  <tr key={request.requestId} className="border-t border-slate-800">
                    <td className="px-4 py-3">{request.username}</td>
                    <td className="px-4 py-3 font-medium text-white">{request.fullName}</td>
                    <td className="px-4 py-3">{request.email || '—'}</td>
                    <td className="px-4 py-3">{roleLabel(request.desiredRole)}</td>
                    <td className="px-4 py-3">{request.status}</td>
                    <td className="px-4 py-3">{new Date(request.createdAt).toLocaleString()}</td>
                    <td className="px-4 py-3">
                      <div className="flex flex-wrap gap-2">
                        <button
                          onClick={() =>
                            approveMutation.mutate({
                              requestId: request.requestId,
                              action: 'approve'
                            })
                          }
                          className="rounded-2xl bg-emerald-500 px-3 py-2 text-sm font-medium text-white hover:bg-emerald-400"
                        >
                          Одобрить
                        </button>
                        <button
                          onClick={() =>
                            approveMutation.mutate({
                              requestId: request.requestId,
                              action: 'reject'
                            })
                          }
                          className="rounded-2xl bg-rose-500 px-3 py-2 text-sm font-medium text-white hover:bg-rose-400"
                        >
                          Отклонить
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
                {!requestsQuery.data?.length && (
                  <tr className="border-t border-slate-800">
                    <td colSpan={7} className="px-4 py-6 text-center text-slate-400">
                      Заявок нет.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </section>
      )}

      {activePanel === 'profiles' && (
        <section className="space-y-5 rounded-3xl border border-slate-800 bg-slate-900/70 p-6">
          <div>
            <h2 className="text-2xl font-semibold text-white">Заполнение профилей</h2>
            <p className="mt-2 text-slate-400">
              Здесь отображаются одобренные пользователи, для которых ещё не заполнен профиль
              студента или преподавателя.
            </p>
          </div>

          <div className="overflow-x-auto rounded-3xl border border-slate-800 bg-slate-950/70">
            <table className="min-w-full text-left text-sm text-slate-300">
              <thead className="bg-slate-900 text-slate-400">
                <tr>
                  <th className="px-4 py-3 font-medium">Логин</th>
                  <th className="px-4 py-3 font-medium">ФИО</th>
                  <th className="px-4 py-3 font-medium">Email</th>
                  <th className="px-4 py-3 font-medium">Роль</th>
                  <th className="px-4 py-3 font-medium">Одобрен</th>
                  <th className="px-4 py-3 font-medium">Дата создания</th>
                  <th className="px-4 py-3 font-medium">Действие</th>
                </tr>
              </thead>
              <tbody>
                {(incompleteProfilesQuery.data ?? []).map((user) => (
                  <tr key={user.userId} className="border-t border-slate-800">
                    <td className="px-4 py-3">{user.username}</td>
                    <td className="px-4 py-3 font-medium text-white">{user.fullName}</td>
                    <td className="px-4 py-3">{user.email || '—'}</td>
                    <td className="px-4 py-3">{roleLabel(user.role)}</td>
                    <td className="px-4 py-3">{yesNoLabel(user.approved)}</td>
                    <td className="px-4 py-3">{new Date(user.createdAt).toLocaleString()}</td>
                    <td className="px-4 py-3">
                      <button
                        onClick={() => {
                          setProfileTarget(user);
                          setProfileForm({
                            groupId: '',
                            studentCard: '',
                            departmentId: '',
                            position: '',
                            phone: ''
                          });
                        }}
                        className="rounded-2xl border border-slate-700 bg-slate-900 px-3 py-2 text-sm font-medium text-slate-200 hover:bg-slate-800"
                      >
                        Заполнить
                      </button>
                    </td>
                  </tr>
                ))}
                {!incompleteProfilesQuery.data?.length && (
                  <tr className="border-t border-slate-800">
                    <td colSpan={7} className="px-4 py-6 text-center text-slate-400">
                      Пользователей без профиля нет.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </section>
      )}

      {directoryMode && (
        <div className="directory-panel-shell w-full max-w-none min-w-0">
          <UserDirectoryPanel
          endpoint="/api/admin/users"
          queryKey={['admin', activePanel]}
          title={
            activePanel === 'directory-management'
              ? 'Управление аккаунтами'
              : 'Поиск, фильтр и сортировка'
          }
          description={
            activePanel === 'directory-management'
              ? 'Создавайте новых пользователей, редактируйте существующие записи и управляйте данными по ролям.'
              : 'Используйте объединённое окно поиска, фильтров и сортировки для работы со справочником пользователей.'
          }
          editable={activePanel === 'directory-management'}
          showSensitiveColumns={activePanel === 'directory-management'}
          activeControl={directoryMode}
          onEdit={openEdit}
          onCreate={openCreate}
         />
        </div>
      )}

      {activePanel === 'schedule' && (
        <div className="schedule-panel-shell w-full max-w-none min-w-0">
          <SchedulePanel
          canManage={true}
          title="Расписание"
          description="Администратор может просматривать расписание выбранной группы и добавлять новые пары с проверкой пересечений."
         />
        </div>
      )}

      {activePanel === 'performance' && (
        <PerformancePanel
          title="Успеваемость"
          description="Выберите студента и предмет, чтобы увидеть оценки, средний балл, прогнозную следующую оценку и график динамики."
        />
      )}

      {activePanel === 'student-profile' && (
        <StudentProfileViewerPanel
          title="Профиль студента"
          description="Выберите студента из списка или найдите его по ФИО, email, группе или студенческому билету. Администратор видит полную информацию, список оценок по всем предметам, прогнозы и рекомендации."
        />
      )}

      {activePanel === 'teacher-schedule' && (
        <TeacherSchedulePanel
          title="Расписание преподавателей"
          description="Выберите преподавателя, чтобы увидеть все его пары."
        />
      )}

      {activePanel === 'group-viewer' && (
        <GroupViewerPanel
          title="Просмотр групп"
          description="Выберите группу, чтобы увидеть список студентов этой группы и их базовую информацию."
        />
      )}

      {activePanel === 'group-comparison' && (
        <GroupComparisonPanel
          title="Сравнение групп"
          description="Здесь отображаются все группы и их средний балл."
        />
      )}

      {activePanel === 'student-comparison' && (
        <StudentComparisonPanel
          title="Сравнение студентов"
          description="Таблица всех студентов со средним баллом по всем оценкам и сортировкой."
        />
      )}

      {activePanel === 'risk-groups' && (
        <RiskGroupsPanel
          title="Группы риска"
          description="Студенты распределены по трем таблицам: высокий уровень, средний уровень и группа риска."
        />
      )}

      {activePanel === 'excel-export' && (
        <ExcelExportPanel
          title="Экспорт в Excel"
          description="Отметьте разделы, которые нужно включить в Excel-отчет: студенты, преподаватели, справочник курсов, расписание занятий, успеваемость и посещаемость."
        />
      )}
      {profileTarget && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/85 p-4">
          <div className="w-full max-w-2xl rounded-3xl border border-slate-800 bg-slate-900 p-6 shadow-2xl shadow-black/40">
            <div className="mb-5 flex items-start justify-between gap-4">
              <div>
                <h3 className="text-lg font-semibold text-white">
                  Заполнение профиля: {profileTarget.fullName}
                </h3>
                <p className="mt-1 text-sm text-slate-400">
                  Роль: {roleLabel(profileTarget.role)}
                </p>
              </div>
              <button
                onClick={() => setProfileTarget(null)}
                className="rounded-2xl border border-slate-700 bg-slate-950 px-4 py-2 text-slate-200 hover:bg-slate-800"
              >
                Закрыть
              </button>
            </div>

            {profileTarget.role === 'student' ? (
              <div className="grid gap-4 md:grid-cols-2">
                <label className="space-y-2">
                  <span className="text-sm text-slate-400">Группа</span>
                  <select
                    value={profileForm.groupId}
                    onChange={(event) =>
                      setProfileForm((prev) => ({ ...prev, groupId: event.target.value }))
                    }
                    className="w-full rounded-2xl border border-slate-700 bg-slate-950 px-4 py-3 text-slate-100 outline-none focus:border-blue-400"
                  >
                    <option value="">Выберите группу</option>
                    {(optionsQuery.data?.groups ?? []).map((group) => (
                      <option key={group.groupId} value={group.groupId}>
                        {group.groupCode} — курс {group.courseNo}
                      </option>
                    ))}
                  </select>
                </label>

                <label className="space-y-2">
                  <span className="text-sm text-slate-400">Студенческий билет</span>
                  <input
                    value={profileForm.studentCard}
                    onChange={(event) =>
                      setProfileForm((prev) => ({
                        ...prev,
                        studentCard: event.target.value
                      }))
                    }
                    className="w-full rounded-2xl border border-slate-700 bg-slate-950 px-4 py-3 text-slate-100 outline-none focus:border-blue-400"
                  />
                </label>
              </div>
            ) : (
              <div className="grid gap-4 md:grid-cols-2">
                <label className="space-y-2">
                  <span className="text-sm text-slate-400">Кафедра</span>
                  <select
                    value={profileForm.departmentId}
                    onChange={(event) =>
                      setProfileForm((prev) => ({
                        ...prev,
                        departmentId: event.target.value
                      }))
                    }
                    className="w-full rounded-2xl border border-slate-700 bg-slate-950 px-4 py-3 text-slate-100 outline-none focus:border-blue-400"
                  >
                    <option value="">Выберите кафедру</option>
                    {(optionsQuery.data?.departments ?? []).map((department) => (
                      <option key={department.departmentId} value={department.departmentId}>
                        {department.departmentName}
                      </option>
                    ))}
                  </select>
                </label>

                <label className="space-y-2">
                  <span className="text-sm text-slate-400">Должность</span>
                  <input
                    value={profileForm.position}
                    onChange={(event) =>
                      setProfileForm((prev) => ({ ...prev, position: event.target.value }))
                    }
                    className="w-full rounded-2xl border border-slate-700 bg-slate-950 px-4 py-3 text-slate-100 outline-none focus:border-blue-400"
                  />
                </label>

                <label className="space-y-2 md:col-span-2">
                  <span className="text-sm text-slate-400">Телефон</span>
                  <input
                    value={profileForm.phone}
                    onChange={(event) =>
                      setProfileForm((prev) => ({ ...prev, phone: event.target.value }))
                    }
                    className="w-full rounded-2xl border border-slate-700 bg-slate-950 px-4 py-3 text-slate-100 outline-none focus:border-blue-400"
                  />
                </label>
              </div>
            )}

            <div className="mt-5 flex flex-wrap gap-3">
              <button
                onClick={() => fillProfileMutation.mutate()}
                className="rounded-2xl bg-blue-500 px-4 py-3 font-medium text-white hover:bg-blue-400"
              >
                Сохранить профиль
              </button>
              <button
                onClick={() => setProfileTarget(null)}
                className="rounded-2xl border border-slate-700 bg-slate-950 px-4 py-3 font-medium text-slate-200 hover:bg-slate-800"
              >
                Отмена
              </button>
            </div>
          </div>
        </div>
      )}

      {upsertMode && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/85 p-4">
          <div className="max-h-[92vh] w-full max-w-[1920px] overflow-y-auto rounded-3xl border border-slate-800 bg-slate-900 p-6 shadow-2xl shadow-black/40">
            <div className="mb-5 flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
              <div>
                <h2 className="text-2xl font-semibold text-white">
                  {upsertMode === 'create'
                    ? 'Создание пользователя'
                    : 'Редактирование пользователя'}
                </h2>
                <p className="mt-1 max-w-3xl text-slate-400">{selectedRoleDescription}</p>
              </div>
              <button
                onClick={closeUpsert}
                className="rounded-2xl border border-slate-700 bg-slate-950 px-4 py-3 font-medium text-slate-200 hover:bg-slate-800"
              >
                Закрыть окно
              </button>
            </div>

            <form onSubmit={submitUpsert} className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
              <label className="space-y-2">
                <span className="text-sm text-slate-400">Логин</span>
                <input
                  value={upsertPayload.username}
                  onChange={(event) =>
                    setUpsertPayload((prev) => ({ ...prev, username: event.target.value }))
                  }
                  className="w-full rounded-2xl border border-slate-700 bg-slate-950 px-4 py-3 text-slate-100 outline-none focus:border-blue-400"
                  required
                />
              </label>

              <label className="space-y-2">
                <span className="text-sm text-slate-400">
                  Пароль {upsertMode === 'edit' ? '(оставьте пустым, если менять не нужно)' : ''}
                </span>
                <input
                  type="password"
                  value={upsertPayload.password}
                  onChange={(event) =>
                    setUpsertPayload((prev) => ({ ...prev, password: event.target.value }))
                  }
                  className="w-full rounded-2xl border border-slate-700 bg-slate-950 px-4 py-3 text-slate-100 outline-none focus:border-blue-400"
                  required={upsertMode === 'create'}
                />
              </label>

              <label className="space-y-2">
                <span className="text-sm text-slate-400">ФИО</span>
                <input
                  value={upsertPayload.fullName}
                  onChange={(event) =>
                    setUpsertPayload((prev) => ({ ...prev, fullName: event.target.value }))
                  }
                  className="w-full rounded-2xl border border-slate-700 bg-slate-950 px-4 py-3 text-slate-100 outline-none focus:border-blue-400"
                  required
                />
              </label>

              <label className="space-y-2 xl:col-span-2">
                <span className="text-sm text-slate-400">Email</span>
                <input
                  type="email"
                  value={upsertPayload.email}
                  onChange={(event) =>
                    setUpsertPayload((prev) => ({ ...prev, email: event.target.value }))
                  }
                  className="w-full rounded-2xl border border-slate-700 bg-slate-950 px-4 py-3 text-slate-100 outline-none focus:border-blue-400"
                />
              </label>

              <label className="space-y-2">
                <span className="text-sm text-slate-400">Роль</span>
                <select
                  value={upsertPayload.role}
                  onChange={(event) =>
                    setUpsertPayload((prev) => ({
                      ...prev,
                      role: event.target.value as 'admin' | 'teacher' | 'student',
                      groupId: event.target.value === 'student' ? prev.groupId : '',
                      studentCard: event.target.value === 'student' ? prev.studentCard : '',
                      departmentId:
                        event.target.value === 'teacher' ? prev.departmentId : '',
                      position: event.target.value === 'teacher' ? prev.position : '',
                      phone: event.target.value === 'teacher' ? prev.phone : ''
                    }))
                  }
                  className="w-full rounded-2xl border border-slate-700 bg-slate-950 px-4 py-3 text-slate-100 outline-none focus:border-blue-400"
                >
                  <option value="admin">Администратор</option>
                  <option value="teacher">Преподаватель</option>
                  <option value="student">Студент</option>
                </select>
              </label>

              <label className="space-y-2">
                <span className="text-sm text-slate-400">Активен</span>
                <select
                  value={String(upsertPayload.active)}
                  onChange={(event) =>
                    setUpsertPayload((prev) => ({
                      ...prev,
                      active: event.target.value === 'true'
                    }))
                  }
                  className="w-full rounded-2xl border border-slate-700 bg-slate-950 px-4 py-3 text-slate-100 outline-none focus:border-blue-400"
                >
                  <option value="true">Да</option>
                  <option value="false">Нет</option>
                </select>
              </label>

              <label className="space-y-2">
                <span className="text-sm text-slate-400">Одобрен</span>
                <select
                  value={String(upsertPayload.approved)}
                  onChange={(event) =>
                    setUpsertPayload((prev) => ({
                      ...prev,
                      approved: event.target.value === 'true'
                    }))
                  }
                  className="w-full rounded-2xl border border-slate-700 bg-slate-950 px-4 py-3 text-slate-100 outline-none focus:border-blue-400"
                >
                  <option value="true">Да</option>
                  <option value="false">Нет</option>
                </select>
              </label>

              {upsertPayload.role === 'student' && (
                <>
                  <label className="space-y-2">
                    <span className="text-sm text-slate-400">Группа</span>
                    <select
                      value={upsertPayload.groupId}
                      onChange={(event) =>
                        setUpsertPayload((prev) => ({ ...prev, groupId: event.target.value }))
                      }
                      className="w-full rounded-2xl border border-slate-700 bg-slate-950 px-4 py-3 text-slate-100 outline-none focus:border-blue-400"
                    >
                      <option value="">Выберите группу</option>
                      {(optionsQuery.data?.groups ?? []).map((group) => (
                        <option key={group.groupId} value={group.groupId}>
                          {group.groupCode} — курс {group.courseNo}
                        </option>
                      ))}
                    </select>
                  </label>

                  <label className="space-y-2">
                    <span className="text-sm text-slate-400">Студенческий билет</span>
                    <input
                      value={upsertPayload.studentCard}
                      onChange={(event) =>
                        setUpsertPayload((prev) => ({
                          ...prev,
                          studentCard: event.target.value
                        }))
                      }
                      className="w-full rounded-2xl border border-slate-700 bg-slate-950 px-4 py-3 text-slate-100 outline-none focus:border-blue-400"
                    />
                  </label>
                </>
              )}

              {upsertPayload.role === 'teacher' && (
                <>
                  <label className="space-y-2">
                    <span className="text-sm text-slate-400">Кафедра</span>
                    <select
                      value={upsertPayload.departmentId}
                      onChange={(event) =>
                        setUpsertPayload((prev) => ({
                          ...prev,
                          departmentId: event.target.value
                        }))
                      }
                      className="w-full rounded-2xl border border-slate-700 bg-slate-950 px-4 py-3 text-slate-100 outline-none focus:border-blue-400"
                    >
                      <option value="">Выберите кафедру</option>
                      {(optionsQuery.data?.departments ?? []).map((department) => (
                        <option key={department.departmentId} value={department.departmentId}>
                          {department.departmentName}
                        </option>
                      ))}
                    </select>
                  </label>

                  <label className="space-y-2">
                    <span className="text-sm text-slate-400">Должность</span>
                    <input
                      value={upsertPayload.position}
                      onChange={(event) =>
                        setUpsertPayload((prev) => ({ ...prev, position: event.target.value }))
                      }
                      className="w-full rounded-2xl border border-slate-700 bg-slate-950 px-4 py-3 text-slate-100 outline-none focus:border-blue-400"
                    />
                  </label>

                  <label className="space-y-2 xl:col-span-2">
                    <span className="text-sm text-slate-400">Телефон</span>
                    <input
                      value={upsertPayload.phone}
                      onChange={(event) =>
                        setUpsertPayload((prev) => ({ ...prev, phone: event.target.value }))
                      }
                      className="w-full rounded-2xl border border-slate-700 bg-slate-950 px-4 py-3 text-slate-100 outline-none focus:border-blue-400"
                    />
                  </label>
                </>
              )}

              <div className="xl:col-span-3 flex flex-wrap gap-3 pt-2">
                <button
                  type="submit"
                  disabled={upsertMutation.isPending}
                  className="rounded-2xl bg-blue-500 px-4 py-3 font-medium text-white hover:bg-blue-400 disabled:opacity-70"
                >
                  {upsertMutation.isPending
                    ? 'Сохранение...'
                    : upsertMode === 'create'
                      ? 'Создать пользователя'
                      : 'Сохранить изменения'}
                </button>
                <button
                  type="button"
                  onClick={closeUpsert}
                  className="rounded-2xl border border-slate-700 bg-slate-950 px-4 py-3 font-medium text-slate-200 hover:bg-slate-800"
                >
                  Отмена
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}