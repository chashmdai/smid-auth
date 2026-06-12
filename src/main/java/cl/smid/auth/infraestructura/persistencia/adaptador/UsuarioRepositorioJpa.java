package cl.smid.auth.infraestructura.persistencia.adaptador;

import cl.smid.auth.dominio.modelo.Usuario;
import cl.smid.auth.dominio.puerto.salida.UsuarioRepositorio;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Adaptador de salida: implementa el puerto {@link UsuarioRepositorio} del
 * dominio usando Spring Data + el mapeador. La transaccion de solo lectura
 * mantiene viva la sesion JPA mientras se traduce a dominio (los EntityGraph ya
 * cargaron todo, asi que no hay lazy loading fuera de transaccion).
 */
@Component
public class UsuarioRepositorioJpa implements UsuarioRepositorio {

    private final UsuarioJpaRepository jpa;
    private final MapeadorDominio mapeador;

    public UsuarioRepositorioJpa(UsuarioJpaRepository jpa, MapeadorDominio mapeador) {
        this.jpa = jpa;
        this.mapeador = mapeador;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorEmailConJerarquia(String emailNormalizado) {
        return jpa.buscarPorEmail(emailNormalizado).map(mapeador::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorIdConJerarquia(Long id) {
        return jpa.buscarPorIdConJerarquia(id).map(mapeador::aDominio);
    }
}
