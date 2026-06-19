package cl.smid.auth.api.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Proyeccion publica del usuario autenticado, anidada en la respuesta de login
 * (contrato 3.4). Solo identificadores opacos y datos de presentacion.
 */
@Schema(description = "Perfil publico del usuario autenticado.")
public record UsuarioDTO(
        @Schema(description = "Identificador publico opaco del usuario.", example = "8f3b2bb8-42bb-4b89-a72a-5b58c33f5d3a")
        String altKey,

        @Schema(description = "Nombres de presentacion del usuario.", example = "Admin")
        String nombres,

        @Schema(description = "Apellidos de presentacion del usuario.", example = "SMID")
        String apellidos,

        @Schema(description = "Sede publica del usuario.")
        SedeDTO sede,

        @Schema(description = "Unidad publica del usuario.")
        UnidadDTO unidad,

        @ArraySchema(schema = @Schema(description = "Codigo estable de rol.", example = "ADMIN_NACIONAL"))
        List<String> roles,

        @Schema(description = "Alcance territorial maximo del usuario.", allowableValues = {"UNIDAD", "SEDE", "NACIONAL"},
                example = "NACIONAL")
        String alcance
) {}
