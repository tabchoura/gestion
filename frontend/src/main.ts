import { bootstrapApplication } from '@angular/platform-browser';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { App } from './app/app';
import { routes } from './app/app.routes';
import { authInterceptorFn } from './app/core/auth-token.interceptor';

bootstrapApplication(App, {
  providers: [
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptorFn])),
  ],
});
