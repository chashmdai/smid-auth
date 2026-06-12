package cl.smid.auth.dominio.puerto.entrada;

/**
 * Puerto de entrada (caso de uso) del dominio de Identidad. La capa api depende
 * de esta interfaz, nunca de la implementacion concreta: asi el controlador y la
 * logica de negocio quedan desacoplados (arquitectura hexagonal, D5).
 */
public interface AutenticacionUseCase {

    /**
     * Autentica por email + contrasena y emite un par de tokens nuevo.
     * @throws cl.smid.auth.dominio.excepcion.CredencialesInvalidasException
     *         si las credenciales no son validas (causa indistinguible).
     */
    ResultadoAutenticacion login(String email, String passwordPlano);

    /**
     * Renueva el access token a partir de un token de refresco vigente, rotando
     * el refresco (one-time use). Si se detecta reuso de un token ya rotado, se
     * revoca toda la familia de sesion.
     * @throws cl.smid.auth.dominio.excepcion.RefreshInvalidoException
     *         si el token de refresco no es utilizable.
     */
    ResultadoAutenticacion refrescar(String refreshTokenPlano);

    /** Revoca la sesion asociada al token de refresco (logout idempotente). */
    void logout(String refreshTokenPlano);
}
