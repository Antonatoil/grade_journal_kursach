import { GithubLoginButton } from '../components/GithubLoginButton';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { Link } from 'react-router-dom';
import { z } from 'zod';
import { api } from '../lib/api';
import type { RegisterResponse } from '../types/auth';

type RegisterFormValues = {
  username: string;
  password: string;
  fullName: string;
  email: string;
  desiredRole: 'teacher' | 'student';
};

const schema = z.object({
  username: z.string().min(3, 'Минимум 3 символа').max(50, 'Максимум 50 символов'),
  password: z.string().min(6, 'Минимум 6 символов').max(100, 'Максимум 100 символов'),
  fullName: z.string().min(5, 'Введите полное ФИО').max(150, 'Максимум 150 символов'),
  email: z.string().email('Некорректный email').or(z.literal('')),
  desiredRole: z.enum(['teacher', 'student'], {
    errorMap: () => ({ message: 'Выберите роль' })
  })
});

export function RegisterPage() {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors }
  } = useForm<RegisterFormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      desiredRole: 'student',
      email: ''
    }
  });

  const mutation = useMutation({
    mutationFn: async (payload: RegisterFormValues) => {
      const response = await api.post<RegisterResponse>('/api/auth/register-request', {
        ...payload,
        email: payload.email || undefined
      });

      return response.data;
    },
    onSuccess: () => {
      reset({
        username: '',
        password: '',
        fullName: '',
        email: '',
        desiredRole: 'student'
      });
    }
  });

  return (
    <div className="mx-auto max-w-2xl rounded-3xl border border-slate-200 bg-white p-8" shadow-sm>
      <h1 className="mb-2 text-3xl font-bold text-slate-900">Регистрация</h1>
      <p className="mb-6 text-slate-500">
        Заполните форму. После отправки заявка попадет на рассмотрение администратору.
      </p>

      <form
        onSubmit={handleSubmit((formData) => {
          mutation.mutate(formData);
        })}
        className="grid gap-5 md:grid-cols-2"
      >
        <div className="md:col-span-1">
          <label className="mb-2 block text-sm text-slate-700">Логин</label>
          <input
            {...register('username')}
            className="w-full rounded-2xl border border-slate-300 bg-white px-4 py-3 text-slate-900 outline-none transition focus:border-blue-400"
            placeholder="student001"
          />
          {errors.username && <p className="mt-2 text-sm text-rose-400">{errors.username.message}</p>}
        </div>

        <div className="md:col-span-1">
          <label className="mb-2 block text-sm text-slate-700">Пароль</label>
          <input
            type="password"
            {...register('password')}
            className="w-full rounded-2xl border border-slate-300 bg-white px-4 py-3 text-slate-900 outline-none transition focus:border-blue-400"
            placeholder="Не менее 6 символов"
          />
          {errors.password && <p className="mt-2 text-sm text-rose-400">{errors.password.message}</p>}
        </div>

        <div className="md:col-span-2">
          <label className="mb-2 block text-sm text-slate-700">ФИО</label>
          <input
            {...register('fullName')}
            className="w-full rounded-2xl border border-slate-300 bg-white px-4 py-3 text-slate-900 outline-none transition focus:border-blue-400"
            placeholder="Иванов Иван Иванович"
          />
          {errors.fullName && <p className="mt-2 text-sm text-rose-400">{errors.fullName.message}</p>}
        </div>

        <div className="md:col-span-1">
          <label className="mb-2 block text-sm text-slate-700">Email</label>
          <input
            {...register('email')}
            className="w-full rounded-2xl border border-slate-300 bg-white px-4 py-3 text-slate-900 outline-none transition focus:border-blue-400"
            placeholder="user@example.com"
          />
          {errors.email && <p className="mt-2 text-sm text-rose-400">{errors.email.message}</p>}
        </div>

        <div className="md:col-span-1">
          <label className="mb-2 block text-sm text-slate-700">Роль</label>
          <select
            {...register('desiredRole')}
            className="w-full rounded-2xl border border-slate-300 bg-white px-4 py-3 text-slate-900 outline-none transition focus:border-blue-400"
          >
            <option value="student">Студент</option>
            <option value="teacher">Преподаватель</option>
          </select>
          {errors.desiredRole && <p className="mt-2 text-sm text-rose-400">{errors.desiredRole.message}</p>}
        </div>

        {mutation.isError && (
          <div className="md:col-span-2 rounded-2xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-300">
            {(mutation.error as any)?.response?.data?.message ?? 'Не удалось отправить заявку'}
          </div>
        )}

        {mutation.isSuccess && (
          <div className="md:col-span-2 rounded-2xl border border-emerald-500/30 bg-emerald-500/10 px-4 py-3 text-sm text-emerald-300">
            {mutation.data.message}
          </div>
        )}

        <div className="md:col-span-2">
          <button
            type="submit"
            disabled={mutation.isPending}
            className="w-full rounded-2xl bg-blue-500 px-4 py-3 font-semibold text-white transition hover:bg-blue-400 disabled:opacity-60"
          >
            {mutation.isPending ? 'Отправка...' : 'Отправить заявку'}
          </button>
        </div>
            <div className="mt-4 space-y-3">
        <GithubLoginButton
          requestedRole="student"
          label="Регистрация студента через GitHub"
        />
        <GithubLoginButton
          requestedRole="teacher"
          label="Регистрация преподавателя через GitHub"
        />
      </div>
</form>

      <p className="mt-6 text-sm text-slate-500">
        Уже есть аккаунт?{' '}
        <Link to="/login" className="text-blue-300 hover:text-blue-200">
          Перейти ко входу
        </Link>
      </p>
    </div>
  );
}