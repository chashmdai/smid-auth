package cl.smid.auth.dominio.servicio;

import cl.smid.auth.dominio.modelo.Usuario;
import cl.smid.auth.dominio.puerto.salida.ProveedorToken;
import cl.smid.auth.dominio.puerto.salida.SesionRefreshRepositorio;
import cl.smid.auth.dominio.puerto.salida.UsuarioRepositorio;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/** Repositorio de usuarios en memoria. */
class DummyUsuarioRepo implements UsuarioRepositorio {
    private final Map<String, Usuario> porEmail = new HashMap<>();
    private final Map<Long, Usuario> porId = new HashMap<>();
    private final Map<String, Usuario> porAltKey = new HashMap<>();

    void guardar(Usuario u) {
        porEmail.put(u.email().toLowerCase(), u);
        porId.put(u.id(), u);
        porAltKey.put(u.altKey(), u);
    }
    @Override public Optional<Usuario> buscarPorEmailConJerarquia(String email) {
        return Optional.ofNullable(porEmail.get(email));
    }
    @Override public Optional<Usuario> buscarPorIdConJerarquia(Long id) {
        return Optional.ofNullable(porId.get(id));
    }
    @Override public Optional<Usuario> buscarPorAltKeyConJerarquia(String altKey) {
        return Optional.ofNullable(porAltKey.get(altKey));
    }
}

/** Lista de revocacion en memoria, con rotacion y corte por familia. */
class DummySesionRepo implements SesionRefreshRepositorio {
    static class Fila {
        Long id; Long idUsuario; String hash; String familia;
        Instant expira; boolean revocada;
    }
    final List<Fila> guardadas = new ArrayList<>();
    private final AtomicLong seq = new AtomicLong(1);
    boolean familiaRevocada = false;

    @Override public void guardar(Long idUsuario, String tokenHash, String familia,
                                  Instant emitidoEn, Instant expiraEn) {
        Fila f = new Fila();
        f.id = seq.getAndIncrement();
        f.idUsuario = idUsuario; f.hash = tokenHash; f.familia = familia;
        f.expira = expiraEn; f.revocada = false;
        guardadas.add(f);
    }
    @Override public Optional<SesionRefresh> buscarPorHash(String tokenHash) {
        return guardadas.stream().filter(f -> f.hash.equals(tokenHash)).findFirst()
                .map(f -> new SesionRefresh(f.id, f.idUsuario, f.familia, f.expira, f.revocada));
    }
    @Override public void revocar(Long idSesion, String motivo) {
        guardadas.stream().filter(f -> f.id.equals(idSesion)).forEach(f -> f.revocada = true);
    }
    @Override public void revocarFamilia(String familia, String motivo) {
        familiaRevocada = true;
        guardadas.stream().filter(f -> f.familia.equals(familia)).forEach(f -> f.revocada = true);
    }
    boolean estaRevocadoPorHash(String hash) {
        return guardadas.stream().filter(f -> f.hash.equals(hash)).anyMatch(f -> f.revocada);
    }
}

/** Proveedor de token de prueba: determinista y sin criptografia real. */
class DummyToken implements ProveedorToken {
    private int contador = 0;
    @Override public String emitirAccessToken(Usuario usuario) {
        return "access-para-" + usuario.altKey();
    }
    @Override public String generarRefreshTokenOpaco() {
        return "refresh-" + (++contador);
    }
    @Override public String hashRefreshToken(String refreshTokenPlano) {
        return "hash:" + refreshTokenPlano; // hash ficticio pero estable
    }
    @Override public long expiracionAccessSegundos() { return 28800; }
    @Override public long expiracionRefreshMillis() { return 86_400_000L; }
}

/** Espia de auditoria. */
class DummyAudit implements AuthAuditPort {
    int exitos, fallos, renovaciones, cierres, reusos;
    String ultimoCierreUsuario;
    String ultimoReusoUsuario;
    @Override public void loginExitoso(Usuario usuario) { exitos++; }
    @Override public void loginFallido(String identificadorPresentado) { fallos++; }
    @Override public void sesionRenovada(Usuario usuario) { renovaciones++; }
    @Override public void sesionCerrada(String usuarioAltKey) {
        cierres++;
        ultimoCierreUsuario = usuarioAltKey;
    }
    @Override public void reusoRefreshDetectado(String usuarioAltKey, String familia) {
        reusos++;
        ultimoReusoUsuario = usuarioAltKey;
    }
}
