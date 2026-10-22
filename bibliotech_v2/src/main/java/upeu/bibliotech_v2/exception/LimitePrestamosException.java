package upeu.bibliotech_v2.exception;

public class LimitePrestamosException extends RuntimeException {
    private final long prestamosActivos;
    private final int limite;

    public LimitePrestamosException(long prestamosActivos, int limite) {
        super("El estudiante tiene " + prestamosActivos + " préstamos activos. "
                + "El límite es " + limite + ". Debe devolver un libro para solicitar otro préstamo.");
        this.prestamosActivos = prestamosActivos;
        this.limite = limite;
    }

    public long getPrestamosActivos() {
        return prestamosActivos;
    }

    public int getLimite() {
        return limite;
    }
}
