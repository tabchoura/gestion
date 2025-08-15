import { HttpInterceptorFn, HttpErrorResponse, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';
import { environment } from '../../environments/environment.development';

function withAuth(req: HttpRequest<any>, token: string) {
  return req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
}

export const authInterceptorFn: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);

  // Ne gère que ton API
  const isApiCall = req.url.startsWith(environment.apiUrl);
  const isAuthCall =
    isApiCall &&
    (req.url.includes('/auth/login') ||
     req.url.includes('/auth/register') ||
     req.url.includes('/auth/refresh'));

  const token = sessionStorage.getItem('token') || localStorage.getItem('token');

  const authReq = (isApiCall && !isAuthCall && token)
    ? withAuth(req, token)
    : req;

  return next(authReq).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status === 401 && isApiCall && !isAuthCall) {
        // ❌ Ne pas effacer le token/role automatiquement ici
        // ❌ Ne pas rediriger automatiquement
        // 👉 On laisse le composant décider (message, bouton Reconnecter)
      }
      return throwError(() => err);
    })
  );
};
