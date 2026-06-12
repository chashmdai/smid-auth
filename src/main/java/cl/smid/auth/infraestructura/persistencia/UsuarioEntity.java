package cl.smid.auth.infraestructura.persistencia;

import jakarta.persistence.*;
import org.springframework.lang.NonNull;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Entidad JPA de la tabla 'usuario'. N:1 con {@link UnidadEntity}, N:M con
 * {@link RolEntity} via 'usuario_rol'. La asociacion a roles es LAZY; el login la
 * resuelve con un fetch join dedicado para evitar el problema N+1.
 */
@Entity
@Table(name = "usuario")
public class UsuarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alt_key", nullable = false, unique = true, length = 36, updatable = false,
            columnDefinition = "CHAR(36)")
    private String altKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_unidad", nullable = false)
    private UnidadEntity unidad;

    @Column(nullable = false, unique = true, length = 12)
    private String rut;

    @Column(nullable = false, length = 120)
    private String nombres;

    @Column(nullable = false, length = 120)
    private String apellidos;

    @Column(nullable = false, unique = true, length = 160)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 72)
    private String passwordHash;

    @Column(length = 120)
    private String cargo;

    @Column(nullable = false)
    private boolean vigente = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "usuario_rol",
            joinColumns = @JoinColumn(name = "id_usuario"),
            inverseJoinColumns = @JoinColumn(name = "id_rol")
    )
    private Set<RolEntity> roles = new HashSet<>();

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    @Column(name = "creado_por", length = 36, columnDefinition = "CHAR(36)")
    private String creadoPor;

    protected UsuarioEntity() {}

    /** Factory de sembrado (perfiles dev). */
    @NonNull
    public static UsuarioEntity crear(UnidadEntity unidad, String rut, String nombres,
                                      String apellidos, String email, String passwordHash,
                                      String cargo, Set<RolEntity> roles) {
        UsuarioEntity e = new UsuarioEntity();
        e.unidad = unidad;
        e.rut = rut;
        e.nombres = nombres;
        e.apellidos = apellidos;
        e.email = email;
        e.passwordHash = passwordHash;
        e.cargo = cargo;
        e.roles = roles;
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
    public UnidadEntity getUnidad() { return unidad; }
    public String getRut() { return rut; }
    public String getNombres() { return nombres; }
    public String getApellidos() { return apellidos; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getCargo() { return cargo; }
    public boolean isVigente() { return vigente; }
    public Set<RolEntity> getRoles() { return roles; }

    public void setUnidad(UnidadEntity unidad) { this.unidad = unidad; }
    public void setRut(String rut) { this.rut = rut; }
    public void setNombres(String nombres) { this.nombres = nombres; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }
    public void setEmail(String email) { this.email = email; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setCargo(String cargo) { this.cargo = cargo; }
    public void setRoles(Set<RolEntity> roles) { this.roles = roles; }
}
