package cl.smid.auth.infraestructura.persistencia;

import cl.smid.auth.dominio.modelo.Alcance;
import jakarta.persistence.*;
import org.springframework.lang.NonNull;

import java.time.Instant;

/** Entidad JPA de la tabla 'rol'. Su identificador publico estable es 'codigo'. */
@Entity
@Table(name = "rol")
public class RolEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Alcance alcance = Alcance.UNIDAD;

    @Column(nullable = false)
    private boolean vigente = true;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    protected RolEntity() {}

    /** Factory de sembrado (perfiles dev). */
    @NonNull
    public static RolEntity crear(String codigo, String nombre, cl.smid.auth.dominio.modelo.Alcance alcance) {
        RolEntity e = new RolEntity();
        e.codigo = codigo;
        e.nombre = nombre;
        e.alcance = alcance;
        e.vigente = true;
        return e;
    }

    @PrePersist
    void alCrear() { creadoEn = Instant.now(); }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public Alcance getAlcance() { return alcance; }
}
