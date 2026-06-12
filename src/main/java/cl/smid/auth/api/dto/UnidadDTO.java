package cl.smid.auth.api.dto;

/** Proyeccion publica de Unidad (solo alt_key). */
public record UnidadDTO(
        String altKey,
        String nombre,
        String tipo
) {}
