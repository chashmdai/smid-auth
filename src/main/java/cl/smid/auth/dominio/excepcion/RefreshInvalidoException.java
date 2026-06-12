package cl.smid.auth.dominio.excepcion;

/**
 * El token de refresco presentado no es utilizable: desconocido, vencido, ya
 * rotado (replay), revocado, o perteneciente a un usuario que dejo de estar
 * vigente. Indistinguible por diseno (AUTZ-002).
 */
public class RefreshInvalidoException extends AuthException {
    public RefreshInvalidoException() {
        super(CodigoError.REFRESH_INVALIDO);
    }
}
