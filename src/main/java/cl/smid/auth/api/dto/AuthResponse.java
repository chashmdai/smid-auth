package cl.smid.auth.api.dto;

/**
 * Respuesta de login y refresh (contrato 3.4). 'expiraEn' es la vigencia del
 * access token en segundos. El refresh token viaja en el cuerpo; el cliente lo
 * custodia y lo presenta en /auth/refresh.
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiraEn,
        UsuarioDTO usuario
) {}
