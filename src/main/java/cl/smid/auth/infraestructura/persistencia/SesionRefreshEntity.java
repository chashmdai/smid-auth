package cl.smid.auth.infraestructura.persistencia;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Entidad JPA de la tabla 'sesion_refresh': lista de revocacion de tokens de
 * refresco (3.5). Guarda solo el hash del token (nunca el valor en claro) y la
 * familia de rotacion para cortar cadenas ante reuso.
 */
@Entity
@Table(name = "sesion_refresh")
public class SesionRefreshEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64,
            columnDefinition = "CHAR(64)")
    private String tokenHash;

    @Column(nullable = false, length = 36, columnDefinition = "CHAR(36)")
    private String familia;

    @Column(name = "emitido_en", nullable = false)
    private Instant emitidoEn;

    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;

    @Column(name = "revocado_en")
    private Instant revocadoEn;

    @Column(name = "motivo_revocacion", length = 30)
    private String motivoRevocacion;

    protected SesionRefreshEntity() {}

    public SesionRefreshEntity(Long idUsuario, String tokenHash, String familia,
                               Instant emitidoEn, Instant expiraEn) {
        this.idUsuario = idUsuario;
        this.tokenHash = tokenHash;
        this.familia = familia;
        this.emitidoEn = emitidoEn;
        this.expiraEn = expiraEn;
    }

    public boolean estaRevocada() { return revocadoEn != null; }

    public void revocar(Instant cuando, String motivo) {
        this.revocadoEn = cuando;
        this.motivoRevocacion = motivo;
    }

    public Long getId() { return id; }
    public Long getIdUsuario() { return idUsuario; }
    public String getFamilia() { return familia; }
    public Instant getExpiraEn() { return expiraEn; }
}
