package com.pruebatecnica.polizas;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import com.jayway.jsonpath.JsonPath;

/**
 * Con el servidor levantado de verdad: crear una póliza avisa al CORE por HTTP después del commit, y el mock deja el
 * evento en el log.
 */
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT,
		properties = { "server.port=18080", "spring.datasource.url=jdbc:h2:mem:e2e;DB_CLOSE_DELAY=-1" })
@ExtendWith(OutputCaptureExtension.class)
class AvisoCoreExtremoAExtremoTest {

	@Test
	void crearUnaPolizaAvisaAlCoreYElMockLoRegistra(CapturedOutput salida) {
		String individual = """
				{"tipo": "INDIVIDUAL",
				 "tomador": {"tipoDocumento": "CC", "numeroDocumento": "6000000001", "nombre": "Ana Arrendataria"},
				 "inicioVigencia": "2026-03-01", "mesesVigencia": 12,
				 "riesgos": [{"inmueble": {"direccion": "Calle 100 # 10-20", "ciudad": "Bogotá"},
				              "arrendatario": {"tipoDocumento": "CC", "numeroDocumento": "6000000001", "nombre": "Ana Arrendataria"},
				              "arrendador": {"tipoDocumento": "CC", "numeroDocumento": "79000000", "nombre": "Pedro Arrendador"},
				              "canon": 1000000}]}
				""";

		ResponseEntity<String> respuesta = RestClient.create("http://localhost:18080").post()
				.uri("/polizas")
				.header("api-key", "123456")
				.contentType(MediaType.APPLICATION_JSON)
				.body(individual)
				.retrieve()
				.toEntity(String.class);

		assertThat(respuesta.getStatusCode().value()).isEqualTo(201);
		long id = ((Number) JsonPath.read(respuesta.getBody(), "$.id")).longValue();
		assertThat(salida.getOut())
				.contains("[CORE-MOCK] Operacion enviada al CORE: evento=ACTUALIZACION, polizaId=" + id)
				.contains("Aviso enviado al CORE: poliza " + id + ", endoso 0 (EMISION)");
	}
}
