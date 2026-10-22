package upeu.bibliotech_v2.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
import upeu.bibliotech_v2.exception.LimitePrestamosException;
import upeu.bibliotech_v2.model.Estudiante;
import upeu.bibliotech_v2.model.Libro;
import upeu.bibliotech_v2.model.Prestamo;
import upeu.bibliotech_v2.repository.EstudianteRepository;
import upeu.bibliotech_v2.repository.LibroRepository;
import upeu.bibliotech_v2.repository.PrestamoRepository;

import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class PrestamoIntegrationTest {
    @Autowired private PrestamoService service;
    @Autowired private EstudianteRepository estudiantes;
    @Autowired private LibroRepository libros;
    @Autowired private PrestamoRepository prestamos;
    @Autowired private TransactionTemplate transaction;

    private Estudiante estudiante;
    private Libro libro;

    @BeforeEach
    void prepararDatos() {
        prestamos.deleteAll();
        libros.deleteAll();
        estudiantes.deleteAll();
        estudiante = estudiantes.save(new Estudiante("Estudiante de prueba", "ACTIVO"));
        libro = libros.save(new Libro("Integración continua", "BiblioTech", 5));
    }

    @Test
    void cuentaSoloActivosDelEstudianteSinDistinguirMayusculas() {
        crearPrestamo(estudiante, "ACTIVO");
        crearPrestamo(estudiante, "activo");
        crearPrestamo(estudiante, "DEVUELTO");
        Estudiante otro = estudiantes.save(new Estudiante("Otro estudiante", "ACTIVO"));
        crearPrestamo(otro, "ACTIVO");

        assertEquals(2, prestamos.countByEstudianteIdAndEstadoIgnoreCase(estudiante.getId(), "ACTIVO"));
        assertTrue(service.puedePrestar(estudiante.getId(), libro.getId()));
    }

    @Test
    void permiteElTerceroYRechazaElCuartoSinDescontarOtroEjemplar() {
        crearPrestamo(estudiante, "ACTIVO");
        crearPrestamo(estudiante, "ACTIVO");

        Prestamo tercero = service.registrarPrestamo(estudiante.getId(), libro.getId());
        assertNotNull(tercero.getId());
        assertEquals("ACTIVO", tercero.getEstado());
        assertFalse(service.puedePrestar(estudiante.getId(), libro.getId()));
        assertThrows(LimitePrestamosException.class,
                () -> service.registrarPrestamo(estudiante.getId(), libro.getId()));

        assertEquals(3, prestamos.countByEstudianteIdAndEstadoIgnoreCase(estudiante.getId(), "ACTIVO"));
        assertEquals(4, libros.findById(libro.getId()).orElseThrow().getEjemplaresDisponibles());
    }

    @Test
    void devolverUnPrestamoLiberaCupo() {
        Prestamo devuelto = crearPrestamo(estudiante, "ACTIVO");
        crearPrestamo(estudiante, "ACTIVO");
        crearPrestamo(estudiante, "ACTIVO");
        assertFalse(service.puedePrestar(estudiante.getId(), libro.getId()));

        devuelto.setEstado("DEVUELTO");
        prestamos.save(devuelto);
        assertTrue(service.puedePrestar(estudiante.getId(), libro.getId()));
        service.registrarPrestamo(estudiante.getId(), libro.getId());

        assertEquals(3, prestamos.countByEstudianteIdAndEstadoIgnoreCase(estudiante.getId(), "ACTIVO"));
        assertEquals(4, prestamos.count());
    }

    @Test
    void reviertePrestamoEInventarioSiLaTransaccionFalla() {
        assertThrows(IllegalStateException.class, () -> transaction.executeWithoutResult(status -> {
            service.registrarPrestamo(estudiante.getId(), libro.getId());
            throw new IllegalStateException("Fallo controlado antes del commit");
        }));

        assertEquals(0, prestamos.count());
        assertEquals(5, libros.findById(libro.getId()).orElseThrow().getEjemplaresDisponibles());
    }

    @Test
    void dosSolicitudesSimultaneasNoSuperanElLimite() throws Exception {
        crearPrestamo(estudiante, "ACTIVO");
        crearPrestamo(estudiante, "ACTIVO");
        CountDownLatch preparadas = new CountDownLatch(2);
        CountDownLatch salida = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var solicitud = (java.util.concurrent.Callable<Boolean>) () -> {
                preparadas.countDown();
                if (!salida.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("No se inició la prueba concurrente");
                }
                try {
                    service.registrarPrestamo(estudiante.getId(), libro.getId());
                    return true;
                } catch (LimitePrestamosException expected) {
                    return false;
                }
            };
            var primera = executor.submit(solicitud);
            var segunda = executor.submit(solicitud);
            boolean listas = preparadas.await(5, TimeUnit.SECONDS);
            salida.countDown();
            assertTrue(listas);
            boolean exitoPrimera = primera.get(15, TimeUnit.SECONDS);
            boolean exitoSegunda = segunda.get(15, TimeUnit.SECONDS);
            assertNotEquals(exitoPrimera, exitoSegunda, "Solo una solicitud debe obtener el último cupo");
        }

        assertEquals(3, prestamos.countByEstudianteIdAndEstadoIgnoreCase(estudiante.getId(), "ACTIVO"));
        assertEquals(4, libros.findById(libro.getId()).orElseThrow().getEjemplaresDisponibles());
    }

    private Prestamo crearPrestamo(Estudiante titular, String estado) {
        return prestamos.save(new Prestamo(titular, libro, LocalDate.now(), estado));
    }
}
