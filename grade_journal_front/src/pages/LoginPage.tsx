import { GithubLoginButton } from '../components/GithubLoginButton';
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
    <div className="mx-auto max-w-xl rounded-3xl border border-slate-200 bg-white p-8" shadow-sm>
      <h1 className="mb-2 text-3xl font-bold text-slate-900">Вход</h1>
      <p className="mb-6 text-slate-500">Введите логин и пароль для входа в аккаунт</p>

      <form onSubmit={handleSubmit((data) => mutation.mutate(data))} className="space-y-5">
        <div>
          <label className="mb-2 block text-sm text-slate-700">Логин</label>
          <input
            {...register('username')}
            className="w-full rounded-2xl border border-slate-300 bg-white px-4 py-3 text-slate-900 outline-none transition focus:border-blue-400"
            placeholder="Введите логин"
          />
          {errors.username && <p className="mt-2 text-sm text-rose-400">{errors.username.message}</p>}
        </div>

        <div>
          <label className="mb-2 block text-sm text-slate-700">Пароль</label>
          <input
            type="password"
            {...register('password')}
            className="w-full rounded-2xl border border-slate-300 bg-white px-4 py-3 text-slate-900 outline-none transition focus:border-blue-400"
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
            <div className="mt-4">
        <GithubLoginButton label="Войти через GitHub" />
      </div>
</form>

      <p className="mt-6 text-sm text-slate-500">
        Нет аккаунта?{' '}
        <Link to="/register" className="text-blue-300 hover:text-blue-200">
          Перейти к регистрации
        </Link>
      </p>
    </div>
  );
}