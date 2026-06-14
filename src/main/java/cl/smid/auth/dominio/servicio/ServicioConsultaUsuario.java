package cl.smid.auth.dominio.servicio;

import cl.smid.auth.dominio.excepcion.UsuarioNoEncontradoException;
import cl.smid.auth.dominio.modelo.ContextoSesion;
import cl.smid.auth.dominio.modelo.Usuario;
import cl.smid.auth.dominio.puerto.entrada.ConsultaUsuarioUseCase;
import cl.smid.auth.dominio.puerto.salida.UsuarioRepositorio;

/**
 * Implementacion del caso de uso de consulta de usuario. POJO puro (sin Spring):
 * se cablea en DominioConfig. Concentra la regla de visibilidad territorial.
 *
 * Decision clave de seguridad (Nucleo 2.3): un usuario inexistente, no vigente o
 * fuera del alcance del solicitante producen EXACTAMENTE la misma excepcion ->
 * 404. No se distingue "no existe" de "existe pero no puedes verlo", para no
 * filtrar la existencia de personal de otras unidades/sedes por enumeracion.
 */
public class ServicioConsultaUsuario implements ConsultaUsuarioUseCase {

    private final UsuarioRepositorio usuarioRepositorio;

    public ServicioConsultaUsuario(UsuarioRepositorio usuarioRepositorio) {
        this.usuarioRepositorio = usuarioRepositorio;
    }

    @Override
    public Usuario consultarPorAltKey(String altKey, ContextoSesion solicitante) {
        Usuario objetivo = usuarioRepositorio.buscarPorAltKeyConJerarquia(altKey)
                .filter(Usuario::vigente)
                .orElseThrow(UsuarioNoEncontradoException::new);

        String idSedeObjetivo   = objetivo.sede()   != null ? objetivo.sede().altKey()   : null;
        String idUnidadObjetivo = objetivo.unidad() != null ? objetivo.unidad().altKey() : null;

        if (!solicitante.puedeVer(idSedeObjetivo, idUnidadObjetivo)) {
            // Fuera de alcance: se enmascara como 404 (no 403).
            throw new UsuarioNoEncontradoException();
        }
        return objetivo;
    }
}
