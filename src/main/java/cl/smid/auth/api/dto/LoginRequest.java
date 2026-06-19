package cl.smid.auth.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Cuerpo de POST /auth/login. La validacion declarativa rechaza entradas vacias
 * o con formato de email invalido antes de tocar el dominio (-> 400 AUTZ-005).
 */
@Schema(description = "Credenciales de acceso para iniciar sesion.")
public record LoginRequest(
        @Schema(description = "Correo institucional del usuario.", example = "admin@defensorianinez.cl")
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato valido")
        String email,

        @Schema(description = "Contrasena del usuario.", example = "Clave.Ficticia.2027")
        @NotBlank(message = "La contrasena es obligatoria")
        String password
) {}
