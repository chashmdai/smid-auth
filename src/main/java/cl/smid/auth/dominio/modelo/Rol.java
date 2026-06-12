package cl.smid.auth.dominio.modelo;

/** Rol del catalogo, con su alcance territorial asociado. Modelo de dominio puro. */
public record Rol(
        Long id,
        String codigo,
        String nombre,
        Alcance alcance
) {}
