package cl.smid.auth.api.dto;

/**
 * Proyeccion publica de Sede. Expone alt_key, jamas el id interno (2.2).
 * DTO distinto de la entidad: las entidades JPA no se serializan a la API (2.9).
 */
public record SedeDTO(
        String altKey,
        String nombre,
        String codigo
) {}
