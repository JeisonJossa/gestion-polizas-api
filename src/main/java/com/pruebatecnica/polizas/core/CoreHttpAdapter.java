package com.pruebatecnica.polizas.core;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.pruebatecnica.polizas.config.ApiKeyFilter;
import com.pruebatecnica.polizas.domain.TipoEndoso;
import com.pruebatecnica.polizas.dto.EventoCoreRequest;

/**
 * Adaptador HTTP hacia el servicio agnóstico de edición. Hoy apunta al mock {@code /core-mock/evento}; en
 * producción solo cambia la URL (capa media WebLogic).
 */
@Component
public class CoreHttpAdapter implements CoreNotifier {

	private static final Logger log = LoggerFactory.getLogger(CoreHttpAdapter.class);

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
	public void notificarActualizacion(long polizaId, int numEndoso, TipoEndoso tipoEndoso) {
		try {
			cliente.post()
					.uri(url)
					.header(ApiKeyFilter.HEADER, apiKey)
					.contentType(MediaType.APPLICATION_JSON)
					.body(EventoCoreRequest.actualizacion(polizaId))
					.retrieve()
					.toBodilessEntity();
			log.info("Aviso enviado al CORE: poliza {}, endoso {} ({})", polizaId, numEndoso, tipoEndoso);
		}
		catch (RestClientException e) {
			// La operación del usuario ya quedó guardada: una falla del CORE no la revierte.
			// En producción el aviso queda pendiente en la tabla AVISO y se reintenta (outbox del Módulo 1).
			log.warn("No se pudo avisar al CORE la poliza {}, endoso {} ({}): {}", polizaId, numEndoso, tipoEndoso,
					e.getMessage());
		}
	}
}
