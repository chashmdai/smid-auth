package cl.smid.auth.api;

import cl.smid.auth.api.dto.*;
import cl.smid.auth.dominio.puerto.entrada.AutenticacionUseCase;
import cl.smid.auth.dominio.puerto.entrada.ResultadoAutenticacion;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
public class AuthController {

    private final AutenticacionUseCase autenticacion;
    private final MapeadorRespuesta mapeador;

    public AuthController(AutenticacionUseCase autenticacion, MapeadorRespuesta mapeador) {
        this.autenticacion = autenticacion;
        this.mapeador = mapeador;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        ResultadoAutenticacion r = autenticacion.login(req.email(), req.password());
        return ResponseEntity.ok(mapeador.aAuthResponse(r));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest req) {
        ResultadoAutenticacion r = autenticacion.refrescar(req.refreshToken());
        return ResponseEntity.ok(mapeador.aAuthResponse(r));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest req) {
        autenticacion.logout(req.refreshToken());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
