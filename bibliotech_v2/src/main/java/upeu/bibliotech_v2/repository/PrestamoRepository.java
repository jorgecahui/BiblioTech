package upeu.bibliotech_v2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import upeu.bibliotech_v2.model.Prestamo;

public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {
    long countByEstudianteIdAndEstadoIgnoreCase(Long estudianteId, String estado);
}
