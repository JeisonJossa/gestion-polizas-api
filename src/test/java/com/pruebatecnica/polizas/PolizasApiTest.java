package com.pruebatecnica.polizas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;
import com.pruebatecnica.polizas.core.CoreNotifier;
import com.pruebatecnica.polizas.domain.TipoEndoso;

/**
 * Pruebas del API sobre la aplicación completa (H2 con los datos precargados). Las pruebas que cambian datos crean
 * sus propias pólizas; las precargadas 1001, 1002 y 1004 solo se leen o se usan en casos que se rechazan, y la 1006
 * solo la renueva la prueba de renovación automática.
 * La fecha de hoy es el 15 de julio de 2026: en una vigencia de enero a diciembre faltan 6 meses.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:api;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class PolizasApiTest {

	private static final String API_KEY = "123456";

	@TestConfiguration
	static class RelojFijo {

		@Bean
		@Primary
		Clock relojFijo() {
			return Clock.fixed(Instant.parse("2026-07-15T15:00:00Z"), ZoneId.of("America/Bogota"));
		}
	}

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private CoreNotifier core;

	@Test
	void exigeElEncabezadoApiKeyYTambienAceptaXApiKey() throws Exception {
		mvc.perform(get("/polizas"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.codigo").value("NO_AUTORIZADO"));
		mvc.perform(get("/polizas").header("api-key", "999"))
				.andExpect(status().isUnauthorized());
		mvc.perform(get("/polizas").header("x-api-key", API_KEY))
				.andExpect(status().isOk());
	}

	@Test
	void listaPolizasPorTipoYEstadoSegunSuUltimoEndoso() throws Exception {
		conApiKey(get("/polizas").param("tipo", "COLECTIVA").param("estado", "VIGENTE"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].id", hasItems(1002, 1003)))
				.andExpect(jsonPath("$[*].tipo", everyItem(is("COLECTIVA"))))
				.andExpect(jsonPath("$[*].estado", everyItem(is("VIGENTE"))))
				.andExpect(jsonPath("$[?(@.id == 1002)].endoso.numero", hasItems(2)))
				.andExpect(jsonPath("$[?(@.id == 1002)].prima", hasItems(34800000.0)));
	}

	@Test
	void losFiltrosSonOpcionales() throws Exception {
		conApiKey(get("/polizas"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].id", hasItems(1001, 1002, 1003, 1004, 1005)));
		conApiKey(get("/polizas").param("estado", "CANCELADA"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].id", hasItems(1004)))
				.andExpect(jsonPath("$[*].estado", everyItem(is("CANCELADA"))));
	}

	@Test
	void rechazaUnTipoDePolizaQueNoExiste() throws Exception {
		conApiKey(get("/polizas").param("tipo", "OTRO"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje", containsString("INDIVIDUAL, COLECTIVA")));
	}

	@Test
	void consultaLosRiesgosDeUnaPolizaEnSuEstadoActual() throws Exception {
		conApiKey(get("/polizas/1002/riesgos"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(3)))
				.andExpect(jsonPath("$[0].estado").value("ACTIVO"))
				.andExpect(jsonPath("$[1].id").value(3))
				.andExpect(jsonPath("$[1].estado").value("CANCELADO"))
				.andExpect(jsonPath("$[1].prima").value(12000000.0))
				.andExpect(jsonPath("$[1].ultimoEndoso").value(2))
				.andExpect(jsonPath("$[2].codigo").value(3));
	}

	@Test
	void respondeNoEncontradoParaUnaPolizaOUnaRutaQueNoExiste() throws Exception {
		conApiKey(get("/polizas/9999/riesgos"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
		conApiKey(get("/no-existe"))
				.andExpect(status().isNotFound());
	}

	@Test
	void noAgregaRiesgosAUnaPolizaIndividual() throws Exception {
		conApiKey(post("/polizas/1001/riesgos").contentType(MediaType.APPLICATION_JSON).content(riesgo("1234", 900_000)))
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.codigo").value("REGLA_DE_NEGOCIO"));
		verify(core, never()).notificarActualizacion(anyLong(), anyInt(), any());
	}

	@Test
	void noRenuevaUnaPolizaCancelada() throws Exception {
		conApiKey(post("/polizas/1004/renovar"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("ESTADO_INVALIDO"));
	}

	@Test
	void unaPolizaIndividualSoloPuedeTenerUnRiesgo() throws Exception {
		String individualConDosRiesgos = """
				{"tipo": "INDIVIDUAL",
				 "tomador": {"tipoDocumento": "CC", "numeroDocumento": "1000000001", "nombre": "Ana Arrendataria"},
				 "inicioVigencia": "2026-03-01", "mesesVigencia": 12,
				 "riesgos": [%s, %s]}
				""".formatted(riesgo("1000000001", 1_000_000), riesgo("1000000001", 1_000_000));
		crear(individualConDosRiesgos)
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.mensaje", containsString("solo puede tener 1 riesgo")));
	}

	@Test
	void validaLosDatosDeEntrada() throws Exception {
		crear("{}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
				.andExpect(jsonPath("$.detalles", hasItems(containsString("tipo:"), containsString("riesgos:"))));
	}

	@Test
	void creaUnaColectivaYLeAgregaUnRiesgoQuePagaLosMesesQueFaltan() throws Exception {
		String respuesta = crear(colectiva(riesgo("2000000001", 1_000_000), riesgo("2000000002", 2_000_000)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.endoso.numero").value(0))
				.andExpect(jsonPath("$.endoso.tipo").value("EMISION"))
				.andExpect(jsonPath("$.endoso.claseMovimiento").value("COBRO"))
				.andExpect(jsonPath("$.prima").value(36000000.0))
				.andExpect(jsonPath("$.riesgos", hasSize(2)))
				.andReturn().getResponse().getContentAsString();
		long id = ((Number) JsonPath.read(respuesta, "$.id")).longValue();

		conApiKey(post("/polizas/" + id + "/riesgos").contentType(MediaType.APPLICATION_JSON)
				.content(riesgo("2000000003", 1_000_000)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.poliza.endoso.numero").value(1))
				.andExpect(jsonPath("$.poliza.endoso.tipo").value("INCLUSION"))
				.andExpect(jsonPath("$.poliza.endoso.claseMovimiento").value("COBRO"))
				.andExpect(jsonPath("$.poliza.endoso.prima").value(6000000.0))
				.andExpect(jsonPath("$.poliza.prima").value(42000000.0))
				.andExpect(jsonPath("$.riesgo.codigo").value(3))
				.andExpect(jsonPath("$.riesgo.prima").value(6000000.0));

		verify(core).notificarActualizacion(id, 0, TipoEndoso.EMISION);
		verify(core).notificarActualizacion(id, 1, TipoEndoso.INCLUSION);
	}

	@Test
	void cancelaUnRiesgoYDevuelveLosMesesQueFaltan() throws Exception {
		String respuesta = crear(colectiva(riesgo("3000000001", 1_000_000), riesgo("3000000002", 2_000_000)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		long riesgo2 = ((Number) JsonPath.read(respuesta, "$.riesgos[1].id")).longValue();

		conApiKey(post("/riesgos/" + riesgo2 + "/cancelar"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.poliza.estado").value("VIGENTE"))
				.andExpect(jsonPath("$.poliza.endoso.tipo").value("EXCLUSION"))
				.andExpect(jsonPath("$.poliza.endoso.claseMovimiento").value("DEVOLUCION"))
				.andExpect(jsonPath("$.poliza.endoso.prima").value(-12000000.0))
				.andExpect(jsonPath("$.poliza.prima").value(24000000.0))
				.andExpect(jsonPath("$.riesgo.estado").value("CANCELADO"))
				.andExpect(jsonPath("$.riesgo.prima").value(12000000.0));

		conApiKey(post("/riesgos/" + riesgo2 + "/cancelar"))
				.andExpect(status().isConflict());
	}

	@Test
	void cancelarLaPolizaCancelaTodosSusRiesgos() throws Exception {
		String respuesta = crear(colectiva(riesgo("4000000001", 1_000_000), riesgo("4000000002", 2_000_000)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		long id = ((Number) JsonPath.read(respuesta, "$.id")).longValue();

		conApiKey(post("/polizas/" + id + "/cancelar"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("CANCELADA"))
				.andExpect(jsonPath("$.endoso.tipo").value("CANCELACION"))
				.andExpect(jsonPath("$.endoso.prima").value(-18000000.0))
				.andExpect(jsonPath("$.canon").value(0.0))
				.andExpect(jsonPath("$.riesgos[*].estado", everyItem(is("CANCELADO"))));

		conApiKey(post("/polizas/" + id + "/renovar")).andExpect(status().isConflict());
		conApiKey(post("/polizas/" + id + "/riesgos").contentType(MediaType.APPLICATION_JSON)
				.content(riesgo("4000000003", 1_000_000)))
				.andExpect(status().isConflict());
	}

	@Test
	void renuevaConElIpcDelAnioAnteriorYExigeQueEsteCargado() throws Exception {
		String individual = """
				{"tipo": "INDIVIDUAL",
				 "tomador": {"tipoDocumento": "CC", "numeroDocumento": "5000000001", "nombre": "Ana Arrendataria"},
				 "inicioVigencia": "2026-03-01", "mesesVigencia": 12,
				 "riesgos": [%s]}
				""".formatted(riesgo("5000000001", 1_000_000));
		String respuesta = crear(individual).andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		long id = ((Number) JsonPath.read(respuesta, "$.id")).longValue();

		conApiKey(post("/polizas/" + id + "/renovar"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("RENOVADA"))
				.andExpect(jsonPath("$.inicioVigencia").value("2027-03-01"))
				.andExpect(jsonPath("$.finVigencia").value("2028-02-29"))
				.andExpect(jsonPath("$.canon").value(1048000.0))
				.andExpect(jsonPath("$.prima").value(12576000.0))
				.andExpect(jsonPath("$.endoso.tipo").value("RENOVACION"));

		conApiKey(post("/polizas/" + id + "/renovar"))
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.mensaje", containsString("No hay IPC cargado para 2027")));
	}

	@Test
	void laRenovacionAutomaticaRenuevaLasVencidasYReintentaLasQueNoPudo() throws Exception {
		// Vence el 31 de diciembre de 2020: su renovación necesita el IPC 2020, que no está cargado.
		String sinIpc = """
				{"tipo": "INDIVIDUAL",
				 "tomador": {"tipoDocumento": "CC", "numeroDocumento": "6000000001", "nombre": "Ana Arrendataria"},
				 "inicioVigencia": "2020-01-01", "mesesVigencia": 12,
				 "riesgos": [%s]}
				""".formatted(riesgo("6000000001", 1_000_000));
		long sinIpcId = ((Number) JsonPath.read(crear(sinIpc).andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString(), "$.id")).longValue();

		// Hoy es 15 de julio: 1006 venció el 30 de junio; 1007 vence el 31 de agosto y todavía no se renueva.
		conApiKey(post("/renovaciones"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fecha").value("2026-07-15"))
				.andExpect(jsonPath("$.revisadas").value(2))
				.andExpect(jsonPath("$.renovadas", hasSize(1)))
				.andExpect(jsonPath("$.renovadas[0].id").value(1006))
				.andExpect(jsonPath("$.renovadas[0].estado").value("RENOVADA"))
				.andExpect(jsonPath("$.renovadas[0].inicioVigencia").value("2026-07-01"))
				.andExpect(jsonPath("$.renovadas[0].finVigencia").value("2027-06-30"))
				.andExpect(jsonPath("$.renovadas[0].canon").value(2102000.0))
				.andExpect(jsonPath("$.renovadas[0].prima").value(25224000.0))
				.andExpect(jsonPath("$.renovadas[0].endoso.tipo").value("RENOVACION"))
				.andExpect(jsonPath("$.omitidas", hasSize(1)))
				.andExpect(jsonPath("$.omitidas[0].polizaId").value(sinIpcId))
				.andExpect(jsonPath("$.omitidas[0].motivo", containsString("No hay IPC cargado para 2020")));
		verify(core).notificarActualizacion(1006, 1, TipoEndoso.RENOVACION);

		// Repetir la corrida no renueva otra vez la 1006; la que no tenía IPC se vuelve a intentar.
		conApiKey(post("/renovaciones"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.renovadas", hasSize(0)))
				.andExpect(jsonPath("$.omitidas[0].polizaId").value(sinIpcId));
	}

	@Test
	void elMockDelCoreRegistraElEventoEnElLog(CapturedOutput salida) throws Exception {
		conApiKey(post("/core-mock/evento").contentType(MediaType.APPLICATION_JSON)
				.content("{\"evento\": \"ACTUALIZACION\", \"polizaId\": 555}"))
				.andExpect(status().isAccepted());

		assertThat(salida.getOut())
				.contains("[CORE-MOCK] Operacion enviada al CORE: evento=ACTUALIZACION, polizaId=555");
	}

	private ResultActions conApiKey(MockHttpServletRequestBuilder peticion) throws Exception {
		return mvc.perform(peticion.header("api-key", API_KEY));
	}

	private ResultActions crear(String json) throws Exception {
		return conApiKey(post("/polizas").contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private static String colectiva(String... riesgos) {
		return """
				{"tipo": "COLECTIVA",
				 "tomador": {"tipoDocumento": "NIT", "numeroDocumento": "900111222",
				             "nombre": "Inmobiliaria de Prueba S.A.S.", "correo": "prueba@inmobiliaria.com"},
				 "inicioVigencia": "2026-01-01", "mesesVigencia": 12,
				 "riesgos": [%s]}
				""".formatted(String.join(",", riesgos));
	}

	private static String riesgo(String documentoArrendatario, int canon) {
		return """
				{"inmueble": {"direccion": "Calle 100 # 10-20 Apto %s", "ciudad": "Bogotá"},
				 "arrendatario": {"tipoDocumento": "CC", "numeroDocumento": "%s", "nombre": "Ana Arrendataria"},
				 "arrendador": {"tipoDocumento": "CC", "numeroDocumento": "79000000", "nombre": "Pedro Arrendador"},
				 "canon": %d}
				""".formatted(documentoArrendatario.substring(documentoArrendatario.length() - 3), documentoArrendatario,
				canon);
	}
}
