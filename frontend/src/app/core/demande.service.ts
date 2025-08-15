import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment.development';

// ===== Interfaces =====
export interface Compte {
  id: number;
  banque: string;
  numeroCompte: string;
  devise: string;
  solde?: number;
}

export interface DemandeChequier {
  id: number;
  compteId: number;
  banque?: string;
  numeroCompte?: string;
  devise?: string;
  dateDemande: string | Date;
  pages: number;
  motif?: string | null; // <- accepte null
  statut: 'EN_ATTENTE' | 'APPROUVEE' | 'REJETEE' | 'ANNULEE';
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateDemandePayload {
  compteId: number;
  dateDemande: string; // yyyy-MM-dd
  pages: number;
  motif?: string | null; // <- accepte null
}

export interface UpdateDemandePayload {
  dateDemande?: string; // yyyy-MM-dd
  pages?: number;
  motif?: string | null; // <- accepte null
}

// ===== Service =====
@Injectable({ providedIn: 'root' })
export class DemandesService {
  private http = inject(HttpClient);
  private api = environment.apiUrl.replace(/\/+$/, ''); // Remove trailing slashes

  // ===== Headers avec token =====
  private getHttpHeaders(): HttpHeaders {
    const token = this.getToken();
    let headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    });
    if (token) headers = headers.set('Authorization', `Bearer ${token}`);
    return headers;
  }

  private getToken(): string | null {
    return sessionStorage.getItem('token') || localStorage.getItem('token');
  }

  // ===== Gestion des erreurs =====
  private handleError = (error: HttpErrorResponse) => {
    const body = error.error;
    const serverMsg =
      (body && (body.message || body.error_description || body.error)) || null;

    let errorMessage = serverMsg || 'Une erreur est survenue';
    if (!serverMsg) {
      if (error.status === 400) errorMessage = 'Requête invalide (vérifiez les champs envoyés)';
      else if (error.status === 401) errorMessage = 'Non autorisé - Veuillez vous reconnecter';
      else if (error.status === 403) errorMessage = 'Accès interdit';
      else if (error.status === 404) errorMessage = 'Ressource non trouvée';
      else if (error.status === 500) errorMessage = 'Erreur serveur interne';
    }
    return throwError(() => new Error(errorMessage));
  };

  // ===== API Calls =====
  getMesComptes(): Observable<Compte[]> {
    return this.http.get<Compte[]>(`${this.api}/comptes`, {
      headers: this.getHttpHeaders()
    }).pipe(catchError(this.handleError));
  }

  getMesDemandes(): Observable<DemandeChequier[]> {
    return this.http.get<DemandeChequier[]>(`${this.api}/demandes`, {
      headers: this.getHttpHeaders()
    }).pipe(catchError(this.handleError));
  }

  getDemandesByCompte(compteId: number): Observable<DemandeChequier[]> {
    return this.http.get<DemandeChequier[]>(`${this.api}/demandes?compteId=${compteId}`, {
      headers: this.getHttpHeaders()
    }).pipe(catchError(this.handleError));
  }

  creerDemande(payload: CreateDemandePayload): Observable<DemandeChequier> {
    return this.http.post<DemandeChequier>(`${this.api}/demandes`, payload, {
      headers: this.getHttpHeaders()
    }).pipe(catchError(this.handleError));
  }

  modifierDemande(id: number, payload: UpdateDemandePayload): Observable<DemandeChequier> {
    return this.http.put<DemandeChequier>(`${this.api}/demandes/${id}`, payload, {
      headers: this.getHttpHeaders()
    }).pipe(catchError(this.handleError));
  }

  annulerDemande(id: number): Observable<DemandeChequier> {
    return this.http.put<DemandeChequier>(`${this.api}/demandes/${id}/annuler`, {}, {
      headers: this.getHttpHeaders()
    }).pipe(catchError(this.handleError));
  }

  supprimerDemande(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/demandes/${id}`, {
      headers: this.getHttpHeaders()
    }).pipe(catchError(this.handleError));
  }

  getDemande(id: number): Observable<DemandeChequier> {
    return this.http.get<DemandeChequier>(`${this.api}/demandes/${id}`, {
      headers: this.getHttpHeaders()
    }).pipe(catchError(this.handleError));
  }
}
