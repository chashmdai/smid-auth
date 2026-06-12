package cl.smid.auth.dominio.puerto.salida;

/**
 * Puerto de salida para verificacion de contrasenas. Aisla el algoritmo de
 * hashing (BCrypt) del dominio. La comparacion es resistente a timing porque
 * la implementacion BCrypt subyacente lo es.
 */
public interface CodificadorPassword {

    /** Verifica una contrasena en claro contra su hash BCrypt almacenado. */
    boolean coincide(String passwordPlano, String hashAlmacenado);
}
