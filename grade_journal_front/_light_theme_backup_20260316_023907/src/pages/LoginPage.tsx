import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { useNavigate, Link } from 'react-router-dom';
import { z } from 'zod';
import { api } from '../lib/api';
import { useAuthStore } from '../store/auth';
import type { LoginRequest, LoginResponse } from '../types/auth';

const schema = z.object({
  username: z.string().min(1, 'Введите логин'),
  password: z.string().min(1, 'Введите пароль')
});

export function LoginPage() {
  const navigate = useNavigate();
  const setAuth = useAuthStore((state) => state.setAuth);

  const {
    register,
    handleSubmit,
    formState: { errors }
  } = useForm<LoginRequest>({
    resolver: zodResolver(schema)
  });

  const mutation = useMutation({
    mutationFn: async (payload: LoginRequest) => {
      const response = await api.post<LoginResponse>('/api/auth/login', payload);
      return response.data;
    },
    onSuccess: (data) => {
      setAuth(data);

      if (data.role === 'admin') {
        navigate('/admin');
        return;
      }

      if (data.role === 'teacher') {
        navigate('/teacher');
        return;
      }

      navigate('/student');
    }
  });

  return (
    <div className="mx-auto max-w-xl rounded-3xl border border-slate-800 bg-slate-900/70 p-8">
      <h1 className="mb-2 text-3xl font-bold text-white">Вход</h1>
      <p className="mb-6 text-slate-400">Введите логин и пароль для входа в аккаунт</p>

      <form onSubmit={handleSubmit((data) => mutation.mutate(data))} className="space-y-5">
        <div>
          <label className="mb-2 block text-sm text-slate-300">Логин</label>
          <input
            {...register('username')}
            className="w-full rounded-2xl border border-slate-700 bg-slate-950 px-4 py-3 text-white outline-none transition focus:border-blue-400"
            placeholder="Введите логин"
          />
          {errors.username && <p className="mt-2 text-sm text-rose-400">{errors.username.message}</p>}
        </div>

        <div>
          <label className="mb-2 block text-sm text-slate-300">Пароль</label>
          <input
            type="password"
            {...register('password')}
            className="w-full rounded-2xl border border-slate-700 bg-slate-950 px-4 py-3 text-white outline-none transition focus:border-blue-400"
            placeholder="Введите пароль"
          />
          {errors.password && <p className="mt-2 text-sm text-rose-400">{errors.password.message}</p>}
        </div>

        {mutation.isError && (
          <div className="rounded-2xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-300">
            {(mutation.error as any)?.response?.data?.message ?? 'Не удалось выполнить вход'}
          </div>
        )}

        <button
          type="submit"
          disabled={mutation.isPending}
          className="w-full rounded-2xl bg-blue-500 px-4 py-3 font-semibold text-white transition hover:bg-blue-400 disabled:opacity-60"
        >
          {mutation.isPending ? 'Выполняется вход...' : 'Войти'}
        </button>
      </form>

      <p className="mt-6 text-sm text-slate-400">
        Нет аккаунта?{' '}
        <Link to="/register" className="text-blue-300 hover:text-blue-200">
          Перейти к регистрации
        </Link>
      </p>
    </div>
  );
}