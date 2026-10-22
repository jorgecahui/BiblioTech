package upeu.bibliotech_v2.Controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import upeu.bibliotech_v2.exception.LimitePrestamosException;

@RestControllerAdvice
public class PrestamoExceptionHandler {
    @ExceptionHandler(LimitePrestamosException.class)
    public ProblemDetail limiteAlcanzado(LimitePrestamosException exception) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, exception.getMessage());
        problema.setTitle("Límite de préstamos alcanzado");
        problema.setProperty("prestamosActivos", exception.getPrestamosActivos());
        problema.setProperty("limite", exception.getLimite());
        return problema;
    }
}
