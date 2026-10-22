import { Component, signal } from '@angular/core';
import { Prestamos } from './components/prestamos/prestamos';

@Component({
  selector: 'app-root',
  imports: [Prestamos],
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App {
  protected readonly title = signal('bibliotech-v2fe');
}
