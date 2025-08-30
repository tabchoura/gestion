import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/auth.service';
import { ToastrService } from 'ngx-toastr';

type ProfileForm = {
  email: string;
  nom: string;
  prenom: string;
  numCin: string;
};

@Component({
  selector: 'app-profileagent',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './profile-agent.html',
  styleUrls: ['./profile-agent.css']
})
export class ProfileComponent implements OnInit {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);
  private toastr = inject(ToastrService);

  editMode = signal(false);
  loading  = signal(false);
  msg      = signal('');
  err      = signal('');

  private previousEmail = '';
  private original: ProfileForm | null = null;

  private readonly labels: Record<keyof ProfileForm, string> = {
    email: 'Email',
    nom: 'Nom',
    prenom: 'Prénom',
    numCin: 'CIN',
  };

  form = this.fb.group({
    email:  ['', [Validators.required, Validators.email]],
    nom:    ['', [Validators.required, Validators.minLength(2)]],
    prenom: ['', [Validators.required, Validators.minLength(2)]],
    numCin: ['', [Validators.required, Validators.pattern(/^[0-9]{8}$/)]]
  });

  ngOnInit() { this.load(); }

  toggleEdit() {
    const newVal = !this.editMode();
    this.editMode.set(newVal);
    if (!newVal && this.original) {
      this.form.patchValue(this.original);
      this.form.markAsPristine();
      this.form.updateValueAndValidity({ onlySelf: false, emitEvent: false });
    }
  }

  get email()  { return this.form.get('email'); }
  get nom()    { return this.form.get('nom'); }
  get prenom() { return this.form.get('prenom'); }
  get numCin() { return this.form.get('numCin'); }

  private extractUser(res: any) {
    return (res?.user ?? res) as {
      id: number;
      nom: string;
      prenom: string;
      email: string;
      role: string;
      numCin: string;
    };
  }

  private snapshot(patch: ProfileForm) {
    this.original = { ...patch };
    this.form.markAsPristine();
    this.form.updateValueAndValidity({ onlySelf: false, emitEvent: false });
  }

  load() {
    this.loading.set(true);
    this.err.set('');
    this.auth.me().subscribe({
      next: (res) => {
        const u = this.extractUser(res);
        this.previousEmail = u.email;
        const patch: ProfileForm = {
          email: u.email,
          nom: u.nom,
          prenom: u.prenom,
          numCin: u.numCin,
        };
        this.form.patchValue(patch);
        this.snapshot(patch);
        this.loading.set(false);
      },
      error: (e) => {
        this.loading.set(false);
        if (e?.status === 401) {
          this.err.set('Session non valide pour /users/me. Vous pouvez vous reconnecter.');
          this.toastr.warning('Session expirée. Veuillez vous reconnecter.');
        } else {
          this.err.set('Erreur lors du chargement du profil.');
          this.toastr.error('Erreur lors du chargement du profil.');
        }
      }
    });
  }

  private getChangedKeys(current: ProfileForm): (keyof ProfileForm)[] {
    const base = this.original ?? current;
    const keys = Object.keys(current) as (keyof ProfileForm)[];
    return keys.filter(k => (current[k] ?? '') !== (base[k] ?? ''));
  }

  save() {
    if (this.form.invalid) {
      this.toastr.error('Formulaire invalide. Vérifiez les champs.');
      this.form.markAllAsTouched();
      return;
    }

    const current = this.form.getRawValue() as ProfileForm;
    const base = this.original ?? current;
    const changed = this.getChangedKeys(current);

    if (changed.length === 0) {
      this.toastr.info('Aucune modification détectée.');
      return;
    }

    this.loading.set(true);
    this.msg.set(''); this.err.set('');

    const emailChanged = current.email !== this.previousEmail;
//put
    this.auth.updateProfile(current as any).subscribe({
      next: () => {
        this.loading.set(false);
        // this.msg.set('Profil mis à jour.');
        this.editMode.set(false);

        changed.forEach((k) => {
          const before = (base[k] ?? '').toString();
          const after  = (current[k] ?? '').toString();
          const valueLine = k === 'email'
            ? `${before} → ${after}`
            : `${before || '—'} → ${after || '—'}`;
          this.toastr.success(`le profil a été  mis à jour avec succèes`);
        });

        if (emailChanged) {
          this.toastr.warning(
            'Votre email a changé. Veuillez vous reconnecter.',
            'Reconnectez-vous'
          );
          this.msg.set('Profil mis à jour. Veuillez vous reconnecter suite au changement d\'email.');
        } else {
          this.load();
        }
      },
      error: (e) => {
        this.loading.set(false);
        const message = e?.error?.message || 'Mise à jour impossible.';
        this.err.set(message);
        this.toastr.error(message, 'Erreur');
      }
    });
  }

  reconnect() {
    this.auth.logout();
    this.router.navigateByUrl('/login');
  }
}
