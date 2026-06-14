package cl.smid.auth.infraestructura.seguridad;

import cl.smid.auth.dominio.modelo.ContextoSesion;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro de autenticacion JWT. Por cada peticion: si trae un Bearer valido,
 * coloca un Authentication en el SecurityContext cuyo PRINCIPAL es el
 * {@link ContextoSesion} (lo recoge el controlador con @AuthenticationPrincipal).
 *
 * Si el token falta o es invalido, NO autentica y limpia el contexto: la cadena
 * de seguridad rechazara la ruta protegida con 401 AUTZ-003 via el entry point.
 * Las rutas publicas (/auth/**) siguen funcionando con o sin token.
 */
@Component
public class FiltroAutenticacionJwt extends OncePerRequestFilter {

    private static final String PREFIJO = "Bearer ";

    private final ValidadorTokenJwt validador;

    public FiltroAutenticacionJwt(ValidadorTokenJwt validador) {
        this.validador = validador;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String cabecera = request.getHeader("Authorization");
        if (cabecera != null && cabecera.startsWith(PREFIJO)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = cabecera.substring(PREFIJO.length()).trim();
            try {
                ContextoSesion contexto = validador.validar(token);

                List<SimpleGrantedAuthority> authorities = contexto.roles().stream()
                        .map(rol -> new SimpleGrantedAuthority("ROLE_" + rol))
                        .toList();

                var auth = new UsernamePasswordAuthenticationToken(contexto, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception ex) {
                // Token invalido: contexto vacio -> 401 por el entry point. No se filtra el motivo.
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
