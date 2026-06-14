package cl.smid.auth.dominio.modelo;

import java.util.List;

/**
 * Contexto de sesion corporativa del SOLICITANTE de una operacion: los atributos
 * de segmentacion territorial que viajan en el JWT validado (Nucleo 2.3/2.4).
 * Es un modelo de dominio puro; el caso de uso lo recibe ya validado y decide la
 * visibilidad con {@link #puedeVer}.
 *
 * @param altKeyUsuario alt_key del usuario en curso (claim 'sub')
 * @param idSede        alt_key de la sede del solicitante (claim 'idSede')
 * @param idUnidad      alt_key de la unidad del solicitante (claim 'idUnidad')
 * @param alcance       alcance territorial efectivo (claim 'alcance')
 * @param roles         codigos de rol (claim 'roles')
 */
public record ContextoSesion(
        String altKeyUsuario,
        String idSede,
        String idUnidad,
        Alcance alcance,
        List<String> roles
) {
    public ContextoSesion {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }

    /**
     * Aplica el filtro territorial estandar (Nucleo 2.3) sobre un recurso ubicado
     * en la sede/unidad indicadas:
     *   NACIONAL  -> ve todo;
     *   SEDE      -> solo recursos de su misma sede;
     *   UNIDAD    -> solo recursos de su misma unidad.
     * La comparacion es por alt_key (identidad publica), nunca por id interno.
     */
    public boolean puedeVer(String idSedeObjetivo, String idUnidadObjetivo) {
        if (alcance == null) return false;
        return switch (alcance) {
            case NACIONAL -> true;
            case SEDE     -> idSede != null && idSede.equals(idSedeObjetivo);
            case UNIDAD   -> idUnidad != null && idUnidad.equals(idUnidadObjetivo);
        };
    }
}
