package cl.smid.auth.dominio.modelo;

/**
 * Sede: raiz de la jerarquia territorial. Modelo de dominio puro (sin anotaciones
 * de persistencia). 'altKey' es el unico identificador que cruza la frontera del
 * servicio (2.2); 'id' es interno y puede ser null en el dominio. 'codigo' es un
 * identificador institucional estable, no necesariamente una region.
 */
public record Sede(
        Long id,
        String altKey,
        String nombre,
        String codigo,
        boolean vigente
) {}
