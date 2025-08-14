import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { HistoriqueService, HistoriqueItem, RessourceType } from '../../../core/historique.service';

@Component({
  standalone: true,
  selector: 'app-historique',
  imports: [CommonModule, DatePipe],
  templateUrl: './historique.html',
  styleUrls: ['./historique.css']
})
export class HistoriqueComponent implements OnInit {
  private api = inject(HistoriqueService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  items = signal<HistoriqueItem[]>([]);
  loading = signal<boolean>(false);
  error = signal<string | null>(null);

  mode = signal<'me' | 'resource' | 'type'>('me');
  currentType = signal<RessourceType | null>(null);
  currentId = signal<number | null>(null);

  expanded = new Set<number>();
  toggleRow(id: number) { this.expanded.has(id) ? this.expanded.delete(id) : this.expanded.add(id); }
  isExpanded(id: number) { return this.expanded.has(id); }

  ngOnInit() {
    this.route.paramMap.subscribe(pm => {
      const type = pm.get('type') as RessourceType | null;
      const id = pm.get('id');
      if (type && id) {
        this.mode.set('resource');
        this.currentType.set(type);
        this.currentId.set(+id);
        this.loadResource(type, +id);
        return;
      }
      const qType = this.route.snapshot.queryParamMap.get('type') as RessourceType | null;
      const qId = this.route.snapshot.queryParamMap.get('id');
      if (qType && qId) {
        this.mode.set('resource');
        this.currentType.set(qType);
        this.currentId.set(+qId);
        this.loadResource(qType, +qId);
        return;
      }
      if (qType && !qId) {
        this.mode.set('type');
        this.currentType.set(qType);
        this.loadByType(qType);
        return;
      }
      this.mode.set('me');
      this.loadMy();
    });
  }

  private loadMy() {
    this.loading.set(true);
    this.api.listMy().subscribe({
      next: rows => { this.items.set(rows); this.loading.set(false); this.error.set(null); },
      error: (e) => { this.error.set(`Impossible de charger votre historique (${e.status || '??'}).`); this.loading.set(false); }
    });
  }

  private loadResource(type: RessourceType, id: number) {
    this.loading.set(true);
    this.api.listByResource(type, id).subscribe({
      next: rows => { this.items.set(rows); this.loading.set(false); this.error.set(null); },
      error: (e) => { this.error.set(`Impossible de charger l’historique de la ressource (${e.status || '??'}).`); this.loading.set(false); }
    });
  }

  private loadByType(type: RessourceType) {
    this.loading.set(true);
    this.api.listByType(type).subscribe({
      next: rows => { this.items.set(rows); this.loading.set(false); this.error.set(null); },
      error: (e) => { this.error.set(`Impossible de charger l’historique par type (${e.status || '??'}).`); this.loading.set(false); }
    });
  }

  back(): Promise<boolean> {
    if (this.mode() === 'resource') {
      const t = this.currentType();
      if (t === 'DEMANDE_CHEQUIER') return this.router.navigate(['/dashboardclient/demandes', this.currentId()]);
      if (t === 'COMPTE')           return this.router.navigate(['/dashboardclient/comptes']);
    }
    return this.router.navigate(['/dashboardclient/profile']);
  }
  
}
