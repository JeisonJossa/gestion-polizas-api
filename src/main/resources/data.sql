-- Datos de prueba. Los valores de IPC son de ejemplo: en producción se cargan los oficiales de cada año.
-- La renovación usa el IPC del año anterior al inicio de la nueva vigencia.
INSERT INTO ipc (anio, porcentaje) VALUES
    (2023, 9.28),
    (2024, 5.20),
    (2025, 5.10),
    (2026, 4.80);

-- Tomadores, arrendatarios (asegurados) y arrendadores (beneficiarios). En las individuales el tomador es el
-- arrendatario, así que es la misma persona.
INSERT INTO persona (id, tipo_documento, numero_documento, nombre, correo, celular) VALUES
    (1,  'CC',  '52345678',   'Laura Gómez Ruiz',                    'laura.gomez@correo.com',          '3001234567'),
    (2,  'CC',  '79111222',   'Carlos Pérez Díaz',                   'carlos.perez@correo.com',         '3109876543'),
    (3,  'NIT', '900123456',  'Inmobiliaria Andina S.A.S.',          'contacto@inmobiliariaandina.com', '6015551234'),
    (4,  'CC',  '1020304050', 'Andrés Molina Rey',                   'andres.molina@correo.com',        '3012223344'),
    (5,  'CC',  '51987654',   'María Torres Gil',                    'maria.torres@correo.com',         '3115556677'),
    (6,  'CC',  '1032456789', 'Sofía Rojas Peña',                    'sofia.rojas@correo.com',          '3023334455'),
    (7,  'CC',  '80123456',   'Jorge Castaño Ríos',                  'jorge.castano@correo.com',        '3126667788'),
    (8,  'CC',  '1015678901', 'Daniel Vargas Soto',                  'daniel.vargas@correo.com',        '3034445566'),
    (9,  'CC',  '52678901',   'Ana Beltrán Cruz',                    'ana.beltran@correo.com',          '3137778899'),
    (10, 'NIT', '900654321',  'Conjunto Residencial Los Pinos P.H.', 'administracion@lospinos.com',     '6017654321'),
    (11, 'CC',  '1098765432', 'Paula Herrera Díaz',                  'paula.herrera@correo.com',        '3045556677'),
    (12, 'CC',  '79555666',   'Luis Ortiz Pardo',                    'luis.ortiz@correo.com',           '3148889900'),
    (13, 'CC',  '1011223344', 'Camilo Suárez León',                  'camilo.suarez@correo.com',        '3056667788'),
    (14, 'CC',  '52999888',   'Diana León Mejía',                    'diana.leon@correo.com',           '3159990011'),
    (15, 'CC',  '1019988776', 'Julián Cárdenas Mora',                'julian.cardenas@correo.com',      '3157778899'),
    (16, 'CC',  '43111222',   'Beatriz Mejía Arango',                'beatriz.mejia@correo.com',        '3161112233'),
    (17, 'CC',  '1036654321', 'Natalia Ospina Vélez',                'natalia.ospina@correo.com',       '3204445566'),
    (18, 'CC',  '70123123',   'Ricardo Gil Salazar',                 'ricardo.gil@correo.com',          '3172223344'),
    (19, 'CC',  '1045123987', 'Mateo Restrepo Lara',                 'mateo.restrepo@correo.com',       '3187654321'),
    (20, 'CC',  '16789012',   'Hernando Salcedo Ruiz',               'hernando.salcedo@correo.com',     '3193334455'),
    (21, 'NIT', '901234567',  'Edificio Torre Central P.H.',         'administracion@torrecentral.com', '6023456789'),
    (22, 'CC',  '1144098765', 'Valeria Castro Mina',                 'valeria.castro@correo.com',       '3205556677'),
    (23, 'CC',  '31456789',   'Gloria Patiño Vera',                  'gloria.patino@correo.com',        '3216667788'),
    (24, 'CC',  '1107654321', 'Santiago Mejía Toro',                 'santiago.mejia@correo.com',       '3227778899');

-- 1001 Individual vigente: el tomador y asegurado es el arrendatario.
-- 1002 Colectiva con historial: endoso 0 emisión, 1 inclusión del riesgo 3 en abril (paga 9 meses),
--      2 exclusión del riesgo 2 en julio (se devuelven 6 meses).
-- 1003 Colectiva vigente de una copropiedad, con 2 riesgos.
-- 1004 Individual cancelada en mayo (se devuelven 9 meses).
-- 1005 Individual renovada en junio de 2026 con el IPC 2025 (5,10 %).
-- 1006 Individual vencida el 30 de junio de 2026 y 1007 colectiva vencida el 31 de agosto de 2026: todavía no se han
--      renovado, así que las toma la renovación automática (POST /renovaciones) con el IPC 2025.
-- Estas pólizas ya están en el CORE: tienen número en el CORE y sus avisos están SINCRONIZADOS.
INSERT INTO poliza (poliza_id, num_endoso, numero_poliza, numero_core, tipo, estado, tipo_endoso, clase_movimiento,
                    inicio_vigencia, fin_vigencia, meses_vigencia, fecha_endoso, tomador_id,
                    canon, prima, prima_endoso, canal, usuario, motivo) VALUES
    (1001, 0, 'POL-1001', '2700001001', 'INDIVIDUAL', 'VIGENTE',   'EMISION',     'COBRO',      DATE '2026-03-01', DATE '2027-02-28', 12, DATE '2026-03-01', 1,
     1500000.00, 18000000.00, 18000000.00, 'CARGA_INICIAL', NULL, NULL),

    (1002, 0, 'POL-1002', '2700001002', 'COLECTIVA',  'VIGENTE',   'EMISION',     'COBRO',      DATE '2026-01-01', DATE '2026-12-31', 12, DATE '2026-01-01', 3,
     3000000.00, 36000000.00, 36000000.00, 'CARGA_INICIAL', NULL, NULL),
    (1002, 1, 'POL-1002', '2700001002', 'COLECTIVA',  'VIGENTE',   'INCLUSION',   'COBRO',      DATE '2026-01-01', DATE '2026-12-31', 12, DATE '2026-04-01', 3,
     4200000.00, 46800000.00, 10800000.00, 'CARGA_INICIAL', NULL, NULL),
    (1002, 2, 'POL-1002', '2700001002', 'COLECTIVA',  'VIGENTE',   'EXCLUSION',   'DEVOLUCION', DATE '2026-01-01', DATE '2026-12-31', 12, DATE '2026-07-01', 3,
     2200000.00, 34800000.00, -12000000.00, 'CARGA_INICIAL', NULL, 'El arrendatario entregó el inmueble'),

    (1003, 0, 'POL-1003', '2700001003', 'COLECTIVA',  'VIGENTE',   'EMISION',     'COBRO',      DATE '2026-06-01', DATE '2027-05-31', 12, DATE '2026-06-01', 10,
     2500000.00, 30000000.00, 30000000.00, 'CARGA_INICIAL', NULL, NULL),

    (1004, 0, 'POL-1004', '2700001004', 'INDIVIDUAL', 'VIGENTE',   'EMISION',     'COBRO',      DATE '2026-02-01', DATE '2027-01-31', 12, DATE '2026-02-01', 15,
     1000000.00, 12000000.00, 12000000.00, 'CARGA_INICIAL', NULL, NULL),
    (1004, 1, 'POL-1004', '2700001004', 'INDIVIDUAL', 'CANCELADA', 'CANCELACION', 'DEVOLUCION', DATE '2026-02-01', DATE '2027-01-31', 12, DATE '2026-05-01', 15,
     0.00, 3000000.00, -9000000.00, 'CARGA_INICIAL', NULL, 'Terminó el contrato de arrendamiento'),

    (1005, 0, 'POL-1005', '2700001005', 'INDIVIDUAL', 'VIGENTE',   'EMISION',     'COBRO',      DATE '2025-06-01', DATE '2026-05-31', 12, DATE '2025-06-01', 17,
     1000000.00, 12000000.00, 12000000.00, 'CARGA_INICIAL', NULL, NULL),
    (1005, 1, 'POL-1005', '2700001005', 'INDIVIDUAL', 'RENOVADA',  'RENOVACION',  'COBRO',      DATE '2026-06-01', DATE '2027-05-31', 12, DATE '2026-06-01', 17,
     1051000.00, 12612000.00, 12612000.00, 'CARGA_INICIAL', NULL, NULL),

    (1006, 0, 'POL-1006', '2700001006', 'INDIVIDUAL', 'VIGENTE',   'EMISION',     'COBRO',      DATE '2025-07-01', DATE '2026-06-30', 12, DATE '2025-07-01', 19,
     2000000.00, 24000000.00, 24000000.00, 'CARGA_INICIAL', NULL, NULL),

    (1007, 0, 'POL-1007', '2700001007', 'COLECTIVA',  'VIGENTE',   'EMISION',     'COBRO',      DATE '2025-09-01', DATE '2026-08-31', 12, DATE '2025-09-01', 21,
     2200000.00, 26400000.00, 26400000.00, 'CARGA_INICIAL', NULL, NULL);

INSERT INTO riesgo (poliza_id, num_endoso, cod_riesgo, riesgo_id, arrendatario_id, arrendador_id,
                    inmueble_direccion, inmueble_ciudad, canon_mensual, prima, prima_endoso,
                    fecha_inclusion, fecha_exclusion, estado, vigente) VALUES
    (1001, 0, 1, 1,  1,  2,  'Carrera 15 # 93-40 Apto 502',        'Bogotá',   1500000.00, 18000000.00, 18000000.00,  DATE '2026-03-01', NULL,              'ACTIVO',    'S'),

    (1002, 0, 1, 2,  4,  5,  'Calle 72 # 10-34 Apto 301',          'Bogotá',   1000000.00, 12000000.00, 12000000.00,  DATE '2026-01-01', NULL,              'ACTIVO',    'S'),
    (1002, 0, 2, 3,  6,  7,  'Avenida 19 # 120-15 Apto 804',       'Bogotá',   2000000.00, 24000000.00, 24000000.00,  DATE '2026-01-01', NULL,              'ACTIVO',    'N'),
    (1002, 1, 3, 4,  8,  9,  'Carrera 7 # 45-12 Apto 1102',        'Bogotá',   1200000.00, 10800000.00, 10800000.00,  DATE '2026-04-01', NULL,              'ACTIVO',    'S'),
    (1002, 2, 2, 3,  6,  7,  'Avenida 19 # 120-15 Apto 804',       'Bogotá',   2000000.00, 12000000.00, -12000000.00, DATE '2026-01-01', DATE '2026-07-01', 'CANCELADO', 'S'),

    (1003, 0, 1, 5,  11, 12, 'Calle 150 # 45-20 Torre 1 Apto 201', 'Bogotá',   1100000.00, 13200000.00, 13200000.00,  DATE '2026-06-01', NULL,              'ACTIVO',    'S'),
    (1003, 0, 2, 6,  13, 14, 'Calle 150 # 45-20 Torre 2 Apto 605', 'Bogotá',   1400000.00, 16800000.00, 16800000.00,  DATE '2026-06-01', NULL,              'ACTIVO',    'S'),

    (1004, 0, 1, 7,  15, 16, 'Calle 53 # 20-10',                   'Medellín', 1000000.00, 12000000.00, 12000000.00,  DATE '2026-02-01', NULL,              'ACTIVO',    'N'),
    (1004, 1, 1, 7,  15, 16, 'Calle 53 # 20-10',                   'Medellín', 1000000.00, 3000000.00,  -9000000.00,  DATE '2026-02-01', DATE '2026-05-01', 'CANCELADO', 'S'),

    (1005, 0, 1, 8,  17, 18, 'Carrera 43A # 1-50 Apto 1203',       'Medellín', 1000000.00, 12000000.00, 12000000.00,  DATE '2025-06-01', NULL,              'ACTIVO',    'N'),
    (1005, 1, 1, 8,  17, 18, 'Carrera 43A # 1-50 Apto 1203',       'Medellín', 1051000.00, 12612000.00, 12612000.00,  DATE '2025-06-01', NULL,              'ACTIVO',    'S'),

    (1006, 0, 1, 9,  19, 20, 'Calle 10 # 38-15 Apto 902',          'Cali',     2000000.00, 24000000.00, 24000000.00,  DATE '2025-07-01', NULL,              'ACTIVO',    'S'),

    (1007, 0, 1, 10, 22, 23, 'Avenida 6N # 23-50 Apto 401',        'Cali',     900000.00,  10800000.00, 10800000.00,  DATE '2025-09-01', NULL,              'ACTIVO',    'S'),
    (1007, 0, 2, 11, 24, 23, 'Avenida 6N # 23-50 Apto 802',        'Cali',     1300000.00, 15600000.00, 15600000.00,  DATE '2025-09-01', NULL,              'ACTIVO',    'S');

-- Avisos de los endosos precargados: ya se entregaron al CORE y al Servicio de Notificaciones.
INSERT INTO aviso (id, poliza_id, num_endoso, destino, evento, estado, intentos, ultimo_error, fecha) VALUES
    (1,  1001, 0, 'CORE',         'ACTUALIZACION',   'SINCRONIZADO', 1, NULL, TIMESTAMP '2026-03-01 08:00:00'),
    (2,  1001, 0, 'NOTIFICACION', 'POLIZA_CREADA',   'SINCRONIZADO', 1, NULL, TIMESTAMP '2026-03-01 08:00:00'),
    (3,  1002, 0, 'CORE',         'ACTUALIZACION',   'SINCRONIZADO', 1, NULL, TIMESTAMP '2026-01-01 08:00:00'),
    (4,  1002, 0, 'NOTIFICACION', 'POLIZA_CREADA',   'SINCRONIZADO', 1, NULL, TIMESTAMP '2026-01-01 08:00:00'),
    (5,  1002, 1, 'CORE',         'ACTUALIZACION',   'SINCRONIZADO', 1, NULL, TIMESTAMP '2026-04-01 08:00:00'),
    (6,  1002, 2, 'CORE',         'ACTUALIZACION',   'SINCRONIZADO', 1, NULL, TIMESTAMP '2026-07-01 08:00:00'),
    (7,  1003, 0, 'CORE',         'ACTUALIZACION',   'SINCRONIZADO', 1, NULL, TIMESTAMP '2026-06-01 08:00:00'),
    (8,  1003, 0, 'NOTIFICACION', 'POLIZA_CREADA',   'SINCRONIZADO', 1, NULL, TIMESTAMP '2026-06-01 08:00:00'),
    (9,  1004, 0, 'CORE',         'ACTUALIZACION',   'SINCRONIZADO', 1, NULL, TIMESTAMP '2026-02-01 08:00:00'),
    (10, 1004, 0, 'NOTIFICACION', 'POLIZA_CREADA',   'SINCRONIZADO', 1, NULL, TIMESTAMP '2026-02-01 08:00:00'),
    (11, 1004, 1, 'CORE',         'ACTUALIZACION',   'SINCRONIZADO', 1, NULL, TIMESTAMP '2026-05-01 08:00:00'),
    (12, 1005, 0, 'CORE',         'ACTUALIZACION',   'SINCRONIZADO', 1, NULL, TIMESTAMP '2025-06-01 08:00:00'),
    (13, 1005, 0, 'NOTIFICACION', 'POLIZA_CREADA',   'SINCRONIZADO', 1, NULL, TIMESTAMP '2025-06-01 08:00:00'),
    (14, 1005, 1, 'CORE',         'ACTUALIZACION',   'SINCRONIZADO', 1, NULL, TIMESTAMP '2026-06-01 08:00:00'),
    (15, 1005, 1, 'NOTIFICACION', 'POLIZA_RENOVADA', 'SINCRONIZADO', 1, NULL, TIMESTAMP '2026-06-01 08:00:00'),
    (16, 1006, 0, 'CORE',         'ACTUALIZACION',   'SINCRONIZADO', 1, NULL, TIMESTAMP '2025-07-01 08:00:00'),
    (17, 1006, 0, 'NOTIFICACION', 'POLIZA_CREADA',   'SINCRONIZADO', 1, NULL, TIMESTAMP '2025-07-01 08:00:00'),
    (18, 1007, 0, 'CORE',         'ACTUALIZACION',   'SINCRONIZADO', 1, NULL, TIMESTAMP '2025-09-01 08:00:00'),
    (19, 1007, 0, 'NOTIFICACION', 'POLIZA_CREADA',   'SINCRONIZADO', 1, NULL, TIMESTAMP '2025-09-01 08:00:00');
