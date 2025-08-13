import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators, FormGroup } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { DemandesService, Compte, DemandeChequier } from '../../../core/demande.service';

@Component({
  standalone: true,
  selector: 'app-demandes-chequier',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './demande.html',
  styleUrls: ['./demande.css']
})
export class DemandeComponent implements OnInit {
  private route = inject(ActivatedRoute);

  comptes: Compte[] = [];
  demandes: DemandeChequier[] = [];
  loading = false;

  compteIdSel = signal<number | null>(null);
  forceCompte  = signal<boolean>(false); // masque le sélecteur quand param présent
  showModal    = signal(false);
  errorMsg     = signal<string | null>(null);

  form!: FormGroup;

  constructor(private fb: FormBuilder, private api: DemandesService) {
    this.form = this.fb.group({
      dateDemande: ['', Validators.required],
      pages: [25, Validators.required],
      motif: ['']
    });
  }

  // ---- Getters pour le template (évite NG5002) ----
  get selectedCompte(): Compte | undefined {
    const id = this.compteIdSel();
    return id != null ? this.comptes.find(c => c.id === id) : undefined;
  }
  get selectedBanque(): string {
    return this.selectedCompte?.banque ?? '—';
  }
  get selectedNumero(): string | number {
    return this.selectedCompte?.numeroCompte ?? (this.compteIdSel() ?? '—');
  }
  // -------------------------------------------------

  ngOnInit() {
    // Charger les comptes (affichage/infos)
    this.api.getMesComptes().subscribe({
      next: c => (this.comptes = c),
      error: () => this.errorMsg.set('Impossible de charger vos comptes.')
    });

    // Si on arrive via /demandes/:compteId -> forcer le compte et charger
    this.route.paramMap.subscribe(pm => {
      const p = pm.get('compteId');
      if (p) {
        const id = Number(p);
        this.forceCompte.set(true);
        this.compteIdSel.set(id);
        this.refresh(id);
      }
    });

    // Compatibilité si arrivée via ?compteId=
    const q = this.route.snapshot.queryParamMap.get('compteId');
    if (q && !this.compteIdSel()) {
      const id = Number(q);
      this.forceCompte.set(true);
      this.compteIdSel.set(id);
      this.refresh(id);
    }
  }

  // en mode forcé, on ignore le sélecteur
  onSelectCompte(id: string | null) {
    if (this.forceCompte()) return;
    if (!id) { this.compteIdSel.set(null); this.demandes = []; return; }
    const num = Number(id);
    this.compteIdSel.set(num);
    this.refresh(num);
  }

  refresh(compteId: number) {
    this.loading = true;
    this.api.getDemandesByCompte(compteId).subscribe({
      next: d => { this.demandes = d; this.loading = false; },
      error: () => { this.errorMsg.set('Chargement des demandes échoué.'); this.loading = false; }
    });
  }

  openModal() { this.form.reset({ pages: 25 }); this.showModal.set(true); }
  closeModal() { this.showModal.set(false); }

  submit() {
    if (!this.compteIdSel() || this.form.invalid) return;
    const payload = {
      compteId: this.compteIdSel()!,
      dateDemande: this.form.value.dateDemande!,
      pages: Number(this.form.value.pages),
      motif: this.form.value.motif || undefined
    };
    this.api.creerDemande(payload).subscribe({
      next: () => { this.closeModal(); this.refresh(this.compteIdSel()!); },
      error: () => this.errorMsg.set('Création impossible. Réessayez.')
    });
  }

  annuler(d: DemandeChequier) {
    this.api.annulerDemande(d.id).subscribe({
      next: () => this.refresh(this.compteIdSel()!)
    });
  }

  supprimer(d: DemandeChequier) {
    if (!confirm('Supprimer cette demande ?')) return;
    this.api.supprimerDemande(d.id).subscribe({
      next: () => this.refresh(this.compteIdSel()!)
    });
  }

  badgeClass(s: string) {
    switch (s) {
      case 'EN_ATTENTE': return 'badge gray';
      case 'APPROUVEE': return 'badge green';
      case 'REJETEE':   return 'badge red';
      case 'ANNULEE':   return 'badge dark';
      default: return 'badge';
    }
  }
}
