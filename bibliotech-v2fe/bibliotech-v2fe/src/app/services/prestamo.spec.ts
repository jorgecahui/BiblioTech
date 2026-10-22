import { TestBed } from '@angular/core/testing';
import {
  provideHttpClient,
  withInterceptorsFromDi
} from '@angular/common/http';

import {
  provideHttpClientTesting,
  HttpTestingController
} from '@angular/common/http/testing';

import { Prestamo } from './prestamo';

describe('Prestamo', () => {

  let service: Prestamo;
  let httpMock: HttpTestingController;

  beforeEach(() => {

    TestBed.configureTestingModule({
      providers: [
        Prestamo,
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(Prestamo);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  // PRUEBA 1: VALIDACIÓN DISPONIBLE
  it('debe validar un préstamo disponible', () => {

    service.validarPrestamo(1, 1).subscribe(resultado => {
      expect(resultado).toBe(true);
    });

    const req = httpMock.expectOne(request =>
      request.url === 'http://localhost:8080/api/prestamos/validar' &&
      request.params.get('estudianteId') === '1' &&
      request.params.get('libroId') === '1'
    );

    expect(req.request.method).toBe('GET');

    req.flush(true);
  });

// PRUEBA 2: VALIDACIÓN NO DISPONIBLE
  it('debe indicar que el préstamo no está disponible', () => {

    service.validarPrestamo(1, 1).subscribe(resultado => {
      expect(resultado).toBe(false);
    });

    const req = httpMock.expectOne(request =>
      request.url === 'http://localhost:8080/api/prestamos/validar' &&
      request.params.get('estudianteId') === '1' &&
      request.params.get('libroId') === '1'
    );

    expect(req.request.method).toBe('GET');

    req.flush(false);
  });

  // PRUEBA 3: REGISTRAR PRÉSTAMO
  it('debe registrar un préstamo', () => {

    const prestamoSimulado = {
      id: 1
    };

    service.registrarPrestamo(1, 2).subscribe(resultado => {
      expect(resultado).toEqual(prestamoSimulado);
    });

    const req = httpMock.expectOne(request =>
      request.url === 'http://localhost:8080/api/prestamos' &&
      request.params.get('estudianteId') === '1' &&
      request.params.get('libroId') === '2'
    );

    expect(req.request.method).toBe('POST');

    req.flush(prestamoSimulado);
  });

  it('conserva el detalle del conflicto por límite de préstamos', () => {
    const problema = {
      detail: 'El estudiante ya tiene 3 préstamos activos. El límite es 3.',
      prestamosActivos: 3,
      limite: 3
    };

    service.registrarPrestamo(1, 4).subscribe({
      next: () => { throw new Error('El registro debe ser rechazado.'); },
      error: error => {
        expect(error.status).toBe(409);
        expect(error.error).toEqual(problema);
      }
    });

    const req = httpMock.expectOne(request =>
      request.url === 'http://localhost:8080/api/prestamos' &&
      request.params.get('estudianteId') === '1' &&
      request.params.get('libroId') === '4'
    );
    expect(req.request.method).toBe('POST');
    req.flush(problema, {
      status: 409,
      statusText: 'Conflict',
      headers: { 'Content-Type': 'application/problem+json' }
    });
  });

});
