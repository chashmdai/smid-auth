package cl.smid.auth.api;

import cl.smid.auth.api.dto.*;
import cl.smid.auth.api.error.ErrorResponse;
import cl.smid.auth.dominio.puerto.entrada.AutenticacionUseCase;
import cl.smid.auth.dominio.puerto.entrada.ResultadoAutenticacion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST de Identidad. Adaptador de entrada de la arquitectura
 * hexagonal: traduce HTTP <-> casos de uso y delega toda la logica al puerto
 * {@link AutenticacionUseCase}. No contiene reglas de negocio.
 *
 * Rutas internas (el Gateway antepone /api y aplica StripPrefix):
 *   POST /auth/login    -> POST /api/auth/login    (publico)
 *   POST /auth/refresh  -> POST /api/auth/refresh   (publico: valida el refresco)
 *   POST /auth/logout   -> POST /api/auth/logout    (publico: idempotente)
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticacion", description = "Endpoints publicos para emitir, renovar y revocar credenciales SMID.")
public class AuthController {

    private final AutenticacionUseCase autenticacion;
    private final MapeadorRespuesta mapeador;

    public AuthController(AutenticacionUseCase autenticacion, MapeadorRespuesta mapeador) {
        this.autenticacion = autenticacion;
        this.mapeador = mapeador;
    }

    @PostMapping("/login")
    @Operation(
            summary = "Iniciar sesion",
            description = "Valida credenciales y emite access token, refresh token y el perfil publico del usuario autenticado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credenciales emitidas",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "AUTZ-005 - Validacion de DTO fallida",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "AUTZ-005", value = """
                                    {
                                      "status": 400,
                                      "error": "Bad Request",
                                      "codigo": "AUTZ-005",
                                      "mensaje": "La solicitud contiene datos invalidos.",
                                      "detalles": { "email": "El email no tiene un formato valido" },
                                      "ruta": "/auth/login",
                                      "timestamp": "2027-01-01T12:00:00Z"
                                    }
                                    """))),
            @ApiResponse(responseCode = "401", description = "AUTZ-001 - Credenciales invalidas",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "AUTZ-001", value = """
                                    {
                                      "status": 401,
                                      "error": "Unauthorized",
                                      "codigo": "AUTZ-001",
                                      "mensaje": "Credenciales invalidas. Verifique sus datos de acceso.",
                                      "ruta": "/auth/login",
                                      "timestamp": "2027-01-01T12:00:00Z"
                                    }
                                    """))),
            @ApiResponse(responseCode = "500", description = "AUTZ-500 - Error interno",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        ResultadoAutenticacion r = autenticacion.login(req.email(), req.password());
        return ResponseEntity.ok(mapeador.aAuthResponse(r));
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Renovar sesion",
            description = "Renueva el access token y rota el refresh token presentado en el cuerpo."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credenciales renovadas",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "AUTZ-005 - Validacion de DTO fallida",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "AUTZ-002 - Refresh token invalido",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "AUTZ-002", value = """
                                    {
                                      "status": 401,
                                      "error": "Unauthorized",
                                      "codigo": "AUTZ-002",
                                      "mensaje": "La sesion no es valida o expiro. Inicie sesion nuevamente.",
                                      "ruta": "/auth/refresh",
                                      "timestamp": "2027-01-01T12:00:00Z"
                                    }
                                    """))),
            @ApiResponse(responseCode = "500", description = "AUTZ-500 - Error interno",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest req) {
        ResultadoAutenticacion r = autenticacion.refrescar(req.refreshToken());
        return ResponseEntity.ok(mapeador.aAuthResponse(r));
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Cerrar sesion",
            description = "Revoca de forma idempotente la sesion asociada al refresh token presentado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Sesion cerrada",
                    content = @Content),
            @ApiResponse(responseCode = "400", description = "AUTZ-005 - Validacion de DTO fallida",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "AUTZ-002 - Refresh token invalido",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "AUTZ-500 - Error interno",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest req) {
        autenticacion.logout(req.refreshToken());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
