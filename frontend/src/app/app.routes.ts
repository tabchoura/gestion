import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';

export const routes: Routes = [
  // Accueil par défaut
  { path: '', loadComponent: () => import('./pages/acceuil/acceuil').then(m => m.AccueilComponent) },

  // (facultatif) URL explicite /acceuil
  { path: 'acceuil', loadComponent: () => import('./pages/acceuil/acceuil').then(m => m.AccueilComponent) },

  { path: 'login', loadComponent: () => import('./pages/login/login').then(m => m.LoginComponent) },
  { path: 'register', loadComponent: () => import('./pages/register/register').then(m => m.RegisterComponent) },

  {
    path: 'dashboardclient',
    loadComponent: () => import('./pages/dashboard-client/dashboard-client').then(m => m.DashboardClientComponent),
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'profile' },
      { path: 'profile', loadComponent: () => import('./pages/dashboard-client/profile/profile').then(m => m.ProfileComponent) },
    ]
  },

  { path: '**', redirectTo: '' } // retombe sur l’accueil
];
