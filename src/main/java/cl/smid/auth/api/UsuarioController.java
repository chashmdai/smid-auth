package cl.smid.auth.api;

import cl.smid.auth.api.dto.UsuarioVerificacionDTO;
import cl.smid.auth.dominio.modelo.ContextoSesion;
import cl.smid.auth.dominio.modelo.Usuario;
import cl.smid.auth.dominio.puerto.entrada.ConsultaUsuarioUseCase;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador de entrada para la consulta de usuario por alt_key. Ruta INTERNA del
 * servicio (puerto 8081); via Gateway seria /api/usuarios/{altKey} (StripPrefix).
 * Es la cara de SOLO LECTURA que consume requerimientos-service para validar la
 * pertenencia de un profesional a una unidad.
 *
 * Autenticado: la cadena de seguridad exige un Bearer valido antes de llegar
 * aqui, de modo que 'solicitante' nunca es null. El controlador no tiene logica:
 * delega en el caso de uso, que aplica el filtro territorial.
 */
@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final ConsultaUsuarioUseCase consultaUsuario;

    public UsuarioController(ConsultaUsuarioUseCase consultaUsuario) {
        this.consultaUsuario = consultaUsuario;
    }

    @GetMapping("/{altKey}")
    public UsuarioVerificacionDTO porAltKey(@PathVariable String altKey,
                                            @AuthenticationPrincipal ContextoSesion solicitante) {
        Usuario u = consultaUsuario.consultarPorAltKey(altKey, solicitante);
        return new UsuarioVerificacionDTO(
                u.altKey(),
                u.unidad() != null ? u.unidad().altKey() : null,   // idUnidad (obligatorio)
                u.sede()   != null ? u.sede().altKey()   : null,   // idSede (extra)
                u.nombreCompleto(),
                u.codigosDeRol()
        );
    }
}
