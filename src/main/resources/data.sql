-- Datos de prueba. Los valores de IPC son de ejemplo: en producción se cargan los oficiales de cada año.
-- La renovación usa el IPC del año anterior al inicio de la nueva vigencia.
INSERT INTO ipc (anio, porcentaje) VALUES
    (2023, 9.28),
    (2024, 5.20),
    (2025, 5.10),
    (2026, 4.80);

-- 1001 Individual vigente: el tomador y asegurado es el arrendatario.
-- 1002 Colectiva con historial: endoso 0 emisión, 1 inclusión del riesgo 3 en abril (paga 9 meses),
--      2 exclusión del riesgo 2 en julio (se devuelven 6 meses).
-- 1003 Colectiva vigente de una copropiedad, con 2 riesgos.
-- 1004 Individual cancelada en mayo (se devuelven 9 meses).
-- 1005 Individual renovada en junio de 2026 con el IPC 2025 (5,10 %).
-- 1006 Individual vencida el 30 de junio de 2026 y 1007 colectiva vencida el 31 de agosto de 2026: todavía no se han
--      renovado, así que las toma la renovación automática (POST /renovaciones) con el IPC 2025.
INSERT INTO poliza (poliza_id, num_endoso, numero_poliza, tipo, estado, tipo_endoso, clase_movimiento,
                    inicio_vigencia, fin_vigencia, meses_vigencia, fecha_endoso,
                    tomador_tipo_documento, tomador_numero_documento, tomador_nombre, tomador_correo, tomador_celular,
                    canon, prima, prima_endoso) VALUES
    (1001, 0, 'POL-1001', 'INDIVIDUAL', 'VIGENTE',   'EMISION',     'COBRO',      DATE '2026-03-01', DATE '2027-02-28', 12, DATE '2026-03-01',
     'CC', '52345678', 'Laura Gómez Ruiz', 'laura.gomez@correo.com', '3001234567', 1500000.00, 18000000.00, 18000000.00),

    (1002, 0, 'POL-1002', 'COLECTIVA',  'VIGENTE',   'EMISION',     'COBRO',      DATE '2026-01-01', DATE '2026-12-31', 12, DATE '2026-01-01',
     'NIT', '900123456', 'Inmobiliaria Andina S.A.S.', 'contacto@inmobiliariaandina.com', '6015551234', 3000000.00, 36000000.00, 36000000.00),
    (1002, 1, 'POL-1002', 'COLECTIVA',  'VIGENTE',   'INCLUSION',   'COBRO',      DATE '2026-01-01', DATE '2026-12-31', 12, DATE '2026-04-01',
     'NIT', '900123456', 'Inmobiliaria Andina S.A.S.', 'contacto@inmobiliariaandina.com', '6015551234', 4200000.00, 46800000.00, 10800000.00),
    (1002, 2, 'POL-1002', 'COLECTIVA',  'VIGENTE',   'EXCLUSION',   'DEVOLUCION', DATE '2026-01-01', DATE '2026-12-31', 12, DATE '2026-07-01',
     'NIT', '900123456', 'Inmobiliaria Andina S.A.S.', 'contacto@inmobiliariaandina.com', '6015551234', 2200000.00, 34800000.00, -12000000.00),

    (1003, 0, 'POL-1003', 'COLECTIVA',  'VIGENTE',   'EMISION',     'COBRO',      DATE '2026-06-01', DATE '2027-05-31', 12, DATE '2026-06-01',
     'NIT', '900654321', 'Conjunto Residencial Los Pinos P.H.', 'administracion@lospinos.com', '6017654321', 2500000.00, 30000000.00, 30000000.00),

    (1004, 0, 'POL-1004', 'INDIVIDUAL', 'VIGENTE',   'EMISION',     'COBRO',      DATE '2026-02-01', DATE '2027-01-31', 12, DATE '2026-02-01',
     'CC', '1019988776', 'Julián Cárdenas Mora', 'julian.cardenas@correo.com', '3157778899', 1000000.00, 12000000.00, 12000000.00),
    (1004, 1, 'POL-1004', 'INDIVIDUAL', 'CANCELADA', 'CANCELACION', 'DEVOLUCION', DATE '2026-02-01', DATE '2027-01-31', 12, DATE '2026-05-01',
     'CC', '1019988776', 'Julián Cárdenas Mora', 'julian.cardenas@correo.com', '3157778899', 0.00, 3000000.00, -9000000.00),

    (1005, 0, 'POL-1005', 'INDIVIDUAL', 'VIGENTE',   'EMISION',     'COBRO',      DATE '2025-06-01', DATE '2026-05-31', 12, DATE '2025-06-01',
     'CC', '1036654321', 'Natalia Ospina Vélez', 'natalia.ospina@correo.com', '3204445566', 1000000.00, 12000000.00, 12000000.00),
    (1005, 1, 'POL-1005', 'INDIVIDUAL', 'RENOVADA',  'RENOVACION',  'COBRO',      DATE '2026-06-01', DATE '2027-05-31', 12, DATE '2026-06-01',
     'CC', '1036654321', 'Natalia Ospina Vélez', 'natalia.ospina@correo.com', '3204445566', 1051000.00, 12612000.00, 12612000.00),

    (1006, 0, 'POL-1006', 'INDIVIDUAL', 'VIGENTE',   'EMISION',     'COBRO',      DATE '2025-07-01', DATE '2026-06-30', 12, DATE '2025-07-01',
     'CC', '1045123987', 'Mateo Restrepo Lara', 'mateo.restrepo@correo.com', '3187654321', 2000000.00, 24000000.00, 24000000.00),

    (1007, 0, 'POL-1007', 'COLECTIVA',  'VIGENTE',   'EMISION',     'COBRO',      DATE '2025-09-01', DATE '2026-08-31', 12, DATE '2025-09-01',
     'NIT', '901234567', 'Edificio Torre Central P.H.', 'administracion@torrecentral.com', '6023456789', 2200000.00, 26400000.00, 26400000.00);

INSERT INTO riesgo (poliza_id, num_endoso, cod_riesgo, riesgo_id, inmueble_direccion, inmueble_ciudad,
                    arrendatario_tipo_documento, arrendatario_numero_documento, arrendatario_nombre, arrendatario_correo, arrendatario_celular,
                    arrendador_tipo_documento, arrendador_numero_documento, arrendador_nombre, arrendador_correo, arrendador_celular,
                    canon, prima, prima_endoso, fecha_inclusion, fecha_exclusion, estado, vigente) VALUES
    (1001, 0, 1, 1, 'Carrera 15 # 93-40 Apto 502', 'Bogotá',
     'CC', '52345678', 'Laura Gómez Ruiz', 'laura.gomez@correo.com', '3001234567',
     'CC', '79111222', 'Carlos Pérez Díaz', 'carlos.perez@correo.com', '3109876543',
     1500000.00, 18000000.00, 18000000.00, DATE '2026-03-01', NULL, 'ACTIVO', 'S'),

    (1002, 0, 1, 2, 'Calle 72 # 10-34 Apto 301', 'Bogotá',
     'CC', '1020304050', 'Andrés Molina Rey', 'andres.molina@correo.com', '3012223344',
     'CC', '51987654', 'María Torres Gil', 'maria.torres@correo.com', '3115556677',
     1000000.00, 12000000.00, 12000000.00, DATE '2026-01-01', NULL, 'ACTIVO', 'S'),
    (1002, 0, 2, 3, 'Avenida 19 # 120-15 Apto 804', 'Bogotá',
     'CC', '1032456789', 'Sofía Rojas Peña', 'sofia.rojas@correo.com', '3023334455',
     'CC', '80123456', 'Jorge Castaño Ríos', 'jorge.castano@correo.com', '3126667788',
     2000000.00, 24000000.00, 24000000.00, DATE '2026-01-01', NULL, 'ACTIVO', 'N'),
    (1002, 1, 3, 4, 'Carrera 7 # 45-12 Apto 1102', 'Bogotá',
     'CC', '1015678901', 'Daniel Vargas Soto', 'daniel.vargas@correo.com', '3034445566',
     'CC', '52678901', 'Ana Beltrán Cruz', 'ana.beltran@correo.com', '3137778899',
     1200000.00, 10800000.00, 10800000.00, DATE '2026-04-01', NULL, 'ACTIVO', 'S'),
    (1002, 2, 2, 3, 'Avenida 19 # 120-15 Apto 804', 'Bogotá',
     'CC', '1032456789', 'Sofía Rojas Peña', 'sofia.rojas@correo.com', '3023334455',
     'CC', '80123456', 'Jorge Castaño Ríos', 'jorge.castano@correo.com', '3126667788',
     2000000.00, 12000000.00, -12000000.00, DATE '2026-01-01', DATE '2026-07-01', 'CANCELADO', 'S'),

    (1003, 0, 1, 5, 'Calle 150 # 45-20 Torre 1 Apto 201', 'Bogotá',
     'CC', '1098765432', 'Paula Herrera Díaz', 'paula.herrera@correo.com', '3045556677',
     'CC', '79555666', 'Luis Ortiz Pardo', 'luis.ortiz@correo.com', '3148889900',
     1100000.00, 13200000.00, 13200000.00, DATE '2026-06-01', NULL, 'ACTIVO', 'S'),
    (1003, 0, 2, 6, 'Calle 150 # 45-20 Torre 2 Apto 605', 'Bogotá',
     'CC', '1011223344', 'Camilo Suárez León', 'camilo.suarez@correo.com', '3056667788',
     'CC', '52999888', 'Diana León Mejía', 'diana.leon@correo.com', '3159990011',
     1400000.00, 16800000.00, 16800000.00, DATE '2026-06-01', NULL, 'ACTIVO', 'S'),

    (1004, 0, 1, 7, 'Calle 53 # 20-10', 'Medellín',
     'CC', '1019988776', 'Julián Cárdenas Mora', 'julian.cardenas@correo.com', '3157778899',
     'CC', '43111222', 'Beatriz Mejía Arango', 'beatriz.mejia@correo.com', '3161112233',
     1000000.00, 12000000.00, 12000000.00, DATE '2026-02-01', NULL, 'ACTIVO', 'N'),
    (1004, 1, 1, 7, 'Calle 53 # 20-10', 'Medellín',
     'CC', '1019988776', 'Julián Cárdenas Mora', 'julian.cardenas@correo.com', '3157778899',
     'CC', '43111222', 'Beatriz Mejía Arango', 'beatriz.mejia@correo.com', '3161112233',
     1000000.00, 3000000.00, -9000000.00, DATE '2026-02-01', DATE '2026-05-01', 'CANCELADO', 'S'),

    (1005, 0, 1, 8, 'Carrera 43A # 1-50 Apto 1203', 'Medellín',
     'CC', '1036654321', 'Natalia Ospina Vélez', 'natalia.ospina@correo.com', '3204445566',
     'CC', '70123123', 'Ricardo Gil Salazar', 'ricardo.gil@correo.com', '3172223344',
     1000000.00, 12000000.00, 12000000.00, DATE '2025-06-01', NULL, 'ACTIVO', 'N'),
    (1005, 1, 1, 8, 'Carrera 43A # 1-50 Apto 1203', 'Medellín',
     'CC', '1036654321', 'Natalia Ospina Vélez', 'natalia.ospina@correo.com', '3204445566',
     'CC', '70123123', 'Ricardo Gil Salazar', 'ricardo.gil@correo.com', '3172223344',
     1051000.00, 12612000.00, 12612000.00, DATE '2025-06-01', NULL, 'ACTIVO', 'S'),

    (1006, 0, 1, 9, 'Calle 10 # 38-15 Apto 902', 'Cali',
     'CC', '1045123987', 'Mateo Restrepo Lara', 'mateo.restrepo@correo.com', '3187654321',
     'CC', '16789012', 'Hernando Salcedo Ruiz', 'hernando.salcedo@correo.com', '3193334455',
     2000000.00, 24000000.00, 24000000.00, DATE '2025-07-01', NULL, 'ACTIVO', 'S'),

    (1007, 0, 1, 10, 'Avenida 6N # 23-50 Apto 401', 'Cali',
     'CC', '1144098765', 'Valeria Castro Mina', 'valeria.castro@correo.com', '3205556677',
     'CC', '31456789', 'Gloria Patiño Vera', 'gloria.patino@correo.com', '3216667788',
     900000.00, 10800000.00, 10800000.00, DATE '2025-09-01', NULL, 'ACTIVO', 'S'),
    (1007, 0, 2, 11, 'Avenida 6N # 23-50 Apto 802', 'Cali',
     'CC', '1107654321', 'Santiago Mejía Toro', 'santiago.mejia@correo.com', '3227778899',
     'CC', '31456789', 'Gloria Patiño Vera', 'gloria.patino@correo.com', '3216667788',
     1300000.00, 15600000.00, 15600000.00, DATE '2025-09-01', NULL, 'ACTIVO', 'S');
