package cl.smid.auth.infraestructura.persistencia.adaptador;

import cl.smid.auth.infraestructura.persistencia.SedeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SedeJpaRepository extends JpaRepository<SedeEntity, Long> {
    Optional<SedeEntity> findByCodigo(String codigo);
}
