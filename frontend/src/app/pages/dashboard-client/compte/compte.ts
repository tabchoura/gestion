import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';   // ✅ import Toastr
import { ComptesService, CompteBancaire } from '../../../core/comptes.service';

function ribOptionalValidator(ctrl: AbstractControl): ValidationErrors | null {
  const v = (ctrl.value ?? '').toString().trim();
  if (!v) return null;
  return /^\d{20}$/.test(v) ? null : { ribInvalid: true };
}

@Component({
  selector: 'app-comptes',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './compte.html',
  styleUrls: ['./compte.css']
})
export class ComptesComponent implements OnInit {
  private fb = inject(FormBuilder);
  private api = inject(ComptesService);
  private router = inject(Router);
  private toastr = inject(ToastrService);   // ✅ inject toastr

  devises = ['TND', 'EUR', 'USD', 'GBP'];

  comptes = signal<CompteBancaire[]>([]);
  loading = signal(true);
  saving = signal(false);
  adding = signal(false);
  editing = signal<CompteBancaire | null>(null);

  isAdding = computed(() => this.adding());
  isEditing = computed(() => this.editing() !== null);

  form = this.fb.group({
    typeCompte: ['', Validators.required], 
    numeroCompte: ['', Validators.required],
    rib: ['', ribOptionalValidator],
    devise: ['TND', Validators.required],
  });

  ngOnInit() { this.reload(); }

  voirChequier(c: { id?: number }, ev?: Event) {
    ev?.preventDefault();
    ev?.stopPropagation();
    if (!c?.id) return;
    this.router.navigate(['/dashboardclient/demandes', c.id]);
  }

  getTypeCompteLabel(typeCompte: string): string {
    const labels: { [key: string]: string } = {
      'compte-epargne': 'Compte épargne',
      'compte-courant': 'Compte courant',
      'compte-devises': 'Compte devises',
      'compte-specifique': 'Compte spécifique ou investissement',
      'compte-joint': 'Compte joint',
      'compte-professionnel': 'Compte professionnel / entreprise'
    };
    return labels[typeCompte] || typeCompte;
  }

  comptesFn() { return this.comptes(); }
  loadingFn() { return this.loading(); }
  savingFn() { return this.saving(); }

  startAdd() { 
    this.editing.set(null); 
    this.adding.set(true); 
    this.form.reset({ devise: 'TND' }); 
  }
  
  cancelAdd() { 
    this.adding.set(false); 
    this.form.reset({ devise: 'TND' }); 
  }

  startEdit(c: CompteBancaire) {
    this.adding.set(false);
    this.editing.set(c);
    this.form.reset({
      typeCompte: c.typeCompte, 
      numeroCompte: c.numeroCompte,
      rib: c.rib || '',
      devise: c.devise || 'TND'
    });
  }
  
  cancelEdit() { 
    this.editing.set(null); 
    this.form.reset({ devise: 'TND' }); 
  }

  submit() { 
    this.isEditing() ? this.submitEdit() : this.submitAdd(); 
  }

  submitAdd() {
    if (this.form.invalid) return;
    this.saving.set(true);
    const body: CompteBancaire = { ...(this.form.value as any), isDefault: false };
    this.api.create(body).subscribe({
      next: (c) => { 
        this.comptes.update(list => [c, ...list]); 
        this.cancelAdd(); 
        this.toastr.success('Compte ajouté avec succès ✅'); // ✅ toast success
      },
      error: (err) => {
        console.error(err);
        this.toastr.error('Erreur lors de l’ajout du compte ❌');
      },
      complete: () => this.saving.set(false)
    });
  }

  submitEdit() {
    if (this.form.invalid || !this.editing()) return;
    this.saving.set(true);
    const id = this.editing()!.id!;
    this.api.update(id, this.form.value as any).subscribe({
      next: (c) => { 
        this.comptes.update(list => list.map(x => x.id === id ? c : x)); 
        this.cancelEdit(); 
        this.toastr.success('Compte mis à jour avec succès ✏️'); // ✅ toast success
      },
      error: (err) => {
        console.error(err);
        this.toastr.error('Erreur lors de la mise à jour ❌');
      },
      complete: () => this.saving.set(false)
    });
  }

  setDefault(c: CompteBancaire) {
    if (!c.id) return;
    this.api.setDefault(c.id).subscribe({
      next: (updated) => {
        this.comptes.update(list => list.map(x => ({...x, isDefault: x.id === updated.id})));
        this.toastr.info('Compte défini comme par défaut ⭐'); // ✅ toast info
      },
      error: () => this.toastr.error('Impossible de définir ce compte par défaut ❌')
    });
  }

  delete(c: CompteBancaire) {
    if (!c.id) return;
    if (!confirm('Supprimer ce compte ?')) return;
    this.api.remove(c.id).subscribe({
      next: () => {
        this.comptes.update(list => list.filter(x => x.id !== c.id));
        this.toastr.warning('Compte supprimé 🗑️'); // ✅ toast warning
      },
      error: () => this.toastr.error('Erreur lors de la suppression ❌')
    });
  }

  formatRib(r?: string | null) {
    const s = (r ?? '').replace(/\s+/g, '');
    return s ? s.replace(/(\d{4})(?=\d)/g, '$1 ').trim() : '';
  }

  private reload() {
    this.loading.set(true);
    this.api.list().subscribe({
      next: (rows) => this.comptes.set(rows),
      error: (err) => { console.error(err); this.toastr.error('Erreur lors du chargement ❌'); },
      complete: () => this.loading.set(false)
    });
  }
}
