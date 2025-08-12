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

  form = this.fb.group({
    email:  ['', [Validators.required, Validators.email]],
    nom:    ['', [Validators.required, Validators.minLength(2)]],
    prenom: ['', [Validators.required, Validators.minLength(2)]],
  });

  ngOnInit() { this.load(); }
  toggleEdit() { this.editMode.update(v => !v); }

  private extractUser(res: any) {
    return (res?.user ?? res) as { id: number; nom: string; prenom: string; email: string; role: string; };
  }

  load() {
    this.loading.set(true);
    this.err.set('');
    this.auth.me().subscribe({
      next: (res) => {
        const u = this.extractUser(res);
        this.previousEmail = u.email;
        this.form.patchValue({ email: u.email, nom: u.nom, prenom: u.prenom });
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.err.set('Session expirée. Veuillez vous reconnecter.');
        this.auth.logout();
        this.router.navigateByUrl('/login');
      }
    });
  }

  save() {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.msg.set(''); this.err.set('');

    const emailChanged = this.form.value.email !== this.previousEmail;

    this.auth.updateProfile(this.form.value as any).subscribe({
      next: () => {
        this.loading.set(false);
        this.msg.set('Profil mis à jour.');
        this.toggleEdit();
        if (emailChanged) {
          this.msg.set('Profil mis à jour. Veuillez vous reconnecter suite au changement d’email.');
          setTimeout(() => { this.auth.logout(); this.router.navigateByUrl('/login'); }, 800);
        } else {
          this.load();
        }
      },
      error: (e) => {
        this.loading.set(false);
        this.err.set(e?.error?.message || 'Mise à jour impossible.');
      }
    });
  }
}
