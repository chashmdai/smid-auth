package cl.smid.auth.infraestructura.seguridad;

import cl.smid.auth.dominio.puerto.salida.CodificadorPassword;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Adaptador del puerto {@link CodificadorPassword} sobre el PasswordEncoder de
 * Spring Security (BCrypt, configurado con costo 12 en SeguridadConfig). La
 * verificacion de BCrypt es de tiempo constante respecto del hash, lo que sostiene
 * la garantia anti-temporizacion del login.
 */
@Component
public class CodificadorPasswordBCrypt implements CodificadorPassword {

    private final PasswordEncoder passwordEncoder;

    public CodificadorPasswordBCrypt(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public boolean coincide(String passwordPlano, String hashAlmacenado) {
        if (passwordPlano == null || hashAlmacenado == null) return false;
        return passwordEncoder.matches(passwordPlano, hashAlmacenado);
    }
}
