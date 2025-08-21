import { Component, OnInit, signal, computed, inject, NgZone } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DemandesService, DemandeChequier, DemandeStatut } from '../../../../app/core/demande.service';
import { ToastrService } from 'ngx-toastr';

interface User { nom: string; prenom: string; email: string; }
interface DemandeChequierAgent extends DemandeChequier { user?: User; }

@Component({
  selector: 'app-demandes-recues-agent',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe],
  templateUrl: './demandes-recues.html',
  styleUrls: ['./demandes-recues.css']
})
export class DemandesRecuesComponent implements OnInit {
  private demandesService = inject(DemandesService);
  private toastr = inject(ToastrService);
  private zone = inject(NgZone);

  demandes = signal<DemandeChequierAgent[]>([]);
  loading = signal<boolean>(false);
  submitting = signal<boolean>(false);

  statutFilter = signal<string>('');
  clientFilter = signal<string>('');

  // on garde ces messages si ton template les affiche déjà (mais on passe aux toasts)
  errorMsg = signal<string>('');
  successMsg = signal<string>('');

  demandesFiltered = computed(() => {
    let list = this.demandes();
    const s = this.statutFilter();
    if (s) list = list.filter(d => d.statut === s);

    const c = this.clientFilter();
    if (c) {
      const lc = c.toLowerCase();
      list = list.filter(d =>
        (d.user?.email?.toLowerCase().includes(lc)) ||
        (`${d.user?.prenom ?? ''} ${d.user?.nom ?? ''}`.trim().toLowerCase().includes(lc))
      );
    }
    return list;
  });

  statistiques = computed(() => ({
    total: this.demandes().length,
    enAttente: this.count('EN_ATTENTE'),
    approuvees: this.count('APPROUVEE'),
    rejetees: this.count('REJETEE'),
    annulees: this.count('ANNULEE'),
  }));

  ngOnInit(): void { this.loadDemandes(); }

  loadDemandes(): void {
    this.loading.set(true);
    this.clearMsgs();
    this.demandesService.getToutesLesDemandes().subscribe({
      next: (data) => { this.demandes.set(data as DemandeChequierAgent[]); this.loading.set(false); },
      error: (e: Error) => { this.loading.set(false); this.toastKO(e.message || 'Erreur de chargement des demandes.'); }
    });
  }
  refresh(): void { this.loadDemandes(); }

  onFilterStatut(v: string) { this.statutFilter.set(v); }
  onFilterClient(v: string) { this.clientFilter.set(v); }
  clearFilters() { this.statutFilter.set(''); this.clientFilter.set(''); }

  private count(s: DemandeStatut) { return this.demandes().filter(d => d.statut === s).length; }
  canAct(d: DemandeChequier): boolean { return d.statut === 'EN_ATTENTE'; }

  approuver(d: DemandeChequier): void {
    if (!this.canAct(d)) return;
    const prev = d.statut;
    this.updateLocal(d.id, 'APPROUVEE');  // MAJ optimiste
    this.demandesService.changerStatutDemande(d.id, 'APPROUVEE').subscribe({
      next: () => this.toastOK(`Demande #${d.id} approuvée`),
      error: (err: Error) => { this.updateLocal(d.id, prev); this.toastKO(err.message || `Échec d'approbation de la demande #${d.id}`); }
    });
  }

  rejeter(d: DemandeChequier): void {
    if (!this.canAct(d)) return;
    const prev = d.statut;
    this.updateLocal(d.id, 'REJETEE');    // MAJ optimiste
    this.demandesService.changerStatutDemande(d.id, 'REJETEE').subscribe({
      next: () => this.toastOK(`Demande #${d.id} rejetée`),
      error: (err: Error) => { this.updateLocal(d.id, prev); this.toastKO(err.message || `Échec du rejet de la demande #${d.id}`); }
    });
  }

  supprimer(d: DemandeChequier): void {
    if (!confirm(`Supprimer la demande #${d.id} ?`)) return;
    this.submitting.set(true); this.clearMsgs();
    this.demandesService.supprimerDemande(d.id).subscribe({
      next: () => {
        this.demandes.set(this.demandes().filter(x => x.id !== d.id));
        this.submitting.set(false);
        this.toastOK(`Demande #${d.id} supprimée`);
      },
      error: (err: Error) => { this.submitting.set(false); this.toastKO(err.message || `Échec de suppression de la demande #${d.id}`); }
    });
  }

  voirDetails(d: DemandeChequierAgent): void {
    const nom = d.user ? `${d.user.prenom} ${d.user.nom}` : 'Client inconnu';
    alert(
      `#${d.id}\nClient: ${nom}\nCompte: ${d.compte?.numeroCompte || d.numeroCompte || '-'}\nPages: ${d.pages}\nStatut: ${d.statut}\nDate: ${new Date(d.dateDemande).toLocaleDateString('fr-FR')}${d.motif ? `\nMotif: ${d.motif}` : ''}`
    );
  }

  // ---------- helpers ----------
  private updateLocal(id: number, statut: DemandeStatut) {
    const arr = this.demandes().slice();
    const i = arr.findIndex(x => x.id === id);
    if (i !== -1) { arr[i] = { ...arr[i], statut }; this.demandes.set(arr); }
  }
  private clearMsgs() { this.errorMsg.set(''); this.successMsg.set(''); }

  // Toasters (dans la zone Angular pour être inratables)
  private toastOK(m: string) {
    this.successMsg.set(m);
    this.zone.run(() => this.toastr.success(m));
    setTimeout(() => this.successMsg.set(''), 3000);
  }
  private toastKO(m: string) {
    this.errorMsg.set(m);
    this.zone.run(() => this.toastr.error(m, 'Erreur'));
    setTimeout(() => this.errorMsg.set(''), 4000);
  }

  // UI classes/labels
  badgeClass(s: DemandeStatut): string {
    switch (s) {
      case 'EN_ATTENTE': return 'badge warning';
      case 'APPROUVEE':  return 'badge success';
      case 'REJETEE':    return 'badge danger';
      case 'ANNULEE':    return 'badge secondary';
      default: return 'badge';
    }
  }
  getRowClass(s: DemandeStatut): string {
    switch (s) {
      case 'EN_ATTENTE': return 'row-pending';
      case 'APPROUVEE':  return 'row-approved';
      case 'REJETEE':    return 'row-rejected';
      case 'ANNULEE':    return 'row-cancelled';
      default: return '';
    }
  }
  getStatusLabel(s: DemandeStatut): string {
    switch (s) {
      case 'EN_ATTENTE': return 'En Attente';
      case 'APPROUVEE':  return 'Approuvée';
      case 'REJETEE':    return 'Rejetée';
      case 'ANNULEE':    return 'Annulée';
      default: return s;
    }
  }
  getClientDisplayName(d: DemandeChequierAgent): string {
    return d.user ? `${d.user.prenom} ${d.user.nom}` : 'Client inconnu';
  }
  getEmptyMessage(): string {
    return (this.statutFilter() || this.clientFilter())
      ? 'Aucune demande ne correspond aux filtres sélectionnés.'
      : 'Aucune demande de chéquier reçue pour le moment.';
  }
}
