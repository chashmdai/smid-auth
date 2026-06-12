package cl.smid.auth.infraestructura.persistencia;

import jakarta.persistence.*;
import org.springframework.lang.NonNull;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidad JPA de la tabla 'sede'. Vive en infraestructura, separada del modelo de
 * dominio (regla 2.9: las entidades JPA no se serializan ni cruzan al dominio sin
 * traduccion). 'altKey' se genera una sola vez al persistir (@PrePersist).
 * 'codigo' es un identificador institucional estable de la sede; no representa
 * necesariamente una region administrativa.
 */
@Entity
@Table(name = "sede")
public class SedeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alt_key", nullable = false, unique = true, length = 36, updatable = false,
            columnDefinition = "CHAR(36)")
    private String altKey;

    @Column(nullable = false, length = 160)
    private String nombre;

    @Column(nullable = false, unique = true, length = 32)
    private String codigo;

    @Column(nullable = false)
    private boolean vigente = true;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    @Column(name = "creado_por", length = 36, columnDefinition = "CHAR(36)")
    private String creadoPor;

    protected SedeEntity() {}

    /** Factory de sembrado (perfiles dev). El alt_key se genera en @PrePersist. */
    @NonNull
    public static SedeEntity crear(String nombre, String codigo) {
        SedeEntity e = new SedeEntity();
        e.nombre = nombre;
        e.codigo = codigo;
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
    public String getNombre() { return nombre; }
    public String getCodigo() { return codigo; }
    public boolean isVigente() { return vigente; }
}
