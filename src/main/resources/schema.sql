-- Modelo del Módulo 1: el endoso vive en las columnas de POLIZA y RIESGO (no hay tabla de endosos).
-- POLIZA: una fila por endoso; la de mayor num_endoso es la póliza vigente.
-- RIESGO: en cada endoso solo se escriben los riesgos que cambian; la fila actual de cada riesgo tiene vigente = 'S'.
-- Las personas (tomador, arrendatario, arrendador) van embebidas: el Módulo 1 las modela en PERSONA.

CREATE TABLE ipc (
    anio        INT            NOT NULL,
    porcentaje  DECIMAL(5, 2)  NOT NULL,
    CONSTRAINT pk_ipc PRIMARY KEY (anio)
);

CREATE SEQUENCE seq_poliza START WITH 2001;
CREATE SEQUENCE seq_riesgo START WITH 101;

CREATE TABLE poliza (
    poliza_id                 BIGINT         NOT NULL,
    num_endoso                INT            NOT NULL,
    numero_poliza             VARCHAR(20)    NOT NULL,
    tipo                      VARCHAR(12)    NOT NULL,
    estado                    VARCHAR(12)    NOT NULL,
    tipo_endoso               VARCHAR(15)    NOT NULL,
    clase_movimiento          VARCHAR(15)    NOT NULL,
    inicio_vigencia           DATE           NOT NULL,
    fin_vigencia              DATE           NOT NULL,
    meses_vigencia            INT            NOT NULL,
    fecha_endoso              DATE           NOT NULL,
    tomador_tipo_documento    VARCHAR(5)     NOT NULL,
    tomador_numero_documento  VARCHAR(20)    NOT NULL,
    tomador_nombre            VARCHAR(120)   NOT NULL,
    tomador_correo            VARCHAR(120),
    tomador_celular           VARCHAR(20),
    canon                     DECIMAL(15, 2) NOT NULL,
    prima                     DECIMAL(15, 2) NOT NULL,
    prima_endoso              DECIMAL(15, 2) NOT NULL,
    CONSTRAINT pk_poliza PRIMARY KEY (poliza_id, num_endoso),
    CONSTRAINT ck_poliza_tipo CHECK (tipo IN ('INDIVIDUAL', 'COLECTIVA')),
    CONSTRAINT ck_poliza_estado CHECK (estado IN ('VIGENTE', 'RENOVADA', 'CANCELADA')),
    CONSTRAINT ck_poliza_tipo_endoso CHECK (tipo_endoso IN
        ('EMISION', 'INCLUSION', 'EXCLUSION', 'MODIFICACION', 'RENOVACION', 'CANCELACION')),
    CONSTRAINT ck_poliza_clase CHECK (clase_movimiento IN ('COBRO', 'DEVOLUCION', 'SIN_MOVIMIENTO'))
);

CREATE INDEX ix_poliza_tipo_estado ON poliza (tipo, estado);

-- riesgo_id identifica al riesgo en el API (/riesgos/{id}) y es el mismo en todas sus filas;
-- la llave del modelo sigue siendo (poliza_id, num_endoso, cod_riesgo).
CREATE TABLE riesgo (
    poliza_id                      BIGINT         NOT NULL,
    num_endoso                     INT            NOT NULL,
    cod_riesgo                     INT            NOT NULL,
    riesgo_id                      BIGINT         NOT NULL,
    inmueble_direccion             VARCHAR(150)   NOT NULL,
    inmueble_ciudad                VARCHAR(60)    NOT NULL,
    arrendatario_tipo_documento    VARCHAR(5)     NOT NULL,
    arrendatario_numero_documento  VARCHAR(20)    NOT NULL,
    arrendatario_nombre            VARCHAR(120)   NOT NULL,
    arrendatario_correo            VARCHAR(120),
    arrendatario_celular           VARCHAR(20),
    arrendador_tipo_documento      VARCHAR(5)     NOT NULL,
    arrendador_numero_documento    VARCHAR(20)    NOT NULL,
    arrendador_nombre              VARCHAR(120)   NOT NULL,
    arrendador_correo              VARCHAR(120),
    arrendador_celular             VARCHAR(20),
    canon                          DECIMAL(15, 2) NOT NULL,
    prima                          DECIMAL(15, 2) NOT NULL,
    prima_endoso                   DECIMAL(15, 2) NOT NULL,
    fecha_inclusion                DATE           NOT NULL,
    fecha_exclusion                DATE,
    estado                         VARCHAR(10)    NOT NULL,
    vigente                        VARCHAR(1)     NOT NULL,
    CONSTRAINT pk_riesgo PRIMARY KEY (poliza_id, num_endoso, cod_riesgo),
    CONSTRAINT fk_riesgo_poliza FOREIGN KEY (poliza_id, num_endoso) REFERENCES poliza (poliza_id, num_endoso),
    CONSTRAINT ck_riesgo_estado CHECK (estado IN ('ACTIVO', 'CANCELADO')),
    CONSTRAINT ck_riesgo_vigente CHECK (vigente IN ('S', 'N'))
);

CREATE INDEX ix_riesgo_vigente ON riesgo (poliza_id, vigente);
CREATE INDEX ix_riesgo_id ON riesgo (riesgo_id, vigente);
