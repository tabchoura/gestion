import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators, FormGroup } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { DemandesService, Compte, DemandeChequier } from '../../../core/demande.service';
import { AuthService } from '../../../core/auth.service';

@Component({
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './demande.html',
  styleUrls: ['./demande.css']
})
export class DemandeComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private authService = inject(AuthService);

  comptes: Compte[] = [];
  demandes: DemandeChequier[] = [];
  loading = false;

  // sélection de compte / UI
  compteIdSel = signal<number | null>(null);
  showModal = signal(false);
  errorMsg = signal<string | null>(null);
  successMsg = signal<string | null>(null);

  // édition
  editMode = signal<boolean>(false);
  editingId = signal<number | null>(null);
  submitting = signal<boolean>(false);

  form!: FormGroup;

  constructor(private fb: FormBuilder, private api: DemandesService) {
    this.form = this.fb.group({
      dateDemande: ['', Validators.required],
      pages: [25, [Validators.required, Validators.min(1), Validators.max(100)]],
      motif: ['', Validators.maxLength(255)]
    });
  }

  // --- Infos pour affichage (colonne "Compte")
  get selectedCompte(): Compte | undefined {
    const id = this.compteIdSel();
    return id != null ? this.comptes.find(c => c.id === id) : undefined;
  }

  ngOnInit() {
    if (!this.authService.isAuthenticated()) {
      this.router.navigate(['/login']);
      return;
    }
    let preselectedId: number | null = null;
    this.route.paramMap.subscribe(pm => {
      const p = pm.get('compteId');
      if (p) preselectedId = Number(p);
    });
    const q = this.route.snapshot.queryParamMap.get('compteId');
    if (!preselectedId && q) preselectedId = Number(q);
    this.loadComptes(preselectedId);
  }

  private loadComptes(preselectedId?: number | null) {
    this.loading = true;
    this.errorMsg.set(null);
    this.api.getMesComptes().subscribe({
      next: (comptes) => {
        this.comptes = comptes || [];
        if (preselectedId && this.comptes.some(k => k.id === preselectedId)) {
          this.compteIdSel.set(preselectedId);
        } else if (this.comptes.length > 0) {
          this.compteIdSel.set(this.comptes[0].id);
        } else {
          this.compteIdSel.set(null);
        }
        const id = this.compteIdSel();
        if (id != null) this.refresh(id); else this.loading = false;
      },
      error: (err) => {
        this.errorMsg.set('Impossible de charger vos comptes: ' + err.message);
        this.loading = false;
        if (err.message.includes('Non autorisé')) {
          setTimeout(() => this.authService.logout(), 2000);
        }
      }
    });
  }

  private refresh(compteId: number) {
    this.loading = true;
    this.errorMsg.set(null);
    this.successMsg.set(null);
    this.api.getDemandesByCompte(compteId).subscribe({
      next: (demandes) => {
        this.demandes = demandes || [];
        this.loading = false;
      },
      error: (err) => {
        this.errorMsg.set('Chargement des demandes échoué: ' + err.message);
        this.loading = false;
        if (err.message.includes('Non autorisé')) {
          setTimeout(() => this.authService.logout(), 2000);
        }
      }
    });
  }

  // Utilitaire: formater en yyyy-MM-dd pour <input type="date">
  private toInputDate(value: string | Date): string {
    const d = value instanceof Date ? value : new Date(value);
    const yyyy = d.getFullYear();
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  }

  // --------- Helpers pour sécuriser le payload ---------
  private sanitizeDate(input: any): string | null {
    if (!input) return null;
    if (/^\d{4}-\d{2}-\d{2}$/.test(String(input))) return String(input);
    const d = new Date(input);
    if (isNaN(d.getTime())) return null;
    const yyyy = d.getFullYear();
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  }
  private sanitizePages(v: any): number | null {
    const n = Number(v);
    if (!Number.isFinite(n)) return null;
    const ni = Math.trunc(n);
    if (ni < 1 || ni > 100) return null;
    return ni;
  }
  private sanitizeMotif(v: any): string | null {
    const s = (v ?? '').toString().trim();
    if (!s) return null;
    return s.length > 255 ? s.slice(0, 255) : s;
  }

  // ---- création
  openModal() {
    const today = new Date();
    this.form.reset({
      dateDemande: this.toInputDate(today),
      pages: 25,
      motif: ''
    });
    this.editMode.set(false);
    this.editingId.set(null);
    this.errorMsg.set(null);
    this.successMsg.set(null);
    this.showModal.set(true);
  }

  // ---- édition
  modifier(d: DemandeChequier) {
    if (d.statut !== 'EN_ATTENTE') {
      this.errorMsg.set('Seules les demandes EN_ATTENTE peuvent être modifiées');
      return;
    }
    const date = d.dateDemande ? this.toInputDate(d.dateDemande as any) : this.toInputDate(new Date());
    this.form.reset({ dateDemande: date, pages: d.pages ?? 25, motif: d.motif ?? '' });
    this.editMode.set(true);
    this.editingId.set(d.id);
    this.errorMsg.set(null);
    this.successMsg.set(null);
    this.showModal.set(true);
  }

  closeModal() {
    this.showModal.set(false);
    this.submitting.set(false);
  }

  submit() {
    const cid = this.compteIdSel();
    if (!cid || this.form.invalid || this.submitting()) return;

    this.submitting.set(true);
    this.errorMsg.set(null);
    this.successMsg.set(null);

    const fv = this.form.value;

    // ====== CAS UPDATE ======
    if (this.editMode() && this.editingId()) {
      const dateStr = this.sanitizeDate(fv.dateDemande);
      const pages   = this.sanitizePages(fv.pages);
      const motif   = this.sanitizeMotif(fv.motif);

      if (!dateStr) {
        this.errorMsg.set('Date invalide (format attendu: YYYY-MM-DD)');
        this.submitting.set(false);
        return;
      }
      if (pages == null) {
        this.errorMsg.set('Pages doit être un entier entre 1 et 100');
        this.submitting.set(false);
        return;
      }

      const payloadUpdate = { dateDemande: dateStr, pages, motif }; // motif peut être null

      this.api.modifierDemande(this.editingId()!, payloadUpdate).subscribe({
        next: () => {
          this.successMsg.set('Demande modifiée avec succès');
          this.closeModal();
          this.refresh(cid);
        },
        error: (err) => {
          this.errorMsg.set(err.message || 'Mise à jour impossible');
          this.submitting.set(false);
        }
      });
      return;
    }

    // ====== CAS CREATION ======
    const dateStr = this.sanitizeDate(fv.dateDemande);
    const pages   = this.sanitizePages(fv.pages);
    const motif   = this.sanitizeMotif(fv.motif);

    if (!dateStr) {
      this.errorMsg.set('Date invalide (format attendu: YYYY-MM-DD)');
      this.submitting.set(false);
      return;
    }
    if (pages == null) {
      this.errorMsg.set('Pages doit être un entier entre 1 et 100');
      this.submitting.set(false);
      return;
    }

    const payloadCreate = { compteId: cid, dateDemande: dateStr, pages, motif }; // motif peut être null

    this.api.creerDemande(payloadCreate).subscribe({
      next: () => {
        this.successMsg.set('Demande créée avec succès');
        this.closeModal();
        this.refresh(cid);
      },
      error: (err) => {
        this.errorMsg.set(err.message || 'Création impossible');
        this.submitting.set(false);
      }
    });
  }

  annuler(d: DemandeChequier) {
    const cid = this.compteIdSel();
    if (!cid || d.statut !== 'EN_ATTENTE') return;
    this.errorMsg.set(null);
    this.api.annulerDemande(d.id).subscribe({
      next: () => {
        this.successMsg.set('Demande annulée');
        this.refresh(cid);
      },
      error: (err) => this.errorMsg.set('Annulation impossible: ' + err.message)
    });
  }

  supprimer(d: DemandeChequier) {
    const cid = this.compteIdSel();
    if (!cid) return;
    if (!confirm('Êtes-vous sûr de vouloir supprimer cette demande ?')) return;
    this.errorMsg.set(null);
    this.api.supprimerDemande(d.id).subscribe({
      next: () => {
        this.successMsg.set('Demande supprimée');
        this.refresh(cid);
      },
      error: (err) => this.errorMsg.set('Suppression impossible: ' + err.message)
    });
  }

  badgeClass(s: string) {
    switch (s) {
      case 'EN_ATTENTE': return 'badge gray';
      case 'APPROUVEE': return 'badge green';
      case 'REJETEE': return 'badge red';
      case 'ANNULEE': return 'badge dark';
      default: return 'badge';
    }
  }

  clearMessages() {
    this.errorMsg.set(null);
    this.successMsg.set(null);
  }
}
