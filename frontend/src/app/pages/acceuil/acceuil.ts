import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Router } from '@angular/router';

@Component({
  selector: 'app-accueil',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './acceuil.html',
  styleUrls: ['./acceuil.css']
})
export class AccueilComponent {
  constructor(private router: Router) {}

  currentYear = new Date().getFullYear();

  services = [
    { icon: '📦', title: 'Demande de chéquier', desc: 'Faites votre demande en ligne' },
    { icon: '🔍', title: 'Suivi en temps réel', desc: 'Consultez l’avancement de votre demande' },
    { icon: '📜', title: 'Historique', desc: 'Gardez une trace de vos chéquiers' }
  ];

  avis = [
    { note: 5, text: 'Super service !', name: 'Ali' },
    { note: 4, text: 'Rapide et efficace', name: 'Sami' },
    { note: 5, text: 'Je recommande', name: 'Amel' }
  ];
  

  go(path: string) {
    this.router.navigateByUrl(path);
  }

  scrollTo(id: string) {
    document.getElementById(id)?.scrollIntoView({ behavior: 'smooth' });
  }
}
