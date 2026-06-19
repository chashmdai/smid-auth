package cl.smid.auth.config;

import cl.smid.auth.api.error.ErrorResponse;
import cl.smid.auth.dominio.excepcion.CodigoError;
import cl.smid.auth.infraestructura.seguridad.FiltroAutenticacionJwt;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuracion de seguridad del servicio.
 *
 *  - Stateless puro: sin sesion de servidor (el estado vive en el JWT) y CSRF
 *    deshabilitado (no hay cookies de sesion que proteger).
 *  - BCrypt con costo 12 (>= 10 exigido por 3.5).
 *  - Rutas publicas: /auth/** (login/refresh/logout), Swagger/OpenAPI y health/info.
 *  - Rutas protegidas: /usuarios/** exige Bearer valido (consulta servicio-a-servicio).
 *  - @EnableMethodSecurity activa @PreAuthorize para autorizacion por rol/alcance.
 *
 * SERVIDOR DE RECURSOS (cambio de responsabilidad): con el endpoint protegido
 * GET /usuarios/{altKey}, este servicio ademas de EMITIR tokens ahora los VALIDA
 * (defensa en profundidad, DT-3). El {@link FiltroAutenticacionJwt} se ejecuta
 * antes del filtro de usuario/clave y, si el Bearer es valido, deja el
 * ContextoSesion como principal. Si falta o es invalido en una ruta protegida,
 * responde 401 AUTZ-003; si el principal carece de rol exigido, 403 AUTZ-004.
 */
@Configuration
@EnableMethodSecurity
public class SeguridadConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           ObjectMapper objectMapper,
                                           FiltroAutenticacionJwt filtroJwt) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/auth/login", "/auth/refresh", "/auth/logout").permitAll()
                    .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                    .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                    .requestMatchers("/usuarios/**").authenticated()
                    .anyRequest().authenticated()
            )
            // El filtro JWT puebla el SecurityContext antes de evaluar la autorizacion.
            .addFilterBefore(filtroJwt, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(eh -> eh
                    // 401: token ausente, invalido o expirado en ruta protegida.
                    .authenticationEntryPoint((req, res, ex) ->
                            escribirError(res, objectMapper, HttpStatus.UNAUTHORIZED,
                                    CodigoError.NO_AUTENTICADO, req.getRequestURI()))
                    // 403: autenticado pero sin el rol/alcance requerido.
                    .accessDeniedHandler((req, res, ex) ->
                            escribirError(res, objectMapper, HttpStatus.FORBIDDEN,
                                    CodigoError.ACCESO_DENEGADO, req.getRequestURI()))
            );
        return http.build();
    }

    /** Serializa el sobre de error unificado (2.5) en respuestas de la cadena de seguridad. */
    private void escribirError(HttpServletResponse res, ObjectMapper objectMapper,
                               HttpStatus estado, CodigoError codigo, String ruta) throws java.io.IOException {
        res.setStatus(estado.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        ErrorResponse cuerpo = ErrorResponse.de(
                estado.value(), estado.getReasonPhrase(),
                codigo.codigo(), codigo.mensajePorDefecto(), ruta);
        objectMapper.writeValue(res.getWriter(), cuerpo);
    }
}
