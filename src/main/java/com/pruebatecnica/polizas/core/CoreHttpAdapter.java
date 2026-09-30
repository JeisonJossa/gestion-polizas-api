package com.pruebatecnica.polizas.core;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.pruebatecnica.polizas.config.ApiKeyFilter;
import com.pruebatecnica.polizas.dto.EventoCoreRequest;

/**
 * Adaptador HTTP hacia el servicio agnóstico de edición. Hoy apunta al mock {@code /core-mock/evento}; en
 * producción solo cambia la URL (capa media WebLogic). El id del aviso viaja en el encabezado id-aviso para que un
 * reintento no se aplique dos veces.
 */
@Component
public class CoreHttpAdapter implements CoreNotifier {

	private static final Logger log = LoggerFactory.getLogger(CoreHttpAdapter.class);
	public static final String ENCABEZADO_AVISO = "id-aviso";

	private final RestClient cliente;
	private final String url;
	private final String apiKey;

	public CoreHttpAdapter(@Value("${polizas.core.url}") String url, @Value("${polizas.api-key}") String apiKey,
			@Value("${polizas.core.timeout}") Duration timeout) {
		SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
		fabrica.setConnectTimeout(timeout);
		fabrica.setReadTimeout(timeout);
		this.cliente = RestClient.builder().requestFactory(fabrica).build();
		this.url = url;
		this.apiKey = apiKey;
	}

	@Override
	public void enviar(long avisoId, long polizaId, int numEndoso) {
		try {
			cliente.post()
					.uri(url)
					.header(ApiKeyFilter.HEADER, apiKey)
					.header(ENCABEZADO_AVISO, String.valueOf(avisoId))
					.contentType(MediaType.APPLICATION_JSON)
					.body(EventoCoreRequest.actualizacion(polizaId))
					.retrieve()
					.onStatus(HttpStatusCode::is4xxClientError, (peticion, respuesta) -> {
						throw new CoreRechazoException("El CORE rechazo el aviso con HTTP " + respuesta.getStatusCode()
								.value());
					})
					.toBodilessEntity();
			log.info("Aviso {} enviado al CORE: poliza {}, endoso {}", avisoId, polizaId, numEndoso);
		}
		catch (CoreRechazoException e) {
			throw e;
		}
		catch (RestClientException e) {
			throw new CoreNoDisponibleException("El CORE no respondio: " + e.getMessage());
		}
	}
}
