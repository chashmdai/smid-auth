package cl.smid.auth.infraestructura.persistencia.adaptador;

import cl.smid.auth.infraestructura.persistencia.SesionRefreshEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

/** Repositorio Spring Data de las sesiones de refresco. */
public interface SesionRefreshJpaRepository extends JpaRepository<SesionRefreshEntity, Long> {

    // Bloqueo pesimista al leer por hash: serializa refrescos concurrentes del
    // mismo token, evitando que dos peticiones simultaneas lo roten dos veces.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SesionRefreshEntity> findByTokenHash(String tokenHash);

    /** Revoca en bloque todas las sesiones activas de una familia (corte por reuso). */
    @Modifying
    @Query("""
            update SesionRefreshEntity s
               set s.revocadoEn = :cuando, s.motivoRevocacion = :motivo
             where s.familia = :familia and s.revocadoEn is null
            """)
    int revocarFamilia(@Param("familia") String familia,
                       @Param("cuando") Instant cuando,
                       @Param("motivo") String motivo);
}
