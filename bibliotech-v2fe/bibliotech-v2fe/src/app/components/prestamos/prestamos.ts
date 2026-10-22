import { ChangeDetectorRef, Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { Prestamo } from '../../services/prestamo';

@Component({
  selector: 'app-prestamos',
  imports: [FormsModule],
  templateUrl: './prestamos.html',
  styleUrl: './prestamos.css'
})
export class Prestamos {

  estudianteId: number | null = 1;
  libroId: number | null = 1;

  mensaje: string = '';
  disponible: boolean | null = null;
  procesando = false;

  constructor(private prestamoService: Prestamo, private changeDetector: ChangeDetectorRef) {}

  validarPrestamo(): void {
    const identificadores = this.iniciarSolicitud();
    if (!identificadores) return;

    this.prestamoService
      .validarPrestamo(identificadores.estudianteId, identificadores.libroId)
      .pipe(finalize(() => this.finalizarSolicitud()))
      .subscribe({
        next: (resultado) => {
          this.disponible = resultado;

          this.mensaje = resultado ? 'El préstamo está disponible.' : 'El préstamo no puede realizarse.';
        },
        error: () => {
          this.disponible = false;
          this.mensaje = 'Error al validar el préstamo.';
        }
      });
  }

  registrarPrestamo(): void {
    const identificadores = this.iniciarSolicitud();
    if (!identificadores) return;

    this.prestamoService
      .registrarPrestamo(identificadores.estudianteId, identificadores.libroId)
      .pipe(finalize(() => this.finalizarSolicitud()))
      .subscribe({
        next: () => {
          this.disponible = true;
          this.mensaje = 'Préstamo registrado correctamente.';
        },
        error: (error) => {
          this.disponible = false;
          const detalle = error?.error?.detail;
          this.mensaje = typeof detalle === 'string' && detalle.trim()
            ? detalle
            : 'No se pudo registrar el préstamo.';
        }
      });
  }

  private iniciarSolicitud(): { estudianteId: number; libroId: number } | null {
    if (this.procesando) return null;

    if (!this.esIdentificadorValido(this.estudianteId) || !this.esIdentificadorValido(this.libroId)) {
      this.disponible = false;
      this.mensaje = 'Ingrese IDs de estudiante y libro que sean números enteros positivos.';
      return null;
    }

    this.procesando = true;
    this.disponible = null;
    this.mensaje = '';
    return { estudianteId: this.estudianteId, libroId: this.libroId };
  }

  private esIdentificadorValido(valor: number | null): valor is number {
    return valor !== null && Number.isInteger(valor) && valor > 0;
  }

  private finalizarSolicitud(): void {
    this.procesando = false;
    this.changeDetector.markForCheck();
  }
}
