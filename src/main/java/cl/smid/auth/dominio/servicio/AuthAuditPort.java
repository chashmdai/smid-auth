package cl.smid.auth.dominio.servicio;

import cl.smid.auth.dominio.modelo.Usuario;

/**
 * Costura de trazabilidad sincrona (decision D4). El servicio de Identidad
 * registra cada evento de sesion en el log del contexto empresarial de forma
 * ligera y sincrona; la publicacion de eventos de dominio al broker asincrono
 * (RabbitMQ) queda reservada para las MUTACIONES de los servicios de negocio
 * posteriores (6.2/6.3), no para la autenticacion.
 *
 * Definir esto como un puerto deja la estructura preparada: el dia que se quiera
 * emitir 'sesion.iniciada' a una cola, se agrega un adaptador sin tocar el nucleo.
 */
public interface AuthAuditPort {

    void loginExitoso(Usuario usuario);

    /** Login fallido. Se registra el identificador presentado, jamas la contrasena. */
    void loginFallido(String identificadorPresentado);

    void sesionRenovada(Usuario usuario);

    void sesionCerrada(String usuarioAltKey);

    /** Senal de seguridad: reuso de un refresco ya rotado -> familia revocada. */
    void reusoRefreshDetectado(String usuarioAltKey, String familia);
}
