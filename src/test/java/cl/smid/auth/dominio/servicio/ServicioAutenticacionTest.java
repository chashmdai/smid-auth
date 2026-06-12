package cl.smid.auth.dominio.servicio;

import cl.smid.auth.dominio.excepcion.CredencialesInvalidasException;
import cl.smid.auth.dominio.excepcion.RefreshInvalidoException;
import cl.smid.auth.dominio.modelo.*;
import cl.smid.auth.dominio.puerto.entrada.ResultadoAutenticacion;
import cl.smid.auth.dominio.puerto.salida.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Pruebas unitarias del nucleo de autenticacion con dobles en memoria (sin Spring
 * ni base de datos). Validan las reglas criticas: anti-enumeracion, emision del
 * par de tokens, rotacion del refresco y deteccion de reuso.
 */
class ServicioAutenticacionTest {

    private DummyUsuarioRepo usuarioRepo;
    private DummySesionRepo sesionRepo;
    private DummyToken token;
    private DummyAudit audit;
    private ServicioAutenticacion servicio;

    private Usuario usuarioValido;

    @BeforeEach
    void setUp() {
        usuarioRepo = new DummyUsuarioRepo();
        sesionRepo = new DummySesionRepo();
        token = new DummyToken();
        audit = new DummyAudit();
        RelojDominio reloj = () -> Instant.parse("2027-01-01T12:00:00Z");
        // Codificador de prueba: "coincide" si la clave es exactamente "correcta".
        CodificadorPassword codificador = (plano, hash) -> "correcta".equals(plano) && hash.startsWith("$2a$");

        servicio = new ServicioAutenticacion(usuarioRepo, sesionRepo, token, codificador, reloj, audit);

        Sede sede = new Sede(1L, "sede-uuid", "Sede Central", "CENTRAL", true);
        Unidad unidad = new Unidad(1L, "unidad-uuid", sede, "UPRJ", TipoUnidad.UPRJ, true);
        Rol rol = new Rol(1L, "PROFESIONAL_UPRJ", "Profesional", Alcance.UNIDAD);
        usuarioValido = new Usuario(10L, "user-uuid", "11111111-1", "Ana", "Perez",
                "ana@defensorianinez.cl", "$2a$12$hashvalido", null, true, unidad, List.of(rol));
    }

    @Test
    void login_conCredencialesValidas_emiteParDeTokens() {
        usuarioRepo.guardar(usuarioValido);

        ResultadoAutenticacion r = servicio.login("ana@defensorianinez.cl", "correcta");

        assertThat(r.accessToken()).isEqualTo("access-para-user-uuid");
        assertThat(r.refreshToken()).isNotBlank();
        assertThat(r.expiraEnSeg()).isEqualTo(28800);
        assertThat(r.usuario().altKey()).isEqualTo("user-uuid");
        assertThat(sesionRepo.guardadas).hasSize(1);
        assertThat(audit.exitos).isEqualTo(1);
    }

    @Test
    void login_emailNormalizadoConMayusculas_funciona() {
        usuarioRepo.guardar(usuarioValido);
        ResultadoAutenticacion r = servicio.login("  ANA@DefensoriaNinez.CL ", "correcta");
        assertThat(r.usuario().altKey()).isEqualTo("user-uuid");
    }

    @Test
    void login_usuarioInexistente_lanzaCredencialesInvalidas() {
        assertThatThrownBy(() -> servicio.login("noexiste@x.cl", "correcta"))
                .isInstanceOf(CredencialesInvalidasException.class);
        assertThat(audit.fallos).isEqualTo(1);
    }

    @Test
    void login_passwordIncorrecta_lanzaCredencialesInvalidas() {
        usuarioRepo.guardar(usuarioValido);
        assertThatThrownBy(() -> servicio.login("ana@defensorianinez.cl", "incorrecta"))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    void login_usuarioNoVigente_lanzaCredencialesInvalidas() {
        Usuario noVigente = new Usuario(11L, "u2", "22222222-2", "B", "B", "b@x.cl",
                "$2a$12$hashvalido", null, false, usuarioValido.unidad(), usuarioValido.roles());
        usuarioRepo.guardar(noVigente);
        assertThatThrownBy(() -> servicio.login("b@x.cl", "correcta"))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    void refrescar_tokenValido_rotaYemiteNuevo() {
        usuarioRepo.guardar(usuarioValido);
        ResultadoAutenticacion login = servicio.login("ana@defensorianinez.cl", "correcta");

        ResultadoAutenticacion ref = servicio.refrescar(login.refreshToken());

        assertThat(ref.accessToken()).isEqualTo("access-para-user-uuid");
        assertThat(ref.refreshToken()).isNotEqualTo(login.refreshToken()); // rotado
        // El refresco original quedo revocado por rotacion.
        assertThat(sesionRepo.estaRevocadoPorHash(token.hashRefreshToken(login.refreshToken()))).isTrue();
        assertThat(audit.renovaciones).isEqualTo(1);
    }

    @Test
    void refrescar_reusoDeTokenRotado_revocaFamilia() {
        usuarioRepo.guardar(usuarioValido);
        ResultadoAutenticacion login = servicio.login("ana@defensorianinez.cl", "correcta");
        // Primer refresco: rota el original.
        servicio.refrescar(login.refreshToken());

        // Reuso del original (ya rotado, aun no vencido) -> debe revocar la familia.
        assertThatThrownBy(() -> servicio.refrescar(login.refreshToken()))
                .isInstanceOf(RefreshInvalidoException.class);
        assertThat(audit.reusos).isEqualTo(1);
        assertThat(audit.ultimoReusoUsuario).isEqualTo("user-uuid");
        assertThat(sesionRepo.familiaRevocada).isTrue();
    }

    @Test
    void refrescar_tokenDesconocido_lanzaRefreshInvalido() {
        assertThatThrownBy(() -> servicio.refrescar("token-que-no-existe"))
                .isInstanceOf(RefreshInvalidoException.class);
    }

    @Test
    void logout_revocaSesionVigente() {
        usuarioRepo.guardar(usuarioValido);
        ResultadoAutenticacion login = servicio.login("ana@defensorianinez.cl", "correcta");

        servicio.logout(login.refreshToken());

        assertThat(sesionRepo.estaRevocadoPorHash(token.hashRefreshToken(login.refreshToken()))).isTrue();
        assertThat(audit.ultimoCierreUsuario).isEqualTo("user-uuid");
        // Tras logout, el refresco ya no sirve.
        assertThatThrownBy(() -> servicio.refrescar(login.refreshToken()))
                .isInstanceOf(RefreshInvalidoException.class);
    }
}
