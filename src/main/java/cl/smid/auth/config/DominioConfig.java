package cl.smid.auth.config;

import cl.smid.auth.dominio.puerto.entrada.AutenticacionUseCase;
import cl.smid.auth.dominio.puerto.entrada.ConsultaUsuarioUseCase;
import cl.smid.auth.dominio.puerto.salida.*;
import cl.smid.auth.dominio.servicio.AuthAuditPort;
import cl.smid.auth.dominio.servicio.ServicioAutenticacion;
import cl.smid.auth.dominio.servicio.ServicioConsultaUsuario;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cableado (composition root) del nucleo hexagonal. El servicio de dominio es un
 * POJO sin anotaciones de Spring; aqui se construye inyectandole sus puertos. Asi
 * el dominio permanece independiente del framework (D5) y se puede instanciar en
 * pruebas con dobles, sin contexto.
 */
@Configuration
public class DominioConfig {

    @Bean
    public AutenticacionUseCase autenticacionUseCase(
            UsuarioRepositorio usuarioRepositorio,
            SesionRefreshRepositorio sesionRepositorio,
            ProveedorToken proveedorToken,
            CodificadorPassword codificadorPassword,
            RelojDominio reloj,
            AuthAuditPort auditoria) {
        return new ServicioAutenticacion(
                usuarioRepositorio, sesionRepositorio, proveedorToken,
                codificadorPassword, reloj, auditoria);
    }

    @Bean
    public ConsultaUsuarioUseCase consultaUsuarioUseCase(UsuarioRepositorio usuarioRepositorio) {
        return new ServicioConsultaUsuario(usuarioRepositorio);
    }
}
