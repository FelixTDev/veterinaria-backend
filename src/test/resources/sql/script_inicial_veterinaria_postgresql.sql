-- ============================================================
-- BASE DE DATOS: veterinaria_db
-- Proyecto: Sistema de Gestión Veterinaria
-- Motor: PostgreSQL
-- ============================================================

BEGIN;

-- ============================================================
-- 1. FUNCIÓN GENERAL PARA UPDATED_AT
-- ============================================================

CREATE OR REPLACE FUNCTION fn_actualizar_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================================
-- 2. USUARIOS, ROLES Y SEGURIDAD
-- ============================================================

CREATE TABLE roles (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre          VARCHAR(30) NOT NULL UNIQUE,
    descripcion     VARCHAR(200),
    activo          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_roles_nombre
        CHECK (nombre IN ('ADMINISTRADOR', 'RECEPCIONISTA', 'VETERINARIO', 'PELUQUERO'))
);

CREATE TABLE usuarios (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    primer_nombre       VARCHAR(60) NOT NULL,
    segundo_nombre      VARCHAR(60),
    primer_apellido     VARCHAR(60) NOT NULL,
    segundo_apellido    VARCHAR(60),
    correo              VARCHAR(150) NOT NULL UNIQUE,
    password_hash       VARCHAR(255) NOT NULL,
    telefono            VARCHAR(20),
    activo              BOOLEAN NOT NULL DEFAULT TRUE,
    bloqueado_hasta     TIMESTAMP,
    intentos_fallidos   INTEGER NOT NULL DEFAULT 0,
    ultimo_acceso       TIMESTAMP,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_usuarios_intentos
        CHECK (intentos_fallidos >= 0)
);

CREATE TABLE usuario_roles (
    usuario_id      BIGINT NOT NULL,
    rol_id          BIGINT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (usuario_id, rol_id),

    CONSTRAINT fk_usuario_roles_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_usuario_roles_rol
        FOREIGN KEY (rol_id) REFERENCES roles(id)
        ON DELETE RESTRICT
);

CREATE TABLE codigos_recuperacion (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id      BIGINT NOT NULL,
    codigo_hash     VARCHAR(255) NOT NULL,
    expira_en       TIMESTAMP NOT NULL,
    usado           BOOLEAN NOT NULL DEFAULT FALSE,
    usado_en        TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_codigos_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
        ON DELETE CASCADE
);

-- ============================================================
-- 3. CLIENTES Y MASCOTAS
-- ============================================================

CREATE TABLE clientes (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    primer_nombre       VARCHAR(60) NOT NULL,
    segundo_nombre      VARCHAR(60),
    primer_apellido     VARCHAR(60) NOT NULL,
    segundo_apellido    VARCHAR(60),
    tipo_documento      VARCHAR(20) NOT NULL,
    numero_documento    VARCHAR(20) NOT NULL,
    fecha_nacimiento    DATE,
    telefono            VARCHAR(20),
    correo              VARCHAR(150),
    activo              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_clientes_documento
        UNIQUE (tipo_documento, numero_documento),

    CONSTRAINT ck_clientes_tipo_documento
        CHECK (tipo_documento IN ('DNI', 'RUC', 'CE', 'PASAPORTE', 'OTRO')),

    CONSTRAINT ck_clientes_fecha_nacimiento
        CHECK (fecha_nacimiento IS NULL OR fecha_nacimiento <= CURRENT_DATE)
);

CREATE TABLE mascotas (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cliente_id              BIGINT NOT NULL,
    nombre                  VARCHAR(80) NOT NULL,
    especie                 VARCHAR(30) NOT NULL,
    raza                    VARCHAR(80),
    color                   VARCHAR(80),
    sexo                    VARCHAR(15),
    peso_kg                 NUMERIC(6,2),
    fecha_nacimiento        DATE,
    edad_aproximada_anios   INTEGER,
    activo                  BOOLEAN NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_mascotas_cliente
        FOREIGN KEY (cliente_id) REFERENCES clientes(id)
        ON DELETE RESTRICT,

    CONSTRAINT ck_mascotas_especie
        CHECK (especie IN ('PERRO', 'GATO', 'OTRO')),

    CONSTRAINT ck_mascotas_sexo
        CHECK (sexo IS NULL OR sexo IN ('MACHO', 'HEMBRA', 'NO_DETERMINADO')),

    CONSTRAINT ck_mascotas_peso
        CHECK (peso_kg IS NULL OR peso_kg > 0),

    CONSTRAINT ck_mascotas_fecha_nacimiento
        CHECK (fecha_nacimiento IS NULL OR fecha_nacimiento <= CURRENT_DATE),

    CONSTRAINT ck_mascotas_edad_aproximada
        CHECK (edad_aproximada_anios IS NULL OR edad_aproximada_anios >= 0)
);

-- ============================================================
-- 4. HORARIOS DE TRABAJO
-- ============================================================

CREATE TABLE horarios_trabajador (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id      BIGINT NOT NULL,
    dia_semana      SMALLINT NOT NULL,
    hora_inicio     TIME NOT NULL,
    hora_fin        TIME NOT NULL,
    descanso_inicio TIME,
    descanso_fin    TIME,
    disponible      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_horarios_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
        ON DELETE CASCADE,

    CONSTRAINT ck_horarios_dia
        CHECK (dia_semana BETWEEN 1 AND 7),

    CONSTRAINT ck_horarios_rango
        CHECK (hora_inicio < hora_fin),

    CONSTRAINT ck_horarios_descanso
        CHECK (
            (descanso_inicio IS NULL AND descanso_fin IS NULL)
            OR
            (
                descanso_inicio IS NOT NULL
                AND descanso_fin IS NOT NULL
                AND descanso_inicio < descanso_fin
                AND descanso_inicio >= hora_inicio
                AND descanso_fin <= hora_fin
            )
        ),

    CONSTRAINT uq_horario_usuario_dia
        UNIQUE (usuario_id, dia_semana)
);

CREATE TABLE indisponibilidades_trabajador (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id      BIGINT NOT NULL,
    fecha_inicio    TIMESTAMP NOT NULL,
    fecha_fin       TIMESTAMP NOT NULL,
    motivo          VARCHAR(250) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_indisponibilidad_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
        ON DELETE CASCADE,

    CONSTRAINT ck_indisponibilidad_rango
        CHECK (fecha_inicio < fecha_fin)
);

-- ============================================================
-- 5. SERVICIOS Y PRECIOS
-- ============================================================

CREATE TABLE servicios (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre              VARCHAR(100) NOT NULL UNIQUE,
    tipo_servicio       VARCHAR(20) NOT NULL,
    descripcion         VARCHAR(250),
    precio_base         NUMERIC(10,2),
    duracion_minutos    INTEGER NOT NULL,
    activo              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_servicios_tipo
        CHECK (tipo_servicio IN ('MEDICO', 'PELUQUERIA')),

    CONSTRAINT ck_servicios_precio
        CHECK (precio_base IS NULL OR precio_base >= 0),

    CONSTRAINT ck_servicios_duracion
        CHECK (duracion_minutos > 0)
);

CREATE TABLE precios_servicio_tamano (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    servicio_id         BIGINT NOT NULL,
    tamano_mascota      VARCHAR(15) NOT NULL,
    precio              NUMERIC(10,2) NOT NULL,
    duracion_minutos    INTEGER NOT NULL,
    activo              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_precio_servicio
        FOREIGN KEY (servicio_id) REFERENCES servicios(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_servicio_tamano
        UNIQUE (servicio_id, tamano_mascota),

    CONSTRAINT ck_precio_tamano
        CHECK (tamano_mascota IN ('PEQUENO', 'MEDIANO', 'GRANDE')),

    CONSTRAINT ck_precio_valor
        CHECK (precio >= 0),

    CONSTRAINT ck_precio_duracion
        CHECK (duracion_minutos > 0)
);

-- ============================================================
-- 6. CITAS
-- ============================================================

CREATE TABLE citas (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    mascota_id              BIGINT NOT NULL,
    trabajador_asignado_id  BIGINT NOT NULL,
    registrado_por_id       BIGINT NOT NULL,
    tipo_cita               VARCHAR(20) NOT NULL,
    estado                  VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    fecha_hora_inicio       TIMESTAMP NOT NULL,
    fecha_hora_fin          TIMESTAMP NOT NULL,
    motivo_consulta         VARCHAR(500),
    motivo_no_atencion      VARCHAR(500),
    motivo_cancelacion      VARCHAR(500),
    observaciones           VARCHAR(500),
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_citas_mascota
        FOREIGN KEY (mascota_id) REFERENCES mascotas(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_citas_trabajador
        FOREIGN KEY (trabajador_asignado_id) REFERENCES usuarios(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_citas_registrado_por
        FOREIGN KEY (registrado_por_id) REFERENCES usuarios(id)
        ON DELETE RESTRICT,

    CONSTRAINT ck_citas_tipo
        CHECK (tipo_cita IN ('MEDICA', 'PELUQUERIA')),

    CONSTRAINT ck_citas_estado
        CHECK (estado IN ('PENDIENTE', 'CONFIRMADA', 'ATENDIDA', 'NO_ATENDIDA', 'CANCELADA')),

    CONSTRAINT ck_citas_rango
        CHECK (fecha_hora_inicio < fecha_hora_fin),

    CONSTRAINT ck_citas_motivo_no_atendida
        CHECK (
            estado <> 'NO_ATENDIDA'
            OR NULLIF(BTRIM(motivo_no_atencion), '') IS NOT NULL
        ),

    CONSTRAINT ck_citas_motivo_cancelada
        CHECK (
            estado <> 'CANCELADA'
            OR NULLIF(BTRIM(motivo_cancelacion), '') IS NOT NULL
        )
);

CREATE TABLE cita_servicios (
    id                          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cita_id                     BIGINT NOT NULL,
    servicio_id                 BIGINT NOT NULL,
    precio_servicio_tamano_id   BIGINT,
    precio_aplicado             NUMERIC(10,2) NOT NULL,
    duracion_aplicada_minutos   INTEGER NOT NULL,
    created_at                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_cita_servicios_cita
        FOREIGN KEY (cita_id) REFERENCES citas(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_cita_servicios_servicio
        FOREIGN KEY (servicio_id) REFERENCES servicios(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_cita_servicios_tamano
        FOREIGN KEY (precio_servicio_tamano_id) REFERENCES precios_servicio_tamano(id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_cita_servicio
        UNIQUE (cita_id, servicio_id),

    CONSTRAINT ck_cita_servicio_precio
        CHECK (precio_aplicado >= 0),

    CONSTRAINT ck_cita_servicio_duracion
        CHECK (duracion_aplicada_minutos > 0)
);

-- ============================================================
-- 7. HISTORIA CLÍNICA Y ATENCIÓN MÉDICA
-- ============================================================

CREATE TABLE atenciones_medicas (
    id                          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cita_id                     BIGINT NOT NULL UNIQUE,
    veterinario_id              BIGINT NOT NULL,
    peso_kg                     NUMERIC(6,2),
    temperatura_c               NUMERIC(4,1),
    sintomas                    TEXT,
    diagnostico                 TEXT,
    motivo_sin_diagnostico      TEXT,
    tratamiento                 TEXT,
    motivo_sin_tratamiento      TEXT,
    receta                      TEXT,
    motivo_sin_receta           TEXT,
    observaciones               TEXT,
    fecha_atencion              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_atencion_cita
        FOREIGN KEY (cita_id) REFERENCES citas(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_atencion_veterinario
        FOREIGN KEY (veterinario_id) REFERENCES usuarios(id)
        ON DELETE RESTRICT,

    CONSTRAINT ck_atencion_peso
        CHECK (peso_kg IS NULL OR peso_kg > 0),

    CONSTRAINT ck_atencion_temperatura
        CHECK (temperatura_c IS NULL OR temperatura_c BETWEEN 30 AND 45),

    CONSTRAINT ck_atencion_diagnostico
        CHECK (
            NULLIF(BTRIM(diagnostico), '') IS NOT NULL
            OR NULLIF(BTRIM(motivo_sin_diagnostico), '') IS NOT NULL
        ),

    CONSTRAINT ck_atencion_tratamiento
        CHECK (
            NULLIF(BTRIM(tratamiento), '') IS NOT NULL
            OR NULLIF(BTRIM(motivo_sin_tratamiento), '') IS NOT NULL
        ),

    CONSTRAINT ck_atencion_receta
        CHECK (
            NULLIF(BTRIM(receta), '') IS NOT NULL
            OR NULLIF(BTRIM(motivo_sin_receta), '') IS NOT NULL
        )
);

CREATE TABLE vacunas (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre              VARCHAR(120) NOT NULL UNIQUE,
    descripcion         VARCHAR(250),
    activo              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE vacunas_aplicadas (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    atencion_medica_id      BIGINT NOT NULL,
    vacuna_id               BIGINT NOT NULL,
    fecha_aplicacion        DATE NOT NULL,
    proxima_fecha           DATE,
    lote                    VARCHAR(80),
    observaciones           VARCHAR(300),
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_vacuna_aplicada_atencion
        FOREIGN KEY (atencion_medica_id) REFERENCES atenciones_medicas(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_vacuna_aplicada_vacuna
        FOREIGN KEY (vacuna_id) REFERENCES vacunas(id)
        ON DELETE RESTRICT,

    CONSTRAINT ck_vacuna_proxima_fecha
        CHECK (proxima_fecha IS NULL OR proxima_fecha >= fecha_aplicacion)
);

-- ============================================================
-- 8. PELUQUERÍA Y EVIDENCIAS
-- ============================================================

CREATE TABLE atenciones_peluqueria (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cita_id             BIGINT NOT NULL UNIQUE,
    peluquero_id        BIGINT NOT NULL,
    observaciones       TEXT,
    fecha_atencion      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_peluqueria_cita
        FOREIGN KEY (cita_id) REFERENCES citas(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_peluqueria_usuario
        FOREIGN KEY (peluquero_id) REFERENCES usuarios(id)
        ON DELETE RESTRICT
);

CREATE TABLE fotos_peluqueria (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    atencion_peluqueria_id  BIGINT NOT NULL,
    tipo_foto               VARCHAR(15) NOT NULL,
    url_archivo             VARCHAR(500) NOT NULL,
    nombre_archivo          VARCHAR(255),
    storage_key             VARCHAR(500),
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_foto_peluqueria
        FOREIGN KEY (atencion_peluqueria_id) REFERENCES atenciones_peluqueria(id)
        ON DELETE CASCADE,

    CONSTRAINT ck_foto_tipo
        CHECK (tipo_foto IN ('ANTES', 'DESPUES', 'FINAL'))
);

-- ============================================================
-- 9. PAGOS Y COMPROBANTES
-- ============================================================

CREATE TABLE pagos (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cita_id             BIGINT NOT NULL,
    registrado_por_id   BIGINT NOT NULL,
    monto_total         NUMERIC(10,2) NOT NULL,
    estado              VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE',
    fecha_pago          TIMESTAMP,
    observaciones       VARCHAR(300),
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_pagos_cita
        FOREIGN KEY (cita_id) REFERENCES citas(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_pagos_usuario
        FOREIGN KEY (registrado_por_id) REFERENCES usuarios(id)
        ON DELETE RESTRICT,

    CONSTRAINT ck_pagos_monto
        CHECK (monto_total >= 0),

    CONSTRAINT ck_pagos_estado
        CHECK (estado IN ('PENDIENTE', 'PAGADO', 'ANULADO')),

    CONSTRAINT ck_pagos_fecha
        CHECK (
            estado <> 'PAGADO'
            OR fecha_pago IS NOT NULL
        )
);

CREATE TABLE detalle_pagos (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    pago_id         BIGINT NOT NULL,
    medio_pago      VARCHAR(15) NOT NULL,
    monto           NUMERIC(10,2) NOT NULL,
    referencia      VARCHAR(100),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_detalle_pago
        FOREIGN KEY (pago_id) REFERENCES pagos(id)
        ON DELETE CASCADE,

    CONSTRAINT ck_detalle_medio
        CHECK (medio_pago IN ('EFECTIVO', 'YAPE', 'PLIN', 'TARJETA')),

    CONSTRAINT ck_detalle_monto
        CHECK (monto > 0)
);

CREATE TABLE comprobantes (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    pago_id             BIGINT NOT NULL UNIQUE,
    tipo_comprobante    VARCHAR(10) NOT NULL,
    serie               VARCHAR(10),
    numero              VARCHAR(20),
    ruc                  VARCHAR(11),
    razon_social        VARCHAR(200),
    direccion_fiscal    VARCHAR(300),
    fecha_emision       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado              VARCHAR(15) NOT NULL DEFAULT 'EMITIDO',
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_comprobante_pago
        FOREIGN KEY (pago_id) REFERENCES pagos(id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_comprobante_serie_numero
        UNIQUE (serie, numero),

    CONSTRAINT ck_comprobante_tipo
        CHECK (tipo_comprobante IN ('BOLETA', 'FACTURA')),

    CONSTRAINT ck_comprobante_estado
        CHECK (estado IN ('EMITIDO', 'ANULADO')),

    CONSTRAINT ck_factura_datos
        CHECK (
            tipo_comprobante <> 'FACTURA'
            OR (
                ruc IS NOT NULL
                AND LENGTH(ruc) = 11
                AND NULLIF(BTRIM(razon_social), '') IS NOT NULL
                AND NULLIF(BTRIM(direccion_fiscal), '') IS NOT NULL
            )
        )
);

-- ============================================================
-- 10. ÍNDICES
-- ============================================================

CREATE INDEX idx_usuarios_correo
    ON usuarios(correo);

CREATE INDEX idx_clientes_documento
    ON clientes(tipo_documento, numero_documento);

CREATE INDEX idx_mascotas_cliente
    ON mascotas(cliente_id);

CREATE INDEX idx_citas_mascota
    ON citas(mascota_id);

CREATE INDEX idx_citas_trabajador_fecha
    ON citas(trabajador_asignado_id, fecha_hora_inicio, fecha_hora_fin);

CREATE INDEX idx_citas_estado
    ON citas(estado);

CREATE INDEX idx_citas_tipo
    ON citas(tipo_cita);

CREATE INDEX idx_pagos_cita
    ON pagos(cita_id);

CREATE INDEX idx_codigos_usuario_expiracion
    ON codigos_recuperacion(usuario_id, expira_en);

-- ============================================================
-- 11. TRIGGERS DE UPDATED_AT
-- ============================================================

CREATE TRIGGER trg_roles_updated_at
BEFORE UPDATE ON roles
FOR EACH ROW EXECUTE FUNCTION fn_actualizar_updated_at();

CREATE TRIGGER trg_usuarios_updated_at
BEFORE UPDATE ON usuarios
FOR EACH ROW EXECUTE FUNCTION fn_actualizar_updated_at();

CREATE TRIGGER trg_clientes_updated_at
BEFORE UPDATE ON clientes
FOR EACH ROW EXECUTE FUNCTION fn_actualizar_updated_at();

CREATE TRIGGER trg_mascotas_updated_at
BEFORE UPDATE ON mascotas
FOR EACH ROW EXECUTE FUNCTION fn_actualizar_updated_at();

CREATE TRIGGER trg_horarios_updated_at
BEFORE UPDATE ON horarios_trabajador
FOR EACH ROW EXECUTE FUNCTION fn_actualizar_updated_at();

CREATE TRIGGER trg_servicios_updated_at
BEFORE UPDATE ON servicios
FOR EACH ROW EXECUTE FUNCTION fn_actualizar_updated_at();

CREATE TRIGGER trg_precios_tamano_updated_at
BEFORE UPDATE ON precios_servicio_tamano
FOR EACH ROW EXECUTE FUNCTION fn_actualizar_updated_at();

CREATE TRIGGER trg_citas_updated_at
BEFORE UPDATE ON citas
FOR EACH ROW EXECUTE FUNCTION fn_actualizar_updated_at();

CREATE TRIGGER trg_atenciones_medicas_updated_at
BEFORE UPDATE ON atenciones_medicas
FOR EACH ROW EXECUTE FUNCTION fn_actualizar_updated_at();

CREATE TRIGGER trg_vacunas_updated_at
BEFORE UPDATE ON vacunas
FOR EACH ROW EXECUTE FUNCTION fn_actualizar_updated_at();

CREATE TRIGGER trg_atenciones_peluqueria_updated_at
BEFORE UPDATE ON atenciones_peluqueria
FOR EACH ROW EXECUTE FUNCTION fn_actualizar_updated_at();

CREATE TRIGGER trg_pagos_updated_at
BEFORE UPDATE ON pagos
FOR EACH ROW EXECUTE FUNCTION fn_actualizar_updated_at();

CREATE TRIGGER trg_comprobantes_updated_at
BEFORE UPDATE ON comprobantes
FOR EACH ROW EXECUTE FUNCTION fn_actualizar_updated_at();

-- ============================================================
-- 12. DATOS INICIALES
-- ============================================================

INSERT INTO roles (nombre, descripcion) VALUES
('ADMINISTRADOR', 'Acceso completo a todas las pantallas y funciones'),
('RECEPCIONISTA', 'Gestiona clientes, mascotas, citas y pagos'),
('VETERINARIO', 'Gestiona citas médicas e historia clínica'),
('PELUQUERO', 'Gestiona citas y atenciones de peluquería');

INSERT INTO servicios
(nombre, tipo_servicio, descripcion, precio_base, duracion_minutos)
VALUES
('Consulta veterinaria', 'MEDICO', 'Evaluación médica general', 50.00, 30),
('Vacunación', 'MEDICO', 'Aplicación de vacuna', 40.00, 20),
('Desparasitación', 'MEDICO', 'Servicio de desparasitación', 35.00, 20),
('Curación', 'MEDICO', 'Curación y limpieza de heridas', 45.00, 30),
('Control médico', 'MEDICO', 'Seguimiento posterior a una atención', 30.00, 20),
('Baño', 'PELUQUERIA', 'Servicio de baño para mascota', NULL, 60),
('Corte', 'PELUQUERIA', 'Servicio de corte para mascota', NULL, 60),
('Baño y corte', 'PELUQUERIA', 'Servicio combinado de baño y corte', NULL, 90);

INSERT INTO precios_servicio_tamano
(servicio_id, tamano_mascota, precio, duracion_minutos)
SELECT id, 'PEQUENO', 35.00, 45
FROM servicios WHERE nombre = 'Baño';

INSERT INTO precios_servicio_tamano
(servicio_id, tamano_mascota, precio, duracion_minutos)
SELECT id, 'MEDIANO', 45.00, 60
FROM servicios WHERE nombre = 'Baño';

INSERT INTO precios_servicio_tamano
(servicio_id, tamano_mascota, precio, duracion_minutos)
SELECT id, 'GRANDE', 60.00, 90
FROM servicios WHERE nombre = 'Baño';

INSERT INTO precios_servicio_tamano
(servicio_id, tamano_mascota, precio, duracion_minutos)
SELECT id, 'PEQUENO', 40.00, 45
FROM servicios WHERE nombre = 'Corte';

INSERT INTO precios_servicio_tamano
(servicio_id, tamano_mascota, precio, duracion_minutos)
SELECT id, 'MEDIANO', 50.00, 60
FROM servicios WHERE nombre = 'Corte';

INSERT INTO precios_servicio_tamano
(servicio_id, tamano_mascota, precio, duracion_minutos)
SELECT id, 'GRANDE', 65.00, 90
FROM servicios WHERE nombre = 'Corte';

INSERT INTO precios_servicio_tamano
(servicio_id, tamano_mascota, precio, duracion_minutos)
SELECT id, 'PEQUENO', 65.00, 75
FROM servicios WHERE nombre = 'Baño y corte';

INSERT INTO precios_servicio_tamano
(servicio_id, tamano_mascota, precio, duracion_minutos)
SELECT id, 'MEDIANO', 80.00, 90
FROM servicios WHERE nombre = 'Baño y corte';

INSERT INTO precios_servicio_tamano
(servicio_id, tamano_mascota, precio, duracion_minutos)
SELECT id, 'GRANDE', 100.00, 120
FROM servicios WHERE nombre = 'Baño y corte';

COMMIT;

-- ============================================================
-- REGLAS QUE DEBEN VALIDARSE EN SPRING BOOT
-- ============================================================
-- 1. El administrador tiene acceso total por permisos.
-- 2. Un usuario puede tener uno o varios roles.
-- 3. Una cita solo puede ser MÉDICA o de PELUQUERÍA.
-- 4. No permitir cruces de horario para un mismo trabajador.
-- 5. No permitir citas fuera del horario laboral o durante descansos.
-- 6. Las citas CANCELADAS no bloquean horarios.
-- 7. El trabajador asignado debe tener el rol correspondiente.
-- 8. Una cita médica ATENDIDA debe tener una atención médica registrada.
-- 9. Una cita de peluquería ATENDIDA debe tener:
--      - foto ANTES y DESPUÉS, o
--      - como mínimo una foto FINAL.
-- 10. La suma de detalle_pagos debe coincidir con pagos.monto_total.
-- 11. Solo se genera comprobante cuando el pago está PAGADO.
-- 12. El código de recuperación tiene 6 dígitos, vence en 10 minutos
--     y solo puede utilizarse una vez.
