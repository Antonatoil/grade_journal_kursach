const url = new URL(window.location.href);
const status = url.searchParams.get('oauth2_status');
const message = url.searchParams.get('oauth2_message');

if (status) {
  if (status === 'approved') {
    const accessToken = url.searchParams.get('oauth2_access');
    const refreshToken = url.searchParams.get('oauth2_refresh');
    const role = url.searchParams.get('oauth2_role');

    if (accessToken) {
      localStorage.setItem('accessToken', accessToken);
      localStorage.setItem('refreshToken', refreshToken ?? '');
      localStorage.setItem('oauth2Role', role ?? '');
      sessionStorage.setItem('oauth2FlashMessage', message ?? 'Вход через Google выполнен успешно.');
    }
  } else {
    sessionStorage.setItem(
      'oauth2FlashMessage',
      message ?? 'Заявка через Google отправлена администратору.'
    );
  }

  url.searchParams.delete('oauth2_status');
  url.searchParams.delete('oauth2_message');
  url.searchParams.delete('oauth2_access');
  url.searchParams.delete('oauth2_refresh');
  url.searchParams.delete('oauth2_role');
  url.searchParams.delete('oauth2_email');

  window.history.replaceState({}, document.title, url.pathname + (url.search ? url.search : ''));

  if (status === 'approved') {
    window.location.href = '/';
  } else {
    window.location.href = '/login';
  }
}

export {};