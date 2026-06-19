package cl.smid.auth.api.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;

/**
 * Sobre de error unificado del ecosistema SMID (decision D2, Nucleo 2.5).
 * Estructura estable consumida por el frontend y el Gateway:
 *
 *   status   : codigo HTTP numerico
 *   error    : razon HTTP legible ("Unauthorized", "Bad Request", ...)
 *   codigo   : codigo estable de negocio, prefijo AUTZ-xxx (sobre el que se conmuta)
 *   mensaje  : texto orientado a persona usuaria (puede cambiar de redaccion)
 *   detalles : mapa campo->mensaje; presente solo en errores de validacion
 *   ruta     : path de la peticion
 *   timestamp: instante UTC del error (ISO 8601)
 *
 * 'detalles' se omite del JSON cuando es null (JsonInclude.NON_NULL) para no
 * ensuciar las respuestas de error que no son de validacion.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Sobre de error unificado del ecosistema SMID.")
public record ErrorResponse(
        @Schema(description = "Codigo HTTP numerico.", example = "401")
        int status,

        @Schema(description = "Razon HTTP legible.", example = "Unauthorized")
        String error,

        @Schema(description = "Codigo estable de negocio.", allowableValues = {"AUTZ-001", "AUTZ-002", "AUTZ-005", "AUTZ-500"},
                example = "AUTZ-001")
        String codigo,

        @Schema(description = "Mensaje orientado a persona usuaria.", example = "Credenciales invalidas. Verifique sus datos de acceso.")
        String mensaje,

        @Schema(description = "Mapa campo-mensaje presente solo en errores de validacion.",
                example = "{\"email\":\"El email no tiene un formato valido\"}")
        Map<String, String> detalles,

        @Schema(description = "Ruta que produjo el error.", example = "/auth/login")
        String ruta,

        @Schema(description = "Instante UTC del error.", example = "2027-01-01T12:00:00Z")
        Instant timestamp
) {
    public static ErrorResponse de(int status, String error, String codigo,
                                   String mensaje, String ruta) {
        return new ErrorResponse(status, error, codigo, mensaje, null, ruta, Instant.now());
    }

    public static ErrorResponse deValidacion(int status, String error, String codigo,
                                             String mensaje, Map<String, String> detalles,
                                             String ruta) {
        return new ErrorResponse(status, error, codigo, mensaje, detalles, ruta, Instant.now());
    }
}
