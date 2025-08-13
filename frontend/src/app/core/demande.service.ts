import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment.development'; // ou .development selon ton setup

export interface Compte {
  id: number;
  banque: string;
  numeroCompte: string;
  devise: string;
}

export type DemandeStatut = 'EN_ATTENTE' | 'APPROUVEE' | 'REJETEE' | 'ANNULEE';

export interface DemandeChequier {
  id: number;
  compteId: number;
  banque: string;
  numeroCompte: string;
  devise: string;
  dateDemande: string; // ISO
  pages: number;
  motif?: string;
  statut: DemandeStatut;
  createdAt: string;   // ISO
  updatedAt: string;   // ISO
}

@Injectable({ providedIn: 'root' })
export class DemandesService {
  private api = environment.apiUrl; // ex: http://localhost:8080

  constructor(private http: HttpClient) {}

  // Si ton back expose /comptes ET /comptes/mine, tu peux garder /mine :
  getMesComptes(): Observable<Compte[]> {
    return this.http.get<Compte[]>(`${this.api}/comptes/mine`);
    // sinon: return this.http.get<Compte[]>(`${this.api}/comptes`);
  }

  getDemandesByCompte(compteId: number): Observable<DemandeChequier[]> {
    return this.http.get<DemandeChequier[]>(`${this.api}/demandes?compteId=${compteId}`);
  }

  creerDemande(payload: { compteId: number; dateDemande?: string; pages: number; motif?: string }):
    Observable<DemandeChequier> {
    return this.http.post<DemandeChequier>(`${this.api}/demandes`, payload);
  }

  annulerDemande(id: number) {
    return this.http.put(`${this.api}/demandes/${id}/annuler`, {});
  }

  supprimerDemande(id: number) {
    return this.http.delete(`${this.api}/demandes/${id}`);
  }
}
