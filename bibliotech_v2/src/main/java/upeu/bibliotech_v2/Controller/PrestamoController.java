package upeu.bibliotech_v2.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import upeu.bibliotech_v2.model.Prestamo;
import upeu.bibliotech_v2.service.PrestamoService;

@RestController
@RequestMapping("/api/prestamos")
@CrossOrigin(origins = "http://localhost:4200")
public class PrestamoController {
    private final PrestamoService prestamoService;

    public PrestamoController(PrestamoService prestamoService) {
        this.prestamoService = prestamoService;
    }

    @GetMapping("/validar")
    public ResponseEntity<Boolean> validarPrestamo(
            @RequestParam Long estudianteId,
            @RequestParam Long libroId) {

        boolean resultado =
                prestamoService.puedePrestar(estudianteId, libroId);

        return ResponseEntity.ok(resultado);
    }

    @PostMapping
    public ResponseEntity<Prestamo> registrarPrestamo(
            @RequestParam Long estudianteId,
            @RequestParam Long libroId) {

        Prestamo prestamo =
                prestamoService.registrarPrestamo(estudianteId, libroId);

        return ResponseEntity.ok(prestamo);
    }
}
