package cl.smid.auth.infraestructura.persistencia.adaptador;

import cl.smid.auth.dominio.puerto.salida.SesionRefreshRepositorio;
import cl.smid.auth.infraestructura.persistencia.SesionRefreshEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/** Adaptador de salida del puerto {@link SesionRefreshRepositorio}. */
@Component
public class SesionRefreshRepositorioJpa implements SesionRefreshRepositorio {

    private final SesionRefreshJpaRepository jpa;

    public SesionRefreshRepositorioJpa(SesionRefreshJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public void guardar(Long idUsuario, String tokenHash, String familia,
                        Instant emitidoEn, Instant expiraEn) {
        jpa.save(new SesionRefreshEntity(idUsuario, tokenHash, familia, emitidoEn, expiraEn));
    }

    @Override
    @Transactional
    public Optional<SesionRefresh> buscarPorHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash).map(this::aVista);
    }

    @Override
    @Transactional
    public void revocar(Long idSesion, String motivo) {
        Long idSesionNoNulo = java.util.Objects.requireNonNull(idSesion, "idSesion");
        jpa.findById(idSesionNoNulo).ifPresent(s -> {
            if (!s.estaRevocada()) {
                s.revocar(Instant.now(), motivo);
                jpa.save(s);
            }
        });
    }

    @Override
    @Transactional
    public void revocarFamilia(String familia, String motivo) {
        jpa.revocarFamilia(familia, Instant.now(), motivo);
    }

    private SesionRefresh aVista(SesionRefreshEntity e) {
        return new SesionRefresh(e.getId(), e.getIdUsuario(), e.getFamilia(),
                e.getExpiraEn(), e.estaRevocada());
    }
}
