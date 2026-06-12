package cl.smid.auth.dominio.excepcion;

/**
 * Login fallido. Se lanza de forma deliberadamente indistinguible tanto si el
 * usuario no existe como si la contrasena es incorrecta o la cuenta no esta
 * vigente: el llamador no puede deducir cual de las tres causas ocurrio
 * (anti-enumeracion, regla 3.5 / Nucleo 2.4 -> AUTZ-001).
 */
public class CredencialesInvalidasException extends AuthException {
    public CredencialesInvalidasException() {
        super(CodigoError.CREDENCIALES_INVALIDAS);
    }
}
