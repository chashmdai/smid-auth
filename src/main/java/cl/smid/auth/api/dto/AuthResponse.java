package cl.smid.auth.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Respuesta de login y refresh (contrato 3.4). 'expiraEn' es la vigencia del
 * access token en segundos. El refresh token viaja en el cuerpo; el cliente lo
 * custodia y lo presenta en /auth/refresh.
 */
@Schema(description = "Respuesta publica de login y refresh.")
public record AuthResponse(
        @Schema(description = "Token de acceso emitido por el servicio. Ejemplo ficticio, no usar como credencial.",
                example = "access-token-ejemplo")
        String accessToken,

        @Schema(description = "Refresh token opaco de un solo uso. Ejemplo ficticio, no usar como credencial.",
                example = "refresh-token-opaco-ejemplo")
        String refreshToken,

        @Schema(description = "Vigencia del access token en segundos.", example = "28800")
        long expiraEn,

        @Schema(description = "Perfil publico del usuario autenticado.")
        UsuarioDTO usuario
) {}
