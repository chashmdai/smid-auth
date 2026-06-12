package cl.smid.auth.dominio.puerto.salida;

import cl.smid.auth.dominio.modelo.Usuario;
import java.util.Optional;

/**
 * Puerto de salida hacia la persistencia de usuarios. El dominio lo define; la
 * infraestructura lo implementa con JPA. El dominio ignora como se almacenan los
 * datos.
 */
public interface UsuarioRepositorio {

    /** Busca por email (normalizado) resolviendo unidad, sede y roles en una sola carga. */
    Optional<Usuario> buscarPorEmailConJerarquia(String emailNormalizado);

    /**
     * Busca por id interno resolviendo unidad, sede y roles. Usado al refrescar:
     * la sesion persiste el id interno del usuario, no su alt_key.
     */
    Optional<Usuario> buscarPorIdConJerarquia(Long id);
}
