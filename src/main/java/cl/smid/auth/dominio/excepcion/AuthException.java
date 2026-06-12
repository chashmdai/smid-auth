package cl.smid.auth.dominio.excepcion;

/**
 * Excepcion base del dominio de Identidad. Transporta un {@link CodigoError} del
 * catalogo AUTZ-xxx, lo que permite al manejador global construir el sobre de
 * error unificado sin acoplarse a tipos concretos de excepcion.
 */
public class AuthException extends RuntimeException {

    private final CodigoError codigoError;

    public AuthException(CodigoError codigoError) {
        super(codigoError.mensajePorDefecto());
        this.codigoError = codigoError;
    }

    public AuthException(CodigoError codigoError, String mensaje) {
        super(mensaje);
        this.codigoError = codigoError;
    }

    public CodigoError codigoError() {
        return codigoError;
    }
}
