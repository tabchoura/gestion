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

  form = this.fb.group(
    {
      role: ['', Validators.required],
      nom: ['', [Validators.required, Validators.minLength(2)]],
      prenom: ['', [Validators.required, Validators.minLength(2)]],
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

  // ✅ Getters pour les champs
  get role() { return this.form.get('role'); }
  get nom() { return this.form.get('nom'); }
  get prenom() { return this.form.get('prenom'); }
  get email() { return this.form.get('email'); }
  get password() { return this.form.get('password'); }
  get confirmPassword() { return this.form.get('confirmPassword'); }

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
        email: this.email?.errors,
        password: this.password?.errors,
        confirmPassword: this.confirmPassword?.errors
      });
      return;
    }

    // ✅ CORRECTION: Enlever confirmPassword avant l'envoi
    const { confirmPassword, ...registerData } = this.form.value;
    console.log('📤 Données à envoyer:', { ...registerData, password: '***' });

    this.loading = true;
    this.auth.register(registerData as any)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (response) => {
          console.log('✅ Inscription réussie:', response);
          
          // ✅ CORRECTION: Gestion sécurisée de la réponse
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
          
          // ✅ AMÉLIORATION: Gestion d'erreurs plus précise
          if (err.error?.message) {
            this.serverError = err.error.message;
          } else if (err.error?.error) {
            this.serverError = err.error.error;
          } else {
            switch (err.status) {
              case 400:
                this.serverError = 'Données invalides. Vérifiez vos informations.';
                break;
              case 409:
                this.serverError = 'Un compte avec cet email existe déjà.';
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

  // ✅ AJOUT: Méthode pour rediriger selon le rôle
  private redirectByRole(role: string) {
    switch (role.toUpperCase()) {
      case 'ADMIN':
        this.router.navigate(['/admin/dashboard']);
        break;
      case 'AGENT':
        this.router.navigate(['/agent/dashboard']);
        break;
      case 'CLIENT':
      default:
        this.router.navigate(['/profile']);
        break;
    }
  }
}