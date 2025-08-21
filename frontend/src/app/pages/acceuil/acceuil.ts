import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';

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
    { icon: '🔍', title: 'Suivi en temps réel', desc: 'Consultez l\'avancement de votre demande' },
    { icon: '📜', title: 'Historique', desc: 'Gardez une trace de vos chéquiers' }
  ];

  avis = [
    { note: 5, text: 'Super service !', name: 'Ali' },
    { note: 4, text: 'Rapide et efficace', name: 'Sami' },
    { note: 5, text: 'Je recommande', name: 'Amel' }
  ];

  navigateToLogin() {
    console.log('Navigation vers /login'); 
    this.router.navigate(['/login']);
  }

  navigateToRegister() {
    console.log('Navigation vers /register'); 
    this.router.navigate(['/register']);
  }

  go(path: string) {
    console.log(`Navigation vers ${path}`); 
    this.router.navigateByUrl(path);
  }

  // Méthode pour le scroll vers une section
  scrollTo(id: string) {
    const element = document.getElementById(id);
    if (element) {
      element.scrollIntoView({ behavior: 'smooth' });
    }
  }
}