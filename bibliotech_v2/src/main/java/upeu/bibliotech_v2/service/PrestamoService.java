package upeu.bibliotech_v2.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import upeu.bibliotech_v2.exception.LimitePrestamosException;
import upeu.bibliotech_v2.model.Estudiante;
import upeu.bibliotech_v2.model.Libro;
import upeu.bibliotech_v2.model.Prestamo;
import upeu.bibliotech_v2.repository.EstudianteRepository;
import upeu.bibliotech_v2.repository.LibroRepository;
import upeu.bibliotech_v2.repository.PrestamoRepository;

import java.time.LocalDate;

@Service
public class PrestamoService {

    public static final int MAX_PRESTAMOS_ACTIVOS = 3;
    private static final String ACTIVO = "ACTIVO";

    private final EstudianteRepository estudianteRepository;
    private final LibroRepository libroRepository;
    private final PrestamoRepository prestamoRepository;

    public PrestamoService(
            EstudianteRepository estudianteRepository,
            LibroRepository libroRepository,
            PrestamoRepository prestamoRepository) {

        this.estudianteRepository = estudianteRepository;
        this.libroRepository = libroRepository;
        this.prestamoRepository = prestamoRepository;
    }

    @Transactional(readOnly = true)
    public boolean puedePrestar(Long estudianteId, Long libroId) {

        Estudiante estudiante = estudianteRepository.findById(estudianteId)
                .orElseThrow(() ->
                        new RuntimeException("Estudiante no existe"));

        if (!ACTIVO.equalsIgnoreCase(estudiante.getEstado())) {
            return false;
        }

        long prestamosActivos =
                prestamoRepository.countByEstudianteIdAndEstadoIgnoreCase(
                        estudianteId, ACTIVO);

        if (prestamosActivos > MAX_PRESTAMOS_ACTIVOS) {
            return false;
        }

        Libro libro = libroRepository.findById(libroId)
                .orElseThrow(() ->
                        new RuntimeException("Libro no existe"));

        if (libro.getEjemplaresDisponibles() <= 0) {
            return false;
        }

        return true;
    }

    @Transactional
    public Prestamo registrarPrestamo(Long estudianteId, Long libroId) {
        // Serializa los registros del mismo estudiante antes de consultar su cupo.
        Estudiante estudiante = estudianteRepository.findByIdForUpdate(estudianteId)
                .orElseThrow(() -> new RuntimeException("Estudiante no existe"));

        if (!ACTIVO.equalsIgnoreCase(estudiante.getEstado())) {
            throw new RuntimeException("Préstamo no autorizado: estudiante inactivo");
        }

        long prestamosActivos = prestamoRepository.countByEstudianteIdAndEstadoIgnoreCase(
                estudianteId, ACTIVO);
        if (prestamosActivos >= MAX_PRESTAMOS_ACTIVOS) {
            throw new LimitePrestamosException(prestamosActivos, MAX_PRESTAMOS_ACTIVOS);
        }

        Libro libro = libroRepository.findByIdForUpdate(libroId)
                .orElseThrow(() -> new RuntimeException("Libro no existe"));
        if (libro.getEjemplaresDisponibles() <= 0) {
            throw new RuntimeException("Préstamo no autorizado: sin ejemplares disponibles");
        }

        Prestamo prestamo = new Prestamo(
                estudiante,
                libro,
                LocalDate.now(),
                ACTIVO
        );

        libro.setEjemplaresDisponibles(
                libro.getEjemplaresDisponibles() - 1
        );

        libroRepository.save(libro);

        return prestamoRepository.save(prestamo);
    }

}
