package cl.smid.auth.infraestructura.seguridad;

import cl.smid.auth.dominio.modelo.Alcance;
import cl.smid.auth.dominio.modelo.ContextoSesion;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

/**
 * Validador de JWT entrante (defensa en profundidad, cierra DT-3). Aunque el
 * Gateway ya valido el token, este servicio lo REVALIDA por su cuenta antes de
 * confiar en sus claims, siguiendo el contrato 2.4:
 *
 *   1. Firma HS256, seleccionando la clave por el 'kid' de la cabecera (mismo
 *      mapa kid -> secreto que usa el emisor; habilita rotacion sin caida).
 *   2. Emisor: iss == smid-auth.
 *   3. Audiencia: aud contiene smid-servicios.
 *   4. Expiracion: exp no vencido (con tolerancia de reloj de 30 s).
 *
 * Cualquier fallo lanza una excepcion (no autentica -> 401 AUTZ-003). Reutiliza
 * exactamente PropiedadesJwt, por lo que el endpoint no requiere configuracion
 * nueva.
 */
@Component
public class ValidadorTokenJwt {

    private static final long TOLERANCIA_RELOJ_SEG = 30;

    private final PropiedadesJwt props;

    public ValidadorTokenJwt(PropiedadesJwt props) {
        this.props = props;
    }

    /**
     * Valida el token y proyecta sus claims al contexto de sesion del solicitante.
     * @throws JwtException si la firma, el emisor, la audiencia o la expiracion no validan.
     */
    public ContextoSesion validar(String token) {
        Jws<Claims> jws = Jwts.parser()
                // Resolucion de clave por kid: misma estructura de rotacion que el emisor.
                .keyLocator(header -> {
                    if (header instanceof JwsHeader jwsHeader) {
                        String kid = jwsHeader.getKeyId();
                        String secreto = kid != null ? props.claves().get(kid) : null;
                        if (secreto == null || secreto.isBlank()) {
                            throw new JwtException("kid no reconocido en la cabecera del token");
                        }
                        return Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
                    }
                    throw new JwtException("El token no es un JWS firmado");
                })
                .requireIssuer(props.emisor())            // iss == smid-auth
                .clockSkewSeconds(TOLERANCIA_RELOJ_SEG)   // tolerancia de reloj para exp/nbf
                .build()
                .parseSignedClaims(token);                // valida firma y exp; lanza si fallan

        Claims c = jws.getPayload();

        // Audiencia: el claim 'aud' es un conjunto; debe contener la esperada.
        Set<String> audiencias = c.getAudience();
        if (audiencias == null || !audiencias.contains(props.audiencia())) {
            throw new JwtException("Audiencia del token no valida");
        }

        return new ContextoSesion(
                c.getSubject(),
                c.get("idSede", String.class),
                c.get("idUnidad", String.class),
                leerAlcance(c),
                leerRoles(c)
        );
    }

    private Alcance leerAlcance(Claims c) {
        String valor = c.get("alcance", String.class);
        try {
            return valor != null ? Alcance.valueOf(valor) : Alcance.UNIDAD;
        } catch (IllegalArgumentException ex) {
            // Alcance desconocido -> el mas restrictivo, nunca se eleva por un valor invalido.
            return Alcance.UNIDAD;
        }
    }

    private List<String> leerRoles(Claims c) {
        Object roles = c.get("roles");
        if (roles instanceof List<?> lista) {
            return lista.stream().map(String::valueOf).toList();
        }
        return List.of();
    }
}
