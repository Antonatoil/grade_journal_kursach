import { useState } from 'react';
import { api } from '../lib/api';

type GithubRepoDto = {
  name: string;
  htmlUrl: string;
  description?: string | null;
  language?: string | null;
  stars?: number | null;
  updatedAt?: string | null;
};

type GithubProfileDto = {
  username: string;
  displayName?: string | null;
  avatarUrl?: string | null;
  htmlUrl?: string | null;
  bio?: string | null;
  publicRepos?: number | null;
  followers?: number | null;
  following?: number | null;
  repositories: GithubRepoDto[];
};

type GithubProfileCardProps = {
  defaultUsername?: string;
};

export function GithubProfileCard({ defaultUsername = '' }: GithubProfileCardProps) {
  const [username, setUsername] = useState(defaultUsername);
  const [data, setData] = useState<GithubProfileDto | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const load = async () => {
    if (!username.trim()) {
      setError('Введите username GitHub.');
      setData(null);
      return;
    }

    try {
      setLoading(true);
      setError(null);

      const response = await api.get<GithubProfileDto>(`/api/integrations/github/users/${username.trim()}`);
      setData(response.data);
    } catch {
      setError('Не удалось загрузить данные GitHub.');
      setData(null);
    } finally {
      setLoading(false);
    }
  };

  return (
    <section className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
      <h3 className="text-xl font-semibold text-slate-900">GitHub профиль</h3>
      <p className="mt-2 text-slate-500">
        Введите GitHub username, чтобы показать профиль и последние репозитории.
      </p>

      <div className="mt-4 flex flex-col gap-3 md:flex-row">
        <input
          value={username}
          onChange={(event) => setUsername(event.target.value)}
          placeholder="Например, octocat"
          className="flex-1 rounded-2xl border border-slate-300 bg-white px-4 py-3 text-slate-800 outline-none focus:border-blue-400"
        />
        <button
          type="button"
          onClick={load}
          disabled={loading}
          className="rounded-2xl bg-blue-500 px-4 py-3 font-medium text-white hover:bg-blue-400 disabled:opacity-70"
        >
          {loading ? 'Загрузка...' : 'Загрузить GitHub'}
        </button>
      </div>

      {error && (
        <div className="mt-4 rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-rose-700">
          {error}
        </div>
      )}

      {data && (
        <div className="mt-5 space-y-5">
          <div className="flex flex-col gap-4 md:flex-row md:items-center">
            {data.avatarUrl && (
              <img
                src={data.avatarUrl}
                alt={data.username}
                className="h-20 w-20 rounded-full border border-slate-200 object-cover"
              />
            )}
            <div>
              <h4 className="text-lg font-semibold text-slate-900">
                {data.displayName || data.username}
              </h4>
              <a
                href={data.htmlUrl || '#'}
                target="_blank"
                rel="noreferrer"
                className="text-sm text-blue-600 hover:underline"
              >
                @{data.username}
              </a>
              <p className="mt-2 text-slate-600">{data.bio || 'Описание не указано.'}</p>
              <p className="mt-2 text-sm text-slate-500">
                Репозиториев: {data.publicRepos ?? 0} · Подписчиков: {data.followers ?? 0} · Подписок: {data.following ?? 0}
              </p>
            </div>
          </div>

          <div className="overflow-x-auto rounded-2xl border border-slate-200">
            <table className="min-w-full text-left text-sm">
              <thead className="bg-slate-50 text-slate-500">
                <tr>
                  <th className="px-4 py-3 font-medium">Репозиторий</th>
                  <th className="px-4 py-3 font-medium">Язык</th>
                  <th className="px-4 py-3 font-medium">Звезды</th>
                  <th className="px-4 py-3 font-medium">Обновлен</th>
                </tr>
              </thead>
              <tbody>
                {data.repositories.map((repo) => (
                  <tr key={repo.name} className="border-t border-slate-200">
                    <td className="px-4 py-3">
                      <a
                        href={repo.htmlUrl}
                        target="_blank"
                        rel="noreferrer"
                        className="font-medium text-blue-600 hover:underline"
                      >
                        {repo.name}
                      </a>
                      <div className="mt-1 text-slate-500">{repo.description || 'Без описания'}</div>
                    </td>
                    <td className="px-4 py-3 text-slate-700">{repo.language || '—'}</td>
                    <td className="px-4 py-3 text-slate-700">{repo.stars ?? 0}</td>
                    <td className="px-4 py-3 text-slate-700">
                      {repo.updatedAt ? new Date(repo.updatedAt).toLocaleString() : '—'}
                    </td>
                  </tr>
                ))}
                {!data.repositories.length && (
                  <tr className="border-t border-slate-200">
                    <td colSpan={4} className="px-4 py-6 text-center text-slate-500">
                      Репозиториев нет.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </section>
  );
}