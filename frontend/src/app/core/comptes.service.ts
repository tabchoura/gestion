import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { environment } from '../../environments/environment.development';
import { Observable } from 'rxjs';

export interface CompteBancaire {
  id?: number;
  typeCompte: string;
  numeroCompte: string;
  rib?: string | null;
  iban?: string | null;
  devise: string;
  isDefault?: boolean;
}

@Injectable({ providedIn: 'root' })
export class ComptesService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/comptes`;

  /** Liste tous les comptes */
  list(): Observable<CompteBancaire[]> {
    // Changed from this.base to this.base + '/mine'
    return this.http.get<CompteBancaire[]>(`${this.base}/mine`, { headers: this.getHeaders() });
  }

  /** Crée un nouveau compte */
  create(body: CompteBancaire): Observable<CompteBancaire> {
    return this.http.post<CompteBancaire>(this.base, body, { headers: this.getHeaders() });
  }

  /** Met à jour un compte existant */
  update(id: number, body: Partial<CompteBancaire>): Observable<CompteBancaire> {
    return this.http.put<CompteBancaire>(`${this.base}/${id}`, body, { headers: this.getHeaders() });
  }

  /** Supprime un compte */
  remove(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`, { headers: this.getHeaders() });
  }

  /** Définit un compte comme compte par défaut */
  setDefault(id: number): Observable<CompteBancaire> {
    return this.http.put<CompteBancaire>(`${this.base}/${id}/default`, {}, { headers: this.getHeaders() });
  }

  /** Génère les headers avec le token JWT */
  private getHeaders(): HttpHeaders {
    const token = sessionStorage.getItem('token') || localStorage.getItem('token');
    let headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'Accept': 'application/json',
    });
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }
    return headers;
  }
}