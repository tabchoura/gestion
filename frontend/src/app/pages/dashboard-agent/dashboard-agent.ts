import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-dashboard-agent',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './dashboard-agent.html',
  styleUrls: ['./dashboard-agent.css'],
})
export class DashboardAgentComponent {
  constructor(private router: Router) {}

 logout() {
  // 🔑 Nettoyer localStorage
  localStorage.removeItem('token');
  localStorage.removeItem('user');
  localStorage.removeItem('role');

  // 🔑 Nettoyer sessionStorage aussi (si remember = false)
  sessionStorage.removeItem('token');
  sessionStorage.removeItem('user');
  sessionStorage.removeItem('role');

  // 🔑 Redirection vers login
  this.router.navigate(['/login']);
}

}
