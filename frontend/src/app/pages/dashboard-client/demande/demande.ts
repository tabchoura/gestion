import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators, FormGroup, FormControl, FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { DemandesService, Compte, DemandeChequier } from '../../../core/demande.service';
import { AuthService } from '../../../core/auth.service';

@Component({
  standalone: true,
  // ✅ Ajout de FormsModule pour ngModel
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
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

  // Sélecteur de compte (Reactive)
  compteCtrl = new FormControl<number | null>(null);
  // Pour le reste de l'UI
  compteIdSel = signal<number | null>(null);
  showModal = signal(false);
  errorMsg = signal<string | null>(null);
  successMsg = signal<string | null>(null);

  editMode = signal<boolean>(false);
  editingId = signal<number | null>(null);
  submitting = signal<boolean>(false);

  form!: FormGroup;

  constructor(private fb: FormBuilder, private api: DemandesService) {
    this.form = this.fb.group({
      compteId: [null, Validators.required],
      dateDemande: ['', Validators.required],
      pages: [25, [Validators.required, Validators.min(1), Validators.max(100)]],
      motif: ['', Validators.maxLength(255)]
    });
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

    // quand l'utilisateur change de compte dans le <select>
    this.compteCtrl.valueChanges.subscribe((id) => {
      this.compteIdSel.set(id ?? null);
      if (id != null) this.refresh(id);
    });

    this.loadComptes(preselectedId);
  }

  // ✅ Méthode manquante ajoutée
  onSelectCompte(compteId: number | null) {
    this.compteIdSel.set(compteId);
    if (compteId != null) {
      this.refresh(compteId);
    }
  }

  private loadComptes(preselectedId?: number | null) {
    this.loading = true;
    this.errorMsg.set(null);

    this.api.getMesComptes().subscribe({
      next: (comptes) => {
        this.comptes = comptes || [];

        let chosen: number | null = null;
        if (preselectedId && this.comptes.some(k => k.id === preselectedId)) {
          chosen = preselectedId;
        } else if (this.comptes.length > 0) {
          chosen = this.comptes[0].id;
        }

        this.compteIdSel.set(chosen);
        this.compteCtrl.setValue(chosen); // ✅ synchronise le select

        if (chosen != null) this.refresh(chosen); else this.loading = false;
      },
      error: (err) => {
        this.errorMsg.set('Impossible de charger vos comptes: ' + err.message);
        this.loading = false;
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
      }
    });
  }

  private toInputDate(value: string | Date): string {
    const d = value instanceof Date ? value : new Date(value);
    const yyyy = d.getFullYear();
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  }

  openModal() {
    const today = new Date();
    const currentCompteId = this.compteIdSel();

    this.form.reset({
      compteId: currentCompteId,
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

  modifier(d: DemandeChequier) {
    if (d.statut !== 'EN_ATTENTE') {
      this.errorMsg.set('Seules les demandes EN_ATTENTE peuvent être modifiées');
      return;
    }
    const date = d.dateDemande ? this.toInputDate(d.dateDemande as any) : this.toInputDate(new Date());

    this.form.reset({
      compteId: (d as any).compte?.id ?? this.compteIdSel(),
      dateDemande: date,
      pages: d.pages ?? 25,
      motif: d.motif ?? ''
    });

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
    if (this.form.invalid || this.submitting()) return;

    const cid = this.form.value.compteId as number | null;
    if (!cid) {
      this.errorMsg.set('Veuillez choisir un compte');
      return;
    }

    this.submitting.set(true);
    this.errorMsg.set(null);
    this.successMsg.set(null);

    if (this.editMode() && this.editingId()) {
      const payload = {
        dateDemande: this.form.value.dateDemande,
        pages: this.form.value.pages,
        motif: this.form.value.motif ?? null
      };

      this.api.modifierDemande(this.editingId()!, payload).subscribe({
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

    // Création
    const payload = {
      compteId: cid,
      dateDemande: this.form.value.dateDemande,
      pages: this.form.value.pages,
      motif: this.form.value.motif ?? null
    };

    this.api.creerDemande(payload).subscribe({
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
}