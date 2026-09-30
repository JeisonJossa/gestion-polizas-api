package com.pruebatecnica.polizas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;
import com.pruebatecnica.polizas.core.CoreNoDisponibleException;
import com.pruebatecnica.polizas.core.CoreNotifier;
import com.pruebatecnica.polizas.core.CoreRechazoException;
import com.pruebatecnica.polizas.core.ProcesadorAvisos;

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

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private ProcesadorAvisos procesador;

	@MockitoBean
	private CoreNotifier core;

	@Test
	void exigeElEncabezadoApiKeyYTambienAceptaXApiKey() throws Exception {
		mvc.perform(get("/polizas"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.resultado").value(-1))
				.andExpect(jsonPath("$.proceso.tipoProceso").value("CONSULTA_POLIZAS"))
				.andExpect(jsonPath("$.errores[0].codigo").value("NO_AUTORIZADO"))
				.andExpect(jsonPath("$.datos").doesNotExist());
		mvc.perform(get("/polizas").header("api-key", "999"))
				.andExpect(status().isUnauthorized());
		mvc.perform(get("/polizas").header("x-api-key", API_KEY))
				.andExpect(status().isOk());
	}

	@Test
	void todaRespuestaTraeLaTransaccionYElProceso() throws Exception {
		MvcResult resultado = conApiKey(get("/polizas"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.idTransaccion", notNullValue()))
				.andExpect(jsonPath("$.proceso.tipoProceso").value("CONSULTA_POLIZAS"))
				.andExpect(jsonPath("$.proceso.fechaInicio", notNullValue()))
				.andExpect(jsonPath("$.proceso.fechaFin", notNullValue()))
				.andExpect(jsonPath("$.resultado").value(0))
				.andExpect(jsonPath("$.errores", hasSize(0)))
				.andReturn();
		String id = JsonPath.read(resultado.getResponse().getContentAsString(), "$.idTransaccion");
		assertThat(resultado.getResponse().getHeader("id-transaccion")).isEqualTo(id);
	}

	@Test
	void listaPolizasPorTipoYEstadoSegunSuUltimoEndoso() throws Exception {
		conApiKey(get("/polizas").param("tipo", "COLECTIVA").param("estado", "VIGENTE"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.datos[*].polizaId", hasItems(1002, 1003)))
				.andExpect(jsonPath("$.datos[*].tipo", everyItem(is("COLECTIVA"))))
				.andExpect(jsonPath("$.datos[*].estado", everyItem(is("VIGENTE"))))
				.andExpect(jsonPath("$.datos[?(@.polizaId == 1002)].ultimoEndoso.numero", hasItems(2)))
				.andExpect(jsonPath("$.datos[?(@.polizaId == 1002)].ultimoEndoso.claseMovimiento", hasItems("DEVOLUCION")))
				.andExpect(jsonPath("$.datos[?(@.polizaId == 1002)].primaTotal", hasItems(34800000.0)));
	}

	@Test
	void losFiltrosSonOpcionales() throws Exception {
		conApiKey(get("/polizas"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.datos[*].polizaId", hasItems(1001, 1002, 1003, 1004, 1005)));
		conApiKey(get("/polizas").param("estado", "CANCELADA"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.datos[*].polizaId", hasItems(1004)))
				.andExpect(jsonPath("$.datos[*].estado", everyItem(is("CANCELADA"))));
	}

	@Test
	void rechazaUnTipoDePolizaQueNoExiste() throws Exception {
		conApiKey(get("/polizas").param("tipo", "OTRO"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.resultado").value(-1))
				.andExpect(jsonPath("$.errores[0].detalle", containsString("INDIVIDUAL, COLECTIVA")));
	}

	@Test
	void consultaLosRiesgosDeUnaPolizaEnSuEstadoActual() throws Exception {
		conApiKey(get("/polizas/1002/riesgos"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.proceso.tipoProceso").value("CONSULTA_RIESGOS"))
				.andExpect(jsonPath("$.datos", hasSize(3)))
				.andExpect(jsonPath("$.datos[0].estado").value("ACTIVO"))
				.andExpect(jsonPath("$.datos[1].id").value(3))
				.andExpect(jsonPath("$.datos[1].estado").value("CANCELADO"))
				.andExpect(jsonPath("$.datos[1].prima").value(12000000.0))
				.andExpect(jsonPath("$.datos[1].ultimoEndoso").value(2))
				.andExpect(jsonPath("$.datos[2].codigo").value(3));
	}

	@Test
	void respondeNoEncontradoParaUnaPolizaOUnaRutaQueNoExiste() throws Exception {
		conApiKey(get("/polizas/9999/riesgos"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errores[0].codigo").value("NO_ENCONTRADO"));
		conApiKey(get("/no-existe"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.resultado").value(-1));
	}

	@Test
	void noAgregaRiesgosAUnaPolizaIndividual() throws Exception {
		enviar("/polizas/1001/riesgos", inclusion(riesgo("1234", 900_000), null))
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.proceso.tipoProceso").value("INCLUSION_RIESGO"))
				.andExpect(jsonPath("$.errores[0].codigo").value("REGLA_DE_NEGOCIO"));
		verify(core, never()).enviar(anyLong(), anyLong(), anyInt());
	}

	@Test
	void rechazaUnTipoProcesoQueNoCorrespondeALaRuta() throws Exception {
		String cuerpo = """
				{%s, "riesgo": %s}
				""".formatted(proceso("CANCELACION", null), riesgo("1235", 900_000));
		enviar("/polizas/1003/riesgos", cuerpo)
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.errores[0].codigo").value("PROCESO_INVALIDO"))
				.andExpect(jsonPath("$.errores[0].detalle", containsString("se esperaba INCLUSION_RIESGO")));
	}

	@Test
	void noRenuevaUnaPolizaCancelada() throws Exception {
		enviar("/polizas/1004/renovar", soloProceso("RENOVACION"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errores[0].codigo").value("ESTADO_INVALIDO"));
	}

	@Test
	void unaPolizaIndividualSoloPuedeTenerUnRiesgo() throws Exception {
		emitir(individual("1000000001", "2026-03-01"), riesgo("1000000001", 1_000_000), riesgo("1000000001", 1_000_000))
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.errores[0].detalle", containsString("solo puede tener 1 riesgo")));
	}

	@Test
	void validaLosDatosDeEntrada() throws Exception {
		enviar("/polizas", "{}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("La petición tiene datos inválidos."))
				.andExpect(jsonPath("$.errores[*].codigo", everyItem(is("DATOS_INVALIDOS"))))
				.andExpect(jsonPath("$.errores[*].detalle",
						hasItems(containsString("proceso:"), containsString("poliza:"), containsString("riesgos:"))));
		enviar("/polizas/1003/cancelar", """
				{"proceso": {"tipoProceso": "CANCELACION"}}
				""")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[*].detalle",
						hasItems(containsString("proceso.canal:"), containsString("motivo:"))));
	}

	@Test
	void emiteUnaColectivaYLeAgregaUnRiesgoQuePagaLosMesesQueFaltan() throws Exception {
		String respuesta = emitir(colectiva(), riesgo("2000000001", 1_000_000), riesgo("2000000002", 2_000_000))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.proceso.tipoProceso").value("EMISION"))
				.andExpect(jsonPath("$.resultado").value(0))
				.andExpect(jsonPath("$.datos.numeroEndoso").value(0))
				.andExpect(jsonPath("$.datos.tipoEndoso").value("EMISION"))
				.andExpect(jsonPath("$.datos.claseMovimiento").value("COBRO"))
				.andExpect(jsonPath("$.datos.valorMovimiento").value(36000000.0))
				.andExpect(jsonPath("$.datos.primaTotalPoliza").value(36000000.0))
				.andExpect(jsonPath("$.datos.riesgos", hasSize(2)))
				.andExpect(jsonPath("$.datos.riesgos[*].meses", everyItem(is(12))))
				.andReturn().getResponse().getContentAsString();
		long id = numero(respuesta, "$.datos.polizaId");

		enviar("/polizas/" + id + "/riesgos", inclusion(riesgo("2000000003", 1_000_000), null))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.proceso.tipoProceso").value("INCLUSION_RIESGO"))
				.andExpect(jsonPath("$.datos.numeroEndoso").value(1))
				.andExpect(jsonPath("$.datos.tipoEndoso").value("INCLUSION"))
				.andExpect(jsonPath("$.datos.claseMovimiento").value("COBRO"))
				.andExpect(jsonPath("$.datos.valorMovimiento").value(6000000.0))
				.andExpect(jsonPath("$.datos.primaTotalPoliza").value(42000000.0))
				.andExpect(jsonPath("$.datos.canonMensualPoliza").value(4000000.0))
				.andExpect(jsonPath("$.datos.riesgos", hasSize(1)))
				.andExpect(jsonPath("$.datos.riesgos[0].codigo").value(3))
				.andExpect(jsonPath("$.datos.riesgos[0].meses").value(6))
				.andExpect(jsonPath("$.datos.riesgos[0].valorMovimiento").value(6000000.0));

		verify(core).enviar(anyLong(), eq(id), eq(0));
		verify(core).enviar(anyLong(), eq(id), eq(1));
	}

	@Test
	void usaLaFechaDelMovimientoYLaValidaContraLaVigencia() throws Exception {
		long id = numero(emitir(colectiva(), riesgo("2100000001", 1_000_000))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.datos.polizaId");

		// En octubre faltan 3 meses de la vigencia (octubre a diciembre).
		enviar("/polizas/" + id + "/riesgos", inclusion(riesgo("2100000002", 1_000_000), "2026-10-01"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.datos.fechaEndoso").value("2026-10-01"))
				.andExpect(jsonPath("$.datos.riesgos[0].meses").value(3))
				.andExpect(jsonPath("$.datos.valorMovimiento").value(3000000.0));

		enviar("/polizas/" + id + "/riesgos", inclusion(riesgo("2100000003", 1_000_000), "2025-12-15"))
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.errores[0].detalle", containsString("anterior al inicio de la vigencia")));
	}

	@Test
	void cancelaUnRiesgoYDevuelveLosMesesQueFaltan() throws Exception {
		String respuesta = emitir(colectiva(), riesgo("3000000001", 1_000_000), riesgo("3000000002", 2_000_000))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		long riesgo2 = numero(respuesta, "$.datos.riesgos[1].id");

		enviar("/riesgos/" + riesgo2 + "/cancelar", cancelacion("EXCLUSION_RIESGO"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.proceso.tipoProceso").value("EXCLUSION_RIESGO"))
				.andExpect(jsonPath("$.datos.estadoPoliza").value("VIGENTE"))
				.andExpect(jsonPath("$.datos.tipoEndoso").value("EXCLUSION"))
				.andExpect(jsonPath("$.datos.claseMovimiento").value("DEVOLUCION"))
				.andExpect(jsonPath("$.datos.valorMovimiento").value(-12000000.0))
				.andExpect(jsonPath("$.datos.primaTotalPoliza").value(24000000.0))
				.andExpect(jsonPath("$.datos.riesgos[0].estado").value("CANCELADO"))
				.andExpect(jsonPath("$.datos.riesgos[0].meses").value(6));

		enviar("/riesgos/" + riesgo2 + "/cancelar", cancelacion("EXCLUSION_RIESGO"))
				.andExpect(status().isConflict());
	}

	@Test
	void cancelarLaPolizaCancelaTodosSusRiesgosYGuardaElOrigen() throws Exception {
		long id = numero(emitir(colectiva(), riesgo("4000000001", 1_000_000), riesgo("4000000002", 2_000_000))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.datos.polizaId");

		enviar("/polizas/" + id + "/cancelar", cancelacion("CANCELACION"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.datos.estadoPoliza").value("CANCELADA"))
				.andExpect(jsonPath("$.datos.tipoEndoso").value("CANCELACION"))
				.andExpect(jsonPath("$.datos.valorMovimiento").value(-18000000.0))
				.andExpect(jsonPath("$.datos.canonMensualPoliza").value(0.0))
				.andExpect(jsonPath("$.datos.riesgos", hasSize(2)))
				.andExpect(jsonPath("$.datos.riesgos[*].estado", everyItem(is("CANCELADO"))));

		Map<String, Object> endoso = jdbc.queryForMap(
				"SELECT canal, usuario, motivo FROM poliza WHERE poliza_id = ? AND num_endoso = 1", id);
		assertThat(endoso).containsEntry("CANAL", "PRUEBAS").containsEntry("USUARIO", "tester")
				.containsEntry("MOTIVO", "Terminó el contrato de arrendamiento");

		enviar("/polizas/" + id + "/renovar", soloProceso("RENOVACION")).andExpect(status().isConflict());
		enviar("/polizas/" + id + "/riesgos", inclusion(riesgo("4000000003", 1_000_000), null))
				.andExpect(status().isConflict());
	}

	@Test
	void renuevaConElIpcDelAnioAnteriorYExigeQueEsteCargado() throws Exception {
		long id = numero(emitir(individual("5000000001", "2026-03-01"), riesgo("5000000001", 1_000_000))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.datos.polizaId");

		enviar("/polizas/" + id + "/renovar", soloProceso("RENOVACION"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.proceso.tipoProceso").value("RENOVACION"))
				.andExpect(jsonPath("$.datos.estadoPoliza").value("RENOVADA"))
				.andExpect(jsonPath("$.datos.inicioVigencia").value("2027-03-01"))
				.andExpect(jsonPath("$.datos.finVigencia").value("2028-02-29"))
				.andExpect(jsonPath("$.datos.canonMensualPoliza").value(1048000.0))
				.andExpect(jsonPath("$.datos.valorMovimiento").value(12576000.0))
				.andExpect(jsonPath("$.datos.primaTotalPoliza").value(12576000.0))
				.andExpect(jsonPath("$.datos.tipoEndoso").value("RENOVACION"));

		enviar("/polizas/" + id + "/renovar", soloProceso("RENOVACION"))
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.errores[0].detalle", containsString("No hay IPC cargado para 2027")));
	}

	@Test
	void laRenovacionAutomaticaRenuevaLasVencidasYReintentaLasQueNoPudo() throws Exception {
		// Vence el 31 de diciembre de 2020: su renovación necesita el IPC 2020, que no está cargado.
		long sinIpcId = numero(emitir(individual("6000000001", "2020-01-01"), riesgo("6000000001", 1_000_000))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.datos.polizaId");

		// Hoy es 15 de julio: 1006 venció el 30 de junio; 1007 vence el 31 de agosto y todavía no se renueva.
		enviar("/renovaciones", soloProceso("RENOVACION_AUTOMATICA"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.proceso.tipoProceso").value("RENOVACION_AUTOMATICA"))
				.andExpect(jsonPath("$.datos.fechaCorte").value("2026-07-15"))
				.andExpect(jsonPath("$.datos.revisadas").value(2))
				.andExpect(jsonPath("$.datos.renovadas", hasSize(1)))
				.andExpect(jsonPath("$.datos.renovadas[0].polizaId").value(1006))
				.andExpect(jsonPath("$.datos.renovadas[0].estadoPoliza").value("RENOVADA"))
				.andExpect(jsonPath("$.datos.renovadas[0].inicioVigencia").value("2026-07-01"))
				.andExpect(jsonPath("$.datos.renovadas[0].finVigencia").value("2027-06-30"))
				.andExpect(jsonPath("$.datos.renovadas[0].canonMensualPoliza").value(2102000.0))
				.andExpect(jsonPath("$.datos.renovadas[0].valorMovimiento").value(25224000.0))
				.andExpect(jsonPath("$.datos.renovadas[0].tipoEndoso").value("RENOVACION"))
				.andExpect(jsonPath("$.datos.omitidas", hasSize(1)))
				.andExpect(jsonPath("$.datos.omitidas[0].polizaId").value(sinIpcId))
				.andExpect(jsonPath("$.datos.omitidas[0].motivo", containsString("No hay IPC cargado para 2020")));
		verify(core).enviar(anyLong(), eq(1006L), eq(1));

		// Repetir la corrida no renueva otra vez la 1006; la que no tenía IPC se vuelve a intentar.
		enviar("/renovaciones", soloProceso("RENOVACION_AUTOMATICA"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.datos.renovadas", hasSize(0)))
				.andExpect(jsonPath("$.datos.omitidas[*].polizaId", hasItem((int) sinIpcId)));
	}

	@Test
	void lasPersonasQuedanEnPersonaYSeReutilizan() throws Exception {
		long primera = numero(emitir(individual("7000000001", "2026-03-01"), riesgo("7000000001", 1_000_000))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.datos.polizaId");
		long segunda = numero(emitir(individual("7000000001", "2026-05-01"), riesgo("7000000001", 1_200_000))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.datos.polizaId");

		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM persona WHERE numero_documento = '7000000001'",
				Integer.class)).as("tomador y arrendatario son una sola persona").isEqualTo(1);
		Long tomador = jdbc.queryForObject("SELECT tomador_id FROM poliza WHERE poliza_id = ? AND num_endoso = 0",
				Long.class, primera);
		assertThat(jdbc.queryForObject("SELECT tomador_id FROM poliza WHERE poliza_id = ? AND num_endoso = 0",
				Long.class, segunda)).isEqualTo(tomador);
		assertThat(jdbc.queryForObject("SELECT arrendatario_id FROM riesgo WHERE poliza_id = ? AND num_endoso = 0",
				Long.class, primera)).isEqualTo(tomador);
	}

	@Test
	void cadaEndosoGuardaSusAvisosYElProcesadorLosEntrega() throws Exception {
		long id = numero(emitir(colectiva(), riesgo("8000000001", 1_000_000))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.datos.polizaId");
		enviar("/polizas/" + id + "/riesgos", inclusion(riesgo("8000000002", 1_000_000), null))
				.andExpect(status().isCreated());

		assertThat(avisos(id)).containsExactly(
				"0 CORE ACTUALIZACION SINCRONIZADO 1",
				"0 NOTIFICACION POLIZA_CREADA SINCRONIZADO 1",
				"1 CORE ACTUALIZACION SINCRONIZADO 1");
	}

	@Test
	void siElCoreNoRespondeElAvisoQuedaPendienteYSeReintenta() throws Exception {
		doThrow(new CoreNoDisponibleException("El CORE no respondio")).when(core).enviar(anyLong(), anyLong(), anyInt());
		long id = numero(emitir(colectiva(), riesgo("8100000001", 1_000_000))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.datos.polizaId");

		// La emisión quedó guardada; solo el aviso al CORE queda pendiente.
		assertThat(avisos(id)).containsExactly(
				"0 CORE ACTUALIZACION PENDIENTE 1",
				"0 NOTIFICACION POLIZA_CREADA SINCRONIZADO 1");
		assertThat(jdbc.queryForObject("SELECT ultimo_error FROM aviso WHERE poliza_id = ? AND destino = 'CORE'",
				String.class, id)).isEqualTo("El CORE no respondio");

		// El CORE vuelve: el reintento entrega el mismo aviso.
		reset(core);
		procesador.reintentarPendientes();
		assertThat(avisos(id)).containsExactly(
				"0 CORE ACTUALIZACION SINCRONIZADO 2",
				"0 NOTIFICACION POLIZA_CREADA SINCRONIZADO 1");
	}

	@Test
	void siElCoreRechazaElAvisoQuedaRechazadoYNoSeReintenta() throws Exception {
		doThrow(new CoreRechazoException("El CORE rechazo el aviso con HTTP 422")).when(core)
				.enviar(anyLong(), anyLong(), anyInt());
		long id = numero(emitir(colectiva(), riesgo("8200000001", 1_000_000))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.datos.polizaId");

		procesador.reintentarPendientes();

		assertThat(avisos(id)).contains("0 CORE ACTUALIZACION RECHAZADO 1");
		verify(core, times(1)).enviar(anyLong(), eq(id), eq(0));
	}

	@Test
	void elMockDelCoreRegistraElEventoEnElLog(CapturedOutput salida) throws Exception {
		enviar("/core-mock/evento", "{\"evento\": \"ACTUALIZACION\", \"polizaId\": 555}")
				.andExpect(status().isAccepted());

		assertThat(salida.getOut())
				.contains("[CORE-MOCK] Operacion enviada al CORE: evento=ACTUALIZACION, polizaId=555");
	}

	// ------------------------------------------------------------------ peticiones

	private ResultActions conApiKey(MockHttpServletRequestBuilder peticion) throws Exception {
		return mvc.perform(peticion.header("api-key", API_KEY));
	}

	private ResultActions enviar(String ruta, String json) throws Exception {
		return conApiKey(post(ruta).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private ResultActions emitir(String poliza, String... riesgos) throws Exception {
		return enviar("/polizas", """
				{%s, "poliza": %s, "riesgos": [%s]}
				""".formatted(proceso("EMISION", null), poliza, String.join(",", riesgos)));
	}

	/** Avisos de una póliza como "endoso destino evento estado intentos", en orden de endoso. */
	private List<String> avisos(long polizaId) {
		return jdbc.queryForList("SELECT num_endoso || ' ' || destino || ' ' || evento || ' ' || estado || ' ' || intentos "
				+ "FROM aviso WHERE poliza_id = ? ORDER BY num_endoso, id", String.class, polizaId);
	}

	private static long numero(String json, String ruta) {
		return ((Number) JsonPath.read(json, ruta)).longValue();
	}

	private static String proceso(String tipo, String fechaMovimiento) {
		String fecha = fechaMovimiento == null ? "" : ", \"fechaMovimiento\": \"" + fechaMovimiento + "\"";
		return """
				"proceso": {"tipoProceso": "%s", "canal": "PRUEBAS", "usuario": "tester"%s}
				""".formatted(tipo, fecha).strip();
	}

	private static String soloProceso(String tipo) {
		return "{" + proceso(tipo, null) + "}";
	}

	private static String inclusion(String riesgo, String fechaMovimiento) {
		return "{" + proceso("INCLUSION_RIESGO", fechaMovimiento) + ", \"riesgo\": " + riesgo + "}";
	}

	private static String cancelacion(String tipo) {
		return "{" + proceso(tipo, null) + ", \"motivo\": \"Terminó el contrato de arrendamiento\"}";
	}

	private static String colectiva() {
		return """
				{"tipo": "COLECTIVA",
				 "tomador": {"tipoDocumento": "NIT", "numeroDocumento": "900111222",
				             "nombre": "Inmobiliaria de Prueba S.A.S.", "correo": "prueba@inmobiliaria.com"},
				 "inicioVigencia": "2026-01-01", "mesesVigencia": 12}
				""";
	}

	private static String individual(String documento, String inicioVigencia) {
		return """
				{"tipo": "INDIVIDUAL",
				 "tomador": {"tipoDocumento": "CC", "numeroDocumento": "%s", "nombre": "Ana Arrendataria"},
				 "inicioVigencia": "%s", "mesesVigencia": 12}
				""".formatted(documento, inicioVigencia);
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
