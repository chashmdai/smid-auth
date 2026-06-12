package cl.smid.auth.api.dto;

import java.util.List;

/**
 * Proyeccion publica del usuario autenticado, anidada en la respuesta de login
 * (contrato 3.4). Solo identificadores opacos y datos de presentacion.
 */
public record UsuarioDTO(
        String altKey,
        String nombres,
        String apellidos,
        SedeDTO sede,
        UnidadDTO unidad,
        List<String> roles,
        String alcance
) {}
