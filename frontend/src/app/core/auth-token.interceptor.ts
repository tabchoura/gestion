import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';

export const authInterceptorFn: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  // Seules ces routes sont publiques (pas d’Authorization)
  const isPublicAuth = req.url.includes('/auth/login') || req.url.includes('/auth/register');
  const token = sessionStorage.getItem('token') || localStorage.getItem('token');

  const authReq = (!isPublicAuth && token)
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(authReq).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status === 401 && !isPublicAuth) {
        localStorage.removeItem('token'); sessionStorage.removeItem('token');
        localStorage.removeItem('role');  sessionStorage.removeItem('role');
        router.navigateByUrl('/login');
      }
      return throwError(() => err);
    })
  );
};
