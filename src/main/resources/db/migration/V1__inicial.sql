-- ============================================================================
-- SMID 6.1 - Identidad y Acceso
-- V1__inicial.sql : esquema fundacional inmaculado (rewrite total, D6).
--
-- Fuente normativa: Nucleo Fundacional SMID, secciones 2.2 (alt_key), 2.6
-- (persistencia), 3.3 (modelo de datos objetivo del servicio 6.1).
--
-- Decisiones de implementacion documentadas (desviaciones controladas):
--  [1] ENUM -> VARCHAR + CHECK. Semanticamente identico para el dominio, pero
--      estable frente al validador de esquema de Hibernate (ddl-auto=validate),
--      que es quien custodia la correspondencia entidad<->tabla en el arranque.
--  [2] TIMESTAMP -> DATETIME(6). Evita el limite del ano 2038 y la conversion
--      implicita de zona horaria de TIMESTAMP. La aplicacion escribe SIEMPRE
--      en UTC (hibernate.jdbc.time_zone=UTC + callbacks de entidad en UTC).
--  [3] Se agregan actualizado_en y creado_por a las tablas de negocio para
--      cumplir el estandar transversal 2.6 (el modelo 3.3 los omite por brevedad).
--  [4] Las llaves primarias BIGINT son PRIVADAS de esta base de datos: jamas
--      cruzan la frontera del servicio. Todo contrato publico viaja por alt_key
--      (regla inviolable 2.2, correccion de raiz del IDOR del legado).
--  [5] 'sede.codigo' es un identificador institucional estable de la sede. No
--      modela direcciones ni necesariamente regiones administrativas; para
--      autorizacion importan la sede del usuario y su alcance efectivo.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- SEDE: raiz de la jerarquia territorial Sede -> Unidad -> Profesional.
-- ----------------------------------------------------------------------------
CREATE TABLE sede (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    alt_key         CHAR(36)     NOT NULL,                 -- identificador publico opaco (UUID)
    nombre          VARCHAR(160) NOT NULL,
    codigo          VARCHAR(32)  NOT NULL,                 -- codigo institucional estable [5]
    vigente         TINYINT(1)   NOT NULL DEFAULT 1,       -- borrado logico: no hay borrado fisico
    creado_en       DATETIME(6)  NOT NULL,
    actualizado_en  DATETIME(6)  NOT NULL,
    creado_por      CHAR(36)     NULL,                     -- alt_key del usuario creador (NULL = sistema)
    PRIMARY KEY (id),
    UNIQUE KEY uk_sede_alt_key (alt_key),
    UNIQUE KEY uk_sede_codigo (codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------------------------------------------------------
-- UNIDAD: pertenece a una sede; tipifica el area operativa.
-- ----------------------------------------------------------------------------
CREATE TABLE unidad (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    alt_key         CHAR(36)     NOT NULL,
    id_sede         BIGINT       NOT NULL,
    nombre          VARCHAR(160) NOT NULL,
    tipo            VARCHAR(12)  NOT NULL,                 -- UPRJ | UEG | UPDD | GABINETE  [1]
    vigente         TINYINT(1)   NOT NULL DEFAULT 1,
    creado_en       DATETIME(6)  NOT NULL,
    actualizado_en  DATETIME(6)  NOT NULL,
    creado_por      CHAR(36)     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_unidad_alt_key (alt_key),
    KEY idx_unidad_sede (id_sede),                         -- indice territorial explicito (2.6)
    CONSTRAINT fk_unidad_sede FOREIGN KEY (id_sede) REFERENCES sede (id),
    CONSTRAINT chk_unidad_tipo CHECK (tipo IN ('UPRJ','UEG','UPDD','GABINETE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------------------------------------------------------
-- ROL: catalogo de roles. Su identificador publico y estable es 'codigo'
-- (p. ej. PROFESIONAL_UPRJ); por eso no requiere alt_key (3.3).
-- 'alcance' alimenta la autorizacion territorial registro a registro (2.3).
-- ----------------------------------------------------------------------------
CREATE TABLE rol (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    codigo      VARCHAR(40)  NOT NULL,                     -- 'PROFESIONAL_UPRJ', 'COORDINACION_SEDE', ...
    nombre      VARCHAR(120) NOT NULL,
    alcance     VARCHAR(10)  NOT NULL DEFAULT 'UNIDAD',    -- UNIDAD | SEDE | NACIONAL  [1]
    vigente     TINYINT(1)   NOT NULL DEFAULT 1,
    creado_en   DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_rol_codigo (codigo),
    CONSTRAINT chk_rol_alcance CHECK (alcance IN ('UNIDAD','SEDE','NACIONAL'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------------------------------------------------------
-- USUARIO: personal interno (profesionales). Las personas de casos NO viven
-- aqui (pertenecen a 6.2 Personas). 'vigente' gobierna existencia logica y
-- habilitacion de inicio de sesion.
-- ----------------------------------------------------------------------------
CREATE TABLE usuario (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    alt_key         CHAR(36)     NOT NULL,                 -- viaja como 'sub' en el token (2.4)
    id_unidad       BIGINT       NOT NULL,
    rut             VARCHAR(12)  NOT NULL,                 -- normalizado: sin puntos, con guion, K mayuscula
    nombres         VARCHAR(120) NOT NULL,
    apellidos       VARCHAR(120) NOT NULL,
    email           VARCHAR(160) NOT NULL,                 -- almacenado en minusculas
    password_hash   VARCHAR(72)  NOT NULL,                 -- BCrypt (costo >= 10; configurado en 12)
    cargo           VARCHAR(120) NULL,
    vigente         TINYINT(1)   NOT NULL DEFAULT 1,
    creado_en       DATETIME(6)  NOT NULL,
    actualizado_en  DATETIME(6)  NOT NULL,
    creado_por      CHAR(36)     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_usuario_alt_key (alt_key),
    UNIQUE KEY uk_usuario_rut (rut),
    UNIQUE KEY uk_usuario_email (email),
    KEY idx_usuario_unidad (id_unidad),                    -- indice territorial explicito (2.6)
    CONSTRAINT fk_usuario_unidad FOREIGN KEY (id_unidad) REFERENCES unidad (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------------------------------------------------------
-- USUARIO_ROL: asignacion N:M de roles. Tabla tecnica de union pura.
-- ----------------------------------------------------------------------------
CREATE TABLE usuario_rol (
    id_usuario  BIGINT NOT NULL,
    id_rol      BIGINT NOT NULL,
    PRIMARY KEY (id_usuario, id_rol),
    KEY idx_usuario_rol_rol (id_rol),
    CONSTRAINT fk_usuario_rol_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id),
    CONSTRAINT fk_usuario_rol_rol     FOREIGN KEY (id_rol)     REFERENCES rol (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------------------------------------------------------
-- SESION_REFRESH: lista de revocacion en tabla exigida por 3.5, implementada
-- con rotacion de token de refresco y deteccion de reuso por familia.
--   * El token de refresco es OPACO (256 bits aleatorios); aqui se persiste
--     solo su hash SHA-256: una fuga de la tabla no compromete sesiones.
--   * 'familia' agrupa la cadena de rotaciones nacida en un login; si se
--     presenta un token ya rotado (replay), se revoca la familia completa.
--   * Tabla tecnica interna: no expone alt_key porque jamas cruza la API.
-- ----------------------------------------------------------------------------
CREATE TABLE sesion_refresh (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    id_usuario          BIGINT      NOT NULL,
    token_hash          CHAR(64)    NOT NULL,              -- SHA-256 hex del token opaco
    familia             CHAR(36)    NOT NULL,              -- UUID de la cadena de rotacion
    emitido_en          DATETIME(6) NOT NULL,
    expira_en           DATETIME(6) NOT NULL,
    revocado_en         DATETIME(6) NULL,                  -- NULL = sesion activa
    motivo_revocacion   VARCHAR(30) NULL,                  -- ROTACION | REUSO | LOGOUT | USUARIO_NO_VIGENTE
    PRIMARY KEY (id),
    UNIQUE KEY uk_sesion_refresh_hash (token_hash),
    KEY idx_sesion_refresh_familia (familia),
    KEY idx_sesion_refresh_usuario (id_usuario),
    KEY idx_sesion_refresh_expira (expira_en),             -- soporta la purga periodica de vencidos
    CONSTRAINT fk_sesion_refresh_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------------------------------------------------------
-- CATALOGO BASE DE SEDES: datos institucionales estables usados por Identidad
-- para asociar usuarios a una sede. No incluye direcciones fisicas.
-- ----------------------------------------------------------------------------
INSERT INTO sede (alt_key, nombre, codigo, vigente, creado_en, actualizado_en, creado_por)
VALUES
    ('11111111-1111-4111-8111-111111111111', 'Sede Central', 'CENTRAL', 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), NULL),
    ('22222222-2222-4222-8222-222222222222', 'Arica y Parinacota', 'ARICA_PARINACOTA', 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), NULL),
    ('33333333-3333-4333-8333-333333333333', 'Tarapaca', 'TARAPACA', 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), NULL),
    ('44444444-4444-4444-8444-444444444444', 'Antofagasta', 'ANTOFAGASTA', 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), NULL),
    ('55555555-5555-4555-8555-555555555555', 'Coquimbo', 'COQUIMBO', 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), NULL),
    ('66666666-6666-4666-8666-666666666666', 'Valparaiso', 'VALPARAISO', 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), NULL),
    ('77777777-7777-4777-8777-777777777777', 'O''Higgins', 'OHIGGINS', 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), NULL),
    ('88888888-8888-4888-8888-888888888888', 'Biobio', 'BIOBIO', 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), NULL),
    ('99999999-9999-4999-8999-999999999999', 'La Araucania', 'ARAUCANIA', 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), NULL),
    ('aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', 'Aysen y Magallanes', 'AYSEN_MAGALLANES', 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), NULL);
