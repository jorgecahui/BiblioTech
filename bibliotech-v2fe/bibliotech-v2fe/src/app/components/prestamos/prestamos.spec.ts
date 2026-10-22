import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Prestamos } from './prestamos';
import { HttpErrorResponse } from '@angular/common/http';
import { of, Subject, throwError } from 'rxjs';
import { vi } from 'vitest';
import { Prestamo } from '../../services/prestamo';

describe('Prestamos', () => {
  let component: Prestamos;
  let fixture: ComponentFixture<Prestamos>;
  let servicio: {
    validarPrestamo: ReturnType<typeof vi.fn>;
    registrarPrestamo: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    servicio = {
      validarPrestamo: vi.fn().mockReturnValue(of(true)),
      registrarPrestamo: vi.fn().mockReturnValue(of({ id: 1 }))
    };
    await TestBed.configureTestingModule({
      imports: [Prestamos],
      providers: [{ provide: Prestamo, useValue: servicio }],
    }).compileComponents();

    fixture = TestBed.createComponent(Prestamos);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('muestra la regla de máximo tres préstamos activos', () => {
    expect(fixture.nativeElement.textContent).toContain('máximo de 3 préstamos activos');
  });

  it('muestra el detalle del rechazo cuando se alcanza el límite', () => {
    const detalle = 'El estudiante ya tiene 3 préstamos activos. El límite es 3.';
    servicio.registrarPrestamo.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 409,
      error: { detail: detalle, prestamosActivos: 3, limite: 3 }
    })));

    component.registrarPrestamo();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('#resultado').textContent).toContain(detalle);
    expect(component.disponible).toBe(false);
    expect(component.procesando).toBe(false);
  });

  it('muestra un mensaje general si el servidor no incluye un detalle', () => {
    servicio.registrarPrestamo.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));

    component.registrarPrestamo();

    expect(component.mensaje).toBe('No se pudo registrar el préstamo.');
    expect(component.procesando).toBe(false);
  });

  it('registra un préstamo permitido y libera el formulario', () => {
    component.registrarPrestamo();

    expect(servicio.registrarPrestamo).toHaveBeenCalledWith(1, 1);
    expect(component.mensaje).toBe('Préstamo registrado correctamente.');
    expect(component.procesando).toBe(false);
  });

  it('bloquea solicitudes repetidas mientras se registra el préstamo', async () => {
    const respuesta = new Subject<{ id: number }>();
    servicio.registrarPrestamo.mockReturnValue(respuesta);
    const boton = fixture.nativeElement.querySelector('#btnRegistrar') as HTMLButtonElement;

    boton.click();
    boton.click();
    component.registrarPrestamo();
    component.validarPrestamo();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(servicio.registrarPrestamo).toHaveBeenCalledTimes(1);
    expect(servicio.validarPrestamo).not.toHaveBeenCalled();
    expect(boton.disabled).toBe(true);
    expect(fixture.nativeElement.querySelector('#btnValidar').disabled).toBe(true);
    expect(fixture.nativeElement.querySelector('#estudianteId').disabled).toBe(true);

    respuesta.next({ id: 1 });
    respuesta.complete();
    await fixture.whenStable();

    expect(boton.disabled).toBe(false);
    expect(component.mensaje).toBe('Préstamo registrado correctamente.');
    expect(fixture.nativeElement.querySelector('#resultado').textContent).toContain('Préstamo registrado correctamente.');
  });

  it('mantiene la respuesta booleana de validación para un préstamo rechazado', () => {
    servicio.validarPrestamo.mockReturnValue(of(false));

    component.validarPrestamo();

    expect(component.disponible).toBe(false);
    expect(component.mensaje).toBe('El préstamo no puede realizarse.');
    expect(component.procesando).toBe(false);
  });

  it.each([0, -1, 1.5, null, Number.NaN, Number.POSITIVE_INFINITY])(
    'rechaza el ID de estudiante inválido %s antes de enviar solicitudes', (valor) => {
      component.estudianteId = valor;

      component.registrarPrestamo();
      component.validarPrestamo();

      expect(servicio.registrarPrestamo).not.toHaveBeenCalled();
      expect(servicio.validarPrestamo).not.toHaveBeenCalled();
      expect(component.mensaje).toContain('números enteros positivos');
    }
  );

  it.each([0, -1, 1.5, null])('rechaza el ID de libro inválido %s', (valor) => {
    component.libroId = valor;

    component.registrarPrestamo();
    component.validarPrestamo();

    expect(servicio.registrarPrestamo).not.toHaveBeenCalled();
    expect(servicio.validarPrestamo).not.toHaveBeenCalled();
  });

  it('rechaza un campo borrado desde el formulario', async () => {
    const entrada = fixture.nativeElement.querySelector('#estudianteId') as HTMLInputElement;
    entrada.value = '';
    entrada.dispatchEvent(new Event('input'));
    await fixture.whenStable();

    fixture.nativeElement.querySelector('#btnRegistrar').click();

    expect(servicio.registrarPrestamo).not.toHaveBeenCalled();
    expect(component.mensaje).toContain('números enteros positivos');
  });
});
