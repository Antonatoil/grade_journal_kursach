type GithubLoginButtonProps = {
  requestedRole?: 'student' | 'teacher';
  label?: string;
  className?: string;
};

const apiBaseUrl =
  import.meta.env.VITE_API_BASE_URL ??
  import.meta.env.VITE_API_URL ??
  'http://localhost:8080';

export function GithubLoginButton({
  requestedRole = 'student',
  label = 'Войти через GitHub',
  className = ''
}: GithubLoginButtonProps) {
  const handleClick = () => {
    window.location.href =
      `${apiBaseUrl}/api/auth/oauth2/github/start?role=${requestedRole}`;
  };

  return (
    <button
      type="button"
      onClick={handleClick}
      className={
        className ||
        'w-full rounded-2xl border border-slate-300 bg-white px-4 py-3 font-medium text-slate-700 shadow-sm transition hover:bg-slate-50'
      }
    >
      {label}
    </button>
  );
}