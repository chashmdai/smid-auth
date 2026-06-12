package cl.smid.auth.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Cuerpo de POST /auth/login. La validacion declarativa rechaza entradas vacias
 * o con formato de email invalido antes de tocar el dominio (-> 400 AUTZ-005).
 */
public record LoginRequest(
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato valido")
        String email,

        @NotBlank(message = "La contrasena es obligatoria")
        String password
) {}
