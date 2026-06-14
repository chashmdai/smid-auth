package cl.smid.auth.api.dto;

import java.util.List;

/**
 * Proyeccion publica para GET /usuarios/{altKey}. Endpoint de verificacion de
 * PERTENENCIA (servicio-a-servicio): expone identidad e idUnidad, no un volcado
 * del usuario. Sin RUT, sin hash, sin tokens.
 *
 * Contrato (nombres EXACTOS que el consumidor deserializa):
 *  - altKey   (OBLIGATORIO) alt_key del usuario.
 *  - idUnidad (OBLIGATORIO) alt_key de la UNIDAD del usuario (mismo nombre que el
 *             claim del JWT y que personas-service; NUNCA la PK interna).
 * Campos adicionales coherentes con la convencion (el consumidor ignora los que
 * no conoce): idSede, nombre, roles.
 */
public record UsuarioVerificacionDTO(
        String altKey,
        String idUnidad,
        String idSede,
        String nombre,
        List<String> roles
) {}
