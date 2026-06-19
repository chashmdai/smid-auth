# 6.1 — Identidad y Acceso (`smid-auth`)

Microservicio de autenticación del ecosistema **SMID**. Es la fuente única de la
estructura **Sede → Unidad → Profesional** y de los roles, y la única autoridad
emisora de tokens **JWT HS256** del ecosistema. Corre en el puerto `8081`, detrás
del API Gateway (que lo expone como `/api/auth/**`).

> Rediseño completo (*rewrite total*) del componente de autenticación. Reemplaza la
> base anterior `cl.smid.auth` por una arquitectura hexagonal limpia, cerrando las
> deudas técnicas P0 del Núcleo Fundacional (DT-2, DT-3, DT-4, DT-5) y formalizando
> el contrato de claims territoriales 2.4.

---

## 1. Responsabilidad del servicio

Este servicio **solo emite y renueva** credenciales. **No valida** los tokens de
las peticiones entrantes a otros servicios: esa validación de firma/expiración/
emisor/audiencia la realizan el Gateway y cada microservicio de negocio aguas
abajo (defensa en profundidad). Por eso aquí **no hay** filtro JWT ni
`oauth2-resource-server`: sería código muerto contrario a la responsabilidad del
componente.

```
         POST /api/auth/login                emite JWT (HS256, claims 2.4)
React ───────────────────────────► Gateway ───────────────► Auth Service :8081
                                   (StripPrefix /api)              │
                                    (valida firma en               ▼
                                     cada request posterior)    MySQL (BD nueva)
```

La clave secreta HS256 es **compartida** entre este servicio y el Gateway, y debe
coincidir **byte a byte**.

---

## 2. Stack

| Componente | Versión / Detalle |
|---|---|
| Java | 21 (virtual threads habilitados) |
| Spring Boot | 3.5.15 |
| Seguridad | Spring Security — login stateless, BCrypt (costo 12) |
| Persistencia | Spring Data JPA + Hibernate, MySQL 8 (InnoDB, `utf8mb4`) |
| Migraciones | Flyway (`flyway-core` + `flyway-mysql`) |
| JWT | jjwt 0.12.5 — firma HS256 simétrica con `kid` |
| OpenAPI | SpringDoc OpenAPI 2.8.x — Swagger UI para Spring MVC |
| Build | Maven |
| Utilidades | Lombok |

---

## 3. Arquitectura hexagonal (Ports & Adapters)

El código se organiza en cuatro paquetes raíz independientes. La regla de
dependencia apunta **siempre hacia el dominio**: `api` e `infraestructura`
dependen de `dominio`; `dominio` no depende de nadie (ni de Spring, ni de JPA, ni
de jjwt).

```
cl.smid.auth
├── api                      ← adaptador de ENTRADA (REST)
│   ├── AuthController            POST /auth/{login,refresh,logout}
│   ├── MapeadorRespuesta         dominio → DTO público (oculta ids internos)
│   ├── dto/                      LoginRequest, RefreshRequest, AuthResponse,
│   │                            UsuarioDTO, SedeDTO, UnidadDTO
│   └── error/                    ErrorResponse + ManejadorGlobalExcepciones
│
├── dominio                  ← NÚCLEO puro (sin framework)
│   ├── modelo/                   Usuario, Sede, Unidad, Rol, Alcance, TipoUnidad
│   ├── puerto/entrada/           AutenticacionUseCase, ResultadoAutenticacion
│   ├── puerto/salida/            UsuarioRepositorio, SesionRefreshRepositorio,
│   │                            ProveedorToken, CodificadorPassword, RelojDominio
│   ├── servicio/                 ServicioAutenticacion (lógica de negocio),
│   │                            AuthAuditPort (costura de trazabilidad)
│   └── excepcion/                AuthException, CodigoError (catálogo AUTZ-xxx)
│
├── infraestructura          ← adaptadores de SALIDA
│   ├── persistencia/             *Entity (JPA) + adaptador/ (repos + mapeador)
│   ├── seguridad/                ProveedorTokenJwt, CodificadorPasswordBCrypt,
│   │                            RelojSistema, PropiedadesJwt
│   └── auditoria/                AuditoriaLog (trazabilidad síncrona)
│
└── config                   ← composition root
    ├── SeguridadConfig           cadena de filtros, BCrypt, rutas públicas
    ├── DominioConfig             cablea el ServicioAutenticacion (POJO)
    └── SembradorDatos            datos de ejemplo (SOLO perfiles local|dev)
```

**Por qué importa:** el `ServicioAutenticacion` es un POJO sin anotaciones de
Spring. Se construye en `DominioConfig` inyectándole sus puertos, y se prueba con
dobles en memoria sin levantar contexto (ver `src/test`). El framework vive en los
bordes, nunca en el centro.

---

## 4. Modelo de datos

Jerarquía estricta **Sede → Unidad → Profesional** (1:N:N). El esquema y el
catálogo base lo crea Flyway en `V1__inicial.sql`; Hibernate solo lo **valida**
(`ddl-auto=validate`).

| Tabla | Rol | Relaciones |
|---|---|---|
| `sede` | raíz territorial | — |
| `unidad` | área operativa | N:1 → `sede` (`id_sede`) |
| `rol` | catálogo de roles + `alcance` | — |
| `usuario` | profesional interno | N:1 → `unidad` (`id_unidad`), N:M → `rol` (`usuario_rol`) |
| `usuario_rol` | unión N:M | — |
| `sesion_refresh` | lista de revocación de refresh tokens | N:1 → `usuario` (`id_usuario`) |

Reglas transversales aplicadas (Núcleo 2.2 / 2.6):

- **`alt_key CHAR(36)`** (UUID) en `sede`, `unidad`, `usuario`: identificador
  **público opaco**. Las PK `BIGINT` son **privadas** y jamás cruzan la frontera
  del servicio (corrige de raíz el IDOR del legado). `rol` se identifica
  públicamente por su `codigo` estable, por lo que no requiere `alt_key`.
- `sede.codigo` es un identificador institucional estable (`CENTRAL`,
  `ARICA_PARINACOTA`, etc.). No modela direcciones ni necesariamente regiones
  administrativas; para autorización solo importa la pertenencia del usuario a
  una sede y su alcance (`UNIDAD`, `SEDE`, `NACIONAL`).
- Las entidades JPA declaran explícitamente las columnas `CHAR(36)`/`CHAR(64)`
  con `columnDefinition`, para que `ddl-auto=validate` coincida con el esquema
  creado por Flyway. Si se cambia el SQL, se debe actualizar también la entidad.
- Columnas estándar `vigente` (borrado lógico), `creado_en`, `actualizado_en`,
  `creado_por`.
- `DATETIME(6)` en UTC (la app escribe siempre en UTC); `ENUM` modelados como
  `VARCHAR` + `CHECK` para estabilidad frente al validador de esquema.

### Token de refresco — rotación y detección de reúso

`sesion_refresh` **no** almacena el token en claro: guarda su **SHA-256**. Cada
refresco se usa **una sola vez** (rotación *one-time*): al renovar se revoca el
anterior y se emite otro en la misma `familia`. Si reaparece un refresco ya
rotado (replay), se **revoca la familia completa**.

---

## 5. Contrato de la API

Rutas internas del servicio (el Gateway antepone `/api` y aplica *StripPrefix*).
Todas son **públicas** porque validan la credencial en su propio cuerpo.

### `POST /auth/login`  → `POST /api/auth/login`

El frontend consume esta operacion via Gateway en `POST /api/auth/login`; el
servicio la recibe internamente como `POST /auth/login`.

**Request**
```json
{ "email": "admin@defensorianinez.cl", "password": "secreto" }
```

**Respuesta `200 OK`**
```json
{
  "accessToken": "<jwt>",
  "refreshToken": "<token-opaco>",
  "expiraEn": 28800,
  "usuario": {
    "altKey": "8f3b...-uuid",
    "nombres": "Admin",
    "apellidos": "SMID",
    "sede":   { "altKey": "uuid-sede",   "nombre": "Sede Central", "codigo": "CENTRAL" },
    "unidad": { "altKey": "uuid-unidad", "nombre": "Unidad de Protección y Representación Judicial", "tipo": "UPRJ" },
    "roles":  ["ADMIN_NACIONAL", "PROFESIONAL_UPRJ"],
    "alcance": "NACIONAL"
  }
}
```

`expiraEn` es la vigencia del *access token* en segundos. El `refreshToken` viaja
en el cuerpo; el cliente lo custodia y lo presenta en `/auth/refresh`.

### `POST /auth/refresh`  → `POST /api/auth/refresh`

El frontend consume esta operacion via Gateway en `POST /api/auth/refresh`; el
servicio la recibe internamente como `POST /auth/refresh`.

Renueva el *access token* y **rota** el refresco.

```json
{ "refreshToken": "<token-opaco>" }
```
Respuesta: idéntica a la de login (con un `refreshToken` nuevo).

### `POST /auth/logout`  → `POST /api/auth/logout`

El frontend consume esta operacion via Gateway en `POST /api/auth/logout`; el
servicio la recibe internamente como `POST /auth/logout`.

Revoca la sesión asociada al refresco. Idempotente.

```json
{ "refreshToken": "<token-opaco>" }
```
Respuesta: `204 No Content`.

### Documentacion OpenAPI / Swagger

La especificacion OpenAPI queda disponible en:

```text
GET /v3/api-docs
```

La interfaz Swagger UI queda disponible en:

```text
GET /swagger-ui/index.html
```

Los endpoints de autenticacion son publicos y por eso la documentacion no define
Bearer JWT como requisito global. Los contratos documentados usan solamente DTOs
publicos de la capa `api` y mantienen identificadores opacos `altKey`, sin
exponer entidades JPA, ids numericos internos ni detalles de la logica de
autenticacion.

---

## 6. Contrato del token JWT (Núcleo 2.4)

Firma **HS256** con la clave del `kid` activo. La cabecera incluye `kid` para que
los consumidores seleccionen la clave correcta durante una rotación.

**Cabecera**
```json
{ "alg": "HS256", "typ": "JWT", "kid": "smid-2026-06" }
```

**Payload**
```json
{
  "sub": "8f3b...-uuid",         // alt_key del USUARIO (no el RUT, no el id interno)
  "iss": "smid-auth",
  "aud": "smid-servicios",
  "jti": "<uuid>",               // identificador único (habilita revocación)
  "roles": ["ADMIN_NACIONAL"],
  "idSede": "uuid-sede",         // alt_key de la sede (no id numérico)
  "idUnidad": "uuid-unidad",     // alt_key de la unidad (no id numérico)
  "alcance": "NACIONAL",         // UNIDAD | SEDE | NACIONAL
  "nombre": "Admin SMID",
  "iat": 0,
  "exp": 0
}
```

El `alcance` es el **máximo** de los alcances de los roles del usuario
(`NACIONAL > SEDE > UNIDAD`). Es la pieza angular del modelo territorial 2.3: con
`idSede`/`idUnidad`/`alcance`, los servicios de Personas, Catálogo y
Requerimientos filtran registro a registro sin volver a consultar a Identidad.

---

## 7. Manejo de errores (sobre unificado 2.5)

Todas las respuestas de error usan el sobre del ecosistema. El frontend y los
servicios consumidores conmutan sobre `codigo` (estable), no sobre `mensaje`.

```json
{
  "status": 401,
  "error": "Unauthorized",
  "codigo": "AUTZ-001",
  "mensaje": "Credenciales inválidas. Verifique sus datos de acceso.",
  "detalles": { "email": "El email no tiene un formato válido" },
  "ruta": "/auth/login",
  "timestamp": "2027-01-01T12:00:00Z"
}
```

`detalles` (mapa campo→mensaje) aparece **solo** en errores de validación.

| Código | HTTP | Caso |
|---|---|---|
| `AUTZ-001` | `401` | Login fallido (causa **indistinguible**: usuario inexistente, contraseña incorrecta o cuenta no vigente) |
| `AUTZ-002` | `401` | Refresco inválido (desconocido, vencido, rotado, revocado o usuario no vigente) |
| `AUTZ-003` | `401` | No autenticado (falta token en ruta protegida) |
| `AUTZ-004` | `403` | Acceso denegado por rol/alcance |
| `AUTZ-005` | `400` | Validación de DTO fallida (`+ detalles`) |
| `AUTZ-404` | `404` | Recurso no encontrado |
| `AUTZ-500` | `500` | Error interno (nunca filtra el detalle al cliente; queda en el log) |

---

## 8. Seguridad

- **Hashing:** BCrypt, costo 12.
- **Anti-enumeración:** un email inexistente y una contraseña incorrecta devuelven
  el mismo `401 AUTZ-001`. Además, el login ejecuta **siempre** una verificación
  BCrypt (con un hash *señuelo* cuando el usuario no existe) para igualar el tiempo
  de respuesta y cerrar el canal lateral de temporización.
- **Stateless:** sin sesión de servidor (`SessionCreationPolicy.STATELESS`), CSRF
  deshabilitado.
- **Refresh tokens:** opacos (256 bits de `SecureRandom`), persistidos solo como
  SHA-256, con rotación *one-time* y corte de familia ante reúso.
- **Rutas públicas:** `/auth/{login,refresh,logout}` y `/actuator/{health,info}`.
  Todo lo demás exige autenticación (`@EnableMethodSecurity` activo para futuros
  endpoints administrativos con `@PreAuthorize`).
- **Trazabilidad de sesión:** `auth` registra eventos propios de autenticación en
  `AUDIT.SMID.AUTH` usando identificadores opacos (`alt_key`), nunca ids internos,
  contraseñas ni tokens.
- **Secretos externalizados (DT-2):** ningún secreto vive en archivos versionados.

---

## 9. Auditoría y Trazabilidad

Este servicio mantiene una auditoría **operativa y de seguridad** de su propio
contexto: autenticación y sesiones. No es el repositorio central de auditoría de
todo SMID.

Eventos registrados por `auth`:

| Evento | Logger | Identificador |
|---|---|---|
| Login exitoso | `AUDIT.SMID.AUTH` | `usuario=<alt_key>`, `unidad=<alt_key>`, `alcance=<valor>` |
| Login fallido | `AUDIT.SMID.AUTH` | `identificador=<email normalizado>` |
| Sesión renovada | `AUDIT.SMID.AUTH` | `usuario=<alt_key>` |
| Sesión cerrada | `AUDIT.SMID.AUTH` | `usuario=<alt_key>` |
| Reúso de refresh token | `AUDIT.SMID.AUTH` | `usuario=<alt_key>`, `familia=<uuid>` |

Los ids internos (`usuario.id`, `sede.id`, `unidad.id`) son privados de la base de
datos y no deben aparecer en logs de auditoría ni contratos públicos. Si un evento
de sesión ya no puede resolver el usuario asociado, se registra
`usuario=desconocido` en vez de filtrar el id interno.

Para el resto del ecosistema, cada microservicio debe emitir sus propios eventos
de negocio: quién creó/modificó/vio un recurso, qué recurso fue afectado, desde
qué servicio y con qué resultado. Esos eventos deberían centralizarse luego en un
servicio o infraestructura transversal de auditoría, por ejemplo vía cola/eventos
o un colector dedicado.

Ejemplo de evento futuro fuera de `auth`:

```json
{
  "actor": "a33239a3-2200-4782-96d1-a1e915c4340f",
  "servicio": "personas",
  "accion": "PERSONA_ACTUALIZADA",
  "recurso": "persona",
  "recursoId": "uuid-publico",
  "resultado": "OK",
  "timestamp": "2026-06-12T19:25:46Z"
}
```

Auditar lecturas (`quién vio`) puede generar mucho volumen y debe reservarse para
recursos sensibles o vistas críticas, como fichas, casos, documentos o datos
personales especialmente protegidos.

---

## 10. Configuración

`application.yml` contiene **solo** referencias `${VARIABLE}`. Las variables se
documentan en `.env.example`. El archivo `.env` local es solo para desarrollo,
debe estar ignorado por Git y nunca debe versionarse con secretos reales.

| Variable | Descripción | Ejemplo / Default |
|---|---|---|
| `JWT_SECRET` | Clave HS256 (≥ 256 bits). **Compartida con el Gateway, byte a byte.** | *(sin default — obligatoria)* |
| `JWT_KID` | `kid` de la clave activa de firma | `smid-2026-06` |
| `JWT_ISSUER` | Claim `iss` | `smid-auth` |
| `JWT_AUDIENCE` | Claim `aud` | `smid-servicios` |
| `JWT_EXPIRATION_MS` | Vigencia del *access token* (ms) | `28800000` (8 h) |
| `JWT_REFRESH_EXPIRATION_MS` | Vigencia del refresco (ms) | `86400000` (24 h) |
| `DB_URL` | JDBC de MySQL | Local: `jdbc:mysql://localhost:3306/smid_auth_dev?...`; prod: `jdbc:mysql://<host>:3306/db_auth?...` |
| `DB_USER` | Usuario de BD (no usar `root`) | `smid_auth` |
| `DB_PASSWORD` | Contraseña de BD | *(sin default — obligatoria)* |
| `RBAC_ENABLED` | Activa la autorización por roles | `true` |
| `SPRING_PROFILES_ACTIVE` | Perfil activo | `local` \| `dev` \| `prod` |
| `SEED_PASSWORD` | Contraseña de los usuarios semilla (**solo** `local`/`dev`) | `Smid.Local.2026` |

### Rotación de clave HS256 (DT-3)

El mapa `smid.jwt.claves` asocia `kid → secreto`. Para rotar sin invalidar tokens
vigentes:

1. Agregar la clave nueva con su `kid` al mapa `smid.jwt.claves` (y su variable
   de entorno).
2. Cambiar `JWT_KID` al nuevo `kid` (se firma con la clave nueva).
3. Conservar la clave anterior en el mapa durante la ventana de rotación (los
   consumidores aún validan tokens viejos por su `kid`).
4. Retirar la clave anterior cuando hayan expirado todos los tokens emitidos con
   ella.

En este repo el mapa vive en `application.yml`; si se agrega un `kid` nuevo, debe
existir una entrada explícita para ese `kid` (por ejemplo, otra variable
`JWT_SECRET_2026_09` o el mecanismo equivalente del orquestador).

---

## 11. Cómo correr en **desarrollo** (perfil `local`)

Requisitos: **JDK 21**, **Maven 3.9+**, **MySQL 8** en marcha.

Si vienes del servicio legado, **no reutilices** una base con tablas antiguas
(`users`, `roles`, `sedes`, etc.). Este rewrite usa un esquema nuevo (`usuario`,
`rol`, `sede`, `unidad`, `sesion_refresh`) y Flyway debe crearlo desde cero.

### 11.1 Crear la base de datos y el usuario

```sql
CREATE DATABASE smid_auth_dev CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'smid_auth'@'%' IDENTIFIED BY 'una-clave-de-dev';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
ON smid_auth_dev.* TO 'smid_auth'@'%';
FLUSH PRIVILEGES;
```

No ejecutes ningún `CREATE TABLE` a mano: **Flyway** crea el esquema y el catálogo
base desde las migraciones `src/main/resources/db/migration`.

### 11.2 Definir las variables de entorno

Copia `.env.example` y complétalo:

```bash
cp .env.example .env
openssl rand -base64 48
```

Copia el valor generado en la línea `JWT_SECRET=` de `.env`.

En Windows, si no tienes OpenSSL, genera un secreto equivalente con PowerShell:

```powershell
$bytes = New-Object byte[] 48
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
[Convert]::ToBase64String($bytes)
```

Edita `.env` con la BD local:

```env
DB_URL=jdbc:mysql://localhost:3306/smid_auth_dev?useSSL=false&allowPublicKeyRetrieval=true&connectionTimeZone=UTC
DB_USER=smid_auth
DB_PASSWORD=una-clave-de-dev
SPRING_PROFILES_ACTIVE=local
```

Si MySQL está en otro host pero trabajas con túnel SSH, mantén
`localhost:3306` en `DB_URL` y abre el túnel antes de arrancar la app:

```bash
ssh -p <puerto-ssh> -L 3306:127.0.0.1:3306 usuario@host -N
```

Exporta las variables a la sesión (o usa tu IDE / un cargador de `.env`):

```bash
set -a && source .env && set +a
```

En PowerShell:

```powershell
Get-Content .env | Where-Object { $_ -and $_ -notmatch '^\s*#' } | ForEach-Object {
  $name, $value = $_ -split '=', 2
  Set-Item -Path "Env:$name" -Value $value
}
```

En VS Code puedes crear `.vscode/launch.json` con `envFile`:

```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "Run smid-auth (local)",
      "request": "launch",
      "mainClass": "cl.smid.auth.AuthApplication",
      "projectName": "smid-auth",
      "envFile": "${workspaceFolder}/.env"
    }
  ]
}
```

### 11.3 Arrancar

```bash
./mvnw spring-boot:run
```

En Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Con `SPRING_PROFILES_ACTIVE=local`, el **`SembradorDatos`** se activa y, si la
tabla `usuario` está vacía, crea una sede, una unidad y un usuario de ejemplo:

```
email:    admin@defensorianinez.cl
password: el valor de SEED_PASSWORD   (nunca se imprime en el log)
```

### 11.4 Probar el login

```bash
curl -s -X POST http://localhost:8081/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@defensorianinez.cl","password":"Smid.Local.2026"}' | jq
```

En PowerShell:

```powershell
$login = Invoke-RestMethod `
  -Method Post `
  -Uri http://localhost:8081/auth/login `
  -ContentType 'application/json' `
  -Body (@{
    email = 'admin@defensorianinez.cl'
    password = 'Smid.Local.2026'
  } | ConvertTo-Json)

$login
```

Refresh y logout:

```powershell
$refresh = Invoke-RestMethod `
  -Method Post `
  -Uri http://localhost:8081/auth/refresh `
  -ContentType 'application/json' `
  -Body (@{ refreshToken = $login.refreshToken } | ConvertTo-Json)

Invoke-WebRequest `
  -Method Post `
  -Uri http://localhost:8081/auth/logout `
  -ContentType 'application/json' `
  -Body (@{ refreshToken = $refresh.refreshToken } | ConvertTo-Json)
```

El perfil `local` además activa `show-sql` y logging `DEBUG` de `cl.smid.auth`.

### 11.5 Pruebas

```bash
./mvnw test
```

En Windows:

```powershell
.\mvnw.cmd test
```

Ejecuta las pruebas unitarias del dominio (login feliz, anti-enumeración, rotación
de refresco, detección de reúso, logout) con dobles en memoria — no requieren BD.

---

## 12. Cómo correr en **producción** (perfil `prod`)

### 12.1 Empaquetar

```bash
./mvnw clean package
# genera target/smid-auth-1.0.0.jar
```

### 12.2 Provisionar la base de datos

Crea una BD limpia, por ejemplo `db_auth` (`utf8mb4`), y un usuario **dedicado**
(jamás `root`). Flyway aplicará las migraciones versionadas en el primer arranque.

Opción simple para el primer despliegue: el usuario de la app tiene permisos DDL
para que Flyway migre al arrancar.

```sql
CREATE DATABASE db_auth CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'smid_auth'@'%' IDENTIFIED BY '<clave-fuerte>';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
ON db_auth.* TO 'smid_auth'@'%';
FLUSH PRIVILEGES;
```

Opción endurecida: ejecutar Flyway con un usuario migrador y dejar a la app solo
con permisos DML (`SELECT`, `INSERT`, `UPDATE`, `DELETE`) después de migrar.

### 12.3 Inyectar la configuración (sin archivos de secretos)

Define las variables vía gestor de secretos / variables de entorno del orquestador
(Docker secrets, Kubernetes Secrets, etc.). **Mínimo obligatorio:**

```bash
export SPRING_PROFILES_ACTIVE=prod
export JWT_SECRET='<clave HS256 idéntica a la del Gateway>'
export JWT_KID='smid-2026-06'
export JWT_ISSUER='smid-auth'
export JWT_AUDIENCE='smid-servicios'
export JWT_EXPIRATION_MS=28800000
export JWT_REFRESH_EXPIRATION_MS=86400000
export DB_URL='jdbc:mysql://<host-prod>:3306/db_auth?useSSL=true&connectionTimeZone=UTC'
export DB_USER='smid_auth'
export DB_PASSWORD='<clave fuerte>'
export RBAC_ENABLED=true
```

Puntos críticos de producción:

- **`JWT_SECRET` debe coincidir byte a byte con el del Gateway.** Si difieren,
  el Gateway rechazará todos los tokens.
- `JWT_KID` identifica la clave activa. Cambia cuando rotas `JWT_SECRET`; no se
  rota diariamente. `JWT_ISSUER` y `JWT_AUDIENCE` son parte del contrato y deben
  coincidir con lo que valida el Gateway.
- En `prod` el `SembradorDatos` **no se ejecuta** (está confinado a `local`/`dev`):
  los usuarios reales se cargan por el proceso de aprovisionamiento del padrón.
- Usa `useSSL=true`, configura certificados/truststore según tu infraestructura
  MySQL y nunca uses `root`. En la opción endurecida, la app queda sin permisos
  DDL después de migrar.
- No definas `SEED_PASSWORD` en producción.
- Define un proceso explícito para la carga inicial real: catálogo de sedes,
  unidades, roles y primer usuario administrador. No uses el sembrador local para
  producción.

### 12.4 Arrancar

```bash
java -jar target/smid-auth-1.0.0.jar
```

### 12.5 Verificar salud

```bash
curl -s http://<host>:8081/actuator/health
# {"status":"UP"}
```

En producción este servicio queda **detrás del Gateway**; no se expone
directamente a Internet. El Gateway es responsable de CORS, *rate limiting* y de
validar la firma de cada token en las rutas protegidas.

---

## 13. Notas operativas

- **Flyway es el único dueño del esquema.** Nunca modifiques tablas a mano; toda
  evolución va en una nueva migración `V2__...`, `V3__...`. Con `ddl-auto=validate`,
  si las entidades y el esquema divergen, el servicio **no arranca** (es
  intencional: falla rápido y visible).
- **Trazabilidad síncrona (D4):** los eventos de sesión se registran en
  `AUDIT.SMID.AUTH`. La costura `AuthAuditPort` permite añadir después un
  adaptador de publicación a cola o servicio de auditoría sin tocar el núcleo.
- **Limpieza de `sesion_refresh`:** la tabla acumula sesiones rotadas/vencidas. Se
  recomienda una purga periódica de filas con `expira_en < NOW()` (job externo o
  evento programado); el índice `idx_sesion_refresh_expira` la soporta.
