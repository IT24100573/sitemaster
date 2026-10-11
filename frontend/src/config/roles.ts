import type { CurrentUser, RoleName, RolePath } from '../types/auth';

/** Role -> URL segment. Must match RoleName.rolePath on the server. */
export const ROLE_PATHS: Record<RoleName, RolePath> = {
  ADMIN: 'admin',
  PROJECT_MANAGER: 'pm',
  SITE_ENGINEER: 'site',
  WORKER: 'worker',
  HR_MANAGER: 'hr',
  ACCOUNTANT: 'accounts',
  SUPPLIER: 'supplier',
};

export const ROLE_ROUTES = (Object.keys(ROLE_PATHS) as RoleName[]).map((role) => ({
  role,
  path: ROLE_PATHS[role],
}));

export function dashboardPath(user: Pick<CurrentUser, 'rolePath'>): string {
  return `/${user.rolePath}/dashboard`;
}

/** True when `path` belongs to this user's area, e.g. /site/... for a site engineer. */
export function isOwnArea(user: Pick<CurrentUser, 'rolePath'>, path: string): boolean {
  return path.startsWith(`/${user.rolePath}/`);
}
