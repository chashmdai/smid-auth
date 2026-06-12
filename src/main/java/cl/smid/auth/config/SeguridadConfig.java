package cl.smid.auth.config;

import cl.smid.auth.api.error.ErrorResponse;
import cl.smid.auth.dominio.excepcion.CodigoError;
import com.fasterxml.jackson.databind.ObjectMapper;
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

/**
 * Configuracion de seguridad del servicio (cierra DT-4).
 *
 *  - Stateless puro: sin sesion de servidor (el estado vive en el JWT) y CSRF
 *    deshabilitado (no hay cookies de sesion que proteger).
 *  - BCrypt con costo 12 (>= 10 exigido por 3.5).
 *  - Rutas publicas: /auth/** (login/refresh/logout) y el health de Actuator.
 *    Todo lo demas exige autenticacion -> habilita endpoints administrativos
 *    futuros (alta/edicion de usuarios) sin reabrir la configuracion.
 *  - @EnableMethodSecurity activa @PreAuthorize para autorizacion por rol/alcance
 *    en los endpoints que se agreguen (matchers por metodo).
 *  - El punto de entrada de error responde 401 con el sobre unificado, no con la
 *    pagina HTML por defecto de Spring Security.
 *
 * Validacion de JWT entrante: este servicio NO la realiza (solo emite). La hace
 * el Gateway. Por eso aqui no hay filtro JWT ni resource-server: agregar uno seria
 * codigo muerto que contradice la responsabilidad del componente.
 */
@Configuration
@EnableMethodSecurity
public class SeguridadConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/auth/login", "/auth/refresh", "/auth/logout").permitAll()
                    .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                    .anyRequest().authenticated()
            )
            // 401 con cuerpo JSON unificado cuando falta autenticacion.
            .exceptionHandling(eh -> eh.authenticationEntryPoint((req, res, ex) -> {
                res.setStatus(HttpStatus.UNAUTHORIZED.value());
                res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                res.setCharacterEncoding("UTF-8");
                ErrorResponse cuerpo = ErrorResponse.de(
                        HttpStatus.UNAUTHORIZED.value(),
                        HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                        CodigoError.NO_AUTENTICADO.codigo(),
                        CodigoError.NO_AUTENTICADO.mensajePorDefecto(),
                        req.getRequestURI());
                objectMapper.writeValue(res.getWriter(), cuerpo);
            }));
        return http.build();
    }
}
