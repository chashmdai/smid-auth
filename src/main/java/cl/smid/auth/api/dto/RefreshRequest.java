package cl.smid.auth.api.dto;

import jakarta.validation.constraints.NotBlank;

/** Cuerpo de POST /auth/refresh y POST /auth/logout. */
public record RefreshRequest(
        @NotBlank(message = "El token de refresco es obligatorio")
        String refreshToken
) {}
