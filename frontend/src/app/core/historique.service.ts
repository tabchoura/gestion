import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';

export type RessourceType =
  | 'COMPTE' | 'DEMANDE_CHEQUIER' | 'CHEQUIER' | 'CHEQUE' | 'AUTH' | 'AUTRE';
export type ActionType =
  | 'CREER' | 'MODIFIER' | 'SUPPRIMER' | 'CHANGER_STATUT'
  | 'ANNULER' | 'APPROUVER' | 'REJETER' | 'CONNEXION' | 'DECONNEXION';

export interface HistoriqueItem {
  id: number;
  acteurEmail: string;
  acteurRole?: string | null;
  ressourceType: RessourceType;
  ressourceId?: number | null;
  ressourceLabel?: string | null;
  action: ActionType;
  message?: string | null;
  payloadJson?: string | null;
  creeLe: string;
}

@Injectable({ providedIn: 'root' })
export class HistoriqueService {
  private http = inject(HttpClient);
  private base = `/api/historique`;

  private parseList(txt: string): HistoriqueItem[] {
    if (!txt) return [];                 // body vide => []
    const s = txt.trim();
    if (s.startsWith('<')) return [];    // HTML (ex: redirection vers login) => []
    try { return JSON.parse(s) as HistoriqueItem[]; }
    catch { return []; }
  }

  listMy(): Observable<HistoriqueItem[]> {
    return this.http.get(this.base, { responseType: 'text' as const }).pipe(
      map(t => this.parseList(t)),
      catchError(() => of([]))
    );
  }

  listByResource(type: RessourceType, id: number): Observable<HistoriqueItem[]> {
    return this.http.get(`${this.base}/resource`, {
      responseType: 'text' as const, params: { type, id } as any
    }).pipe(
      map(t => this.parseList(t)),
      catchError(() => of([]))
    );
  }

  listByType(type: RessourceType): Observable<HistoriqueItem[]> {
    return this.http.get(`${this.base}/type/${type}`, {
      responseType: 'text' as const
    }).pipe(
      map(t => this.parseList(t)),
      catchError(() => of([]))
    );
  }
  addTest() {
  // URL relative => l’interceptor mettra le Bearer automatiquement
  return this.http.post('/api/historique/test', {});
}

}
