package cl.smid.auth.infraestructura.auditoria;

import cl.smid.auth.dominio.modelo.Usuario;
import cl.smid.auth.dominio.servicio.AuthAuditPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Implementacion sincrona y ligera de la trazabilidad (decision D4): registra
 * cada evento de sesion en el log del contexto empresarial. Se registran
 * identificadores opacos (alt_key) y NUNCA contrasenas ni tokens.
 *
 * Costura preparada: cuando exista el servicio de Auditoria/Notificaciones, se
 * podra agregar un segundo adaptador de {@link AuthAuditPort} que publique a
 * RabbitMQ, sin tocar el nucleo. La autenticacion en si permanece sincrona; el
 * broker asincrono queda para las mutaciones de los servicios de negocio.
 */
@Component
public class AuditoriaLog implements AuthAuditPort {

    private static final Logger log = LoggerFactory.getLogger("AUDIT.SMID.AUTH");

    @Override
    public void loginExitoso(Usuario u) {
        log.info("login.exitoso usuario={} unidad={} alcance={}",
                u.altKey(),
                u.unidad() != null ? u.unidad().altKey() : "-",
                u.alcanceEfectivo());
    }

    @Override
    public void loginFallido(String identificadorPresentado) {
        // Se registra el identificador para deteccion de fuerza bruta, sin la clave.
        log.warn("login.fallido identificador={}", identificadorPresentado);
    }

    @Override
    public void sesionRenovada(Usuario u) {
        log.info("sesion.renovada usuario={}", u.altKey());
    }

    @Override
    public void sesionCerrada(String usuarioAltKey) {
        log.info("sesion.cerrada usuario={}", usuarioAltKey);
    }

    @Override
    public void reusoRefreshDetectado(String usuarioAltKey, String familia) {
        log.warn("seguridad.reuso_refresh usuario={} familia={} -> familia revocada",
                usuarioAltKey, familia);
    }
}
