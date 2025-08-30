import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../.././environments/environment.development';

/* ====== Modèles ====== */
export interface Compte {
  id: number;
  typeCompte: string;
  numeroCompte: string;
  devise: string;
  isDefault?: boolean;
}

export type DemandeStatut = 'EN_ATTENTE' | 'APPROUVEE' | 'REJETEE' | 'ANNULEE';

export interface DemandeChequier {
  id: number;
  compte?: Compte;
  numeroCompte?: string;
  devise?: string;
  dateDemande: string | Date;
  pages: number;
  motif?: string | null;
  statut: DemandeStatut;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateDemandePayload {
  compteId: number;
  dateDemande: string; // yyyy-MM-dd
  pages: number;
  motif?: string | null;
}

export interface UpdateDemandePayload {
  dateDemande?: string; // yyyy-MM-dd
  pages?: number;
  motif?: string | null;
}

/* ====== Service ====== */
@Injectable({ providedIn: 'root' })
export class DemandesService {
  private http = inject(HttpClient);
  private api = environment.apiUrl.replace(/\/+$/, ''); // trim trailing slash

  private getHttpHeaders(): HttpHeaders {
    const token = sessionStorage.getItem('token') || localStorage.getItem('token');
    let headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'Accept': 'application/json',
    });
    if (token) headers = headers.set('Authorization', `Bearer ${token}`);
    return headers;
  }

  private handleError = (error: HttpErrorResponse) => {
    const body = error.error;
    const serverMsg = (body && (body.message || body.error_description || body.error)) || null;

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

  /* ---- Comptes ---- */
  getMesComptes(): Observable<Compte[]> {
    // Changed from /comptes to /comptes/mine
    return this.http
      .get<Compte[]>(`${this.api}/comptes/mine`, { headers: this.getHttpHeaders() })
      .pipe(catchError(this.handleError));
  }

 getDemandesByCompte(compteId: number): Observable<DemandeChequier[]> {
  return this.http
    .get<DemandeChequier[]>(`${this.api}/demandes/mine?compteId=${compteId}`, { headers: this.getHttpHeaders() })
    .pipe(catchError(this.handleError));
}

// Also, update getMesDemandes to use the correct endpoint:
getMesDemandes(): Observable<DemandeChequier[]> {
  return this.http
    .get<DemandeChequier[]>(`${this.api}/demandes/mine`, { headers: this.getHttpHeaders() })
    .pipe(catchError(this.handleError));
}

  creerDemande(payload: CreateDemandePayload): Observable<DemandeChequier> {
    return this.http
      .post<DemandeChequier>(`${this.api}/demandes`, payload, { headers: this.getHttpHeaders() })
      .pipe(catchError(this.handleError));
  }

  modifierDemande(id: number, payload: UpdateDemandePayload): Observable<DemandeChequier> {
    return this.http
      .put<DemandeChequier>(`${this.api}/demandes/${id}`, payload, { headers: this.getHttpHeaders() })
      .pipe(catchError(this.handleError));
  }

  annulerDemande(id: number): Observable<DemandeChequier> {
    return this.http
      .put<DemandeChequier>(`${this.api}/demandes/${id}/annuler`, {}, { headers: this.getHttpHeaders() })
      .pipe(catchError(this.handleError));
  }

  supprimerDemande(id: number): Observable<void> {
    return this.http
      .delete<void>(`${this.api}/demandes/${id}`, { headers: this.getHttpHeaders() })
      .pipe(catchError(this.handleError));
  }

  /* ---- Demandes (Agent/Admin) ---- */
  getToutesLesDemandes(): Observable<DemandeChequier[]> {
    return this.http
      .get<DemandeChequier[]>(`${this.api}/demandes/all`, { headers: this.getHttpHeaders() })
      .pipe(catchError(this.handleError));
  }

  // ✅ Envoie le statut en query param (comme Postman) → évite le 400
  changerStatutDemande(id: number, statut: DemandeStatut): Observable<DemandeChequier> {
    return this.http
      .put<DemandeChequier>(
        `${this.api}/demandes/${id}/statut`,
        null, // pas de body
        { headers: this.getHttpHeaders(), params: { statut } } // ?statut=APPROUVEE/REJETEE
      )
      .pipe(catchError(this.handleError));
  }
}