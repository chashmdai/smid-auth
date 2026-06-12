package cl.smid.auth;

import cl.smid.auth.infraestructura.seguridad.PropiedadesJwt;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Punto de entrada del microservicio SMID 6.1 - Identidad y Acceso.
 * Habilita el binding de las propiedades tipadas del JWT.
 */
@SpringBootApplication
@EnableConfigurationProperties(PropiedadesJwt.class)
public class AuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
