// src/app/pages/dashboard-client/historique/historique.ts
import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HistoriqueService, Historique } from '../../../core/historique.service';

@Component({
  selector: 'app-historique',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './historique.html',
  styleUrls: ['./historique.css']
})
export class HistoriqueComponent implements OnInit {
  private hist = inject(HistoriqueService);

  historique = signal<Historique[]>([]);
  loading = signal<boolean>(true);
  errorMsg = signal<string | null>(null);

  ngOnInit() {
    console.log('=== HISTORIQUE COMPONENT INIT ===');
    this.load();
  }

  load() {
    console.log('=== LOAD HISTORIQUE ===');
    this.loading.set(true);
    this.errorMsg.set(null);

    const token = localStorage.getItem('token') || sessionStorage.getItem('token');
    if (!token) {
      console.error('Aucun token trouvé');
      this.errorMsg.set('Non authentifié - pas de token');
      this.loading.set(false);
      return;
    }

    this.hist.mine().subscribe({
      next: (res: Historique[]) => {
        console.log('Réponse reçue:', res);
        this.historique.set(res ?? []);
      },
      error: (err: any) => {
        console.error('Erreur lors du chargement:', err);
        let msg = 'Impossible de charger l’historique.';
        if (err.status === 401) msg = 'Non authentifié - token invalide ou expiré';
        else if (err.status === 403) msg = 'Accès refusé';
        else if (err.status === 0) msg = 'Impossible de contacter le serveur';
        this.errorMsg.set(msg);
      },
      complete: () => {
        console.log('Chargement terminé');
        this.loading.set(false);
      }
    });
  }

  // === Manque précédemment : méthodes utilisées par le template ===
  actionLabel(action: string): string {
    const map: Record<string, string> = {
      LOGIN: 'Connexion',
      LOGOUT: 'Déconnexion',
      DEMANDE_CREE: 'Demande créée',
      DEMANDE_MISE_A_JOUR: 'Demande mise à jour',
      DEMANDE_SUPPRIMEE: 'Demande supprimée',
      PROFILE_MIS_A_JOUR: 'Profil mis à jour',
      TEST: 'Événement de test',
      TEST_LOG_COMPONENT: 'Test (composant)',
      TEST_LOG_OBSERVABLE: 'Test (observable)',
      TEST_MANUEL: 'Test manuel'
    };
    return map[action] ?? action;
  }

  summary(item: Historique): string {
    const d = item?.creeLe ? new Date(item.creeLe) : new Date();
    const heure = d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false });
    const id = item.ressourceId ? ` #${item.ressourceId}` : '';

    switch (item.action) {
      case 'LOGIN': return `Connexion effectuée à ${heure}.`;
      case 'LOGOUT': return `Déconnexion effectuée à ${heure}.`;
      case 'DEMANDE_CREE': return `Demande${id} créée à ${heure}.`;
      case 'DEMANDE_MISE_A_JOUR': return `Demande${id} mise à jour à ${heure}.`;
      case 'DEMANDE_SUPPRIMEE': return `Demande${id} supprimée à ${heure}.`;
      case 'PROFILE_MIS_A_JOUR': return `Profil mis à jour à ${heure}.`;
      default: return `${this.actionLabel(item.action)} à ${heure}.`;
    }
  }
  // === fin des méthodes manquantes ===

  // Test de log depuis le composant
  testLog() {
    console.log('=== TEST LOG BUTTON CLICKED ===');
    const token = localStorage.getItem('token') || sessionStorage.getItem('token');
    if (!token) {
      console.error('Pas de token pour le test');
      alert('Pas de token - vous devez être connecté');
      return;
    }

    this.hist.logAction(
      'TEST_LOG_COMPONENT',
      'Test depuis le composant historique',
      'COMPONENT',
      'historique-component',
      'Composant Historique',
      '/dashboard/historique'
    );

    setTimeout(() => this.load(), 800);
  }

  testLogObservable() {
    console.log('=== TEST LOG OBSERVABLE ===');
    this.hist.log('TEST_LOG_OBSERVABLE', '/dashboard/historique').subscribe({
      next: () => { alert('Log enregistré avec succès !'); this.load(); },
      error: (e) => { console.error('Erreur log observable:', e); alert('Erreur lors de l’enregistrement du log'); }
    });
  }

  testManualLog() {
    console.log('=== TEST MANUAL LOG ===');

    const testEntry = {
      action: 'TEST_MANUEL',
      message: 'Test manuel depuis le composant',
      page: typeof window !== 'undefined' ? window.location.pathname : '/dashboard/historique',
      ressourceType: 'TEST',
      ressourceId: 'manual-test-' + Date.now(),
      ressourceLabel: 'Test Manuel',
      payloadJson: JSON.stringify({
        test: true,
        timestamp: new Date().toISOString(),
        component: 'HistoriqueComponent'
      })
    };

    this.hist.addHistoryEntry(testEntry).subscribe({
      next: () => { alert('Test manuel réussi !'); this.load(); },
      error: (error) => { console.error('Erreur test manuel:', error); alert('Erreur test manuel'); }
    });
  }
}
