package cl.smid.auth.dominio.excepcion;

/**
 * El usuario solicitado no existe, no esta vigente, o cae FUERA DEL ALCANCE
 * territorial del solicitante. Los tres casos son indistinguibles por diseno y
 * resuelven en 404 (no 403): no se revela la existencia del recurso (Nucleo 2.3).
 */
public class UsuarioNoEncontradoException extends AuthException {
    public UsuarioNoEncontradoException() {
        super(CodigoError.RECURSO_NO_ENCONTRADO);
    }
}
