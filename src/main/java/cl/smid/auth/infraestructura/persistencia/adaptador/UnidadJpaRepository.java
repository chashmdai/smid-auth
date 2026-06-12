package cl.smid.auth.infraestructura.persistencia.adaptador;

import cl.smid.auth.infraestructura.persistencia.UnidadEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnidadJpaRepository extends JpaRepository<UnidadEntity, Long> {}
