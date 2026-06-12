package cl.smid.auth.infraestructura.seguridad;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * Propiedades tipadas del JWT (prefijo 'smid.jwt' en application.yml). Incluye el
 * mapa kid -> secreto que habilita la rotacion de clave sin invalidar tokens
 * vigentes (DT-3 / Nucleo 2.4).
 *
 * @param emisor          valor del claim 'iss'
 * @param audiencia       valor del claim 'aud'
 * @param kidActivo       kid de la clave con la que se FIRMAN los tokens nuevos
 * @param expiracionMs    vigencia del access token (ms)
 * @param refreshExpiracionMs vigencia del token de refresco (ms)
 * @param claves          mapa kid -> secreto; debe contener al menos kidActivo
 */
@ConfigurationProperties(prefix = "smid.jwt")
public record PropiedadesJwt(
        String emisor,
        String audiencia,
        String kidActivo,
        long expiracionMs,
        long refreshExpiracionMs,
        Map<String, String> claves
) {}
