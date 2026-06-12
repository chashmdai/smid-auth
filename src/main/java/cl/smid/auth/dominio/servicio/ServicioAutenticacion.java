package cl.smid.auth.dominio.servicio;

import cl.smid.auth.dominio.excepcion.CredencialesInvalidasException;
import cl.smid.auth.dominio.excepcion.RefreshInvalidoException;
import cl.smid.auth.dominio.modelo.Usuario;
import cl.smid.auth.dominio.puerto.entrada.AutenticacionUseCase;
import cl.smid.auth.dominio.puerto.entrada.ResultadoAutenticacion;
import cl.smid.auth.dominio.puerto.salida.*;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementacion del caso de uso de autenticacion. Es el nucleo de la
 * arquitectura hexagonal: contiene la logica de negocio pura y depende solo de
 * puertos (interfaces), nunca de Spring, JPA ni jjwt. Eso permite probarlo con
 * dobles de prueba sin levantar contexto (ver pruebas unitarias).
 *
 * Reglas implementadas:
 *  - Login anti-enumeracion (3.5): usuario inexistente, contrasena incorrecta y
 *    cuenta no vigente producen exactamente la misma excepcion. Ademas se ejecuta
 *    SIEMPRE una verificacion BCrypt (con hash senuelo cuando el usuario no
 *    existe) para no filtrar la existencia por diferencia de tiempo de respuesta.
 *  - Refresco con rotacion one-time (3.5): cada refresco se usa una sola vez; al
 *    renovar se revoca el anterior y se emite uno nuevo en la misma familia.
 *  - Deteccion de reuso: si llega un refresco ya rotado/revocado pero aun no
 *    vencido, se interpreta como replay y se revoca la familia completa.
 */
public class ServicioAutenticacion implements AutenticacionUseCase {

    /**
     * Hash BCrypt senuelo (de la cadena "senuelo-sin-uso"), valido en formato.
     * Se verifica contra el cuando el email no existe, igualando el costo
     * temporal del camino feliz para cerrar el canal lateral de temporizacion.
     */
    private static final String HASH_SENUELO =
            "$2a$12$C6UzMDM.H6dfI/f/IKcEeO3WpossQjl3R7jcSjN9Q/0kqK0g7eYpe";

    private final UsuarioRepositorio usuarioRepositorio;
    private final SesionRefreshRepositorio sesionRepositorio;
    private final ProveedorToken proveedorToken;
    private final CodificadorPassword codificadorPassword;
    private final RelojDominio reloj;
    private final AuthAuditPort auditoria;

    public ServicioAutenticacion(UsuarioRepositorio usuarioRepositorio,
                                 SesionRefreshRepositorio sesionRepositorio,
                                 ProveedorToken proveedorToken,
                                 CodificadorPassword codificadorPassword,
                                 RelojDominio reloj,
                                 AuthAuditPort auditoria) {
        this.usuarioRepositorio = usuarioRepositorio;
        this.sesionRepositorio = sesionRepositorio;
        this.proveedorToken = proveedorToken;
        this.codificadorPassword = codificadorPassword;
        this.reloj = reloj;
        this.auditoria = auditoria;
    }

    // ------------------------------------------------------------------ LOGIN
    @Override
    public ResultadoAutenticacion login(String email, String passwordPlano) {
        String emailNormalizado = normalizarEmail(email);

        Optional<Usuario> posible =
                usuarioRepositorio.buscarPorEmailConJerarquia(emailNormalizado);

        // Verificacion BCrypt incondicional: hash real si existe, senuelo si no.
        // Igualar el costo evita distinguir "usuario inexistente" por el tiempo.
        String hashAComparar = posible.map(Usuario::passwordHash).orElse(HASH_SENUELO);
        boolean passwordOk = codificadorPassword.coincide(passwordPlano, hashAComparar);

        if (posible.isEmpty() || !passwordOk || !posible.get().vigente()) {
            auditoria.loginFallido(emailNormalizado);
            throw new CredencialesInvalidasException();
        }

        Usuario usuario = posible.get();
        ResultadoAutenticacion resultado = emitirParDeTokens(usuario, UUID.randomUUID().toString());
        auditoria.loginExitoso(usuario);
        return resultado;
    }

    // --------------------------------------------------------------- REFRESCO
    @Override
    public ResultadoAutenticacion refrescar(String refreshTokenPlano) {
        if (refreshTokenPlano == null || refreshTokenPlano.isBlank()) {
            throw new RefreshInvalidoException();
        }

        String hash = proveedorToken.hashRefreshToken(refreshTokenPlano);
        var sesion = sesionRepositorio.buscarPorHash(hash)
                .orElseThrow(RefreshInvalidoException::new);

        Instant ahora = reloj.ahora();

        // Replay: el token existe pero ya fue rotado/revocado. Si aun no vence,
        // es un intento de reuso -> se corta la familia entera por seguridad.
        if (sesion.revocada()) {
            if (sesion.expiraEn().isAfter(ahora)) {
                sesionRepositorio.revocarFamilia(sesion.familia(), "REUSO");
                auditoria.reusoRefreshDetectado(altKeyDeUsuario(sesion.idUsuario()), sesion.familia());
            }
            throw new RefreshInvalidoException();
        }

        // Token vencido: invalido, sin alarma de replay.
        if (!sesion.expiraEn().isAfter(ahora)) {
            throw new RefreshInvalidoException();
        }

        // El usuario debe seguir existiendo y estar vigente al renovar.
        Usuario usuario = usuarioRepositorio
                .buscarPorIdConJerarquia(sesion.idUsuario())
                .filter(Usuario::vigente)
                .orElseThrow(() -> {
                    sesionRepositorio.revocar(sesion.id(), "USUARIO_NO_VIGENTE");
                    return new RefreshInvalidoException();
                });

        // Rotacion one-time: se revoca el refresco usado y se emite otro en la
        // misma familia, encadenando la sesion.
        sesionRepositorio.revocar(sesion.id(), "ROTACION");
        ResultadoAutenticacion resultado = emitirParDeTokens(usuario, sesion.familia());
        auditoria.sesionRenovada(usuario);
        return resultado;
    }

    // ----------------------------------------------------------------- LOGOUT
    @Override
    public void logout(String refreshTokenPlano) {
        if (refreshTokenPlano == null || refreshTokenPlano.isBlank()) {
            return; // idempotente: nada que revocar
        }
        String hash = proveedorToken.hashRefreshToken(refreshTokenPlano);
        sesionRepositorio.buscarPorHash(hash).ifPresent(s -> {
            if (!s.revocada()) {
                sesionRepositorio.revocar(s.id(), "LOGOUT");
                auditoria.sesionCerrada(altKeyDeUsuario(s.idUsuario()));
            }
        });
    }

    // ------------------------------------------------------------- AUXILIARES
    /**
     * Emite el access token (JWT) y un refresco opaco nuevo, persistiendo el
     * hash del refresco en la familia indicada. Centraliza lo comun a login y
     * refresh para no duplicar la politica de emision.
     */
    private ResultadoAutenticacion emitirParDeTokens(Usuario usuario, String familia) {
        String accessToken = proveedorToken.emitirAccessToken(usuario);
        String refreshPlano = proveedorToken.generarRefreshTokenOpaco();
        String refreshHash = proveedorToken.hashRefreshToken(refreshPlano);

        Instant emitido = reloj.ahora();
        Instant expira = emitido.plusMillis(proveedorToken.expiracionRefreshMillis());
        sesionRepositorio.guardar(usuario.id(), refreshHash, familia, emitido, expira);

        return new ResultadoAutenticacion(
                accessToken,
                refreshPlano,
                proveedorToken.expiracionAccessSegundos(),
                usuario
        );
    }

    private String normalizarEmail(String email) {
        if (email == null) return "";
        return email.trim().toLowerCase();
    }

    private String altKeyDeUsuario(Long idUsuario) {
        return usuarioRepositorio.buscarPorIdConJerarquia(idUsuario)
                .map(Usuario::altKey)
                .orElse("desconocido");
    }
}
