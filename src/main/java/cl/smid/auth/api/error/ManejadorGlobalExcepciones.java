package cl.smid.auth.api.error;

import cl.smid.auth.dominio.excepcion.AuthException;
import cl.smid.auth.dominio.excepcion.CodigoError;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manejador global que materializa el sobre de error unificado (D2 / 2.5).
 * Centraliza TODA traduccion excepcion -> HTTP + codigo AUTZ, de modo que ningun
 * controlador arma respuestas de error a mano y el contrato queda consistente.
 *
 * Politica anti-fuga: los 500 nunca exponen el mensaje interno de la excepcion;
 * se registra el detalle en el log y al cliente se le entrega un texto generico.
 */
@RestControllerAdvice
public class ManejadorGlobalExcepciones {

    private static final Logger log = LoggerFactory.getLogger(ManejadorGlobalExcepciones.class);

    /** Excepciones de dominio: cada una ya trae su CodigoError; se mapea a HTTP. */
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> manejarAuth(AuthException ex, HttpServletRequest req) {
        HttpStatus estado = estadoPara(ex.codigoError());
        ErrorResponse cuerpo = ErrorResponse.de(
                estado.value(),
                estado.getReasonPhrase(),
                ex.codigoError().codigo(),
                ex.getMessage(),
                req.getRequestURI()
        );
        return ResponseEntity.status(estado).body(cuerpo);
    }

    /** Validacion de @Valid: 400 con mapa campo->mensaje en 'detalles' (AUTZ-005). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> manejarValidacion(MethodArgumentNotValidException ex,
                                                            HttpServletRequest req) {
        Map<String, String> detalles = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fe ->
                detalles.putIfAbsent(fe.getField(),
                        fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "valor invalido"));

        ErrorResponse cuerpo = ErrorResponse.deValidacion(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                CodigoError.VALIDACION.codigo(),
                CodigoError.VALIDACION.mensajePorDefecto(),
                detalles,
                req.getRequestURI()
        );
        return ResponseEntity.badRequest().body(cuerpo);
    }

    /**
     * Fallo de autenticacion proveniente de la cadena de Spring Security
     * (p. ej. acceso sin token a una ruta protegida). Se unifica a AUTZ-003.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> manejarNoAutenticado(AuthenticationException ex,
                                                              HttpServletRequest req) {
        return construir(HttpStatus.UNAUTHORIZED, CodigoError.NO_AUTENTICADO, req);
    }

    /** Autorizacion denegada por rol/alcance: 403 AUTZ-004. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> manejarAccesoDenegado(AccessDeniedException ex,
                                                              HttpServletRequest req) {
        return construir(HttpStatus.FORBIDDEN, CodigoError.ACCESO_DENEGADO, req);
    }

    /** Ruta inexistente: 404 con sobre unificado en vez del HTML por defecto. */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> manejarNoEncontrado(NoHandlerFoundException ex,
                                                            HttpServletRequest req) {
        ErrorResponse cuerpo = ErrorResponse.de(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                "AUTZ-404",
                "Recurso no encontrado.",
                req.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(cuerpo);
    }

    /** Red de seguridad final: cualquier excepcion no prevista -> 500 generico. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> manejarInesperado(Exception ex, HttpServletRequest req) {
        // El detalle real queda SOLO en el log del servidor; nunca viaja al cliente.
        log.error("Error no controlado en {}", req.getRequestURI(), ex);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR, CodigoError.ERROR_INTERNO, req);
    }

    // ------------------------------------------------------------- AUXILIARES
    private ResponseEntity<ErrorResponse> construir(HttpStatus estado, CodigoError codigo,
                                                    HttpServletRequest req) {
        ErrorResponse cuerpo = ErrorResponse.de(
                estado.value(), estado.getReasonPhrase(),
                codigo.codigo(), codigo.mensajePorDefecto(), req.getRequestURI());
        return ResponseEntity.status(estado).body(cuerpo);
    }

    /** Correspondencia codigo de negocio -> estado HTTP. */
    private HttpStatus estadoPara(CodigoError codigo) {
        return switch (codigo) {
            case CREDENCIALES_INVALIDAS, REFRESH_INVALIDO, NO_AUTENTICADO -> HttpStatus.UNAUTHORIZED;
            case ACCESO_DENEGADO -> HttpStatus.FORBIDDEN;
            case VALIDACION -> HttpStatus.BAD_REQUEST;
            case ERROR_INTERNO -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
