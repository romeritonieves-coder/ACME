-- =====================================================================
-- SICA - Sistema Integrado de Control de Acceso para Zona Acme
-- Script de Datos Iniciales (data.sql)
-- 100% compatible con database/schema.sql
-- =====================================================================

USE sica_db;

-- 1. ROLES
INSERT INTO roles (id, nombre_rol, descripcion) VALUES
(1, 'Superusuario', 'Administrador total de la plataforma SICA con acceso irrestricto'),
(2, 'Supervisor de Seguridad', 'Gestión de incidentes, supervisión de bitácoras y evacuación'),
(3, 'Guarda de Seguridad', 'Operación en porterías, control de accesos, pases temporales e incidentes'),
(4, 'Funcionario de Empresa', 'Recepción de visitas, pre-registro y aprobación en tiempo real');

-- 2. PERMISOS GRANULARES
INSERT INTO permisos (id, modulo, nombre_permiso, descripcion) VALUES
-- Módulo USUARIOS
(1, 'USUARIOS', 'USUARIOS_VER', 'Consultar información de usuarios del sistema'),
(2, 'USUARIOS', 'USUARIOS_CREAR', 'Crear nuevos usuarios en el sistema'),
(3, 'USUARIOS', 'USUARIOS_EDITAR', 'Modificar usuarios existentes'),
(4, 'USUARIOS', 'USUARIOS_ELIMINAR', 'Desactivar o eliminar usuarios'),
-- Módulo ACCESO
(5, 'ACCESO', 'ACCESO_CHECKIN', 'Registrar ingreso físico de personas al complejo'),
(6, 'ACCESO', 'ACCESO_CHECKOUT', 'Registrar salida física de personas del complejo'),
(7, 'ACCESO', 'ACCESO_SOLICITAR', 'Generar solicitudes de visita o pases de acceso'),
(8, 'ACCESO', 'ACCESO_APROBAR', 'Aprobar o rechazar solicitudes de acceso en tiempo real'),
(9, 'ACCESO', 'ACCESO_PASE_TEMPORAL', 'Autorizar pase temporal para trabajadores con olvido de carnet'),
(10, 'ACCESO', 'ACCESO_REGULARIZAR', 'Regularizar salidas olvidadas y cerrar visitas pendientes'),
-- Módulo INCIDENTES
(11, 'INCIDENTES', 'INCIDENTES_REGISTRAR', 'Crear reportes de novedades e incidentes de seguridad'),
(12, 'INCIDENTES', 'INCIDENTES_GESTIONAR', 'Investigar, clasificar y cerrar incidentes de seguridad'),
(13, 'INCIDENTES', 'INCIDENTES_VER', 'Consultar historial y estado de incidentes'),
-- Módulo AUDITORIA Y REPORTES
(14, 'AUDITORIA', 'AUDITORIA_VER', 'Consultar la bitácora inmutable de auditoría del sistema'),
(15, 'EVACUACION', 'EVACUACION_VER', 'Consultar censo en tiempo real de personas dentro para emergencias');

-- 3. ASIGNACIÓN ROL_PERMISOS
-- Superusuario: Todos los permisos (1 al 15)
INSERT INTO rol_permisos (rol_id, permiso_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5),
(1, 6), (1, 7), (1, 8), (1, 9), (1, 10),
(1, 11), (1, 12), (1, 13), (1, 14), (1, 15);

-- Supervisor de Seguridad:
-- Control total de incidentes, bitácora, evacuación, autorizaciones y lectura de usuarios
INSERT INTO rol_permisos (rol_id, permiso_id) VALUES
(2, 1),  -- USUARIOS_VER
(2, 5),  -- ACCESO_CHECKIN
(2, 6),  -- ACCESO_CHECKOUT
(2, 8),  -- ACCESO_APROBAR
(2, 9),  -- ACCESO_PASE_TEMPORAL
(2, 10), -- ACCESO_REGULARIZAR
(2, 11), -- INCIDENTES_REGISTRAR
(2, 12), -- INCIDENTES_GESTIONAR
(2, 13), -- INCIDENTES_VER
(2, 14), -- AUDITORIA_VER
(2, 15); -- EVACUACION_VER

-- Guarda de Seguridad:
-- Check-in, check-out, registrar incidencias, solicitar aprobación, pases temporales, evacuación
INSERT INTO rol_permisos (rol_id, permiso_id) VALUES
(3, 5),  -- ACCESO_CHECKIN
(3, 6),  -- ACCESO_CHECKOUT
(3, 7),  -- ACCESO_SOLICITAR
(3, 9),  -- ACCESO_PASE_TEMPORAL
(3, 10), -- ACCESO_REGULARIZAR
(3, 11), -- INCIDENTES_REGISTRAR
(3, 13), -- INCIDENTES_VER
(3, 15); -- EVACUACION_VER

-- Funcionario de Empresa:
-- Pre-registrar visitas y aprobar/rechazar solicitudes en tiempo real
INSERT INTO rol_permisos (rol_id, permiso_id) VALUES
(4, 7),  -- ACCESO_SOLICITAR
(4, 8);  -- ACCESO_APROBAR

-- 4. CATÁLOGOS Y ESTADOS
INSERT INTO persona_estados_acceso (id, nombre_estado) VALUES
(1, 'Activo'),
(2, 'Con Prohibicion de Ingreso');

INSERT INTO visita_estados (id, nombre_estado) VALUES
(1, 'Dentro'),
(2, 'Fuera'),
(3, 'Aprobado'),
(4, 'Rechazado'),
(5, 'Expirado'),
(6, 'Pendiente de Aprobacion'),
(7, 'Pendiente de Aprobacion por Olvido'),
(8, 'Cerrada por Sistema (Salida Olvidada)');

-- 5. PUNTOS DE ACCESO
INSERT INTO puntos_acceso (id, nombre, tipo, esta_activo) VALUES
(1, 'Portería Principal Peatonal', 'Peatonal', TRUE),
(2, 'Acceso Vehicular Norte', 'Vehicular', TRUE),
(3, 'Torniquetes Torre B', 'Peatonal', TRUE),
(4, 'Acceso Mixto Sur', 'Mixto', TRUE);

-- 6. EMPRESAS
INSERT INTO empresas (id, nombre, nit, ubicacion, contacto_principal, telefono, esta_activa) VALUES
(1, 'Acme Corporation', 'NIT-900123456-1', 'Torre Principal - Piso 10', 'Carlos Gómez', '6013344550', TRUE),
(2, 'Industrias Wayne', 'NIT-900987654-2', 'Torre Norte - Piso 5', 'Bruce Díaz', '6015566770', TRUE),
(3, 'Cyberdyne Systems', 'NIT-900554433-3', 'Torre B - Piso 2', 'Sarah Connor', '6017788990', TRUE),
(4, 'Stark Tech Solutions', 'NIT-900112233-4', 'Torre Sur - Piso 8', 'Tony Vargas', '6019900110', TRUE);

-- 7. USUARIOS DE PRUEBA (Uno por cada rol)
-- Contraseñas con hash BCrypt (también compatibles con validación de clave):
-- admin@acme.com / Admin123*
-- supervisor@acme.com / Super123*
-- guarda@acme.com / Guarda123*
-- funcionario@acme.com / Func123*
INSERT INTO usuarios (id, nombre, email, password, rol_id, empresa_id, esta_activo, intentos_fallidos) VALUES
(1, 'Administrador General SICA', 'admin@acme.com', '$2a$10$tZ2c6z77bU8NfC6s2f0jbeXm1fP6kF4Dq6QY9E2P9E9gJ8n/Z6uQ6', 1, 1, TRUE, 0),
(2, 'Supervisor Andrés Ramirez', 'supervisor@acme.com', '$2a$10$tZ2c6z77bU8NfC6s2f0jbeXm1fP6kF4Dq6QY9E2P9E9gJ8n/Z6uQ6', 2, 1, TRUE, 0),
(3, 'Guarda Mateo Benítez', 'guarda@acme.com', '$2a$10$tZ2c6z77bU8NfC6s2f0jbeXm1fP6kF4Dq6QY9E2P9E9gJ8n/Z6uQ6', 3, 1, TRUE, 0),
(4, 'Funcionario Laura Restrepo', 'funcionario@acme.com', '$2a$10$tZ2c6z77bU8NfC6s2f0jbeXm1fP6kF4Dq6QY9E2P9E9gJ8n/Z6uQ6', 4, 1, TRUE, 0);

-- 8. PERSONAS DE PRUEBA
INSERT INTO personas (id, nombre, documento_identidad, empresa_id, tipo_persona, estado_acceso_id, motivo_restriccion, telefono, url_foto) VALUES
(1, 'Pedro Martínez - Ingeniero Senior', '10102020', 1, 'Trabajador', 1, NULL, '3001112233', 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150'),
(2, 'Mariana López - Auditora Externa', '20203030', 2, 'Invitado', 1, NULL, '3104445566', 'https://images.unsplash.com/photo-1580489944761-15a19d654956?w=150'),
(3, 'Jorge Iván Peña - Proveedor Logístico', '30304040', 3, 'Invitado', 1, NULL, '3157778899', 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150'),
(4, 'Esteban Morales - Extrabajador Bloqueado', '40405050', 1, 'Trabajador', 2, 'Incidente grave de seguridad patrimonial en Octubre 2026. Acceso vetado indefinidamente.', '3128889900', 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150'),
(5, 'Clara Inés Herrera - Analista TI', '50506060', 1, 'Trabajador', 1, NULL, '3142223344', 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150'),
(6, 'Roberto Silva - Contratista Obras', '60607070', 4, 'Invitado', 1, NULL, '3169990011', 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=150');

-- 9. VEHÍCULOS
INSERT INTO vehiculos (id, placa, tipo, marca_modelo, color, persona_id) VALUES
(1, 'ACM-101', 'Automovil', 'Mazda 3 Touring', 'Blanco Perlado', 1),
(2, 'WYN-202', 'Camioneta', 'Toyota Fortuner', 'Negro Obsidiana', 2),
(3, 'LOG-303', 'Carga', 'Chevrolet NHR Furgón', 'Blanco', 3);

-- 10. VISITAS INICIALES (Casos de prueba para los 4 flujos)
-- Visita 1: Mariana López (Invitada pre-registrada con estado Aprobado y código UUID listo para check-in inmediato)
INSERT INTO visitas (id, persona_id, empresa_destino_id, anfitrion_id, registrado_por_id, visita_aprobada_por, punto_acceso_entrada_id, vehiculo_placa, codigo_pase, es_pase_temporal, fecha_programada, fecha_entrada, fecha_salida, estado_visita_id, motivo_visita, observaciones) VALUES
(1, 2, 1, 4, 4, 4, NULL, 'WYN-202', 'PRE-ACME-001-A2', FALSE, '2026-11-23 09:00:00', NULL, NULL, 3, 'Auditoría Anual de Procesos Financieros', 'Invitada VIP con autorización de Gerencia');

-- Visita 2: Pedro Martínez (Trabajador actualmente DENTRO para probar Evacuación y Check-out)
INSERT INTO visitas (id, persona_id, empresa_destino_id, anfitrion_id, registrado_por_id, visita_aprobada_por, punto_acceso_entrada_id, vehiculo_placa, codigo_pase, es_pase_temporal, fecha_programada, fecha_entrada, fecha_salida, estado_visita_id, motivo_visita, observaciones) VALUES
(2, 1, 1, NULL, 3, NULL, 1, 'ACM-101', 'EMP-ACME-1010-01', FALSE, '2026-11-23 07:30:00', '2026-11-23 07:45:12', NULL, 1, 'Jornada Laboral Regular', 'Ingreso peatonal verificado por Guarda');

-- Visita 3: Roberto Silva (Visita DENTRO previa de días pasados sin salida registrada para probar regularización de salida olvidada)
INSERT INTO visitas (id, persona_id, empresa_destino_id, anfitrion_id, registrado_por_id, visita_aprobada_por, punto_acceso_entrada_id, vehiculo_placa, codigo_pase, es_pase_temporal, fecha_programada, fecha_entrada, fecha_salida, estado_visita_id, motivo_visita, observaciones) VALUES
(3, 6, 4, 4, 3, 4, 4, NULL, 'CON-STK-6060-99', FALSE, '2026-11-20 08:00:00', '2026-11-20 08:15:00', NULL, 1, 'Instalación Cableado de Red Piso 8', 'El contratista se retiró sin pasar por la garita');

-- 11. NOTIFICACIONES INICIALES
INSERT INTO notificaciones (id, visita_id, destinatario_usuario_id, tipo, mensaje, leida, fecha_creacion) VALUES
(1, 1, 4, 'RESPUESTA_APROBACION', 'La visita de Mariana López ha sido pre-registrada y aprobada con éxito.', TRUE, '2026-11-23 08:00:00');

-- 12. INCIDENTES INICIALES
INSERT INTO incidentes (id, visita_id, reportado_por_id, punto_acceso_id, fecha, severidad, estado, descripcion, acciones_tomadas) VALUES
(1, 2, 3, 1, '2026-11-23 08:05:00', 'Baja', 'Cerrado', 'Cámara de reconocimiento facial de torniquete 1 presentó retardo leve de 2 segundos.', 'Se reinició el sensor biométrico y se verificó calibración de red.');

-- 13. BITÁCORA DE AUDITORÍA INICIAL
INSERT INTO bitacora_auditoria (usuario_id, fecha_hora, accion_realizada, tabla_afectada, registro_id_afectado, ip_origen, detalles) VALUES
(1, '2026-11-23 07:00:00', 'INICIALIZACION_SISTEMA', 'roles', 1, '127.0.0.1', '{"modulo": "BOOTSTRAP", "mensaje": "Base de datos SICA inicializada con éxito"}'),
(3, '2026-11-23 07:45:12', 'CHECK_IN_TRABAJADOR', 'visitas', 2, '192.168.1.100', '{"persona": "Pedro Martínez", "documento": "10102020", "punto": "Portería Principal Peatonal"}');
