package upeu.bibliotech_v2.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import upeu.bibliotech_v2.exception.LimitePrestamosException;
import upeu.bibliotech_v2.model.Estudiante;
import upeu.bibliotech_v2.model.Libro;
import upeu.bibliotech_v2.model.Prestamo;
import upeu.bibliotech_v2.repository.EstudianteRepository;
import upeu.bibliotech_v2.repository.LibroRepository;
import upeu.bibliotech_v2.repository.PrestamoRepository;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrestamoServiceTest {

    private static final Long ESTUDIANTE_ID = 7L;
    private static final Long LIBRO_ID = 11L;

    @Mock
    private EstudianteRepository estudianteRepository;

    @Mock
    private LibroRepository libroRepository;

    @Mock
    private PrestamoRepository prestamoRepository;

    @InjectMocks
    private PrestamoService service;

    @ParameterizedTest(name = "Permite validar con {0} préstamos activos")
    @ValueSource(longs = {0, 1, 2})
    void permiteValidarMientrasNoAlcanzaElLimite(long activos) {
        when(estudianteRepository.findById(ESTUDIANTE_ID))
                .thenReturn(Optional.of(estudianteActivo()));
        when(prestamoRepository.countByEstudianteIdAndEstadoIgnoreCase(ESTUDIANTE_ID, "ACTIVO"))
                .thenReturn(activos);
        when(libroRepository.findById(LIBRO_ID))
                .thenReturn(Optional.of(libroDisponible()));

        assertTrue(service.puedePrestar(ESTUDIANTE_ID, LIBRO_ID));

        verify(prestamoRepository).countByEstudianteIdAndEstadoIgnoreCase(ESTUDIANTE_ID, "ACTIVO");
        verify(prestamoRepository, never()).save(any(Prestamo.class));
    }

    @ParameterizedTest(name = "Rechaza validar con {0} préstamos activos")
    @ValueSource(longs = {3, 4, 8})
    void rechazaValidarDesdeElTercerPrestamoActivo(long activos) {
        when(estudianteRepository.findById(ESTUDIANTE_ID))
                .thenReturn(Optional.of(estudianteActivo()));
        when(prestamoRepository.countByEstudianteIdAndEstadoIgnoreCase(ESTUDIANTE_ID, "ACTIVO"))
                .thenReturn(activos);
        // El libro válido hace que una regresión del límite falle en assertFalse.
        // Es lenient porque el comportamiento correcto no consulta el libro.
        lenient().when(libroRepository.findById(LIBRO_ID))
                .thenReturn(Optional.of(libroDisponible()));

        assertFalse(service.puedePrestar(ESTUDIANTE_ID, LIBRO_ID));

        verifyNoInteractions(libroRepository);
        verify(prestamoRepository, never()).save(any(Prestamo.class));
    }

    @Test
    void registraElTercerPrestamoYDescuentaUnEjemplar() {
        Estudiante estudiante = estudianteActivo();
        Libro libro = libroDisponible();
        when(estudianteRepository.findByIdForUpdate(ESTUDIANTE_ID))
                .thenReturn(Optional.of(estudiante));
        when(prestamoRepository.countByEstudianteIdAndEstadoIgnoreCase(ESTUDIANTE_ID, "ACTIVO"))
                .thenReturn(2L);
        when(libroRepository.findByIdForUpdate(LIBRO_ID))
                .thenReturn(Optional.of(libro));
        when(prestamoRepository.save(any(Prestamo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        LocalDate fechaAntes = LocalDate.now();

        Prestamo resultado = service.registrarPrestamo(ESTUDIANTE_ID, LIBRO_ID);

        ArgumentCaptor<Prestamo> captor = ArgumentCaptor.forClass(Prestamo.class);
        verify(prestamoRepository).save(captor.capture());
        Prestamo guardado = captor.getValue();
        assertSame(guardado, resultado);
        assertSame(estudiante, guardado.getEstudiante());
        assertSame(libro, guardado.getLibro());
        assertEquals("ACTIVO", guardado.getEstado());
        assertFalse(guardado.getFechaPrestamo().isBefore(fechaAntes));
        assertFalse(guardado.getFechaPrestamo().isAfter(LocalDate.now()));
        assertEquals(4, libro.getEjemplaresDisponibles());
        verify(libroRepository).save(libro);
        verify(estudianteRepository).findByIdForUpdate(ESTUDIANTE_ID);
        verify(libroRepository).findByIdForUpdate(LIBRO_ID);
    }

    @ParameterizedTest(name = "Rechaza registrar cuando ya hay {0} préstamos activos")
    @ValueSource(longs = {3, 4, 8})
    void rechazaElCuartoPrestamoSinModificarInventario(long activos) {
        when(estudianteRepository.findByIdForUpdate(ESTUDIANTE_ID))
                .thenReturn(Optional.of(estudianteActivo()));
        when(prestamoRepository.countByEstudianteIdAndEstadoIgnoreCase(ESTUDIANTE_ID, "ACTIVO"))
                .thenReturn(activos);

        LimitePrestamosException error = assertThrows(LimitePrestamosException.class,
                () -> service.registrarPrestamo(ESTUDIANTE_ID, LIBRO_ID));

        assertEquals(activos, error.getPrestamosActivos());
        assertEquals(3, error.getLimite());
        assertEquals(3, PrestamoService.MAX_PRESTAMOS_ACTIVOS);
        verifyNoInteractions(libroRepository);
        verify(prestamoRepository, never()).save(any(Prestamo.class));
    }

    @Test
    void rechazaValidacionParaEstudianteSuspendido() {
        Estudiante suspendido = new Estudiante("Ana", "SUSPENDIDO");
        when(estudianteRepository.findById(ESTUDIANTE_ID))
                .thenReturn(Optional.of(suspendido));

        assertFalse(service.puedePrestar(ESTUDIANTE_ID, LIBRO_ID));

        verifyNoInteractions(prestamoRepository, libroRepository);
    }

    @Test
    void rechazaRegistroParaEstudianteSuspendidoSinGuardar() {
        Estudiante suspendido = new Estudiante("Ana", "SUSPENDIDO");
        when(estudianteRepository.findByIdForUpdate(ESTUDIANTE_ID))
                .thenReturn(Optional.of(suspendido));

        assertThrows(RuntimeException.class,
                () -> service.registrarPrestamo(ESTUDIANTE_ID, LIBRO_ID));

        verifyNoInteractions(prestamoRepository, libroRepository);
    }

    @Test
    void rechazaValidacionCuandoNoHayEjemplaresDisponibles() {
        when(estudianteRepository.findById(ESTUDIANTE_ID))
                .thenReturn(Optional.of(estudianteActivo()));
        when(prestamoRepository.countByEstudianteIdAndEstadoIgnoreCase(ESTUDIANTE_ID, "ACTIVO"))
                .thenReturn(2L);
        when(libroRepository.findById(LIBRO_ID))
                .thenReturn(Optional.of(new Libro("Libro agotado", "Autor", 0)));

        assertFalse(service.puedePrestar(ESTUDIANTE_ID, LIBRO_ID));

        verify(prestamoRepository, never()).save(any(Prestamo.class));
    }

    @Test
    void rechazaRegistroCuandoNoHayEjemplaresSinModificarInventario() {
        Libro agotado = new Libro("Libro agotado", "Autor", 0);
        when(estudianteRepository.findByIdForUpdate(ESTUDIANTE_ID))
                .thenReturn(Optional.of(estudianteActivo()));
        when(prestamoRepository.countByEstudianteIdAndEstadoIgnoreCase(ESTUDIANTE_ID, "ACTIVO"))
                .thenReturn(2L);
        when(libroRepository.findByIdForUpdate(LIBRO_ID))
                .thenReturn(Optional.of(agotado));

        assertThrows(RuntimeException.class,
                () -> service.registrarPrestamo(ESTUDIANTE_ID, LIBRO_ID));

        assertEquals(0, agotado.getEjemplaresDisponibles());
        verify(libroRepository, never()).save(any(Libro.class));
        verify(prestamoRepository, never()).save(any(Prestamo.class));
    }

    @Test
    void informaCuandoElEstudianteNoExisteAlValidar() {
        when(estudianteRepository.findById(ESTUDIANTE_ID)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> service.puedePrestar(ESTUDIANTE_ID, LIBRO_ID));

        verifyNoInteractions(prestamoRepository, libroRepository);
    }

    @Test
    void informaCuandoElEstudianteNoExisteAlRegistrar() {
        when(estudianteRepository.findByIdForUpdate(ESTUDIANTE_ID)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> service.registrarPrestamo(ESTUDIANTE_ID, LIBRO_ID));

        verifyNoInteractions(prestamoRepository, libroRepository);
    }

    @Test
    void informaCuandoElLibroNoExisteAlValidar() {
        when(estudianteRepository.findById(ESTUDIANTE_ID))
                .thenReturn(Optional.of(estudianteActivo()));
        when(prestamoRepository.countByEstudianteIdAndEstadoIgnoreCase(ESTUDIANTE_ID, "ACTIVO"))
                .thenReturn(2L);
        when(libroRepository.findById(LIBRO_ID)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> service.puedePrestar(ESTUDIANTE_ID, LIBRO_ID));

        verify(prestamoRepository, never()).save(any(Prestamo.class));
    }

    @Test
    void informaCuandoElLibroNoExisteAlRegistrar() {
        when(estudianteRepository.findByIdForUpdate(ESTUDIANTE_ID))
                .thenReturn(Optional.of(estudianteActivo()));
        when(prestamoRepository.countByEstudianteIdAndEstadoIgnoreCase(ESTUDIANTE_ID, "ACTIVO"))
                .thenReturn(2L);
        when(libroRepository.findByIdForUpdate(LIBRO_ID)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> service.registrarPrestamo(ESTUDIANTE_ID, LIBRO_ID));

        verify(libroRepository, never()).save(any(Libro.class));
        verify(prestamoRepository, never()).save(any(Prestamo.class));
    }

    private Estudiante estudianteActivo() {
        Estudiante estudiante = new Estudiante("Ana", "ACTIVO");
        estudiante.setId(ESTUDIANTE_ID);
        return estudiante;
    }

    private Libro libroDisponible() {
        Libro libro = new Libro("Pruebas de software", "Autor", 5);
        libro.setId(LIBRO_ID);
        return libro;
    }
}
