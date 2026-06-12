package cl.smid.auth.dominio.puerto.salida;

import java.time.Instant;

/**
 * Abstraccion del reloj. Inyectarlo (en vez de llamar a Instant.now() en linea)
 * mantiene el dominio determinista y testeable: las pruebas fijan el tiempo.
 */
public interface RelojDominio {
    Instant ahora();
}
