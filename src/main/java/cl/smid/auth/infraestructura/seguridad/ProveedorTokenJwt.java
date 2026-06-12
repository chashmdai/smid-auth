package cl.smid.auth.infraestructura.seguridad;

import cl.smid.auth.dominio.modelo.Usuario;
import cl.smid.auth.dominio.puerto.salida.ProveedorToken;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;

/**
 * Adaptador de salida que emite el access token JWT segun el contrato 2.4 e
 * implementa el resto de operaciones de token (refresco opaco + hashing).
 *
 * Decisiones de seguridad:
 *  - Firma HS256 con la clave del 'kidActivo'. La cabecera incluye 'kid' para que
 *    los servicios consumidores seleccionen la clave correcta del mapa de rotacion.
 *  - Claims exactos del Nucleo 2.4: sub=altKey, iss, aud, jti, roles, idSede,
 *    idUnidad, alcance, nombre, iat, exp.
 *  - idSede/idUnidad viajan como alt_key (UUID), nunca como id interno (2.2).
 *  - El refresco es opaco (256 bits de SecureRandom, base64url); de el solo se
 *    persiste su SHA-256, de modo que una fuga de la tabla no revela tokens.
 *
 * NOTA: este servicio solo EMITE. La validacion de firma/iss/aud/exp la realizan
 * el Gateway y cada servicio de negocio (defensa en profundidad, 2.4). La clave
 * de cada 'kid' debe coincidir byte a byte entre todos los participantes.
 */
@Component
public class ProveedorTokenJwt implements ProveedorToken {

    private final PropiedadesJwt props;
    private final SecureRandom secureRandom = new SecureRandom();

    /** Clave de firma activa, derivada del secreto del kidActivo. */
    private SecretKey claveFirmaActiva;

    public ProveedorTokenJwt(PropiedadesJwt props) {
        this.props = props;
    }

    @PostConstruct
    void validarYprepararClaves() {
        if (props.claves() == null || !props.claves().containsKey(props.kidActivo())) {
            throw new IllegalStateException(
                    "Configuracion JWT invalida: no existe secreto para el kid activo '"
                            + props.kidActivo() + "'. Revise smid.jwt.claves y JWT_SECRET.");
        }
        String secretoActivo = props.claves().get(props.kidActivo());
        if (secretoActivo == null || secretoActivo.isBlank()) {
            throw new IllegalStateException(
                    "El secreto JWT para el kid activo esta vacio. Defina la variable JWT_SECRET.");
        }
        // HS256 exige clave de >= 256 bits; Keys.hmacShaKeyFor lo valida.
        this.claveFirmaActiva = Keys.hmacShaKeyFor(secretoActivo.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String emitirAccessToken(Usuario usuario) {
        Instant ahora = Instant.now();
        Instant expira = ahora.plusMillis(props.expiracionMs());

        // Claims territoriales como alt_key (no ids internos).
        String idSede = usuario.sede() != null ? usuario.sede().altKey() : null;
        String idUnidad = usuario.unidad() != null ? usuario.unidad().altKey() : null;

        // IMPORTANTE (jjwt 0.12.x): los claims personalizados se agregan UNO A UNO
        // con .claim(...). Se evita .claims(Map), que REEMPLAZA el conjunto de
        // claims y, si se invoca tras los setters registrados, borraria
        // sub/iss/jti/iat/exp (ver jjwt issue #678). El orden aqui es seguro.
        return Jwts.builder()
                .header().keyId(props.kidActivo()).and()   // cabecera: kid
                .subject(usuario.altKey())                 // sub = alt_key del usuario
                .issuer(props.emisor())                    // iss
                .audience().add(props.audiencia()).and()   // aud
                .id(java.util.UUID.randomUUID().toString())// jti
                .issuedAt(Date.from(ahora))                // iat
                .expiration(Date.from(expira))             // exp
                .claim("roles", usuario.codigosDeRol())
                .claim("idSede", idSede)
                .claim("idUnidad", idUnidad)
                .claim("alcance", usuario.alcanceEfectivo().name())
                .claim("nombre", usuario.nombreCompleto())
                .signWith(claveFirmaActiva, Jwts.SIG.HS256)
                .compact();
    }

    @Override
    public String generarRefreshTokenOpaco() {
        byte[] bytes = new byte[32]; // 256 bits de entropia
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public String hashRefreshToken(String refreshTokenPlano) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(refreshTokenPlano.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            // SHA-256 siempre esta disponible; si no, es un fallo de plataforma.
            throw new IllegalStateException("No se pudo calcular el hash del token de refresco", e);
        }
    }

    @Override
    public long expiracionAccessSegundos() {
        return props.expiracionMs() / 1000;
    }

    @Override
    public long expiracionRefreshMillis() {
        return props.refreshExpiracionMs();
    }
}
