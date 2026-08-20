import { AuthApi, Configuration, ResponseError } from '../generated/api';
import type { InitOverrideFunction } from '../generated/api';
import type { UserRole } from '../generated/api';

const apiBaseUrl = import.meta.env.VITE_TICKET_API_BASE_URL ?? 'http://localhost:8080';

const authApi = new AuthApi(
  new Configuration({
    basePath: apiBaseUrl,
    credentials: 'include',
  }),
);

type LoginCredentials = {
  email: string;
  password: string;
};

export type AuthenticatedUser = {
  id: string;
  email: string;
  role: UserRole;
};

export async function login(credentials: LoginCredentials): Promise<AuthenticatedUser> {
  await prepareCsrfToken();

  const response = await authApi.login(
    {
      loginRequest: {
        login: credentials.email,
        password: credentials.password,
      },
    },
    withCsrfHeader,
  );

  return {
    id: response.id,
    email: response.email,
    role: response.role,
  };
}

export async function register(credentials: LoginCredentials): Promise<AuthenticatedUser> {
  await prepareCsrfToken();

  const response = await authApi.registerUser(
    {
      registerUserRequest: {
        email: credentials.email,
        password: credentials.password,
      },
    },
    withCsrfHeader,
  );

  return {
    id: response.id,
    email: response.email,
    role: response.role,
  };
}

export async function logout(): Promise<void> {
  await prepareCsrfToken();
  await authApi.logout(withCsrfHeader);
}

export async function toUserMessage(error: unknown): Promise<string> {
  if (error instanceof ResponseError) {
    if (error.response.status === 400) {
      return 'Check the form values and try again.';
    }

    if (error.response.status === 401) {
      return 'The email or password is not valid.';
    }

    if (error.response.status === 409) {
      return 'An account with this email already exists.';
    }
  }

  return 'The server is unavailable. Try again in a moment.';
}

async function prepareCsrfToken(): Promise<void> {
  await authApi.getCsrfToken();
}

const withCsrfHeader: InitOverrideFunction = async ({ init }) => {
  const csrfToken = getCookieValue('XSRF-TOKEN');

  if (!csrfToken) {
    return {};
  }

  return {
    headers: {
      ...init.headers,
      'X-XSRF-TOKEN': csrfToken,
    },
  };
};

function getCookieValue(name: string): string | undefined {
  return document.cookie
    .split('; ')
    .find((cookie) => cookie.startsWith(`${name}=`))
    ?.split('=')
    .slice(1)
    .join('=');
}
