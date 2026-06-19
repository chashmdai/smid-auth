package cl.smid.auth.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Proyeccion publica de Sede. Expone alt_key, jamas el id interno (2.2).
 * DTO distinto de la entidad: las entidades JPA no se serializan a la API (2.9).
 */
@Schema(description = "Sede publica asociada al usuario autenticado.")
public record SedeDTO(
        @Schema(description = "Identificador publico opaco de la sede.", example = "1f6d2a95-9e5f-4d98-bcb0-1e7d0f8f1a10")
        String altKey,

        @Schema(description = "Nombre de presentacion de la sede.", example = "Sede Central")
        String nombre,

        @Schema(description = "Codigo institucional estable de la sede.", example = "CENTRAL")
        String codigo
) {}
