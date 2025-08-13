import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ChequiersService, Chequier } from '../../../core/chequier.service';

@Component({
  standalone: true,
  selector: 'app-chequiers',
  imports: [CommonModule, RouterLink],
  templateUrl: './chequiers.html',
  styleUrls: ['./chequiers.css']
})
export class ChequiersComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private service = inject(ChequiersService);

  compteId!: number;
  loading = true;
  error = '';
  chequiers: Chequier[] = [];

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('compteId');
    if (!id) { this.error = 'Compte introuvable.'; this.loading = false; return; }
    this.compteId = +id;
    this.load();
  }

  async load() {
    this.loading = true;
    this.error = '';
    try {
      this.chequiers = await this.service.listByCompte(this.compteId);
    } catch (e: any) {
      this.error = e?.error?.message || 'Erreur de chargement.';
    } finally {
      this.loading = false;
    }
  }
}
