package cl.smid.auth.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** Cuerpo de POST /auth/refresh y POST /auth/logout. */
@Schema(description = "Refresh token opaco usado para renovar o cerrar una sesion.")
public record RefreshRequest(
        @Schema(description = "Refresh token opaco. Ejemplo ficticio, no usar como credencial.",
                example = "refresh-token-opaco-ejemplo")
        @NotBlank(message = "El token de refresco es obligatorio")
        String refreshToken
) {}
