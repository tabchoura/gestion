import { CanMatchFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';

export const roleGuard: CanMatchFn = (route) => {
  const router = inject(Router);
  const auth = inject(AuthService);
  const role = auth.getRole();
  const allowed = (route.data?.['roles'] as string[]) || [];

  if (role && allowed.includes(role)) return true;

  if (role === 'CLIENT') router.navigateByUrl('/dashboardclient/profile');
  else if (role === 'AGENT') router.navigateByUrl('/dashboardagent');
  else router.navigateByUrl('/login');

  return false;
};
