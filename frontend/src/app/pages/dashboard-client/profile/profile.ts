import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/auth.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './profile.html',
  styleUrls: ['./profile.css']
})
export class ProfileComponent implements OnInit {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);

  editMode = signal(false);
  loading  = signal(false);
  msg      = signal('');
  err      = signal('');

  private previousEmail = '';
  private previousCin = '';
  private previousCompteBancaire = '';

  // ✅ Regex pour validation
  private readonly cinPattern = /^[0-9]{8}$/;
  private readonly comptePattern = /^[0-9]{10,20}$/;

  form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    nom: ['', [Validators.required, Validators.minLength(2)]],
    prenom: ['', [Validators.required, Validators.minLength(2)]],
    numCin: ['', [Validators.required, Validators.pattern(this.cinPattern)]],
    numCompteBancaire: ['', [Validators.required, Validators.pattern(this.comptePattern)]]
  });

  // ✅ Getters pour la validation
  get email() { return this.form.get('email'); }
  get nom() { return this.form.get('nom'); }
  get prenom() { return this.form.get('prenom'); }
  get numCin() { return this.form.get('numCin'); }
  get numCompteBancaire() { return this.form.get('numCompteBancaire'); }

  ngOnInit() { this.load(); }

  toggleEdit() { 
    if (this.editMode()) {
      // Annuler : restaurer les valeurs précédentes
      this.form.patchValue({
        email: this.previousEmail,
        nom: this.form.get('nom')?.value,
        prenom: this.form.get('prenom')?.value,
        numCin: this.previousCin,
        numCompteBancaire: this.previousCompteBancaire
      });
      this.form.markAsUntouched();
      this.err.set('');
      this.msg.set('');
    }
    this.editMode.update(v => !v); 
  }

  private extractUser(res: any) {
    return (res?.user ?? res) as { 
      id: number; 
      nom: string; 
      prenom: string; 
      email: string; 
      role: string;
      numCin: string;
      numCompteBancaire: string;
    };
  }

  load() {
    this.loading.set(true);
    this.err.set('');
    this.msg.set('');
    
    this.auth.me().subscribe({
      next: (res) => {
        const u = this.extractUser(res);
        
        // Stocker les valeurs précédentes
        this.previousEmail = u.email;
        this.previousCin = u.numCin;
        this.previousCompteBancaire = u.numCompteBancaire;
        
        // Remplir le formulaire
        this.form.patchValue({ 
          email: u.email, 
          nom: u.nom, 
          prenom: u.prenom,
          numCin: u.numCin,
          numCompteBancaire: u.numCompteBancaire
        });
        
        this.loading.set(false);
      },
      error: (e) => {
        this.loading.set(false);
        if (e?.status === 401) {
          this.err.set('Session non valide pour /users/me. Vous pouvez vous reconnecter.');
        } else {
          this.err.set('Erreur lors du chargement du profil.');
        }
      }
    });
  }

  save() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.err.set('Veuillez corriger les erreurs dans le formulaire.');
      return;
    }

    this.loading.set(true);
    this.msg.set(''); 
    this.err.set('');

    const formData = this.form.value;
    const emailChanged = formData.email !== this.previousEmail;
    const cinChanged = formData.numCin !== this.previousCin;
    const compteChanged = formData.numCompteBancaire !== this.previousCompteBancaire;

    this.auth.updateProfile(formData as any).subscribe({
      next: () => {
        this.loading.set(false);
        this.toggleEdit(); // Sortir du mode édition
        
        if (emailChanged || cinChanged || compteChanged) {
          let message = 'Profil mis à jour.';
          if (emailChanged) {
            message += ' Veuillez vous reconnecter suite au changement d\'email.';
          } else if (cinChanged || compteChanged) {
            message += ' Les informations sensibles ont été modifiées.';
          }
          this.msg.set(message);
          
          // Recharger les données pour mettre à jour les valeurs précédentes
          setTimeout(() => this.load(), 1000);
        } else {
          this.msg.set('Profil mis à jour.');
          this.load(); // Recharger immédiatement
        }
      },
      error: (e) => {
        this.loading.set(false);
        
        // Gestion d'erreurs spécifiques
        if (e?.error?.message) {
          this.err.set(e.error.message);
        } else if (e?.error?.error) {
          // Gestion des erreurs d'unicité
          if (e.error.error.includes('CIN')) {
            this.err.set('Ce numéro CIN est déjà utilisé par un autre utilisateur.');
          } else if (e.error.error.includes('compte')) {
            this.err.set('Ce numéro de compte bancaire est déjà utilisé par un autre utilisateur.');
          } else if (e.error.error.includes('Email')) {
            this.err.set('Cette adresse email est déjà utilisée par un autre utilisateur.');
          } else {
            this.err.set(e.error.error);
          }
        } else {
          switch (e?.status) {
            case 400:
              this.err.set('Données invalides. Vérifiez vos informations.');
              break;
            case 409:
              this.err.set('Conflit : ces informations sont déjà utilisées.');
              break;
            case 422:
              this.err.set('Données de formulaire invalides.');
              break;
            default:
              this.err.set('Mise à jour impossible. Veuillez réessayer.');
          }
        }
      }
    });
  }

  // Bouton dans le template pour forcer la reconnexion propre
  reconnect() {
    this.auth.logout();
    this.router.navigateByUrl('/login');
  }
}