package cl.smid.auth.dominio.modelo;

/** Unidad operativa perteneciente a una {@link Sede}. Modelo de dominio puro. */
public record Unidad(
        Long id,
        String altKey,
        Sede sede,
        String nombre,
        TipoUnidad tipo,
        boolean vigente
) {}
