// src/app/pages/register/register.ts
import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators
} from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs/operators';
import { AuthService } from '../../core/auth.service';

// ✅ Interface mise à jour pour le payload d'inscription
export interface RegisterPayload {
  role: string;
  nom: string;
  prenom: string;
  email: string;
  password: string;
  numCin: string;
  numCompteBancaire?: string; // ✅ Optionnel maintenant
}

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './register.html',
  styleUrls: ['./register.css']
})
export class RegisterComponent {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);

  year = new Date().getFullYear();
  hide = true;
  hideConfirm = true;
  loading = false;
  serverError = '';

  // ✅ Regex pour mot de passe fort
  private readonly passwordPattern =
    /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^\da-zA-Z]).{8,}$/;

  // ✅ Regex pour CIN (8 chiffres exactement)
  private readonly cinPattern = /^[0-9]{8}$/;

  // ✅ Regex pour numéro de compte bancaire (10-20 chiffres)
  private readonly comptePattern = /^[0-9]{10,20}$/;

  form = this.fb.group(
    {
      role: ['', Validators.required],
      nom: ['', [Validators.required, Validators.minLength(2)]],
      prenom: ['', [Validators.required, Validators.minLength(2)]],
      numCin: ['', [Validators.required, Validators.pattern(this.cinPattern)]],
      numCompteBancaire: [''], // ✅ Pas de validators par défaut, sera géré dynamiquement
      email: ['', [Validators.required, Validators.email]],
      password: [
        '',
        [
          Validators.required,
          Validators.minLength(8),
          Validators.pattern(this.passwordPattern)
        ]
      ],
      confirmPassword: ['', [Validators.required]]
    },
    { validators: [RegisterComponent.passwordsMatch] }
  );

  // ✅ Getters pour tous les champs
  get role() { return this.form.get('role'); }
  get nom() { return this.form.get('nom'); }
  get prenom() { return this.form.get('prenom'); }
  get numCin() { return this.form.get('numCin'); }
  get numCompteBancaire() { return this.form.get('numCompteBancaire'); }
  get email() { return this.form.get('email'); }
  get password() { return this.form.get('password'); }
  get confirmPassword() { return this.form.get('confirmPassword'); }

  constructor() {
    // ✅ Observer les changements de rôle pour ajuster la validation
    this.role?.valueChanges.subscribe(roleValue => {
      this.updateAccountNumberValidation(roleValue || '');
    });
  }

  // ✅ Méthode pour ajuster la validation du compte bancaire selon le rôle
  private updateAccountNumberValidation(role: string) {
    const accountControl = this.numCompteBancaire;
    
    if (role === 'CLIENT') {
      // Pour les clients : champ requis avec pattern
      accountControl?.setValidators([
        Validators.required, 
        Validators.pattern(this.comptePattern)
      ]);
    } else {
      // Pour les agents : pas de validation (champ optionnel et caché)
      accountControl?.clearValidators();
      accountControl?.setValue(''); // Vider le champ
    }
    
    // Mettre à jour la validation
    accountControl?.updateValueAndValidity();
  }

  // ✅ Validator statique pour la correspondance des mots de passe
  private static passwordsMatch(group: AbstractControl): ValidationErrors | null {
    const password = group.get('password')?.value;
    const confirmPassword = group.get('confirmPassword')?.value;
    
    if (password && confirmPassword && password !== confirmPassword) {
      return { passwordMismatch: true };
    }
    return null;
  }

  submit() {
    console.log('🚀 Soumission du formulaire d\'inscription');
    
    this.serverError = '';
    this.form.markAllAsTouched();
    this.form.updateValueAndValidity({ onlySelf: false, emitEvent: true });
    
    if (this.form.invalid) {
      console.log('❌ Formulaire invalide:', this.form.errors);
      console.log('❌ Erreurs par champ:', {
        role: this.role?.errors,
        nom: this.nom?.errors,
        prenom: this.prenom?.errors,
        numCin: this.numCin?.errors,
        numCompteBancaire: this.numCompteBancaire?.errors,
        email: this.email?.errors,
        password: this.password?.errors,
        confirmPassword: this.confirmPassword?.errors
      });
      return;
    }

    // ✅ CORRECTION PRINCIPALE: Préparer les données selon le rôle
    const { confirmPassword, ...formData } = this.form.value;
    
    // ✅ Créer le payload final en excluant numCompteBancaire pour les agents
    let registerData: any = {
      role: formData.role,
      nom: formData.nom,
      prenom: formData.prenom,
      numCin: formData.numCin,
      email: formData.email,
      password: formData.password
    };
    
    // ✅ N'ajouter numCompteBancaire QUE pour les clients
    if (formData.role === 'CLIENT' && formData.numCompteBancaire) {
      registerData.numCompteBancaire = formData.numCompteBancaire;
    }
    
    console.log('📤 Données à envoyer:', { 
      ...registerData, 
      password: '***',
      numCompteBancaire: registerData.numCompteBancaire ? registerData.numCompteBancaire.substring(0, 4) + '***' : undefined
    });

    this.loading = true;
    this.auth.register(registerData)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (response) => {
          console.log('✅ Inscription réussie:', response);
          
          // ✅ Gestion sécurisée de la réponse
          if (response.token && response.user) {
            // Si le backend retourne token + user, l'utilisateur est connecté automatiquement
            console.log('✅ Connexion automatique après inscription');
            this.redirectByRole(response.user.role);
          } else {
            // Sinon, rediriger vers login avec message de succès
            console.log('✅ Inscription réussie, redirection vers login');
            this.router.navigate(['/login'], {
              queryParams: { message: 'Inscription réussie ! Veuillez vous connecter.' }
            });
          }
        },
        error: (err: HttpErrorResponse) => {
          console.error('❌ Erreur inscription:', err);
          
          // ✅ Gestion d'erreurs plus précise
          if (err.error?.message) {
            this.serverError = err.error.message;
          } else if (err.error?.error) {
            this.serverError = err.error.error;
          } else {
            switch (err.status) {
              case 400:
                // Gestion spécifique pour les nouveaux champs
                if (err.error?.error?.includes('CIN')) {
                  this.serverError = 'Ce numéro CIN est déjà utilisé.';
                } else if (err.error?.error?.includes('compte')) {
                  this.serverError = 'Ce numéro de compte bancaire est déjà utilisé.';
                } else {
                  this.serverError = 'Données invalides. Vérifiez vos informations.';
                }
                break;
              case 409:
                this.serverError = 'Un compte avec ces informations existe déjà.';
                break;
              case 422:
                this.serverError = 'Données de formulaire invalides.';
                break;
              case 500:
                this.serverError = 'Erreur serveur. Veuillez réessayer plus tard.';
                break;
              default:
                this.serverError = 'Inscription impossible. Veuillez réessayer.';
            }
          }
        }
      });
  }

  // ✅ Redirection selon le rôle
  private redirectByRole(role: string) {
    switch (role.toUpperCase()) {
       case 'ADMIN':
         this.router.navigate(['/login']);
         break;
      case 'AGENT':
        this.router.navigate(['/login']);
        break;
      case 'CLIENT':
      default:
        this.router.navigate(['/login']);
        break;
    }
  }
} 