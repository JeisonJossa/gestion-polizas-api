# API de Gestión de Pólizas de Arrendamiento

Módulo 2 de la prueba técnica. Implementa lo esencial del diseño del Módulo 1:

- Pólizas individuales y colectivas, con sus riesgos.
- Renovación con IPC, de una póliza o la renovación automática del día.
- Cancelación.
- Los avisos al CORE (outbox con reintentos), a través de un mock del servicio agnóstico de edición.

Usa el modelo de datos del Módulo 1: PERSONA, POLIZA, RIESGO, IPC y AVISO.

- Spring Boot 4.1.1 y Java 17, con Maven (wrapper incluido).
- Base de datos H2 en memoria con datos precargados, lista para probar apenas arranca.
- Capas: `controller`, `service`, `repository`, más `domain` con las reglas de negocio.
- Contrato al estilo de los servicios del CORE: cada operación declara su **proceso** en la entrada y todas las respuestas tienen la **misma forma** (`idTransaccion`, `proceso`, `resultado`, `mensaje`, `errores`, `datos`).

## Requisitos

- JDK 17 o superior. No hace falta instalar Maven: el proyecto trae `mvnw`.
- Internet la primera vez, para que Maven descargue las dependencias.

## Cómo obtenerlo y ejecutarlo

```bash
git clone https://github.com/JeisonJossa/gestion-polizas-api.git
cd gestion-polizas-api
```

```bash
./mvnw spring-boot:run            # Linux, macOS o Git Bash
.\mvnw.cmd spring-boot:run        # Windows (PowerShell o cmd)
```

La API queda en `http://localhost:8080`. **Todas las peticiones llevan el encabezado `api-key: 123456`**, como pide el enunciado; sin él la respuesta es 401. También se acepta con el nombre `x-api-key`.

## Contrato del API

### Entrada: el bloque `proceso`

Toda petición que cambia una póliza envía primero el bloque `proceso` y después los datos que necesita ese proceso:

```json
{
  "proceso": {
    "tipoProceso": "INCLUSION_RIESGO",
    "canal": "INMOBILIARIA",
    "usuario": "jjossa",
    "fechaMovimiento": "2026-09-29"
  },
  "riesgo": { "...": "datos del riesgo nuevo" }
}
```

| Campo | Obligatorio | Qué es |
|---|---|---|
| `tipoProceso` | Sí | El proceso que se ejecuta. Tiene que corresponder a la ruta (ver la tabla de endpoints); si no, 422 `PROCESO_INVALIDO`. |
| `canal` | Sí | Quién origina la operación: `FRONT`, `INMOBILIARIA`, `PROGRAMADOR`... |
| `usuario` | No | Usuario que la ejecuta. |
| `fechaMovimiento` | No | Fecha del endoso; si no viene, es hoy. Tiene que estar dentro de la vigencia de la póliza (si no, 422). En la renovación automática es la fecha de corte. La emisión y la renovación no la usan: la emisión toma el inicio de la vigencia y la renovación el día siguiente al fin. |

`canal`, `usuario` y el `motivo` de las cancelaciones quedan guardados en la fila del endoso (tabla `POLIZA`). Las personas que llegan en la petición (tomador, arrendatario y arrendador) quedan en la tabla `PERSONA`.

### Salida: la misma forma para todo

```json
{
  "idTransaccion": "c7101f24-57df-4601-9fd9-b7a40c770839",
  "proceso": {
    "tipoProceso": "INCLUSION_RIESGO",
    "fechaInicio": "2026-09-29T21:13:41.058",
    "fechaFin": "2026-09-29T21:13:41.407"
  },
  "resultado": 0,
  "mensaje": "Riesgo 3 incluido en la póliza 1003.",
  "errores": [],
  "datos": {
    "polizaId": 1003, "numeroPoliza": "POL-1003", "tipoPoliza": "COLECTIVA", "estadoPoliza": "VIGENTE",
    "numeroEndoso": 1, "tipoEndoso": "INCLUSION", "fechaEndoso": "2026-09-29",
    "claseMovimiento": "COBRO", "valorMovimiento": 11700000.00,
    "riesgos": [
      { "id": 101, "codigo": 3, "estado": "ACTIVO", "canon": 1300000.00, "meses": 9, "valorMovimiento": 11700000.00 }
    ],
    "inicioVigencia": "2026-06-01", "finVigencia": "2027-05-31",
    "canonMensualPoliza": 3800000.00, "primaTotalPoliza": 41700000.00
  }
}
```

- `idTransaccion` identifica la petición. También va en el encabezado `id-transaccion` de la respuesta y en cada línea del log, para seguir la operación de punta a punta.
- `resultado` es `0` si la operación salió bien y `-1` si falló; en ese caso `errores` trae el código y el detalle, y `datos` es `null`.
- **Lo que se cobra o se devuelve** es `datos.valorMovimiento`: positivo con `COBRO`, negativo con `DEVOLUCION`. En el ejemplo, el riesgo nuevo paga su canon por los 9 meses que faltan de la vigencia (1.300.000 × 9 = 11.700.000). `primaTotalPoliza` es el acumulado de la vigencia (los 30.000.000 de la emisión más los 11.700.000 de este endoso) y `canonMensualPoliza` es la suma de los arriendos mensuales de los riesgos activos; ninguno de los dos se cobra de nuevo.

Qué trae `datos` en cada proceso:

| Proceso | `datos` |
|---|---|
| EMISION, INCLUSION_RIESGO, EXCLUSION_RIESGO, RENOVACION, CANCELACION | El endoso generado, como en el ejemplo. `riesgos` son los riesgos escritos en ese endoso, con los meses y el valor que movió cada uno. |
| RENOVACION_AUTOMATICA | `fechaCorte`, `revisadas`, `renovadas` (el endoso de cada póliza renovada) y `omitidas` (póliza y motivo). |
| CONSULTA_POLIZAS | Lista de pólizas según su último endoso, con `canonMensual`, `primaTotal` y `ultimoEndoso`. |
| CONSULTA_RIESGOS | Lista de riesgos de la póliza en su estado actual. |

### Errores

Los errores tienen la misma forma, con `resultado: -1`, y el código HTTP dice qué tipo de error fue:

```json
{
  "idTransaccion": "a90a874d-ea8f-428d-87ee-6c84f7cae8d3",
  "proceso": { "tipoProceso": "INCLUSION_RIESGO", "fechaInicio": "...", "fechaFin": "..." },
  "resultado": -1,
  "mensaje": "La operación no cumple una regla de negocio.",
  "errores": [
    { "codigo": "REGLA_DE_NEGOCIO", "detalle": "Solo se pueden agregar riesgos a pólizas colectivas; la póliza 1001 es individual." }
  ],
  "datos": null
}
```

| HTTP | Código | Cuándo |
|---|---|---|
| 400 | `DATOS_INVALIDOS` | Falta un campo, un valor no es válido o el JSON está mal formado. Trae un error por campo. |
| 401 | `NO_AUTORIZADO` | Falta `api-key` o no coincide. |
| 404 | `NO_ENCONTRADO` | La póliza o el riesgo no existe. |
| 409 | `ESTADO_INVALIDO` | La póliza está cancelada o el riesgo ya estaba cancelado. |
| 409 | `CONFLICTO` | Dos operaciones cambiaron la misma póliza al mismo tiempo. |
| 422 | `REGLA_DE_NEGOCIO` | La operación no cumple una regla de negocio. |
| 422 | `PROCESO_INVALIDO` | El `tipoProceso` no corresponde a la ruta que se llamó. |
| 500 | `ERROR_INTERNO` | Error inesperado; se busca en el log con el `idTransaccion`. |

## Endpoints

| Método | Ruta | tipoProceso | Cuerpo | Respuestas |
|---|---|---|---|---|
| GET | `/polizas?tipo=&estado=` | CONSULTA_POLIZAS | — | 200, 400 |
| GET | `/polizas/{id}/riesgos` | CONSULTA_RIESGOS | — | 200, 404 |
| POST | `/polizas` | EMISION | `proceso`, `poliza` (tipo, tomador, inicioVigencia, mesesVigencia), `riesgos` | 201, 400, 422 |
| POST | `/polizas/{id}/riesgos` | INCLUSION_RIESGO | `proceso`, `riesgo` | 201, 400, 404, 409, 422 |
| POST | `/riesgos/{id}/cancelar` | EXCLUSION_RIESGO | `proceso`, `motivo` | 200, 400, 404, 409, 422 |
| POST | `/polizas/{id}/renovar` | RENOVACION | `proceso` | 200, 400, 404, 409, 422 |
| POST | `/polizas/{id}/cancelar` | CANCELACION | `proceso`, `motivo` | 200, 400, 404, 409, 422 |
| POST | `/renovaciones` | RENOVACION_AUTOMATICA | `proceso` | 200, 400 |
| POST | `/core-mock/evento` | — | `{"evento": "ACTUALIZACION", "polizaId": 555}` | 202, 400 |

Todas las rutas responden 401 sin `api-key`. En las consultas el API deduce el proceso de la ruta. El mock del CORE conserva el contrato que define el enunciado.

`POST /polizas` (emitir) y `POST /renovaciones` no están en el enunciado del Módulo 2, pero sí en el diseño del Módulo 1. Emitir se incluye porque la regla "una póliza individual solo puede tener 1 riesgo" se valida justamente al crearla; la renovación automática, porque el Módulo 1 la pide.

## Renovación automática

`POST /renovaciones` busca las pólizas cuyo último endoso no está cancelado y cuya vigencia terminó a más tardar en la fecha de corte (la `fechaMovimiento`, o hoy), y renueva cada una con la misma regla de `POST /polizas/{id}/renovar`. Cada póliza se renueva en su propia transacción y avisa al CORE. La respuesta dice cuántas estaban vencidas, cuáles se renovaron y cuáles no, con el motivo (por ejemplo, porque aún no está cargado el IPC); esas se vuelven a intentar en la siguiente corrida. Llamarla dos veces el mismo día no renueva nada dos veces: una póliza renovada ya no está vencida.

En producción la llama un programador de tareas externo una vez al día (cron, Control-M o un CronJob de Kubernetes), a través del API Gateway. Va por fuera de la API para que, con varias copias corriendo, la renovación se ejecute una sola vez. Ejemplo con cron, todos los días a las 2 a. m.:

```bash
0 2 * * * curl -s -X POST http://localhost:8080/renovaciones -H "api-key: 123456" -H "Content-Type: application/json" -d '{"proceso": {"tipoProceso": "RENOVACION_AUTOMATICA", "canal": "PROGRAMADOR"}}'
```

Con los datos precargados, las pólizas 1006 (venció el 30/06/2026) y 1007 (venció el 31/08/2026) están pendientes de renovar.

## Colección de Postman

`postman/gestion-polizas-api.postman_collection.json` trae todos los endpoints, con el encabezado `api-key` ya configurado, los payloads con su bloque `proceso` y pruebas en cada petición. No hay que llenar ningún dato.

1. En Postman: **Import** → seleccione el archivo.
2. Con la app corriendo, envíe cualquier petición, sola y en el orden que quiera:
   - Las variables arrancan con datos precargados: la individual 1001, la colectiva 1003 y el riesgo 6.
   - Las peticiones que necesitan una póliza o un riesgo en cierto estado lo revisan antes de enviarse. Si ya no sirve (por ejemplo, porque se canceló o ya se renovó), crean uno nuevo y actualizan la variable.
3. También se puede ejecutar completa con **Run**:
   - Emite una individual y la renueva.
   - Emite una colectiva, le agrega un riesgo, lo cancela y cancela la póliza.
   - Corre la renovación automática dos veces; la segunda no renueva nada.
   - Prueba los errores (401, 404, 409, 422, 400) y el mock del CORE.

Por consola: `npx newman run postman/gestion-polizas-api.postman_collection.json` (22 peticiones, 34 verificaciones).

## Ejemplos con curl

Los ejemplos están escritos para bash (Git Bash, Linux o macOS). En PowerShell use `curl.exe` en lugar de `curl` (en Windows PowerShell `curl` es otro comando) y escriba cada petición en una sola línea. Los datos van sin tildes: en Windows, el `curl` nativo cambia la codificación de las tildes que van en `-d` y el API responde 400; si necesita tildes, use Postman o guarde el JSON en un archivo UTF-8 y envíelo con `--data-binary @archivo.json`.

```bash
# Colectivas vigentes
curl -H "api-key: 123456" "http://localhost:8080/polizas?tipo=COLECTIVA&estado=VIGENTE"

# Riesgos de la póliza 1002 (tiene historial de endosos)
curl -H "api-key: 123456" http://localhost:8080/polizas/1002/riesgos

# Emitir una póliza individual: el tomador es el arrendatario
curl -X POST http://localhost:8080/polizas \
  -H "api-key: 123456" -H "Content-Type: application/json" \
  -d '{"proceso": {"tipoProceso": "EMISION", "canal": "FRONT", "usuario": "mlopez"},
       "poliza": {"tipo": "INDIVIDUAL",
                  "tomador": {"tipoDocumento": "CC", "numeroDocumento": "1010101010", "nombre": "Mariana Lopez", "correo": "mariana@correo.com"},
                  "inicioVigencia": "2026-10-01", "mesesVigencia": 12},
       "riesgos": [{"inmueble": {"direccion": "Carrera 11 # 82-30 Apto 701", "ciudad": "Bogota"},
                    "arrendatario": {"tipoDocumento": "CC", "numeroDocumento": "1010101010", "nombre": "Mariana Lopez"},
                    "arrendador": {"tipoDocumento": "CC", "numeroDocumento": "79888777", "nombre": "Jaime Rojas"},
                    "canon": 2000000}]}'

# Agregar un riesgo a la colectiva 1003
curl -X POST http://localhost:8080/polizas/1003/riesgos \
  -H "api-key: 123456" -H "Content-Type: application/json" \
  -d '{"proceso": {"tipoProceso": "INCLUSION_RIESGO", "canal": "INMOBILIARIA", "usuario": "jjossa"},
       "riesgo": {"inmueble": {"direccion": "Calle 150 # 45-20 Torre 3 Apto 402", "ciudad": "Bogota"},
                  "arrendatario": {"tipoDocumento": "CC", "numeroDocumento": "1022334455", "nombre": "Valentina Ruiz"},
                  "arrendador": {"tipoDocumento": "CC", "numeroDocumento": "80777666", "nombre": "Hernan Cruz"},
                  "canon": 1300000}}'

# Cancelar el riesgo 6 (colectiva 1003)
curl -X POST http://localhost:8080/riesgos/6/cancelar \
  -H "api-key: 123456" -H "Content-Type: application/json" \
  -d '{"proceso": {"tipoProceso": "EXCLUSION_RIESGO", "canal": "INMOBILIARIA"}, "motivo": "El arrendatario entrego el inmueble"}'

# Renovar la individual 1001 (usa el IPC 2026 precargado, 4,80 %)
curl -X POST http://localhost:8080/polizas/1001/renovar \
  -H "api-key: 123456" -H "Content-Type: application/json" \
  -d '{"proceso": {"tipoProceso": "RENOVACION", "canal": "FRONT"}}'

# Cancelar la colectiva 1003 y todos sus riesgos
curl -X POST http://localhost:8080/polizas/1003/cancelar \
  -H "api-key: 123456" -H "Content-Type: application/json" \
  -d '{"proceso": {"tipoProceso": "CANCELACION", "canal": "INMOBILIARIA"}, "motivo": "Termino el contrato"}'

# Renovación automática del día
curl -X POST http://localhost:8080/renovaciones \
  -H "api-key: 123456" -H "Content-Type: application/json" \
  -d '{"proceso": {"tipoProceso": "RENOVACION_AUTOMATICA", "canal": "PROGRAMADOR"}}'

# Mock del CORE
curl -X POST http://localhost:8080/core-mock/evento \
  -H "api-key: 123456" -H "Content-Type: application/json" \
  -d '{"evento": "ACTUALIZACION", "polizaId": 555}'
```

## Datos precargados

| Póliza | Tipo | Estado | Vigencia | Para qué sirve |
|---|---|---|---|---|
| 1001 | Individual | VIGENTE | mar 2026 – feb 2027 | Probar que no se le pueden agregar riesgos; renovarla. |
| 1002 | Colectiva | VIGENTE | ene – dic 2026 | Historial: endoso 0 emisión, 1 inclusión del riesgo 3 (+10,8 M), 2 exclusión del riesgo 2 (−12 M). |
| 1003 | Colectiva | VIGENTE | jun 2026 – may 2027 | Agregar y cancelar riesgos; cancelar la póliza. |
| 1004 | Individual | CANCELADA | feb 2026 – ene 2027 | Probar que no se renueva una cancelada. |
| 1005 | Individual | RENOVADA | jun 2026 – may 2027 | Póliza ya renovada con el IPC 2025. |
| 1006 | Individual | VIGENTE | jul 2025 – jun 2026 | Vencida sin renovar: la renueva `POST /renovaciones`. |
| 1007 | Colectiva | VIGENTE | sep 2025 – ago 2026 | Vencida sin renovar, con 2 riesgos: la renueva `POST /renovaciones`. |

Riesgos (`/riesgos/{id}`): 1 (póliza 1001); 2, 3 y 4 (1002, el 3 cancelado); 5 y 6 (1003); 7 (1004, cancelado); 8 (1005); 9 (1006); 10 y 11 (1007). Los riesgos nuevos se numeran desde 101 y las pólizas nuevas desde 2001.

IPC precargado: 2023 = 9,28 %, 2024 = 5,20 %, 2025 = 5,10 %, 2026 = 4,80 %. **Son valores de ejemplo**; en producción se cargan los oficiales de cada año.

También vienen 24 personas (las de las pólizas precargadas) y 19 avisos, todos SINCRONIZADOS. Las personas y los avisos nuevos se numeran desde 1001.

La base es en memoria: al reiniciar la aplicación vuelve a estos datos.

## Reglas de negocio

| Regla | Si no se cumple |
|---|---|
| Una póliza individual solo puede tener 1 riesgo, y su tomador es el arrendatario de ese riesgo. | 422 al emitirla |
| Agregar un riesgo exige que la póliza sea colectiva y no esté cancelada. | 422 si es individual, 409 si está cancelada |
| Los riesgos de una póliza individual no se cancelan por separado: se cancela la póliza. | 422 |
| La fecha del movimiento tiene que estar dentro de la vigencia: no se agregan ni se cancelan riesgos, ni se cancela la póliza, antes del inicio o después del fin. | 422 |
| No se puede renovar una póliza cancelada. | 409 |
| Cancelar una póliza cancela todos sus riesgos, en el mismo endoso. | — |
| Un riesgo cancelado no se vuelve a cancelar. | 409 |
| La renovación usa el IPC del año anterior al inicio de la nueva vigencia; si no está cargado, no se renueva. | 422 |
| La renovación automática solo toma pólizas no canceladas con la vigencia terminada; las que no puede renovar quedan como omitidas y se intentan en la siguiente corrida. | — |
| Una colectiva que se queda sin riesgos activos sigue vigente con prima cero, y se renueva igual. | — |
| El `tipoProceso` tiene que corresponder a la ruta. | 422 |

**Prima.** Prima = canon mensual × meses de vigencia, como define el enunciado. Un riesgo que entra o sale a mitad de vigencia paga o devuelve su canon por los meses que faltan, contando completo el mes de la vigencia en que ocurre el movimiento. Los valores de dinero se manejan con `BigDecimal` y dos decimales. En el mercado real, ese valor es el valor asegurado y la prima es una tasa sobre él; el cálculo está en un solo lugar para poder cambiarlo.

**Renovación.** La nueva vigencia dura los mismos meses y empieza el día siguiente al fin de la anterior. El canon de cada riesgo activo se multiplica por (1 + IPC) y la prima arranca con la del periodo nuevo. Ejemplo: canon 1.500.000 con IPC 4,80 % → 1.572.000, y la prima de 12 meses queda en 18.864.000.

## Modelo de datos (Módulo 1)

La base tiene las cinco tablas del diseño del Módulo 1. `schema.sql` las crea al arrancar y Hibernate solo valida que las entidades coincidan con ellas.

| Tabla | Llave | Qué guarda |
|---|---|---|
| PERSONA | `id` | Tomadores, arrendatarios (asegurados) y arrendadores (beneficiarios). Una persona es única por tipo y número de documento: si llega otra vez en una petición, se reutiliza y se actualizan su nombre y sus datos de contacto. |
| POLIZA | `poliza_id` + `num_endoso` | Una fila por endoso. Guarda el número de póliza, el número en el CORE, el tipo y el estado, el tipo de endoso y la clase de movimiento, la vigencia y la fecha del endoso. También el tomador (FK a PERSONA), el canon, la prima y la prima del endoso. Por último, el canal, el usuario y el motivo del proceso que lo originó. |
| RIESGO | `poliza_id` + `num_endoso` + `cod_riesgo` | El arrendatario y el arrendador (FK a PERSONA), el inmueble, el canon mensual, la prima y la prima del endoso. También las fechas de inclusión y exclusión, el estado y `vigente` (S/N). `riesgo_id` es el identificador público para `/riesgos/{id}`. |
| IPC | `anio` | Porcentaje de cada año, para renovar. |
| AVISO | `id` | El outbox: los avisos de cada endoso, para el CORE y para el Servicio de Notificaciones. Guarda el destino, el evento y el estado (PENDIENTE, SINCRONIZADO o RECHAZADO). También los intentos, el último error y la fecha. |

No hay tabla de endosos: el endoso vive en las columnas de `POLIZA` y `RIESGO`.

- **POLIZA**:
  - El endoso 0 es la emisión, y cada operación agrega una fila con el número siguiente.
  - La fila de mayor número es la póliza vigente.
- **RIESGO**: en cada endoso solo se escriben los riesgos que cambian: la fila nueva queda con `vigente = 'S'` y la anterior pasa a `'N'`.
- **Tipo de endoso**: EMISION, INCLUSION, EXCLUSION, RENOVACION o CANCELACION (MODIFICACION queda en el modelo).
- **Clase de movimiento**: sale del signo de la prima del endoso. Es COBRO si es positiva, DEVOLUCION si es negativa y SIN_MOVIMIENTO si es cero.
- **Totalización de la prima**:
  - La prima del endoso es la suma de lo que movieron los riesgos escritos en él.
  - La prima de la póliza es la anterior más la del endoso; en la renovación arranca de nuevo.
  - El canon es la suma de los cánones de los riesgos activos.
- **Llave compuesta**: si dos operaciones intentan crear el mismo endoso a la vez, la llave rechaza la segunda (409).
- **Número en el CORE** (`numero_core`):
  - Lo asigna el CORE cuando registra la póliza.
  - Las pólizas precargadas lo tienen.
  - El mock del Módulo 2 no devuelve número, así que en las pólizas nuevas queda vacío.

Para revisar los datos, abra la consola de H2 en `http://localhost:8080/h2-console`. Use la JDBC URL `jdbc:h2:mem:polizas`, el usuario `sa` y la contraseña vacía; la consola no pide api-key.

```sql
-- Endosos de una póliza, con su tomador
SELECT p.poliza_id, p.num_endoso, p.tipo_endoso, p.clase_movimiento, p.fecha_endoso, p.prima_endoso, p.prima,
       p.canon, p.estado, t.nombre AS tomador, p.canal, p.usuario, p.motivo
FROM poliza p JOIN persona t ON t.id = p.tomador_id
WHERE p.poliza_id = 1002 ORDER BY p.num_endoso;

-- Filas de sus riesgos, con arrendatario y arrendador
SELECT r.riesgo_id, r.cod_riesgo, r.num_endoso, r.estado, r.vigente, r.canon_mensual, r.prima, r.prima_endoso,
       a.nombre AS arrendatario, b.nombre AS arrendador
FROM riesgo r
JOIN persona a ON a.id = r.arrendatario_id
JOIN persona b ON b.id = r.arrendador_id
WHERE r.poliza_id = 1002 ORDER BY r.cod_riesgo, r.num_endoso;

-- Avisos de los endosos (outbox)
SELECT * FROM aviso ORDER BY poliza_id, num_endoso, id;
```

## Integración con el CORE y avisos (outbox)

Las operaciones que cambian estado avisan al CORE con el patrón outbox del Módulo 1. Son emitir, renovar, la renovación automática, cancelar la póliza, agregar un riesgo y cancelar un riesgo.

1. **En la misma transacción del endoso** se guardan sus avisos en `AVISO`:
   - Siempre, uno al CORE, con el evento `ACTUALIZACION`.
   - En la emisión y la renovación, otro para el Servicio de Notificaciones, con el evento `POLIZA_CREADA` o `POLIZA_RENOVADA`.
   - Si el guardado falla, no queda ningún aviso: el CORE nunca recibe el aviso de algo que no existe.
2. **Después del commit**, `ProcesadorAvisos` entrega los avisos pendientes de esa póliza:
   - Los del CORE van por `CoreHttpAdapter`:
     - Envía `POST /core-mock/evento` con `{"evento": "ACTUALIZACION", "polizaId": ...}` y los encabezados `api-key` e `id-aviso`.
     - El mock deja en el log `[CORE-MOCK] Operacion enviada al CORE: evento=ACTUALIZACION, polizaId=...`.
   - Los de notificación van a `LogPublicadorEventos`, que deja el evento en el log. En producción, ese adaptador publica en la cola que lee el Servicio de Notificaciones.
3. **Resultado de cada aviso**:
   - Queda `SINCRONIZADO` si el destino lo recibió.
   - Si el CORE no responde, el aviso sigue `PENDIENTE`:
     - Guarda sus intentos y el último error.
     - El procesador lo reintenta cada 30 segundos (`polizas.avisos.reintento-ms`).
     - Siempre se envía con el mismo `id-aviso`, para que el CORE no lo aplique dos veces.
     - Cada cinco intentos fallidos deja una alerta en el log.
   - Si el CORE lo rechaza (respuesta 4xx), queda `RECHAZADO`. No se reintenta y deja una alerta para revisarlo.
4. Los avisos al CORE de una misma póliza se envían **en orden de endoso**: si uno queda pendiente, los siguientes de esa póliza esperan.

La operación del usuario nunca espera al CORE ni se revierte si el CORE falla. Cada línea del log lleva entre corchetes el `idTransaccion` de la petición que la generó. La URL del CORE se configura en `polizas.core.url`: en producción apunta a la capa media WebLogic y el negocio no cambia.

## Estructura del proyecto

```
├── README.md
├── postman/                          Colección de Postman
├── pom.xml, mvnw, mvnw.cmd
└── src
    ├── main/java/com/pruebatecnica/polizas
    │   ├── controller   PolizaController, RiesgoController, RenovacionController, CoreMockController, Respuestas
    │   ├── service      PolizaService, RiesgoService, RenovacionAutomaticaService, RegistroEndosos, RegistroPersonas
    │   ├── repository   PolizaRepository, RiesgoRepository, PersonaRepository, IpcRepository, AvisoRepository
    │   ├── domain       Poliza, Riesgo, Persona, Ipc, Aviso, PolizaVigente (reglas y totalización), Vigencia, enums
    │   ├── dto          Entradas (bloque proceso) y salidas (RespuestaApi y sus datos); las entidades no se exponen
    │   ├── core         ProcesadorAvisos, AvisoCoreListener, CoreNotifier (puerto) y CoreHttpAdapter,
    │   │                PublicadorEventos (puerto) y LogPublicadorEventos
    │   ├── config       TransaccionFilter, ApiKeyFilter, RelojConfig
    │   └── exception    Excepciones de negocio y GlobalExceptionHandler
    └── main/resources   application.yml, schema.sql (PERSONA, POLIZA, RIESGO, IPC y AVISO), data.sql
```

Las reglas viven en `PolizaVigente`, que no depende de Spring. Cada operación devuelve el endoso nuevo sin tocar la base de datos. El servicio solo registra las personas, carga la póliza, guarda el endoso con sus avisos y responde. `Respuestas` arma todas las respuestas con la forma común.

## Decisiones y simplificaciones frente al Módulo 1

| Módulo 1 (diseño completo) | Aquí |
|---|---|
| Rutas versionadas `/api/v1/...` detrás del API Gateway. | Rutas del enunciado (`/polizas`, `/riesgos`, `/core-mock`). El gateway es quien publica `/api/v1`. |
| Base relacional administrada, con réplica. | H2 en memoria, con el mismo modelo (PERSONA, POLIZA, RIESGO, IPC y AVISO). |
| Cola de eventos y Servicio de Notificaciones (correo y SMS). | Los avisos de notificación se guardan y se entregan al publicador, que los deja en el log. La cola y el servicio quedan fuera del alcance del Módulo 2. |
| Procesador de avisos con varias copias de la API. | El procesador corre dentro de la API. Con varias copias, habría que repartir los avisos entre ellas, por ejemplo bloqueando cada fila al tomarla. |
| OAuth2 en el API Gateway. | `api-key` fija en `application.yml`. |
| Renovación automática diaria por un programador de tareas. | `POST /renovaciones` hace la renovación del día; el programador de tareas externo no se incluye. |

Otros detalles de implementación:

- `riesgo_id` es el identificador público del riesgo para `/riesgos/{id}`. La llave del modelo sigue siendo (póliza, endoso, código de riesgo).
- La fecha de los movimientos es la de hoy en Colombia (`America/Bogota`), salvo que llegue `fechaMovimiento`.
