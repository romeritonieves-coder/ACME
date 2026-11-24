DROP DATABASE IF EXISTS sica_db;
CREATE DATABASE sica_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE sica_db;

-- =====================================================================
-- 1. AUTENTICACIÓN Y AUTORIZACIÓN GRANULAR (RBAC)
-- =====================================================================

CREATE TABLE roles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre_rol VARCHAR(50) NOT NULL UNIQUE,
    descripcion VARCHAR(255)
) ENGINE=InnoDB;

CREATE TABLE permisos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    modulo VARCHAR(50) NOT NULL, -- ej. 'USUARIOS', 'ACCESO', 'INCIDENTES', 'AUDITORIA'
    nombre_permiso VARCHAR(100) NOT NULL UNIQUE,
    descripcion TEXT
) ENGINE=InnoDB;

CREATE TABLE rol_permisos (
    rol_id INT NOT NULL,
    permiso_id INT NOT NULL,
    PRIMARY KEY (rol_id, permiso_id),
    CONSTRAINT fk_rol_permisos_rol
        FOREIGN KEY (rol_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_rol_permisos_permiso
        FOREIGN KEY (permiso_id) REFERENCES permisos(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =====================================================================
-- 2. CATÁLOGOS Y ESTADOS (LOOKUP TABLES)
-- =====================================================================

CREATE TABLE persona_estados_acceso (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre_estado VARCHAR(60) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE visita_estados (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre_estado VARCHAR(80) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE puntos_acceso (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(80) NOT NULL UNIQUE, -- ej. 'Portería Principal', 'Acceso Vehicular Norte', 'Torre B'
    tipo ENUM('Peatonal', 'Vehicular', 'Mixto') NOT NULL DEFAULT 'Mixto',
    esta_activo BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;

-- =====================================================================
-- 3. ENTIDADES OPERACIONALES DEL COMPLEJO
-- =====================================================================

CREATE TABLE empresas (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    nit VARCHAR(30) UNIQUE,
    ubicacion VARCHAR(100), -- ej. 'Torre A - Piso 4'
    contacto_principal VARCHAR(100),
    telefono VARCHAR(25),
    esta_activa BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE usuarios (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    rol_id INT NOT NULL,
    empresa_id INT NULL, -- Obligatorio para 'Funcionario de Empresa'
    esta_activo BOOLEAN NOT NULL DEFAULT TRUE,
    intentos_fallidos INT NOT NULL DEFAULT 0,
    ultimo_acceso DATETIME NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_usuarios_rol
        FOREIGN KEY (rol_id) REFERENCES roles(id),
    CONSTRAINT fk_usuarios_empresa
        FOREIGN KEY (empresa_id) REFERENCES empresas(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE personas (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    documento_identidad VARCHAR(20) NOT NULL UNIQUE,
    empresa_id INT NULL,
    tipo_persona ENUM('Trabajador', 'Invitado') NOT NULL,
    estado_acceso_id INT NOT NULL,
    motivo_restriccion TEXT NULL, -- Si tiene 'Con Prohibicion de Ingreso'
    telefono VARCHAR(25) NULL,
    url_foto VARCHAR(255) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_personas_empresa
        FOREIGN KEY (empresa_id) REFERENCES empresas(id) ON DELETE SET NULL,
    CONSTRAINT fk_personas_estado_acceso
        FOREIGN KEY (estado_acceso_id) REFERENCES persona_estados_acceso(id)
) ENGINE=InnoDB;

CREATE TABLE vehiculos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    placa VARCHAR(10) NOT NULL UNIQUE,
    tipo ENUM('Automovil', 'Motocicleta', 'Camioneta', 'Carga', 'Otro') NOT NULL DEFAULT 'Automovil',
    marca_modelo VARCHAR(80) NULL,
    color VARCHAR(30) NULL,
    persona_id INT NULL,
    CONSTRAINT fk_vehiculos_persona
        FOREIGN KEY (persona_id) REFERENCES personas(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- =====================================================================
-- 4. CONTROL DE ACCESO, VISITAS Y NOTIFICACIONES EN TIEMPO REAL
-- =====================================================================

CREATE TABLE visitas (
    id INT AUTO_INCREMENT PRIMARY KEY,
    persona_id INT NOT NULL,
    empresa_destino_id INT NULL,
    anfitrion_id INT NULL,              -- Funcionario al que visita o que pre-registra
    registrado_por_id INT NULL,         -- Guarda o funcionario que crea el registro
    visita_aprobada_por INT NULL,       -- Funcionario que aprueba el ingreso en tiempo real
    punto_acceso_entrada_id INT NULL,
    punto_acceso_salida_id INT NULL,
    vehiculo_id INT NULL,
    vehiculo_placa VARCHAR(10) NULL,    -- Compatibilidad directa con el modelo del PDF
    codigo_pase VARCHAR(36) NULL UNIQUE,-- UUID / Código QR para check-in rápido
    es_pase_temporal BOOLEAN NOT NULL DEFAULT FALSE, -- TRUE si es trabajador con carnet olvidado
    fecha_programada DATETIME NULL,
    fecha_entrada DATETIME NULL,
    fecha_salida DATETIME NULL,
    estado_visita_id INT NOT NULL,
    motivo_visita VARCHAR(255) NULL,
    observaciones TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_visitas_persona FOREIGN KEY (persona_id) REFERENCES personas(id),
    CONSTRAINT fk_visitas_empresa FOREIGN KEY (empresa_destino_id) REFERENCES empresas(id) ON DELETE SET NULL,
    CONSTRAINT fk_visitas_anfitrion FOREIGN KEY (anfitrion_id) REFERENCES usuarios(id) ON DELETE SET NULL,
    CONSTRAINT fk_visitas_registrado_por FOREIGN KEY (registrado_por_id) REFERENCES usuarios(id) ON DELETE SET NULL,
    CONSTRAINT fk_visitas_aprobada_por FOREIGN KEY (visita_aprobada_por) REFERENCES usuarios(id) ON DELETE SET NULL,
    CONSTRAINT fk_visitas_punto_entrada FOREIGN KEY (punto_acceso_entrada_id) REFERENCES puntos_acceso(id) ON DELETE SET NULL,
    CONSTRAINT fk_visitas_punto_salida FOREIGN KEY (punto_acceso_salida_id) REFERENCES puntos_acceso(id) ON DELETE SET NULL,
    CONSTRAINT fk_visitas_vehiculo FOREIGN KEY (vehiculo_id) REFERENCES vehiculos(id) ON DELETE SET NULL,
    CONSTRAINT fk_visitas_estado FOREIGN KEY (estado_visita_id) REFERENCES visita_estados(id)
) ENGINE=InnoDB;

CREATE TABLE notificaciones (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    visita_id INT NOT NULL,
    destinatario_usuario_id INT NOT NULL, -- Funcionario de Empresa o Guarda
    tipo ENUM('SOLICITUD_INGRESO', 'PASE_TEMPORAL', 'RESPUESTA_APROBACION', 'ALERTA_SEGURIDAD') NOT NULL,
    mensaje VARCHAR(255) NOT NULL,
    leida BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_lectura DATETIME NULL,
    CONSTRAINT fk_notif_visita FOREIGN KEY (visita_id) REFERENCES visitas(id) ON DELETE CASCADE,
    CONSTRAINT fk_notif_usuario FOREIGN KEY (destinatario_usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =====================================================================
-- 5. INCIDENTES Y BITÁCORA DE AUDITORÍA INMUTABLE
-- =====================================================================

CREATE TABLE incidentes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    visita_id INT NOT NULL,
    reportado_por_id INT NOT NULL,
    punto_acceso_id INT NULL,
    fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    severidad ENUM('Baja', 'Media', 'Alta', 'Critica') NOT NULL DEFAULT 'Media',
    estado ENUM('Abierto', 'En Investigacion', 'Cerrado') NOT NULL DEFAULT 'Abierto',
    descripcion TEXT NOT NULL,
    acciones_tomadas TEXT NULL,
    CONSTRAINT fk_incidentes_visita FOREIGN KEY (visita_id) REFERENCES visitas(id),
    CONSTRAINT fk_incidentes_reportado_por FOREIGN KEY (reportado_por_id) REFERENCES usuarios(id),
    CONSTRAINT fk_incidentes_punto FOREIGN KEY (punto_acceso_id) REFERENCES puntos_acceso(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE bitacora_auditoria (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id INT NULL, -- NULL permitido en logins fallidos
    fecha_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    accion_realizada VARCHAR(255) NOT NULL,
    tabla_afectada VARCHAR(100) NULL,
    registro_id_afectado INT NULL,
    ip_origen VARCHAR(45) NULL,
    detalles JSON NULL,
    CONSTRAINT fk_bitacora_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- =====================================================================
-- 6. TRIGGERS DE INMUTABILIDAD PARA LA BITÁCORA
-- =====================================================================

DELIMITER //
CREATE TRIGGER trg_bitacora_no_update
BEFORE UPDATE ON bitacora_auditoria
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
    SET MESSAGE_TEXT = 'Operación denegada: la tabla bitacora_auditoria es inmutable.';
END//

CREATE TRIGGER trg_bitacora_no_delete
BEFORE DELETE ON bitacora_auditoria
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
    SET MESSAGE_TEXT = 'Operación denegada: no se pueden eliminar registros de bitacora_auditoria.';
END//
DELIMITER ;

-- =====================================================================
-- 7. VISTAS OPERACIONALES (EMERGENCIA Y TIEMPO REAL)
-- =====================================================================

-- Recuento exacto de personas dentro del complejo (Evacuación / Emergencias)
CREATE VIEW vw_personas_actualmente_dentro AS
SELECT
    v.id AS visita_id,
    p.documento_identidad,
    p.nombre AS persona_nombre,
    p.tipo_persona,
    e.nombre AS empresa_destino,
    pa.nombre AS punto_entrada,
    v.fecha_entrada,
    v.vehiculo_placa
FROM visitas v
JOIN personas p ON v.persona_id = p.id
JOIN visita_estados ve ON v.estado_visita_id = ve.id
LEFT JOIN empresas e ON v.empresa_destino_id = e.id
LEFT JOIN puntos_acceso pa ON v.punto_acceso_entrada_id = pa.id
WHERE ve.nombre_estado = 'Dentro';

-- Bandeja de solicitudes pendientes para Funcionarios de Empresa
CREATE VIEW vw_solicitudes_pendientes AS
SELECT
    v.id AS visita_id,
    p.nombre AS visitante,
    p.documento_identidad,
    p.tipo_persona,
    ve.nombre_estado AS estado,
    e.nombre AS empresa_destino,
    u.nombre AS anfitrion,
    v.es_pase_temporal,
    v.created_at AS fecha_solicitud
FROM visitas v
JOIN personas p ON v.persona_id = p.id
JOIN visita_estados ve ON v.estado_visita_id = ve.id
LEFT JOIN empresas e ON v.empresa_destino_id = e.id
LEFT JOIN usuarios u ON v.anfitrion_id = u.id
WHERE ve.nombre_estado IN ('Pendiente de Aprobacion', 'Pendiente de Aprobacion por Olvido');

-- =====================================================================
-- 8. ÍNDICES DE RENDIMIENTO
-- =====================================================================

CREATE INDEX idx_personas_doc ON personas(documento_identidad);
CREATE INDEX idx_visitas_persona_estado ON visitas(persona_id, estado_visita_id);
CREATE INDEX idx_visitas_fecha_entrada ON visitas(fecha_entrada);
CREATE INDEX idx_notif_usuario_leida ON notificaciones(destinatario_usuario_id, leida);
CREATE INDEX idx_bitacora_fecha_accion ON bitacora_auditoria(fecha_hora, accion_realizada);
