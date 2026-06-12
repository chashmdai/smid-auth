package cl.smid.auth.dominio.puerto.salida;

import cl.smid.auth.dominio.modelo.Usuario;

/**
 * Puerto de salida para emision de tokens. El dominio expresa QUE necesita
 * (un access token para un usuario); la infraestructura decide COMO (jjwt,
 * HS256, kid, claims del Nucleo 2.4). Mantiene jjwt fuera del nucleo (D5).
 */
public interface ProveedorToken {

    /**
     * Emite un access token JWT firmado para el usuario, con los claims del
     * contrato 2.4: sub=altKey, iss, aud, jti, roles, idSede, idUnidad,
     * alcance, nombre. La cabecera incluye el 'kid' de la clave activa.
     */
    String emitirAccessToken(Usuario usuario);

    /** Genera un token de refresco opaco (256 bits aleatorios, base64url). */
    String generarRefreshTokenOpaco();

    /** Calcula el hash SHA-256 (hex) del token de refresco para su persistencia. */
    String hashRefreshToken(String refreshTokenPlano);

    /** Vigencia configurada del access token, en segundos (para 'expira_en'). */
    long expiracionAccessSegundos();

    /** Vigencia configurada del token de refresco, en milisegundos. */
    long expiracionRefreshMillis();
}
