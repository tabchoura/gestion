import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment.development'; 

export interface Chequier {
  id: number;
  compteId: number;
  dateEmission: string;
  nbPages: number;
  premierNumeroCheque: string;
  dernierNumeroCheque: string;
  statut?: string;
}

@Injectable({ providedIn: 'root' })
export class ChequiersService {
  private http = inject(HttpClient);
private base = `${environment.apiUrl}/api/chequiers`; 

listByCompte(compteId: number): Promise<Chequier[]> {
  return firstValueFrom(
    this.http.get<Chequier[]>(`${this.base}?compteId=${compteId}`)
  );
}
}
