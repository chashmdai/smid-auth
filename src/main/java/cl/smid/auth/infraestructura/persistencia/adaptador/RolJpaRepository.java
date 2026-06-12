package cl.smid.auth.infraestructura.persistencia.adaptador;

import cl.smid.auth.infraestructura.persistencia.RolEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RolJpaRepository extends JpaRepository<RolEntity, Long> {
    Optional<RolEntity> findByCodigo(String codigo);
}
