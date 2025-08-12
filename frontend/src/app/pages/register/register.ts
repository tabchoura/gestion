import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs/operators';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule], // 👈 RouterLink retiré si non utilisé
  templateUrl: './register.html',
  styleUrls: ['./register.css']
})
export class RegisterComponent {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);

  year = new Date().getFullYear();
  hide = true;
  loading = false;
  serverError = '';

  form = this.fb.group({
    role: ['', Validators.required],
    nom: ['', [Validators.required, Validators.minLength(2)]],
    prenom: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
  });

  get role()    { return this.form.get('role'); }
  get nom()     { return this.form.get('nom'); }
  get prenom()  { return this.form.get('prenom'); }
  get email()   { return this.form.get('email'); }
  get password(){ return this.form.get('password'); }

  submit() {
    this.serverError = '';
    this.form.markAllAsTouched();
    if (this.form.invalid) return;

    this.loading = true;
    this.auth.register(this.form.value as any)
      .pipe(finalize(() => this.loading = false))
      .subscribe({
        next: () => this.router.navigateByUrl('/profile'),
        error: (err: HttpErrorResponse) => {
          this.serverError =
            err?.error?.error ||
            err?.error?.message ||
            (err.status === 400 ? 'Email déjà utilisé' : 'Inscription impossible');
        }
      });
  }
}
