package cl.smid.auth.infraestructura.seguridad;

import cl.smid.auth.dominio.puerto.salida.RelojDominio;
import org.springframework.stereotype.Component;

import java.time.Instant;

/** Adaptador del reloj de dominio sobre el reloj real del sistema (UTC). */
@Component
public class RelojSistema implements RelojDominio {
    @Override
    public Instant ahora() {
        return Instant.now();
    }
}
