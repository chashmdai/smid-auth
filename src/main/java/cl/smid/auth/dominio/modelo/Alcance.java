package cl.smid.auth.dominio.modelo;

/**
 * Alcance territorial de autorizacion (Nucleo 2.3).
 * El alcance efectivo de un usuario es el MAXIMO de los alcances de sus roles:
 * NACIONAL > SEDE > UNIDAD. El orden de declaracion del enum (de menor a mayor)
 * codifica esa precedencia, de modo que {@link #maximo} se resuelve por ordinal.
 */
public enum Alcance {
    UNIDAD,
    SEDE,
    NACIONAL;

    /** Devuelve el alcance de mayor amplitud entre este y otro. */
    public Alcance maximo(Alcance otro) {
        if (otro == null) return this;
        return this.ordinal() >= otro.ordinal() ? this : otro;
    }
}
