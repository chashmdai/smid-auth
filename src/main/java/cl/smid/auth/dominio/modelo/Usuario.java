package cl.smid.auth.dominio.modelo;

import java.util.List;
import java.util.Objects;

/**
 * Usuario interno (profesional). Modelo de dominio puro: agrega su unidad, su
 * sede (via unidad) y sus roles, y concentra las reglas de identidad que no
 * dependen de infraestructura.
 */
public record Usuario(
        Long id,
        String altKey,
        String rut,
        String nombres,
        String apellidos,
        String email,
        String passwordHash,
        String cargo,
        boolean vigente,
        Unidad unidad,
        List<Rol> roles
) {
    public Usuario {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }

    /** Nombre completo para el claim 'nombre' del token y la proyeccion de login. */
    public String nombreCompleto() {
        return (nombres + " " + apellidos).trim();
    }

    /** Sede del usuario, derivada de su unidad. */
    public Sede sede() {
        return unidad != null ? unidad.sede() : null;
    }

    /**
     * Alcance territorial efectivo: el maximo de los alcances de sus roles.
     * Sin roles, devuelve UNIDAD (opcion por defecto mas restrictiva y segura).
     */
    public Alcance alcanceEfectivo() {
        return roles.stream()
                .map(Rol::alcance)
                .filter(Objects::nonNull)
                .reduce(Alcance.UNIDAD, Alcance::maximo);
    }

    /** Codigos de rol para el claim 'roles' (RBAC en servicios downstream). */
    public List<String> codigosDeRol() {
        return roles.stream().map(Rol::codigo).toList();
    }
}
