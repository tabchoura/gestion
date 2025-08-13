import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment.development'; // ou .development
import { Observable } from 'rxjs';

export interface CompteBancaire {
  id?: number;
  banque: string;
  titulaire: string;
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

  list(): Observable<CompteBancaire[]> { return this.http.get<CompteBancaire[]>(this.base); }
  create(body: CompteBancaire): Observable<CompteBancaire> { return this.http.post<CompteBancaire>(this.base, body); }
  update(id: number, body: Partial<CompteBancaire>) { return this.http.put<CompteBancaire>(`${this.base}/${id}`, body); }
  remove(id: number) { return this.http.delete(`${this.base}/${id}`); }
  setDefault(id: number) { return this.http.put<CompteBancaire>(`${this.base}/${id}/default`, {}); }
}
