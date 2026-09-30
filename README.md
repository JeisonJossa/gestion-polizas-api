# API de Gestión de Pólizas de Arrendamiento

Módulo 2 de la prueba técnica. Implementa lo esencial del diseño del Módulo 1: pólizas individuales y colectivas, sus riesgos, renovación con IPC, cancelación y el aviso al CORE a través de un mock del servicio agnóstico de edición.

- Spring Boot 4.1.1 y Java 17, con Maven (wrapper incluido).
- Base de datos H2 en memoria con datos precargados, lista para probar apenas arranca.
- Capas: `controller`, `service`, `repository`, más `domain` con las reglas de negocio.

## Requisitos

- JDK 17 o superior. No hace falta instalar Maven: el proyecto trae `mvnw`.

## Cómo ejecutarlo

```bash
./mvnw spring-boot:run          # Linux, macOS o Git Bash
mvnw.cmd spring-boot:run        # Windows (cmd o PowerShell)
```

La API queda en `http://localhost:8080`. **Todas las peticiones llevan el encabezado `api-key: 123456`**, como pide el enunciado; sin él la respuesta es 401. También se acepta con el nombre `x-api-key`.

Para correr las pruebas:

```bash
./mvnw test
```

Son 37 pruebas: las reglas de negocio sin Spring, el API completo sobre H2 y una prueba de extremo a extremo que levanta el servidor en el puerto 18080 y verifica que el aviso llega al mock del CORE.

## Endpoints

| Método | Ruta | Qué hace | Respuestas |
|---|---|---|---|
| GET | `/polizas?tipo=&estado=` | Lista las pólizas según su último endoso. `tipo`: INDIVIDUAL o COLECTIVA; `estado`: VIGENTE, RENOVADA o CANCELADA. Ambos son opcionales. | 200, 400 |
| GET | `/polizas/{id}/riesgos` | Riesgos de la póliza, activos y cancelados, en su estado actual. | 200, 404 |
| POST | `/polizas` | Crea una póliza individual (con su riesgo) o colectiva (con sus riesgos). Es el endoso 0. | 201, 400, 422 |
| POST | `/polizas/{id}/renovar` | Renueva por el mismo periodo: canon y prima + IPC del año anterior; la póliza pasa a RENOVADA. | 200, 404, 409, 422 |
| POST | `/renovaciones` | Renovación automática del día: renueva todas las pólizas no canceladas cuya vigencia ya terminó. La llama el programador de tareas. | 200 |
| POST | `/polizas/{id}/cancelar` | Cancela la póliza y todos sus riesgos. | 200, 404, 409 |
| POST | `/polizas/{id}/riesgos` | Agrega un riesgo. Solo aplica a colectivas. | 201, 400, 404, 409, 422 |
| POST | `/riesgos/{id}/cancelar` | Cancela un riesgo de una colectiva. | 200, 404, 409, 422 |
| POST | `/core-mock/evento` | Mock del CORE: solo registra el evento en el log. | 202, 400 |

`POST /polizas` no está en el enunciado del Módulo 2, pero sí en el diseño del Módulo 1. Se incluye porque la regla "una póliza individual solo puede tener 1 riesgo" se valida justamente al crearla.

## Renovación automática

`POST /renovaciones` busca las pólizas cuyo último endoso no está cancelado y cuya vigencia terminó a más tardar hoy, y renueva cada una con la misma regla de `POST /polizas/{id}/renovar`. Cada póliza se renueva en su propia transacción y avisa al CORE. La respuesta dice cuántas estaban vencidas, cuáles se renovaron y cuáles no, con el motivo (por ejemplo, porque aún no está cargado el IPC); esas se vuelven a intentar en la siguiente corrida. Llamarla dos veces el mismo día no renueva nada dos veces: una póliza renovada ya no está vencida.

En producción la llama un programador de tareas externo una vez al día (cron, Control-M o un CronJob de Kubernetes), a través del API Gateway. Va por fuera de la API para que, con varias copias corriendo, la renovación se ejecute una sola vez. Ejemplo con cron, todos los días a las 2 a. m.:

```bash
0 2 * * * curl -s -X POST -H "api-key: 123456" http://localhost:8080/renovaciones
```

Con los datos precargados, las pólizas 1006 (venció el 30/06/2026) y 1007 (venció el 31/08/2026) están pendientes de renovar.

## Colección de Postman

`postman/gestion-polizas-api.postman_collection.json` trae todos los endpoints, con el encabezado `api-key` ya configurado y pruebas en cada petición.

1. En Postman: **Import** → seleccione el archivo.
2. Con la app corriendo, abra la colección y use **Run** para ejecutarla completa y en orden. Cada petición guarda los ids que usa la siguiente: crea una individual y la renueva; crea una colectiva, le agrega un riesgo, lo cancela y cancela la póliza; corre la renovación automática dos veces (la segunda no renueva nada); después prueba los errores (401, 404, 409, 422, 400) y el mock del CORE.

Por consola: `npx newman run postman/gestion-polizas-api.postman_collection.json` (21 peticiones, 35 verificaciones).

## Ejemplos con curl

Los ejemplos usan datos sin tildes. En Windows, el `curl` nativo cambia la codificación de las tildes que van en `-d` y el API responde 400. Si necesita tildes, use Postman o guarde el JSON en un archivo UTF-8 y envíelo con `--data-binary @archivo.json`.

```bash
# Colectivas vigentes
curl -H "api-key: 123456" "http://localhost:8080/polizas?tipo=COLECTIVA&estado=VIGENTE"

# Riesgos de la póliza 1002 (tiene historial de endosos)
curl -H "api-key: 123456" http://localhost:8080/polizas/1002/riesgos

# Agregar un riesgo a la colectiva 1003
curl -X POST http://localhost:8080/polizas/1003/riesgos \
  -H "api-key: 123456" -H "Content-Type: application/json" \
  -d '{"inmueble": {"direccion": "Calle 150 # 45-20 Torre 3 Apto 402", "ciudad": "Bogota"},
       "arrendatario": {"tipoDocumento": "CC", "numeroDocumento": "1022334455", "nombre": "Valentina Ruiz"},
       "arrendador": {"tipoDocumento": "CC", "numeroDocumento": "80777666", "nombre": "Hernan Cruz"},
       "canon": 1300000}'

# Cancelar el riesgo 6 (colectiva 1003)
curl -X POST -H "api-key: 123456" http://localhost:8080/riesgos/6/cancelar

# Renovar la individual 1001 (usa el IPC 2026 precargado, 4,80 %)
curl -X POST -H "api-key: 123456" http://localhost:8080/polizas/1001/renovar

# Cancelar la colectiva 1003 y todos sus riesgos
curl -X POST -H "api-key: 123456" http://localhost:8080/polizas/1003/cancelar

# Crear una póliza individual: el tomador es el arrendatario
curl -X POST http://localhost:8080/polizas \
  -H "api-key: 123456" -H "Content-Type: application/json" \
  -d '{"tipo": "INDIVIDUAL",
       "tomador": {"tipoDocumento": "CC", "numeroDocumento": "1010101010", "nombre": "Mariana Lopez", "correo": "mariana@correo.com"},
       "inicioVigencia": "2026-10-01", "mesesVigencia": 12,
       "riesgos": [{"inmueble": {"direccion": "Carrera 11 # 82-30 Apto 701", "ciudad": "Bogota"},
                    "arrendatario": {"tipoDocumento": "CC", "numeroDocumento": "1010101010", "nombre": "Mariana Lopez"},
                    "arrendador": {"tipoDocumento": "CC", "numeroDocumento": "79888777", "nombre": "Jaime Rojas"},
                    "canon": 2000000}]}'

# Mock del CORE
curl -X POST http://localhost:8080/core-mock/evento \
  -H "api-key: 123456" -H "Content-Type: application/json" \
  -d '{"evento": "ACTUALIZACION", "polizaId": 555}'
```

Respuesta de agregar un riesgo (abreviada). Con fecha de hoy 29/09/2026, a la vigencia de la 1003 (junio 2026 a mayo 2027) le faltan 9 meses:

```json
{
  "poliza": {
    "id": 1003, "tipo": "COLECTIVA", "estado": "VIGENTE",
    "canon": 3800000.00, "prima": 41700000.00,
    "endoso": { "numero": 1, "tipo": "INCLUSION", "claseMovimiento": "COBRO", "fecha": "2026-09-29", "prima": 11700000.00 }
  },
  "riesgo": { "id": 101, "codigo": 3, "estado": "ACTIVO", "canon": 1300000.00, "prima": 11700000.00, "ultimoEndoso": 1 }
}
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

La base es en memoria: al reiniciar la aplicación vuelve a estos datos.

## Reglas de negocio

| Regla | Si no se cumple |
|---|---|
| Una póliza individual solo puede tener 1 riesgo, y su tomador es el arrendatario de ese riesgo. | 422 al crearla |
| Agregar un riesgo exige que la póliza sea colectiva y no esté cancelada. | 422 si es individual, 409 si está cancelada |
| No se puede renovar una póliza cancelada. | 409 |
| Cancelar una póliza cancela todos sus riesgos, en el mismo endoso. | — |
| Un riesgo cancelado no se vuelve a cancelar. | 409 |
| La renovación usa el IPC del año anterior al inicio de la nueva vigencia; si no está cargado, no se renueva. | 422 |
| Una colectiva que se queda sin riesgos activos sigue vigente con prima cero, y se renueva igual. | — |

**Prima.** Prima = canon mensual × meses de vigencia. Un riesgo que entra o sale a mitad de vigencia paga o devuelve su canon por los meses que faltan, contando completo el mes de la vigencia en que ocurre el movimiento. Los valores de dinero se manejan con `BigDecimal` y dos decimales.

**Renovación.** La nueva vigencia dura los mismos meses y empieza el día siguiente al fin de la anterior. El canon de cada riesgo activo se multiplica por (1 + IPC) y la prima arranca con la del periodo nuevo. Ejemplo: canon 1.500.000 con IPC 4,80 % → 1.572.000, y la prima de 12 meses queda en 18.864.000.

## Modelo de endosos (Módulo 1)

No hay tabla de endosos: el endoso vive en las columnas de `POLIZA` y `RIESGO`.

- **POLIZA** tiene una fila por endoso, con llave (`poliza_id`, `num_endoso`). El endoso 0 es la emisión, y cada operación agrega una fila con el número siguiente. La fila de mayor número es la póliza vigente.
- **RIESGO** tiene llave (`poliza_id`, `num_endoso`, `cod_riesgo`). En cada endoso solo se escriben los riesgos que cambian: la fila nueva queda con `vigente = 'S'` y la anterior pasa a `'N'`.
- **Tipo de endoso**: EMISION, INCLUSION, EXCLUSION, RENOVACION o CANCELACION (MODIFICACION queda en el modelo).
- **Clase de movimiento**: sale del signo de la prima del endoso. Es COBRO si es positiva, DEVOLUCION si es negativa y SIN_MOVIMIENTO si es cero.
- **Totalización de la prima**:
  - La prima del endoso es la suma de lo que movieron los riesgos escritos en él.
  - La prima de la póliza es la anterior más la del endoso; en la renovación arranca de nuevo.
  - El canon es la suma de los cánones de los riesgos activos.
- **Llave compuesta**: si dos operaciones intentan crear el mismo endoso a la vez, la llave rechaza la segunda (409).

Para ver los endosos, abra la consola de H2 en `http://localhost:8080/h2-console`. Use la JDBC URL `jdbc:h2:mem:polizas`, el usuario `sa` y la contraseña vacía; la consola no pide api-key.

```sql
SELECT poliza_id, num_endoso, tipo_endoso, clase_movimiento, fecha_endoso, prima_endoso, prima, canon, estado
FROM poliza WHERE poliza_id = 1002 ORDER BY num_endoso;

SELECT riesgo_id, cod_riesgo, num_endoso, estado, vigente, canon, prima, prima_endoso
FROM riesgo WHERE poliza_id = 1002 ORDER BY cod_riesgo, num_endoso;
```

## Integración con el CORE

Las operaciones que cambian estado (crear, renovar, renovación automática, cancelar póliza, agregar riesgo y cancelar riesgo) avisan al CORE, un aviso por cada endoso:

1. El servicio guarda el endoso y publica un evento dentro de la transacción.
2. **Después del commit**, `AvisoCoreListener` llama al puerto `CoreNotifier`. Así, si el guardado falla, el CORE nunca recibe un aviso de algo que no existe.
3. `CoreHttpAdapter` envía `POST /core-mock/evento` con `{"evento": "ACTUALIZACION", "polizaId": ...}` y el encabezado `api-key`.
4. El mock deja en el log: `[CORE-MOCK] Operacion enviada al CORE: evento=ACTUALIZACION, polizaId=...`.

Si el CORE no responde, se registra una advertencia y la operación del usuario no se revierte. La URL se configura en `polizas.core.url`: en producción apunta a la capa media WebLogic y el negocio no cambia.

## Errores

Todas las respuestas de error tienen la misma forma:

```json
{ "codigo": "REGLA_DE_NEGOCIO", "mensaje": "Solo se pueden agregar riesgos a pólizas colectivas; la póliza 1001 es individual." }
```

| HTTP | Código | Cuándo |
|---|---|---|
| 400 | `DATOS_INVALIDOS` | Falta un campo, un valor no es válido o el JSON está mal formado. Trae `detalles` por campo. |
| 401 | `NO_AUTORIZADO` | Falta `api-key` o no coincide. |
| 404 | `NO_ENCONTRADO` | La póliza o el riesgo no existe. |
| 409 | `ESTADO_INVALIDO` | La póliza está cancelada o el riesgo ya estaba cancelado. |
| 409 | `CONFLICTO` | Dos operaciones cambiaron la misma póliza al mismo tiempo. |
| 422 | `REGLA_DE_NEGOCIO` | La operación no cumple una regla de negocio. |

## Estructura del proyecto

```
src/main/java/com/pruebatecnica/polizas
├── controller   PolizaController, RiesgoController, RenovacionController, CoreMockController
├── service      PolizaService, RiesgoService, RenovacionAutomaticaService, RegistroEndosos (carga la póliza vigente y guarda cada endoso)
├── repository   PolizaRepository, RiesgoRepository, IpcRepository (Spring Data JPA)
├── domain       Poliza, Riesgo, Ipc, PolizaVigente (reglas de negocio y totalización), Vigencia, enums
├── dto          Solicitudes y respuestas (las entidades no se exponen)
├── core         CoreNotifier (puerto), CoreHttpAdapter, AvisoCoreListener
├── config       ApiKeyFilter, RelojConfig
└── exception    Excepciones de negocio y GlobalExceptionHandler
src/main/resources
├── application.yml
├── schema.sql   Tablas POLIZA, RIESGO e IPC
└── data.sql     Datos precargados
```

Las reglas viven en `PolizaVigente`, que no depende de Spring. Cada operación devuelve el endoso nuevo sin tocar la base de datos, y el servicio solo lo carga, lo guarda y avisa.

## Decisiones y simplificaciones frente al Módulo 1

| Módulo 1 (diseño completo) | Aquí |
|---|---|
| Rutas versionadas `/api/v1/...` detrás del API Gateway. | Rutas del enunciado (`/polizas`, `/riesgos`, `/core-mock`). El gateway es quien publica `/api/v1`. |
| Tabla `PERSONA` para tomador, arrendatario y arrendador. | Datos de las personas embebidos en póliza y riesgo. |
| Tabla `AVISO` y Procesador de avisos (outbox) con reintentos. | Aviso directo al CORE después del commit; si falla, queda en el log. |
| Notificaciones por correo y SMS. | Fuera del alcance del Módulo 2. |
| OAuth2 en el API Gateway. | `api-key` fija en `application.yml`. |
| Renovación automática diaria por un programador de tareas. | `POST /renovaciones` hace la renovación del día; el programador de tareas externo no se incluye. |

Otros detalles de implementación:

- `riesgo_id` es el identificador público del riesgo para `/riesgos/{id}`. La llave del modelo sigue siendo (póliza, endoso, código de riesgo).
- La fecha de los movimientos es la de hoy en Colombia (`America/Bogota`). Las pruebas usan un reloj fijo.
