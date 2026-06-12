package cl.smid.auth.dominio.puerto.salida;

import java.time.Instant;
import java.util.Optional;

/**
 * Puerto de salida para la lista de revocacion de tokens de refresco (3.5),
 * implementada con rotacion y deteccion de reuso por familia.
 */
public interface SesionRefreshRepositorio {

    /** Vista minima de una sesion de refresco persistida, para decidir su validez. */
    record SesionRefresh(
            Long id,
            Long idUsuario,
            String familia,
            Instant expiraEn,
            boolean revocada
    ) {}

    /** Persiste una nueva sesion de refresco (token ya hasheado). */
    void guardar(Long idUsuario, String tokenHash, String familia, Instant emitidoEn, Instant expiraEn);

    /** Recupera una sesion por el hash del token presentado. */
    Optional<SesionRefresh> buscarPorHash(String tokenHash);

    /** Marca una sesion individual como revocada con su motivo. */
    void revocar(Long idSesion, String motivo);

    /**
     * Revoca todas las sesiones activas de una familia (defensa ante replay:
     * si reaparece un refresco ya rotado, se corta la cadena completa).
     */
    void revocarFamilia(String familia, String motivo);
}
