package cl.smid.auth.infraestructura.persistencia.adaptador;

import cl.smid.auth.dominio.modelo.*;
import cl.smid.auth.infraestructura.persistencia.RolEntity;
import cl.smid.auth.infraestructura.persistencia.SedeEntity;
import cl.smid.auth.infraestructura.persistencia.UnidadEntity;
import cl.smid.auth.infraestructura.persistencia.UsuarioEntity;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Traduce entidades JPA -> modelo de dominio puro. Es la frontera que mantiene a
 * Hibernate fuera del nucleo (2.9): el dominio jamas recibe una entidad gestionada.
 */
@Component
public class MapeadorDominio {

    public Usuario aDominio(UsuarioEntity e) {
        return new Usuario(
                e.getId(),
                e.getAltKey(),
                e.getRut(),
                e.getNombres(),
                e.getApellidos(),
                e.getEmail(),
                e.getPasswordHash(),
                e.getCargo(),
                e.isVigente(),
                aDominio(e.getUnidad()),
                e.getRoles().stream().map(this::aDominio).toList()
        );
    }

    public Unidad aDominio(UnidadEntity e) {
        if (e == null) return null;
        return new Unidad(e.getId(), e.getAltKey(), aDominio(e.getSede()),
                e.getNombre(), e.getTipo(), e.isVigente());
    }

    public Sede aDominio(SedeEntity e) {
        if (e == null) return null;
        return new Sede(e.getId(), e.getAltKey(), e.getNombre(),
                e.getCodigo(), e.isVigente());
    }

    public Rol aDominio(RolEntity e) {
        return new Rol(e.getId(), e.getCodigo(), e.getNombre(), e.getAlcance());
    }

    public List<Rol> aDominioRoles(List<RolEntity> entidades) {
        return entidades.stream().map(this::aDominio).toList();
    }
}
