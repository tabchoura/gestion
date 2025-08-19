
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule,Router } from '@angular/router';

@Component({
  selector: 'app-dashboard-client',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard-client.html',
  styleUrls: ['./dashboard-client.css'],
})
export class DashboardClientComponent 
{
    constructor(private router: Router) {}

logout() {
  localStorage.removeItem('token');
  sessionStorage.removeItem('token');  // au cas où tu l’as stocké là
  this.router.navigate(['/login'], { replaceUrl: true }); // redirige sans retour possible
}

}
