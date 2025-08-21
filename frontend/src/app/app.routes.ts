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



    {
    path: 'dashboardagent',
    loadComponent: () => import('./pages/dashboard-agent/dashboard-agent').then(m => m.DashboardAgentComponent),
    canActivate: [authGuard],
     children: [
       { path: '', pathMatch: 'full', redirectTo: 'profileagent,' },
{ path: 'profileagent',  loadComponent: () => import('./pages/dashboard-agent/profileagent/profile-agent').then(m => m.ProfileComponent) },

{ path: 'demandesrecues', loadComponent: () => import('./pages/dashboard-agent/demandes-recues/demandes-recues').then(m => m.DemandesRecuesComponent) },

    //   { path: 'demandes/:compteId', loadComponent: () => import('./pages/dashboard-client/demande/demande').then(m => m.DemandeComponent) },
    //   { path: 'demandes',           loadComponent: () => import('./pages/dashboard-client/demande/demande').then(m => m.DemandeComponent) },

       { path: 'historique',            loadComponent: () => import('./pages/dashboard-client/historique/historique').then(m => m.HistoriqueComponent) }
    //   { path: 'historique/:type/:id',  loadComponent: () => import('./pages/dashboard-client/historique/historique').then(m => m.HistoriqueComponent) },

    
  ]  },
];
  

  
