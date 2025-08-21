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
import { ToastrService } from 'ngx-toastr';
export interface RegisterPayload {
  role: string;
  nom: string;
  prenom: string;
  email: string;
  password: string;
  numCin: string;
}

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './register.html',
  styleUrls: ['./register.css']
})
export class RegisterComponent {
    private toastr = inject(ToastrService);

  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);

  year = new Date().getFullYear();
  hide = true;
  hideConfirm = true;
  loading = false;
  serverError = '';

  // Regex pour mot de passe fort
  private readonly passwordPattern =
    /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^\da-zA-Z]).{8,}$/;

  // Regex pour CIN (exactement 8 chiffres)
  private readonly cinPattern = /^[0-9]{8}$/;

  form = this.fb.group(
    {
      role: ['', Validators.required],
      nom: ['', [Validators.required, Validators.minLength(2)]],
      prenom: ['', [Validators.required, Validators.minLength(2)]],
      numCin: ['', [Validators.required, Validators.pattern(this.cinPattern)]],
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

  // Getters
  get role() { return this.form.get('role'); }
  get nom() { return this.form.get('nom'); }
  get prenom() { return this.form.get('prenom'); }
  get numCin() { return this.form.get('numCin'); }
  get email() { return this.form.get('email'); }
  get password() { return this.form.get('password'); }
  get confirmPassword() { return this.form.get('confirmPassword'); }

 

  // Validator pour correspondance des mots de passe
  private static passwordsMatch(group: AbstractControl): ValidationErrors | null {
    const password = group.get('password')?.value;
    const confirmPassword = group.get('confirmPassword')?.value;
    
    if (password && confirmPassword && password !== confirmPassword) {
      return { passwordMismatch: true };
    }
    return null;
  }

  onCinInput(event: any) {
    const input = event.target;
    const value = input.value;
    const numericValue = value.replace(/[^0-9]/g, '');
    const limitedValue = numericValue.substring(0, 8);
    
    if (value !== limitedValue) {
      input.value = limitedValue;
      this.form.get('numCin')?.setValue(limitedValue);
    }
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
        email: this.email?.errors,
        password: this.password?.errors,
        confirmPassword: this.confirmPassword?.errors
      });
      return;
    }

    const { confirmPassword, ...formData } = this.form.value;
    
    const registerData: RegisterPayload = {
      role: formData.role!,
      nom: formData.nom!,
      prenom: formData.prenom!,
      numCin: formData.numCin!,
      email: formData.email!,
      password: formData.password!
    };
    
    console.log('📤 Données à envoyer:', { 
      ...registerData, 
      password: '***'
    });

    this.loading = true;
    this.auth.register(registerData)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (response) => {
                    this.toastr.success('Inscription réussie 🎉', 'Succès');

          console.log('✅ Inscription réussie:', response);
          
          if (response.token && response.user) {
            console.log('✅ Connexion automatique après inscription');
            this.redirectByRole(response.user.role);
          } else {
            console.log('✅ Inscription réussie, redirection vers login');
            this.router.navigate(['/login'], {
              queryParams: { message: 'Inscription réussie ! Veuillez vous connecter.' }
            });
          }
        },
        error: (err: HttpErrorResponse) => {
          console.error('❌ Erreur inscription:', err);
                    this.toastr.error(this.serverError, 'Erreur');

          console.error('❌ Détails de l\'erreur:', {
            status: err.status,
            statusText: err.statusText,
            url: err.url,
            error: err.error,
            message: err.message
          });
          
          if (err.error?.message) {
            this.serverError = err.error.message;
          } else if (err.error?.error) {
            this.serverError = err.error.error;
          } else if (err.error?.details) {
            this.serverError = Array.isArray(err.error.details) 
              ? err.error.details.join(', ')
              : err.error.details;
          } else {
            switch (err.status) {
              case 400:
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