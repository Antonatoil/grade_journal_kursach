import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/auth';
import { api } from '../lib/api';

type LayoutProps = {
  children: React.ReactNode;
};

export function Layout({ children }: LayoutProps) {
  const navigate = useNavigate();
  const { role, fullName, refreshToken, logout } = useAuthStore();

  const handleLogout = async () => {
    try {
      if (refreshToken) {
        await api.post('/api/auth/logout', { refreshToken });
      }
    } catch {
      // ignore
    } finally {
      logout();
      navigate('/login');
    }
  };

  const dashboardLink =
    role === 'admin' ? '/admin' :
    role === 'teacher' ? '/teacher' :
    role === 'student' ? '/student' :
    '/';

  return (
    <div className="min-h-screen">
      <header className="border-b border-slate-800 bg-slate-950/80 backdrop-blur">
        <div className="mx-auto flex max-w-[1850px] items-center justify-between px-6 py-4">
          <Link to="/" className="text-lg font-semibold text-white">
            Электронный журнал
          </Link>

          <nav className="flex items-center gap-3 text-sm text-slate-300">
            <NavLink to="/" className="rounded-xl px-3 py-2 hover:bg-slate-800">
              Главная
            </NavLink>

            {!role && (
              <>
                <NavLink to="/login" className="rounded-xl px-3 py-2 hover:bg-slate-800">
                  Вход
                </NavLink>
                <NavLink to="/register" className="rounded-xl px-3 py-2 hover:bg-slate-800">
                  Регистрация
                </NavLink>
              </>
            )}

            {role && (
              <>
                <NavLink to={dashboardLink} className="rounded-xl px-3 py-2 hover:bg-slate-800">
                  Кабинет
                </NavLink>
                <span className="hidden rounded-xl bg-slate-800 px-3 py-2 md:inline-flex">
                  {fullName}
                </span>
                <button
                  onClick={handleLogout}
                  className="rounded-xl bg-rose-500 px-3 py-2 font-medium text-white hover:bg-rose-400"
                >
                  Выйти
                </button>
              </>
            )}
          </nav>
        </div>
      </header>

      <main className="mx-auto max-w-[1850px] px-6 py-8">{children}</main>
    </div>
  );
}