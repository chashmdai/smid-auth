package cl.smid.auth.config;

import cl.smid.auth.dominio.modelo.Alcance;
import cl.smid.auth.dominio.modelo.TipoUnidad;
import cl.smid.auth.infraestructura.persistencia.*;
import cl.smid.auth.infraestructura.persistencia.adaptador.RolJpaRepository;
import cl.smid.auth.infraestructura.persistencia.adaptador.SedeJpaRepository;
import cl.smid.auth.infraestructura.persistencia.adaptador.UnidadJpaRepository;
import cl.smid.auth.infraestructura.persistencia.adaptador.UsuarioJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Sembrador de datos minimos para desarrollo. CONFINADO a los perfiles local|dev
 * (corrige el riesgo R4 de la auditoria: el seeder original corria en todos los
 * perfiles, sembraba claves debiles y las imprimia en el log).
 *
 * Aqui:
 *  - Solo siembra si la tabla 'usuario' esta vacia (idempotente).
 *  - La contrasena proviene de la variable SEED_PASSWORD; JAMAS se imprime.
 *  - Crea una sede, una unidad y un usuario administrador de ejemplo.
 */
@Component
@Profile({"local", "dev"})
public class SembradorDatos implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SembradorDatos.class);

    private final UsuarioJpaRepository usuarioJpa;
    private final RolJpaRepository rolJpa;
    private final SedeJpaRepository sedeJpa;
    private final UnidadJpaRepository unidadJpa;
    private final PasswordEncoder passwordEncoder;

    @Value("${smid.seed.password}")
    private String seedPassword;

    public SembradorDatos(UsuarioJpaRepository usuarioJpa, RolJpaRepository rolJpa,
                          SedeJpaRepository sedeJpa, UnidadJpaRepository unidadJpa,
                          PasswordEncoder passwordEncoder) {
        this.usuarioJpa = usuarioJpa;
        this.rolJpa = rolJpa;
        this.sedeJpa = sedeJpa;
        this.unidadJpa = unidadJpa;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (usuarioJpa.count() > 0) {
            log.info("Sembrado omitido: ya existen usuarios.");
            return;
        }

        SedeEntity sede = sedeJpa.findByCodigo("CENTRAL")
                .orElseGet(() -> sedeJpa.save(SedeEntity.crear("Sede Central", "CENTRAL")));
        UnidadEntity unidad = unidadJpa.save(
                UnidadEntity.crear(sede, "Unidad de Proteccion y Representacion Judicial", TipoUnidad.UPRJ));

        RolEntity rolAdmin = rolJpa.save(RolEntity.crear("ADMIN_NACIONAL", "Administrador Nacional", Alcance.NACIONAL));
        RolEntity rolProf  = rolJpa.save(RolEntity.crear("PROFESIONAL_UPRJ", "Profesional UPRJ", Alcance.UNIDAD));

        UsuarioEntity admin = usuarioJpa.save(UsuarioEntity.crear(
                unidad,
                "11111111-1",
                "Admin",
                "SMID",
                "admin@defensorianinez.cl",
                passwordEncoder.encode(seedPassword),
                "Administrador del sistema",
                Set.of(rolAdmin, rolProf)));

        // Se informa el email para poder iniciar sesion, nunca la contrasena.
        log.info("Sembrado completo (perfil dev). Usuario de ejemplo: {}", admin.getEmail());
    }
}
