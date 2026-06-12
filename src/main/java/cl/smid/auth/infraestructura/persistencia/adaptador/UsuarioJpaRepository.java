package cl.smid.auth.infraestructura.persistencia.adaptador;

import cl.smid.auth.infraestructura.persistencia.UsuarioEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/**
 * Repositorio Spring Data del usuario. Las consultas usan @EntityGraph para
 * traer unidad, sede y roles en una sola ejecucion (sin N+1 ni doble query),
 * resolviendo la jerarquia completa que el login necesita.
 */
public interface UsuarioJpaRepository extends JpaRepository<UsuarioEntity, Long> {

    @EntityGraph(attributePaths = {"unidad", "unidad.sede", "roles"})
    @Query("select u from UsuarioEntity u where lower(u.email) = :email")
    Optional<UsuarioEntity> buscarPorEmail(String email);

    @EntityGraph(attributePaths = {"unidad", "unidad.sede", "roles"})
    @Query("select u from UsuarioEntity u where u.id = :id")
    Optional<UsuarioEntity> buscarPorIdConJerarquia(Long id);
}
