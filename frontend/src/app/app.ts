import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet], // ✅ RouterLink retiré ici
  templateUrl: './app.html',
  styleUrls: ['./app.css']
})
export class App {}
