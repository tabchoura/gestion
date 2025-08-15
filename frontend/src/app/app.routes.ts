import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';

export const routes: Routes = [
  { path: '', loadComponent: () => import('./pages/acceuil/acceuil').then(m => m.AccueilComponent) },
  { path: 'acceuil', loadComponent: () => import('./pages/acceuil/acceuil').then(m => m.AccueilComponent) },

  { path: 'login', loadComponent: () => import('./pages/login/login').then(m => m.LoginComponent) },
  { path: 'register', loadComponent: () => import('./pages/register/register').then(m => m.RegisterComponent) },

  {
    path: 'dashboardclient',
    loadComponent: () => import('./pages/dashboard-client/dashboard-client').then(m => m.DashboardClientComponent),
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'profile' },
      { path: 'profile',  loadComponent: () => import('./pages/dashboard-client/profile/profile').then(m => m.ProfileComponent) },

      { path: 'comptes',  loadComponent: () => import('./pages/dashboard-client/compte/compte').then(m => m.ComptesComponent) },

      { path: 'demandes/:compteId', loadComponent: () => import('./pages/dashboard-client/demande/demande').then(m => m.DemandeComponent) },
      { path: 'demandes',           loadComponent: () => import('./pages/dashboard-client/demande/demande').then(m => m.DemandeComponent) },

      { path: 'historique',            loadComponent: () => import('./pages/dashboard-client/historique/historique').then(m => m.HistoriqueComponent) },
      { path: 'historique/:type/:id',  loadComponent: () => import('./pages/dashboard-client/historique/historique').then(m => m.HistoriqueComponent) },

      // Alias si ton menu pointe sur "historiques"
      { path: 'historiques', pathMatch: 'full', redirectTo: 'historique' },
    ]
  },

  // ✅ SUPPRIME OU COMMENTE cette ligne qui cause le problème !
  // { path: '**', redirectTo: '' }
  
  // ✅ Optionnel: garde une route 404 spécifique si besoin
  // { path: '404', loadComponent: () => import('./pages/not-found/not-found').then(m => m.NotFoundComponent) },
  // { path: '**', redirectTo: '/404' }
];