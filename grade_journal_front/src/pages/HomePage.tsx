import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { api } from '../lib/api';

type DbHealth = {
  connected: boolean;
  database?: string;
  message?: string;
};

export function HomePage() {
  const { data, isLoading } = useQuery({
    queryKey: ['db-health'],
    queryFn: async () => {
      const response = await api.get<DbHealth>('/api/health/db');
      return response.data;
    }
  });

  return (
    <div className="space-y-8">
      <section className="rounded-3xl border border-slate-800 bg-slate-900/70 p-8 shadow-2xl">
        <div className="grid gap-8 md:grid-cols-[1.5fr_1fr] md:items-center">
          <div className="space-y-5">
            <span className="inline-flex rounded-full bg-blue-500/20 px-4 py-2 text-sm font-medium text-blue-300">
              Курсовой проект
            </span>

            <h1 className="text-4xl font-bold leading-tight text-white">
              Электронный журнал успеваемости с модулем прогнозирования оценок
            </h1>

            <p className="max-w-2xl text-base leading-7 text-slate-300">
              В системе доступны главная страница, вход, регистрация с отправкой заявки
              администратору, личные кабинеты по ролям и проверка подключения к базе данных.
            </p>

            <div className="flex flex-wrap gap-3">
              <Link
                to="/login"
                className="rounded-2xl bg-blue-500 px-5 py-3 font-semibold text-white hover:bg-blue-400"
              >
                Перейти ко входу
              </Link>

              <Link
                to="/register"
                className="rounded-2xl border border-slate-700 px-5 py-3 font-semibold text-slate-100 hover:bg-slate-800"
              >
                Зарегистрироваться
              </Link>
            </div>
          </div>

          <div className="rounded-3xl border border-slate-800 bg-slate-950/60 p-6">
            <h2 className="mb-4 text-xl font-semibold text-white">Статус backend и БД</h2>

            {isLoading ? (
              <p className="text-slate-400">Проверка подключения...</p>
            ) : (
              <div className="space-y-3">
                <div className="flex items-center gap-3">
                  <div
                    className={`h-3 w-3 rounded-full ${
                      data?.connected ? 'bg-emerald-400' : 'bg-rose-400'
                    }`}
                  />
                  <span className="text-slate-200">
                    {data?.connected ? 'PostgreSQL подключен' : 'Подключение отсутствует'}
                  </span>
                </div>

                <p className="text-sm text-slate-400">
                  {data?.database ? `База данных: ${data.database}` : data?.message}
                </p>
              </div>
            )}
          </div>
        </div>
      </section>

      <section className="grid gap-4 md:grid-cols-3">
        <div className="rounded-3xl border border-slate-800 bg-slate-900/70 p-6">
          <h3 className="mb-3 text-lg font-semibold text-white">Администратор</h3>
          <p className="text-sm leading-6 text-slate-300">
            Просматривает заявки на регистрацию, одобряет или отклоняет их, управляет доступом.
          </p>
        </div>

        <div className="rounded-3xl border border-slate-800 bg-slate-900/70 p-6">
          <h3 className="mb-3 text-lg font-semibold text-white">Преподаватель</h3>
          <p className="text-sm leading-6 text-slate-300">
            Входит в систему после одобрения и получает личный кабинет с основными данными профиля.
          </p>
        </div>

        <div className="rounded-3xl border border-slate-800 bg-slate-900/70 p-6">
          <h3 className="mb-3 text-lg font-semibold text-white">Студент</h3>
          <p className="text-sm leading-6 text-slate-300">
            Регистрируется, ждет одобрения администратора и после входа получает доступ к своему кабинету.
          </p>
        </div>
      </section>
    </div>
  );
}