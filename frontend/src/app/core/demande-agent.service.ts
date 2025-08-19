import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from '../../environments/environment.development';

// ===== Interfaces pour l'Agent =====
export interface DemandeAgent {
  id: number;
  compteId: number;
  banque?: string;
  numeroCompte?: string;
  devise?: string;
  dateDemande: string | Date;
  pages: number;
  motif?: string | null;
  statut: 'EN_ATTENTE' | 'APPROUVEE' | 'REJETEE' | 'ANNULEE';
  createdAt?: string;
  updatedAt?: string;
}

export interface ClientDemandes {
  clientId: number;
  clientNom: string;
  clientEmail: string;
  demandes: DemandeAgent[];
  totalDemandes: number;
  demandesEnAttente: number;
  demandesApprouvees: number;
  demandesRejetees: number;
  demandesAnnulees: number;
}

// ===== Service Agent =====
@Injectable({ providedIn: 'root' })
export class DemandesAgentService {
  private http = inject(HttpClient);
  private api = environment.apiUrl.replace(/\/+$/, '');

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
      if (error.status === 400) errorMessage = 'Requête invalide';
      else if (error.status === 401) errorMessage = 'Non autorisé - Veuillez vous reconnecter';
      else if (error.status === 403) errorMessage = 'Accès interdit';
      else if (error.status === 404) errorMessage = 'Ressource non trouvée';
      else if (error.status === 500) errorMessage = 'Erreur serveur interne';
    }
    return throwError(() => new Error(errorMessage));
  };

  // ===== API Calls =====
  
  /**
   * Récupère toutes les demandes pour l'agent
   */
  getToutesLesDemandes(): Observable<DemandeAgent[]> {
    return this.http.get<DemandeAgent[]>(`${this.api}/demandes/all`, {
      headers: this.getHttpHeaders()
    }).pipe(catchError(this.handleError));
  }

  /**
   * Récupère toutes les demandes groupées par client
   */
  getDemandesGroupeesParClient(): Observable<ClientDemandes[]> {
    return this.getToutesLesDemandes().pipe(
      map(demandes => this.grouperDemandesParClient(demandes))
    );
  }

  /**
   * Approuver une demande
   */
  approuverDemande(id: number): Observable<DemandeAgent> {
    return this.http.put<DemandeAgent>(`${this.api}/demandes/${id}/approuver`, {}, {
      headers: this.getHttpHeaders()
    }).pipe(catchError(this.handleError));
  }

  /**
   * Rejeter une demande
   */
  rejeterDemande(id: number): Observable<DemandeAgent> {
    return this.http.put<DemandeAgent>(`${this.api}/demandes/${id}/rejeter`, {}, {
      headers: this.getHttpHeaders()
    }).pipe(catchError(this.handleError));
  }

  /**
   * Groupe les demandes par client (logique côté frontend)
   */
  private grouperDemandesParClient(demandes: DemandeAgent[]): ClientDemandes[] {
    // Créer un map pour grouper par email (on utilise l'email comme identifiant unique du client)
    const groupesMap = new Map<string, ClientDemandes>();

    demandes.forEach(demande => {
      // Pour le moment, on simule les infos client
      // Dans un vrai projet, ces infos devraient venir du backend
      const clientEmail = `client${demande.compteId}@example.com`; // À remplacer par vraies données
      const clientNom = `Client ${demande.compteId}`; // À remplacer par vraies données
      const clientId = demande.compteId; // À adapter selon votre structure

      if (!groupesMap.has(clientEmail)) {
        groupesMap.set(clientEmail, {
          clientId,
          clientNom,
          clientEmail,
          demandes: [],
          totalDemandes: 0,
          demandesEnAttente: 0,
          demandesApprouvees: 0,
          demandesRejetees: 0,
          demandesAnnulees: 0
        });
      }

      const groupe = groupesMap.get(clientEmail)!;
      groupe.demandes.push(demande);
      groupe.totalDemandes++;

      // Comptage par statut
      switch (demande.statut) {
        case 'EN_ATTENTE':
          groupe.demandesEnAttente++;
          break;
        case 'APPROUVEE':
          groupe.demandesApprouvees++;
          break;
        case 'REJETEE':
          groupe.demandesRejetees++;
          break;
        case 'ANNULEE':
          groupe.demandesAnnulees++;
          break;
      }
    });

    // Trier par nom de client et trier les demandes par date
    return Array.from(groupesMap.values())
      .sort((a, b) => a.clientNom.localeCompare(b.clientNom))
      .map(groupe => ({
        ...groupe,
        demandes: groupe.demandes.sort((a, b) => 
          new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime()
        )
      }));
  }
}