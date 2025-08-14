import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { AuthService, type LoginRequest, type LoginResponse } from '../../core/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrls: ['./login.css']
})
export class LoginComponent implements OnInit {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);

  year = new Date().getFullYear();
  loading = false;
  serverError = '';

  form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
    remember: [true]
  });

  ngOnInit() {
    if (this.auth.isAuthenticated()) {
      const role = this.normalizeRole(this.auth.getRole());
      this.redirectByRole(role);
    }
  }

  onSubmit() {
    this.serverError = '';
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }

    const v = this.form.value;

    // 🔧 normaliser l'email (évite les espaces/majuscules)
    const email = (v.email ?? '').toString().normalize('NFKC').trim().toLowerCase();

    const payload: LoginRequest = {
      email,
      password: v.password!
    };

    this.loading = true;
    this.auth.login(payload)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (res: LoginResponse) => {
const rawRole = res.user?.role ?? res.role ?? null;
          const role = this.normalizeRole(rawRole);

          // Conserver le rôle normalisé (utile pour les guards)
          if (role) localStorage.setItem('role', role);

          // "remember": bascule token/user vers sessionStorage si décoché
          if (!v.remember) {
            const token = localStorage.getItem('token');
            const user  = localStorage.getItem('user');
            if (token && user) {
              sessionStorage.setItem('token', token);
              sessionStorage.setItem('user',  user);
              localStorage.removeItem('token');
              localStorage.removeItem('user');
            }
          }

          this.redirectByRole(role);
        },
        error: (err) => {
          let msg = 'Erreur de connexion';
          if (err?.status === 401)       msg = 'Email ou mot de passe incorrect';
          else if (err?.status === 403)  msg = 'Accès refusé';
          else if (err?.status === 500)  msg = 'Erreur serveur. Réessayez plus tard';
          else if (err?.error?.message)  msg = err.error.message;
          this.serverError = msg;
        }
      });
  }

  // ✅ Normalise le rôle renvoyé par le back
  private normalizeRole(role: string | null | undefined): 'CLIENT' | 'AGENT' | 'ADMIN' | null {
    if (!role) return null;
    const r = role.toString().trim().toUpperCase().replace(/^ROLE_/, '');
    if (r === 'CLIENT' || r === 'AGENT' || r === 'ADMIN') return r as any;
    return null;
  }

  // ✅ Route mapping centralisé
  private redirectByRole(role: 'CLIENT' | 'AGENT' | 'ADMIN' | null) {
    const map: Record<'CLIENT' | 'AGENT' | 'ADMIN', string> = {
      CLIENT: '/dashboardclient/profile',
      AGENT:  '/dashboardagent',
      ADMIN:  '/dashboardadmin'
    };

    const target = role ? map[role] : null;

    if (target) {
      this.router.navigateByUrl(target);
    } else {
      // Si le rôle ne matche pas, tente un fallback utile (ex: profil client)
      // ou reste sur login. Ici on reste sur l'accueil par défaut si rien d'autre.
      this.router.navigateByUrl('/');
    }
  }

  // Helpers template
  get emailErrors() {
    const c = this.form.get('email');
    if (c?.errors && c.touched) {
      if (c.errors['required']) return 'Email requis';
      if (c.errors['email'])    return 'Format email invalide';
    }
    return null;
  }

  get passwordErrors() {
    const c = this.form.get('password');
    if (c?.errors && c.touched) {
      if (c.errors['required'])  return 'Mot de passe requis';
      if (c.errors['minlength']) return 'Minimum 6 caractères';
    }
    return null;
  }
}
