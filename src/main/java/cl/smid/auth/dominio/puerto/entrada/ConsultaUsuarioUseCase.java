package cl.smid.auth.dominio.puerto.entrada;

import cl.smid.auth.dominio.modelo.ContextoSesion;
import cl.smid.auth.dominio.modelo.Usuario;

/**
 * Puerto de entrada (caso de uso) para la consulta de un usuario por su alt_key,
 * aplicando el alcance territorial del solicitante. Lo consume el controlador
 * REST; la implementacion vive en el dominio (POJO).
 */
public interface ConsultaUsuarioUseCase {

    /**
     * Devuelve el usuario identificado por {@code altKey} si existe, esta vigente
     * y cae dentro del alcance territorial del {@code solicitante}.
     *
     * @throws cl.smid.auth.dominio.excepcion.UsuarioNoEncontradoException
     *         si no existe, no esta vigente o esta fuera de alcance (404; causa
     *         indistinguible, no se revela la existencia).
     */
    Usuario consultarPorAltKey(String altKey, ContextoSesion solicitante);
}
