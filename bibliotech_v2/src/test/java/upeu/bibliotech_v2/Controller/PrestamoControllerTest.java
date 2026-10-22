package upeu.bibliotech_v2.Controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import upeu.bibliotech_v2.exception.LimitePrestamosException;
import upeu.bibliotech_v2.model.Prestamo;
import upeu.bibliotech_v2.service.PrestamoService;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PrestamoControllerTest {
    private PrestamoService service;
    private MockMvc mvc;

    @BeforeEach
    void configurarApi() {
        service = mock(PrestamoService.class);
        mvc = MockMvcBuilders.standaloneSetup(new PrestamoController(service))
                .setControllerAdvice(new PrestamoExceptionHandler()).build();
    }

    @Test
    void mantieneValidacionBooleanaParaElCliente() throws Exception {
        when(service.puedePrestar(1L, 2L)).thenReturn(false);

        mvc.perform(get("/api/prestamos/validar").param("estudianteId", "1").param("libroId", "2"))
                .andExpect(status().isOk()).andExpect(content().string("false"));
    }

    @Test
    void informaConflictoYDetalleCuandoSeAlcanzaElLimite() throws Exception {
        when(service.registrarPrestamo(1L, 2L)).thenThrow(new LimitePrestamosException(3, 3));

        mvc.perform(post("/api/prestamos").header("Origin", "http://localhost:4200")
                        .param("estudianteId", "1").param("libroId", "2"))
                .andExpect(status().isConflict())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Límite de préstamos alcanzado"))
                .andExpect(jsonPath("$.prestamosActivos").value(3))
                .andExpect(jsonPath("$.limite").value(3))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Debe devolver un libro")));
    }

    @Test
    void registraUnPrestamoAutorizado() throws Exception {
        Prestamo prestamo = new Prestamo();
        prestamo.setId(10L);
        prestamo.setEstado("ACTIVO");
        when(service.registrarPrestamo(1L, 2L)).thenReturn(prestamo);

        mvc.perform(post("/api/prestamos").param("estudianteId", "1").param("libroId", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
    }
}
