package cl.smid.auth.api;

import cl.smid.auth.api.dto.*;
import cl.smid.auth.dominio.modelo.Sede;
import cl.smid.auth.dominio.modelo.Unidad;
import cl.smid.auth.dominio.modelo.Usuario;
import cl.smid.auth.dominio.puerto.entrada.ResultadoAutenticacion;
import org.springframework.stereotype.Component;

/**
 * Traduce el resultado del dominio al DTO publico. Vive en la capa api porque es
 * responsable del contrato externo; garantiza que ningun id interno ni entidad
 * JPA se filtre en la respuesta (2.2 / 2.9).
 */
@Component
public class MapeadorRespuesta {

    public AuthResponse aAuthResponse(ResultadoAutenticacion r) {
        return new AuthResponse(
                r.accessToken(),
                r.refreshToken(),
                r.expiraEnSeg(),
                aUsuarioDTO(r.usuario())
        );
    }

    public UsuarioDTO aUsuarioDTO(Usuario u) {
        return new UsuarioDTO(
                u.altKey(),
                u.nombres(),
                u.apellidos(),
                aSedeDTO(u.sede()),
                aUnidadDTO(u.unidad()),
                u.codigosDeRol(),
                u.alcanceEfectivo().name()
        );
    }

    private SedeDTO aSedeDTO(Sede s) {
        if (s == null) return null;
        return new SedeDTO(s.altKey(), s.nombre(), s.codigo());
    }

    private UnidadDTO aUnidadDTO(Unidad un) {
        if (un == null) return null;
        return new UnidadDTO(un.altKey(), un.nombre(), un.tipo() != null ? un.tipo().name() : null);
    }
}
