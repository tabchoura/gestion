// src/app/core/historique.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, EMPTY } from 'rxjs';
import { environment } from '../../environments/environment.development';

export interface Historique {
  id?: number;
  acteurEmail: string;
  acteurRole?: string;
  action: string;
  creeLe: string;            
  message?: string;
  payloadJson?: string;
  ressourceId?: number | string;
  ressourceLabel?: string;
  ressourceType?: string;
  page?: string;
}

export interface HistoriqueRequest {
  action: string;
  message?: string;
  ressourceType?: string;
  ressourceId?: number | string;
  ressourceLabel?: string;
  page?: string;
  payloadJson?: string;
}

@Injectable({ providedIn: 'root' })
export class HistoriqueService {
  private http = inject(HttpClient);
  private readonly api = environment.apiUrl.replace(/\/+$/, '') + '/historique';

  private buildHeaders(json = false): HttpHeaders {
    const token = localStorage.getItem('token') || sessionStorage.getItem('token');
    let headers = new HttpHeaders({ 'Accept': 'application/json' });
    if (json) headers = headers.set('Content-Type', 'application/json');
    if (token) headers = headers.set('Authorization', `Bearer ${token}`);
    return headers;
  }

  /** Récupérer l'historique de l'utilisateur connecté */
  getMyHistory(): Observable<Historique[]> {
    return this.http.get<Historique[]>(`${this.api}/mine`, {
      headers: this.buildHeaders(false)
    });
  }

  /** Alias pour compatibilité */
  mine(): Observable<Historique[]> {
    return this.getMyHistory();
  }

  /** (Admin) — nécessite un endpoint back /historique/all */
  getAllHistory(): Observable<Historique[]> {
    return this.http.get<Historique[]>(`${this.api}/all`, {
      headers: this.buildHeaders(false)
    });
  }

  /** Ajouter une entrée d'historique — nécessite un endpoint back /historique/add */
  addHistoryEntry(entry: HistoriqueRequest): Observable<Historique> {
    return this.http.post<Historique>(`${this.api}/add`, entry, {
      headers: this.buildHeaders(true)
    });
  }

  /** Récupérer une entrée par ID — nécessite /historique/{id} */
  getHistoryById(id: number): Observable<Historique> {
    return this.http.get<Historique>(`${this.api}/${id}`, {
      headers: this.buildHeaders(false)
    });
  }

  /** Supprimer une entrée — nécessite DELETE /historique/{id} */
  deleteHistoryEntry(id: number): Observable<string> {
    return this.http.delete<string>(`${this.api}/${id}`, {
      headers: this.buildHeaders(false)
    });
  }

  /** Filtrer par action — nécessite /historique/action/{action} */
  getHistoryByAction(action: string): Observable<Historique[]> {
    return this.http.get<Historique[]>(`${this.api}/action/${encodeURIComponent(action)}`, {
      headers: this.buildHeaders(false)
    });
  }

  /** 10 dernières actions — nécessite /historique/recent */
  getRecentHistory(): Observable<Historique[]> {
    return this.http.get<Historique[]>(`${this.api}/recent`, {
      headers: this.buildHeaders(false)
    });
  }

  /** Compter les actions — nécessite /historique/count */
  getHistoryCount(): Observable<number> {
    return this.http.get<number>(`${this.api}/count`, {
      headers: this.buildHeaders(false)
    });
  }

  /**
   * Fire-and-forget pour logguer côté front (si ton back expose /historique/add).
   * Si aucun token -> ne fait rien (évite les 401 bruyants).
   */
  logAction(
    action: string,
    message?: string,
    ressourceType?: string,
    ressourceId?: number | string,
    ressourceLabel?: string,
    page?: string
  ): void {
    const token = localStorage.getItem('token') || sessionStorage.getItem('token');
    if (!token) {
      // Pas connecté -> on ne tente pas l'appel pour éviter "Unauthorized"
      return;
    }

    const entry: HistoriqueRequest = {
      action,
      message: message || `Action ${action} effectuée`,
      ressourceType,
      ressourceId,
      ressourceLabel,
      page: page || (typeof window !== 'undefined' ? window.location.pathname : undefined),
      payloadJson: JSON.stringify({
        timestamp: new Date().toISOString(),
        userAgent: typeof navigator !== 'undefined' ? navigator.userAgent : '',
        url: typeof window !== 'undefined' ? window.location.href : ''
      })
    };

    this.addHistoryEntry(entry).subscribe({
      next: () => {},
      error: () => {
        // on reste silencieux en prod; à activer si besoin de debug:
        // console.error('Failed to log action:', error);
      }
    });
  }

  /** Alias compatible : retourne un Observable si tu veux gérer toi-même la souscription */
  log(action: string, page?: string): Observable<Historique> {
    const payload: HistoriqueRequest = {
      action,
      message: `Action ${action} effectuée`,
      page: page || (typeof window !== 'undefined' ? window.location.pathname : undefined),
      payloadJson: JSON.stringify({
        timestamp: new Date().toISOString(),
        userAgent: typeof navigator !== 'undefined' ? navigator.userAgent : '',
        url: typeof window !== 'undefined' ? window.location.href : ''
      })
    };
    return this.addHistoryEntry(payload);
  }

  /** (Optionnel) Endpoint pratique si ton back a /historique/test */
  testLog(): Observable<any> {
    return this.http.post(`${this.api}/test`, {}, { headers: this.buildHeaders(true) });
  }
}
