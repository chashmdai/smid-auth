package cl.smid.auth.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Proyeccion publica de Unidad (solo alt_key). */
@Schema(description = "Unidad publica asociada al usuario autenticado.")
public record UnidadDTO(
        @Schema(description = "Identificador publico opaco de la unidad.", example = "d612ee84-62ea-4eaa-9376-73d19f17b8b5")
        String altKey,

        @Schema(description = "Nombre de presentacion de la unidad.", example = "Unidad de Proteccion y Representacion Judicial")
        String nombre,

        @Schema(description = "Tipo funcional de unidad.", example = "UPRJ")
        String tipo
) {}
