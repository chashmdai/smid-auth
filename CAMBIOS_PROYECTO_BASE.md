# Cambios Realizados al Proyecto Base `smid-auth`

Este documento resume los cambios aplicados durante la estabilización del rewrite
del microservicio de autenticación SMID 6.1.

## 1. Correcciones de Compilación

- Se agregaron imports faltantes en `SembradorDatos.java` para:
  - `RolJpaRepository`
  - `SedeJpaRepository`
  - `UnidadJpaRepository`
- Se reemplazó el uso directo de `new UsuarioEntity()` por una factory pública
  `UsuarioEntity.crear(...)`, manteniendo el constructor protegido para JPA.
- Se limpiaron imports sin uso en configuración y pruebas.
- Se corrigió un warning de null-safety en `SesionRefreshRepositorioJpa`.

## 2. Limpieza de Null-Safety

- Se marcaron las factories de entidades con `@NonNull` para evitar warnings del
  analizador Java/Spring:
  - `SedeEntity.crear(...)`
  - `UnidadEntity.crear(...)`
  - `RolEntity.crear(...)`
  - `UsuarioEntity.crear(...)`
- Se ajustó el sembrador para trabajar con entidades no nulas de forma explícita.

## 3. Configuración Local

- Se creó `.env` local a partir de `.env.example`.
- Se generó un `JWT_SECRET` fuerte para desarrollo.
- Se configuró la conexión local a `smid_auth_dev`.
- Se agregó `.env` al `.gitignore`.
- Se creó `.vscode/launch.json` para cargar `${workspaceFolder}/.env` al ejecutar
  la app desde VS Code.

No se documentan secretos ni contraseñas reales en este archivo.

## 4. YAML y Metadata de Spring

- Se corrigieron warnings del plugin de Spring Boot en YAML escapando claves de
  mapas:
  - `hibernate.jdbc.time_zone`
  - `hibernate.format_sql`
  - `cl.smid.auth`
- Se agregó metadata adicional en:
  - `src/main/resources/META-INF/additional-spring-configuration-metadata.json`
- Con eso VS Code/Spring reconoce las propiedades custom `smid.*`.

## 5. Esquema Flyway y Entidades JPA

- Se alinearon entidades JPA con el SQL real de Flyway usando `columnDefinition`
  para columnas `CHAR`:
  - `alt_key CHAR(36)`
  - `creado_por CHAR(36)`
  - `token_hash CHAR(64)`
  - `familia CHAR(36)`
- Se corrigió la incompatibilidad Hibernate/Flyway donde Hibernate esperaba
  `VARCHAR(36)` pero MySQL tenía `CHAR(36)`.

## 6. Catálogo de Sedes

- Se reemplazó el concepto `codigoRegion` por `codigo`.
- `sede.codigo` ahora representa un identificador institucional estable, no una
  región administrativa ni una dirección física.
- Se actualizó `V1__inicial.sql` para que el esquema nazca directamente con:
  - `sede.codigo VARCHAR(32)`
  - `UNIQUE KEY uk_sede_codigo`
- Se incorporó el catálogo base de sedes en `V1__inicial.sql`:
  - `CENTRAL`
  - `ARICA_PARINACOTA`
  - `TARAPACA`
  - `ANTOFAGASTA`
  - `COQUIMBO`
  - `VALPARAISO`
  - `OHIGGINS`
  - `BIOBIO`
  - `ARAUCANIA`
  - `AYSEN_MAGALLANES`
- Se eliminó la migración `V2__catalogo_sedes_base.sql` porque el proyecto sigue
  en desarrollo y se decidió aplanar el esquema en `V1`.

## 7. Sembrador de Datos

- `SembradorDatos` sigue restringido a perfiles `local` y `dev`.
- El sembrador ya no crea una sede arbitraria como fuente principal.
- Ahora busca la sede `CENTRAL` y la usa para crear la unidad y el usuario admin
  local.
- Si `CENTRAL` no existiera, la crea como fallback defensivo.

Usuario local sembrado:

```text
email: admin@defensorianinez.cl
password: valor de SEED_PASSWORD
```

## 8. Auditoría de Autenticación

- Se mantuvo la auditoría propia de `auth` para eventos de sesión:
  - login exitoso
  - login fallido
  - refresh
  - logout
  - reuso de refresh token
- Se corrigió el log para no exponer ids internos como `usuarioId=1`.
- Los eventos de logout y reuso ahora registran `usuario=<alt_key>`.
- Si no se puede resolver el usuario, se registra `usuario=desconocido`.
- Se agregaron asserts en pruebas para asegurar que auditoría usa `altKey`.

## 9. README y Documentación

- Se actualizó el README principal (`README.md`) para reflejar:
  - configuración con `.env`
  - ejecución local en PowerShell y VS Code
  - uso de `smid_auth_dev`
  - catálogo de sedes con `codigo`
  - contrato de login con `sede.codigo`
  - auditoría y trazabilidad
  - diferencia entre auditoría propia de `auth` y futura auditoría transversal
  - recomendaciones para producción
- Se actualizó el documento de refactor del Gateway:
  - `C:/Users/Benja/Downloads/auditoria-refactor-gateway.md`
- Ese documento ahora está alineado con el contrato real de `smid-auth`.

## 10. Base de Datos de Desarrollo

- Se reseteó `smid_auth_dev` en MySQL dev para que Flyway aplicara el `V1`
  actualizado desde cero.
- La BD quedó vacía antes del arranque.
- Al iniciar la app, Flyway creó el esquema y el sembrador pobló los datos de
  desarrollo.

## 11. Verificaciones Realizadas

Pruebas unitarias:

```text
.\mvnw.cmd test
BUILD SUCCESS
Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
```

Prueba funcional local:

```text
GET /actuator/health -> UP
POST /auth/login -> 200 con accessToken y refreshToken
POST /auth/refresh -> 200 y refresh token rotado
POST /auth/logout -> 204
POST /auth/refresh con token revocado -> 401 AUTZ-002
```

Validación de sede en respuesta de login:

```text
nombre: Sede Central
codigo: CENTRAL
```

## 12. Pendientes Relevantes

- Definir mecanismo formal de carga inicial para producción.
- No usar el sembrador local/dev en producción.
- Coordinar `JWT_SECRET`, `JWT_KID`, `JWT_ISSUER` y `JWT_AUDIENCE` con el Gateway.
- Refactorizar el Gateway para:
  - externalizar secretos
  - validar `kid`, `iss`, `aud` y `exp`
  - adoptar el sobre de error vigente
  - usar CORS por entorno
  - conservar trazabilidad con `X-Request-Id`
- Evaluar una auditoría transversal futura para eventos de negocio:
  - quién creó
  - quién modificó
  - quién vio
  - qué recurso fue afectado
