import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../lib/api';
import type { AdminUser } from '../types/adminUser';

export type DirectoryControlMode = 'explorer' | 'management';

type SortKey =
  | 'fullName'
  | 'email'
  | 'role'
  | 'groupCode'
  | 'courseNo'
  | 'departmentName'
  | 'position'
  | 'createdAt';

type Props = {
  endpoint: string;
  queryKey: string[];
  title: string;
  description: string;
  editable?: boolean;
  showSensitiveColumns?: boolean;
  activeControl?: DirectoryControlMode;
  onEdit?: (user: AdminUser) => void;
  onCreate?: () => void;
};

function roleLabel(role: string) {
  if (role === 'admin') return 'Администратор';
  if (role === 'teacher') return 'Преподаватель';
  return 'Студент';
}

function yesNo(value: boolean) {
  return value ? 'Да' : 'Нет';
}

function normalize(value: string | number | null | undefined) {
  return String(value ?? '').toLowerCase().trim();
}

function compareValues(a: string | number, b: string | number, direction: 'asc' | 'desc') {
  if (a < b) return direction === 'asc' ? -1 : 1;
  if (a > b) return direction === 'asc' ? 1 : -1;
  return 0;
}

function getSortValue(user: AdminUser, key: SortKey): string | number {
  switch (key) {
    case 'fullName':
      return normalize(user.fullName);
    case 'email':
      return normalize(user.email);
    case 'role':
      return normalize(roleLabel(user.role));
    case 'groupCode':
      return normalize(user.groupCode);
    case 'courseNo':
      return user.courseNo ?? 0;
    case 'departmentName':
      return normalize(user.departmentName);
    case 'position':
      return normalize(user.position);
    case 'createdAt':
      return new Date(user.createdAt).getTime();
    default:
      return normalize(user.fullName);
  }
}

function matchesGlobalSearch(user: AdminUser, query: string) {
  if (!query) return true;

  const values = [
    user.username,
    user.fullName,
    user.email,
    user.groupCode,
    user.departmentName,
    user.position,
    user.facultyName,
    user.specializationName,
    user.departmentCode,
    user.phone,
    user.studentCard
  ].map(normalize);

  return values.some((value) => value.includes(query));
}

export function UserDirectoryPanel({
  endpoint,
  queryKey,
  title,
  description,
  editable = false,
  showSensitiveColumns = false,
  activeControl = 'explorer',
  onEdit,
  onCreate
}: Props) {
  const [globalSearch, setGlobalSearch] = useState('');
  const [fullNameFilter, setFullNameFilter] = useState('');
  const [emailFilter, setEmailFilter] = useState('');
  const [groupFilter, setGroupFilter] = useState('');
  const [departmentFilter, setDepartmentFilter] = useState('');
  const [positionFilter, setPositionFilter] = useState('');
  const [roleFilter, setRoleFilter] = useState('all');
  const [statusFilter, setStatusFilter] = useState('all');
  const [courseFilter, setCourseFilter] = useState('all');
  const [sortKey, setSortKey] = useState<SortKey>('fullName');
  const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('asc');

  const directoryQuery = useQuery({
    queryKey,
    queryFn: async () => {
      const response = await api.get<AdminUser[]>(endpoint);
      return response.data;
    }
  });

  const filteredUsers = useMemo(() => {
    const search = normalize(globalSearch);
    const fullName = normalize(fullNameFilter);
    const email = normalize(emailFilter);
    const group = normalize(groupFilter);
    const department = normalize(departmentFilter);
    const position = normalize(positionFilter);

    return (directoryQuery.data ?? [])
      .filter((user) => matchesGlobalSearch(user, search))
      .filter((user) => (fullName ? normalize(user.fullName).includes(fullName) : true))
      .filter((user) => (email ? normalize(user.email).includes(email) : true))
      .filter((user) => (group ? normalize(user.groupCode).includes(group) : true))
      .filter((user) => (department ? normalize(user.departmentName).includes(department) : true))
      .filter((user) => (position ? normalize(user.position).includes(position) : true))
      .filter((user) => (roleFilter === 'all' ? true : user.role === roleFilter))
      .filter((user) => {
        if (statusFilter === 'all') return true;
        if (statusFilter === 'active') return user.active;
        if (statusFilter === 'inactive') return !user.active;
        if (statusFilter === 'approved') return user.approved;
        if (statusFilter === 'unapproved') return !user.approved;
        return true;
      })
      .filter((user) => (courseFilter === 'all' ? true : String(user.courseNo ?? '') === courseFilter))
      .sort((left, right) => compareValues(getSortValue(left, sortKey), getSortValue(right, sortKey), sortDirection));
  }, [courseFilter, departmentFilter, directoryQuery.data, emailFilter, fullNameFilter, globalSearch, groupFilter, positionFilter, roleFilter, sortDirection, sortKey, statusFilter]);

  const clearAll = () => {
    setGlobalSearch('');
    setFullNameFilter('');
    setEmailFilter('');
    setGroupFilter('');
    setDepartmentFilter('');
    setPositionFilter('');
    setRoleFilter('all');
    setStatusFilter('all');
    setCourseFilter('all');
    setSortKey('fullName');
    setSortDirection('asc');
  };

  const wideManagement = activeControl === 'management';

  return (
    <section className={`space-y-5 rounded-3xl border border-slate-800 bg-slate-900/70 ${wideManagement ? 'p-7' : 'p-6'}`}>
      <div className="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <h2 className="text-2xl font-semibold text-white">{title}</h2>
          <p className="mt-2 max-w-4xl text-slate-400">{description}</p>
        </div>
        <div className="flex flex-wrap items-center gap-3">
          <div className="rounded-2xl border border-slate-800 bg-slate-950 px-4 py-3 text-sm text-slate-300">
            Найдено записей: <span className="font-semibold text-white">{filteredUsers.length}</span>
          </div>
          {editable && activeControl === 'management' && onCreate && (
            <button
              onClick={onCreate}
              className="rounded-2xl bg-blue-500 px-4 py-3 font-medium text-white transition hover:bg-blue-400"
            >
              Создать пользователя
            </button>
          )}
        </div>
      </div>

      {activeControl === 'explorer' && (
        <div className="rounded-3xl border border-blue-500/20 bg-slate-950/80 p-5">
          <div className="mb-4 flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
            <div>
              <h3 className="text-lg font-semibold text-white">Поиск, фильтры и сортировка</h3>
              <p className="text-sm text-slate-400">Все инструменты собраны в одном окне.</p>
            </div>
            <button
              onClick={clearAll}
              className="rounded-2xl border border-slate-700 bg-slate-900 px-4 py-2 text-sm font-medium text-slate-200 hover:bg-slate-800"
            >
              Сбросить параметры
            </button>
          </div>

          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            <label className="space-y-2 xl:col-span-4">
              <span className="text-sm text-slate-400">Глобальный поиск</span>
              <input
                value={globalSearch}
                onChange={(event) => setGlobalSearch(event.target.value)}
                placeholder="ФИО, email, группа, кафедра, должность"
                className="w-full rounded-2xl border border-slate-700 bg-slate-950 px-4 py-3 text-slate-100 outline-none focus:border-blue-400"
              />
            </label>

            <label className="space-y-2">
              <span className="text-sm text-slate-400">ФИО</span>
              <input value={fullNameFilter} onChange={(event) => setFullNameFilter(event.target.value)} className="w-full rounded-2xl border border-slate-700 bg-slate-950 px-4 py-3 text-slate-100 outline-none focus:border-blue-400" />
            </label>
            <label className="space-y-2">
              <span className="text-sm text-slate-400">Email</span>
              <input value={emailFilter} onChange={(event) => setEmailFilter(event.target.value)} className="w-full rounded-2xl border border-slate-700 bg-slate-950 px-4 py-3 text-slate-100 outline-none focus:border-blue-400" />
            </label>
            <label className="space-y-2">
              <span className="text-sm text-slate-400">Группа</span>
              <input value={groupFilter} onChange={(event) => setGroupFilter(event.target.value)} className="w-full rounded-2xl border border-slate-700 bg-slate-950 px-4 py-3 text-slate-100 outline-none focus:border-blue-400" />
            </label>
            <label className="space-y-2">
              <span className="text-sm text-slate-400">Кафедра</span>
              <input value={departmentFilter} onChange={(event) => setDepartmentFilter(event.target.value)} className="w-full rounded-2xl border border-slate-700 bg-slate-950 px-4 py-3 text-slate-100 outline-none focus:border-blue-400" />
            </label>
            <label className="space-y-2">
              <span className="text-sm text-slate-400">Должность</span>
              <input value={positionFilter} onChange={(event) => setPositionFilter(event.target.value)} className="w-full rounded-2xl border border-slate-700 bg-slate-950 px-4 py-3 text-slate-100 outline-none focus:border-blue-400" />
            </label>
            <label className="space-y-2">
              <span className="text-sm text-slate-400">Роль</span>
              <select value={roleFilter} onChange={(event) => setRoleFilter(event.target.value)} className="w-full rounded-2xl border border-slate-700 bg-slate-950 px-4 py-3 text-slate-100 outline-none focus:border-blue-400">
                <option value="all">Все роли</option>
                <option value="admin">Администратор</option>
                <option value="teacher">Преподаватель</option>
                <option value="student">Студент</option>
              </select>
            </label>
            <label className="space-y-2">
              <span className="text-sm text-slate-400">Статус</span>
              <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)} className="w-full rounded-2xl border border-slate-700 bg-slate-950 px-4 py-3 text-slate-100 outline-none focus:border-blue-400">
                <option value="all">Все статусы</option>
                <option value="active">Активен</option>
                <option value="inactive">Не активен</option>
                <option value="approved">Одобрен</option>
                <option value="unapproved">Не одобрен</option>
              </select>
            </label>
            <label className="space-y-2">
              <span className="text-sm text-slate-400">Курс</span>
              <select value={courseFilter} onChange={(event) => setCourseFilter(event.target.value)} className="w-full rounded-2xl border border-slate-700 bg-slate-950 px-4 py-3 text-slate-100 outline-none focus:border-blue-400">
                <option value="all">Все курсы</option>
                <option value="1">1</option>
                <option value="2">2</option>
                <option value="3">3</option>
                <option value="4">4</option>
              </select>
            </label>
            <label className="space-y-2">
              <span className="text-sm text-slate-400">Сортировать по</span>
              <select value={sortKey} onChange={(event) => setSortKey(event.target.value as SortKey)} className="w-full rounded-2xl border border-slate-700 bg-slate-950 px-4 py-3 text-slate-100 outline-none focus:border-blue-400">
                <option value="fullName">ФИО</option>
                <option value="email">Email</option>
                <option value="role">Роль</option>
                <option value="groupCode">Группа</option>
                <option value="courseNo">Курс</option>
                <option value="departmentName">Кафедра</option>
                <option value="position">Должность</option>
                <option value="createdAt">Дата создания</option>
              </select>
            </label>
            <label className="space-y-2">
              <span className="text-sm text-slate-400">Направление сортировки</span>
              <select value={sortDirection} onChange={(event) => setSortDirection(event.target.value as 'asc' | 'desc')} className="w-full rounded-2xl border border-slate-700 bg-slate-950 px-4 py-3 text-slate-100 outline-none focus:border-blue-400">
                <option value="asc">По возрастанию</option>
                <option value="desc">По убыванию</option>
              </select>
            </label>
          </div>
        </div>
      )}

      {directoryQuery.isLoading && <p className="text-slate-400">Загрузка пользователей...</p>}

      {!directoryQuery.isLoading && (
        <div className={`rounded-3xl border border-slate-800 bg-slate-950/70 ${wideManagement ? 'p-5' : 'p-4'}`}>
          <div className="overflow-x-auto">
            <table className="text-left text-sm text-slate-300 min-w-[1700px] min-w-[2200px] table-auto w-max min-w-[2600px]">
              <thead className="bg-slate-900 text-slate-400">
                <tr>
                  <th className="px-4 py-3 font-medium whitespace-nowrap align-top">Логин</th>
                  <th className="px-4 py-3 font-medium whitespace-nowrap align-top">ФИО</th>
                  <th className="px-4 py-3 font-medium whitespace-nowrap align-top">Email</th>
                  <th className="px-4 py-3 font-medium whitespace-nowrap align-top">Роль</th>
                  <th className="px-4 py-3 font-medium whitespace-nowrap align-top">Активен</th>
                  <th className="px-4 py-3 font-medium whitespace-nowrap align-top">Одобрен</th>
                  <th className="px-4 py-3 font-medium whitespace-nowrap align-top">Профиль</th>
                  <th className="px-4 py-3 font-medium whitespace-nowrap align-top">Группа</th>
                  <th className="px-4 py-3 font-medium whitespace-nowrap align-top">Курс</th>
                  <th className="px-4 py-3 font-medium whitespace-nowrap align-top">Кафедра</th>
                  <th className="px-4 py-3 font-medium whitespace-nowrap align-top">Должность</th>
                  {showSensitiveColumns && <th className="px-4 py-3 font-medium whitespace-nowrap align-top">Телефон</th>}
                  {showSensitiveColumns && <th className="px-4 py-3 font-medium whitespace-nowrap align-top">Студ. билет</th>}
                  <th className="px-4 py-3 font-medium whitespace-nowrap align-top">Создан</th>
                  {editable && <th className="px-4 py-3 font-medium whitespace-nowrap align-top">Действия</th>}
                </tr>
              </thead>
              <tbody>
                {filteredUsers.length === 0 && (
                  <tr>
                    <td colSpan={editable ? (showSensitiveColumns ? 15 : 13) : (showSensitiveColumns ? 14 : 12)} className="px-4 py-6 text-center text-slate-500">
                      Пользователи по выбранным параметрам не найдены.
                    </td>
                  </tr>
                )}
                {filteredUsers.map((user) => (
                  <tr key={user.userId} className="border-t border-slate-800 align-top">
                    <td className="px-4 py-3 whitespace-nowrap align-top">{user.username}</td>
                    <td className="px-4 py-3 font-medium text-white whitespace-nowrap align-top">{user.fullName}</td>
                    <td className="px-4 py-3 whitespace-nowrap align-top">{user.email || '—'}</td>
                    <td className="px-4 py-3 whitespace-nowrap align-top">{roleLabel(user.role)}</td>
                    <td className="px-4 py-3 whitespace-nowrap align-top">{yesNo(user.active)}</td>
                    <td className="px-4 py-3 whitespace-nowrap align-top">{yesNo(user.approved)}</td>
                    <td className="px-4 py-3 whitespace-nowrap align-top">{user.profileCompleted ? 'Заполнен' : 'Не заполнен'}</td>
                    <td className="px-4 py-3 whitespace-nowrap align-top">{user.groupCode || '—'}</td>
                    <td className="px-4 py-3 whitespace-nowrap align-top">{user.courseNo || '—'}</td>
                    <td className="px-4 py-3 whitespace-nowrap align-top">{user.departmentName || '—'}</td>
                    <td className="px-4 py-3 whitespace-nowrap align-top">{user.position || '—'}</td>
                    {showSensitiveColumns && <td className="px-4 py-3 whitespace-nowrap align-top">{user.phone || '—'}</td>}
                    {showSensitiveColumns && <td className="px-4 py-3 whitespace-nowrap align-top">{user.studentCard || '—'}</td>}
                    <td className="px-4 py-3 whitespace-nowrap align-top">{new Date(user.createdAt).toLocaleString()}</td>
                    {editable && (
                      <td className="px-4 py-3 whitespace-nowrap align-top">
                        <button
                          onClick={() => onEdit?.(user)}
                          className="rounded-2xl border border-slate-700 bg-slate-900 px-3 py-2 text-sm font-medium text-slate-200 hover:bg-slate-800"
                        >
                          Редактировать
                        </button>
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </section>
  );
}