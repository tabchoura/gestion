import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

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
  creeLe: string; // ISO
}

@Injectable({ providedIn: 'root' })
export class HistoriqueService {
  private http = inject(HttpClient);
  // ⛳️ URL RELATIVE -> l’interceptor ajoute toujours Authorization
  private base = `/api/historique`;

  listMy(): Observable<HistoriqueItem[]> {
    return this.http.get<HistoriqueItem[]>(this.base);
  }

  listByResource(type: RessourceType, id: number): Observable<HistoriqueItem[]> {
    return this.http.get<HistoriqueItem[]>(`${this.base}/resource`, { params: { type, id } as any });
  }

  listByType(type: RessourceType): Observable<HistoriqueItem[]> {
    return this.http.get<HistoriqueItem[]>(`${this.base}/type/${type}`);
  }
}
