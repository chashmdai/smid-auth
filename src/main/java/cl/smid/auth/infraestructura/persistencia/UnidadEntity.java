package cl.smid.auth.infraestructura.persistencia;

import cl.smid.auth.dominio.modelo.TipoUnidad;
import jakarta.persistence.*;
import org.springframework.lang.NonNull;

import java.time.Instant;
import java.util.UUID;

/** Entidad JPA de la tabla 'unidad'. N:1 con {@link SedeEntity}. */
@Entity
@Table(name = "unidad")
public class UnidadEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alt_key", nullable = false, unique = true, length = 36, updatable = false,
            columnDefinition = "CHAR(36)")
    private String altKey;

    // La unidad se carga junto al usuario en login (fetch join explicito en el
    // repositorio); LAZY evita cargas accidentales en otros caminos.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_sede", nullable = false)
    private SedeEntity sede;

    @Column(nullable = false, length = 160)
    private String nombre;

    // El tipo se persiste como texto (VARCHAR + CHECK en V1). EnumType.STRING
    // garantiza estabilidad si se reordena el enum.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private TipoUnidad tipo;

    @Column(nullable = false)
    private boolean vigente = true;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    @Column(name = "creado_por", length = 36, columnDefinition = "CHAR(36)")
    private String creadoPor;

    protected UnidadEntity() {}

    /** Factory de sembrado (perfiles dev). */
    @NonNull
    public static UnidadEntity crear(SedeEntity sede, String nombre, cl.smid.auth.dominio.modelo.TipoUnidad tipo) {
        UnidadEntity e = new UnidadEntity();
        e.sede = sede;
        e.nombre = nombre;
        e.tipo = tipo;
        e.vigente = true;
        return e;
    }

    @PrePersist
    void alCrear() {
        if (altKey == null) altKey = UUID.randomUUID().toString();
        Instant ahora = Instant.now();
        creadoEn = ahora;
        actualizadoEn = ahora;
    }

    @PreUpdate
    void alActualizar() { actualizadoEn = Instant.now(); }

    public Long getId() { return id; }
    public String getAltKey() { return altKey; }
    public SedeEntity getSede() { return sede; }
    public String getNombre() { return nombre; }
    public TipoUnidad getTipo() { return tipo; }
    public boolean isVigente() { return vigente; }
}
