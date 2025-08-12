import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs/operators';
import { environment } from '../../environments/environment.development';

export interface User {
  id: number;
  nom: string;
  prenom: string;
  email: string;
  role: string;
}

export interface LoginPayload { email: string; password: string; remember?: boolean; }
export interface LoginResponse { status: number; token?: string; user?: User; role?: string; message: string; }
export interface RegisterPayload { role: string; nom: string; prenom: string; email: string; password: string; }
export interface RegisterResponse { status: number; message: string; token?: string; user?: User; }

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private api = environment.apiUrl;

  register(payload: RegisterPayload) {
    return this.http.post<RegisterResponse>(`${this.api}/auth/register`, payload).pipe(
      tap(res => {
        if (res?.token) this.persistToken(res.token);
        const role = res?.user?.role || (res as any)?.role;
        if (role) this.persistRole(role);
      })
    );
  }

  login(payload: LoginPayload) {
    return this.http.post<LoginResponse>(`${this.api}/auth/login`, payload).pipe(
      tap(res => {
        if (res?.token) this.persistToken(res.token, !!payload.remember);
        const role = res?.user?.role || res?.role;
        if (role) this.persistRole(role, !!payload.remember);
      })
    );
  }

me() {
  return this.http.get<any>(`${this.api}/users/me`);
}
updateProfile(payload: { nom: string; prenom: string; email: string }) {
  return this.http.put<any>(`${this.api}/users/profile`, payload);
}

  changePassword(payload: { currentPassword: string; newPassword: string }) {
    return this.http.put(`${this.api}/auth/password`, payload);
  }

  logout() {
    localStorage.removeItem('token'); sessionStorage.removeItem('token');
    localStorage.removeItem('role');  sessionStorage.removeItem('role');
  }

  getRole() {
    return localStorage.getItem('role') || sessionStorage.getItem('role');
  }

  private persistToken(token: string, remember = false) {
    if (remember) localStorage.setItem('token', token);
    else sessionStorage.setItem('token', token);
  }

  private persistRole(role: string, remember = false) {
    if (remember) localStorage.setItem('role', role);
    else sessionStorage.setItem('role', role);
  }
}
