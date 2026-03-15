import { Link } from 'react-router-dom';

export function NotFoundPage() {
  return (
    <div className="rounded-3xl border border-slate-800 bg-slate-900/70 p-10 text-center">
      <h1 className="text-4xl font-bold text-white">404</h1>
      <p className="mt-3 text-slate-400">Страница не найдена</p>
      <Link
        to="/"
        className="mt-6 inline-flex rounded-2xl bg-blue-500 px-5 py-3 font-semibold text-white hover:bg-blue-400"
      >
        Вернуться на главную
      </Link>
    </div>
  );
}