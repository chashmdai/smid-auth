package cl.smid.auth.dominio.puerto.entrada;

import cl.smid.auth.dominio.modelo.Usuario;

/**
 * Resultado de un login o refresh exitoso: el par de tokens emitido junto al
 * usuario autenticado. Objeto de transporte interno entre el dominio y la capa
 * api; esta ultima lo proyecta al DTO publico de respuesta.
 *
 * @param accessToken     JWT HS256 firmado con los claims del Nucleo 2.4
 * @param refreshToken    token opaco de refresco (valor en claro, unica vez)
 * @param expiraEnSeg     vigencia del access token, en segundos
 * @param usuario         usuario autenticado, con unidad/sede/roles resueltos
 */
public record ResultadoAutenticacion(
        String accessToken,
        String refreshToken,
        long expiraEnSeg,
        Usuario usuario
) {}
