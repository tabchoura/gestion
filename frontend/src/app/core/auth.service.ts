import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, BehaviorSubject, throwError } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment.development';

// ===== Types =====
export interface User {
  id: number;
  nom: string;
  prenom: string;
  email: string;
  role: string;
}

export interface LoginRequest {
  email: string;
  password: string;
  remember?: boolean;
}

export interface LoginResponse {
  status: number;
  token?: string;
  user?: User;
  role?: string;
  message: string;
}

export interface RegisterPayload {
  role: string;
  nom: string;
  prenom: string;
  email: string;
  password: string;
}

export interface RegisterResponse {
  status: number;
  message: string;
  token?: string;
  user?: User;
}

// ===== Service =====
@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private api = environment.apiUrl.replace(/\/+$/, ''); // ex: http://localhost:8080

  // Signals
  isLoggedIn = signal(false);
  currentUser = signal<User | null>(null);
  currentRole = signal<string | null>(null);

  // BehaviorSubject pour réactivité
  private authState$ = new BehaviorSubject<boolean>(false);
  public authState = this.authState$.asObservable();

  constructor() {
    this.checkAuthState();
  }

  // ===== Headers =====
  private getHttpHeaders(): HttpHeaders {
    const token = this.getToken();
    let headers = new HttpHeaders({ 'Content-Type': 'application/json', 'Accept': 'application/json' });
    if (token) headers = headers.set('Authorization', `Bearer ${token}`);
    return headers;
  }

  // ===== Auth =====
  register(payload: RegisterPayload): Observable<RegisterResponse> {
    return this.http.post<RegisterResponse>(`${this.api}/auth/register`, payload)
      .pipe(
        tap(res => {
          if (res?.token && res?.user) this.persistAuthData(res.token, res.user, false);
        }),
        catchError(err => throwError(() => err))
      );
  }

  login(payload: LoginRequest) {
    return this.http.post<LoginResponse>(`${this.api}/auth/login`, payload).pipe(
      tap(res => {
        // ✅ Toujours stocker le token
        if (res.token) {
          localStorage.setItem('token', res.token);
        }
        // ✅ Stocker user + role si renvoyés (évite d'appeler /users/me juste après)
        if (res.user) {
          localStorage.setItem('user', JSON.stringify(res.user));
          if (res.user.role) localStorage.setItem('role', res.user.role);
          // hydrater les signals
          this.currentUser.set(res.user);
          this.currentRole.set(res.user.role);
          this.isLoggedIn.set(true);
          this.authState$.next(true);
        } else if (res.role) {
          localStorage.setItem('role', res.role);
          this.currentRole.set(res.role);
        }
      })
    );
  }

  // ===== Logout =====
  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    localStorage.removeItem('user');
    sessionStorage.removeItem('token');
    sessionStorage.removeItem('role');
    sessionStorage.removeItem('user');

    this.isLoggedIn.set(false);
    this.currentUser.set(null);
    this.currentRole.set(null);
    this.authState$.next(false);

    this.router.navigateByUrl('/login');
  }

  // ===== Profil =====
  me(): Observable<User> {
    return this.http.get<User>(`${this.api}/users/me`, { headers: this.getHttpHeaders() })
      .pipe(
        tap(user => this.updateUserData(user)),
        catchError(err => throwError(() => err))
      );
  }

  updateProfile(payload: { nom: string; prenom: string; email: string }): Observable<User> {
    return this.http.put<User>(`${this.api}/users/profile`, payload, { headers: this.getHttpHeaders() })
      .pipe(
        tap(user => this.updateUserData(user)),
        catchError(err => throwError(() => err))
      );
  }

  changePassword(payload: { currentPassword: string; newPassword: string }): Observable<any> {
    return this.http.put(`${this.api}/auth/password`, payload, { headers: this.getHttpHeaders() })
      .pipe(catchError(err => throwError(() => err)));
  }

  // ===== Utils =====
  getToken(): string | null {
    return sessionStorage.getItem('token') || localStorage.getItem('token');
  }

  getRole(): string | null {
    return this.currentRole() || sessionStorage.getItem('role') || localStorage.getItem('role');
  }

  getUser(): User | null {
    if (this.currentUser()) return this.currentUser();
    const str = sessionStorage.getItem('user') || localStorage.getItem('user');
    try { return str ? JSON.parse(str) as User : null; } catch { return null; }
  }

  isAuthenticated(): boolean {
    const token = this.getToken();
    if (!token) return false;
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return Date.now() < payload.exp * 1000;
    } catch { return false; }
  }

  // ===== Privé =====
  private persistAuthData(token: string, user: User, remember = false): void {
    this.persistToken(token, remember);
    this.persistRole(user.role, remember);
    this.persistUser(user, remember);
    this.isLoggedIn.set(true);
    this.currentUser.set(user);
    this.currentRole.set(user.role);
    this.authState$.next(true);
  }

  private persistToken(token: string, remember = false): void {
    const storage = remember ? localStorage : sessionStorage;
    const other = remember ? sessionStorage : localStorage;
    storage.setItem('token', token);
    other.removeItem('token');
  }

  private persistRole(role: string, remember = false): void {
    const storage = remember ? localStorage : sessionStorage;
    const other = remember ? sessionStorage : localStorage;
    storage.setItem('role', role);
    other.removeItem('role');
  }

  private persistUser(user: User, remember = false): void {
    const storage = remember ? localStorage : sessionStorage;
    const other = remember ? sessionStorage : localStorage;
    storage.setItem('user', JSON.stringify(user));
    other.removeItem('user');
  }

  private updateUserData(user: User): void {
    this.currentUser.set(user);
    this.currentRole.set(user.role);
    this.isLoggedIn.set(true);
    this.authState$.next(true);
  }

  private checkAuthState(): void {
    const token = this.getToken();
    const user = this.getUser();
    const role = this.getRole();
    if (token && user && role && this.isAuthenticated()) {
      this.isLoggedIn.set(true);
      this.currentUser.set(user);
      this.currentRole.set(role);
      this.authState$.next(true);
    } else {
      this.isLoggedIn.set(false);
      this.currentUser.set(null);
      this.currentRole.set(null);
      this.authState$.next(false);
    }
  }
}
