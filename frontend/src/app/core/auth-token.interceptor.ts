
// src/app/core/auth-token.interceptor.ts
import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';
import { environment } from '../../environments/environment.development';

export const authInterceptorFn: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);

  // Ne gère que les appels vers TON API (évite d’ajouter Authorization sur des assets, autres domaines, etc.)
  const isApiCall = req.url.startsWith(environment.apiUrl);
  const isAuthCall =
    isApiCall &&
    (req.url.includes('/auth/login') ||
     req.url.includes('/auth/register') ||
     req.url.includes('/auth/refresh'));

  const token = sessionStorage.getItem('token') || localStorage.getItem('token');

  const authReq = (isApiCall && !isAuthCall && token)
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(authReq).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status === 401 && isApiCall && !isAuthCall) {
        // On ne redirige que si un token existait (expiré/invalide). Sinon, on laisse le guard/router décider.
        if (token) {
          localStorage.removeItem('token'); sessionStorage.removeItem('token');
          localStorage.removeItem('role');  sessionStorage.removeItem('role');
          router.navigateByUrl('/login');
        }
      }
      return throwError(() => err);
    })
  );
};
