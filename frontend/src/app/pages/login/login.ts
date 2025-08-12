import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { AuthService } from '../../core/auth.service';

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
    const token = localStorage.getItem('token') || sessionStorage.getItem('token');
    if (token) {
      const role = this.auth.getRole();
      if (role === 'CLIENT') this.router.navigateByUrl('/dashboardclient/profile');
      else if (role === 'AGENT') this.router.navigateByUrl('/dashboardagent');
    }
  }

  onSubmit() {
    this.serverError = '';
    if (this.form.invalid) return;

    this.loading = true;
    this.auth.login(this.form.value as any)
      .pipe(finalize(() => this.loading = false))
      .subscribe({
        next: (res) => {
          const role = res?.user?.role || res?.role || this.auth.getRole();
          if (role === 'CLIENT') this.router.navigateByUrl('/dashboardclient/profile');
          else if (role === 'AGENT') this.router.navigateByUrl('/dashboardagent');
          else this.router.navigateByUrl('/');
        },
        error: (err) => {
          this.serverError = err?.error?.message || 'Identifiants invalides';
        }
      });
  }
}
