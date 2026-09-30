package com.pruebatecnica.polizas.controller;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.pruebatecnica.polizas.config.TransaccionFilter;
import com.pruebatecnica.polizas.dto.ErrorDetalle;
import com.pruebatecnica.polizas.dto.ProcesoResponse;
import com.pruebatecnica.polizas.dto.RespuestaApi;
import com.pruebatecnica.polizas.dto.TipoProceso;

import jakarta.servlet.http.HttpServletRequest;

/** Arma todas las respuestas del API con la misma forma, con el id y la hora de inicio de la transacción. */
@Component
public class Respuestas {

	private final Clock reloj;

	public Respuestas(Clock reloj) {
		this.reloj = reloj;
	}

	public <T> RespuestaApi<T> exito(TipoProceso tipoProceso, String mensaje, T datos) {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
				.getRequest();
		return armar(request, tipoProceso, RespuestaApi.EXITO, mensaje, List.of(), datos);
	}

	/** Respuesta de error: el proceso se deduce de la ruta que se llamó. */
	public RespuestaApi<Void> error(HttpServletRequest request, String mensaje, List<ErrorDetalle> errores) {
		TipoProceso tipoProceso = TipoProceso.deLaRuta(request.getMethod(), request.getRequestURI());
		return armar(request, tipoProceso, RespuestaApi.ERROR, mensaje, errores, null);
	}

	private <T> RespuestaApi<T> armar(HttpServletRequest request, TipoProceso tipoProceso, int resultado,
			String mensaje, List<ErrorDetalle> errores, T datos) {
		LocalDateTime fin = LocalDateTime.now(reloj).truncatedTo(ChronoUnit.MILLIS);
		String id = request.getAttribute(TransaccionFilter.ID) instanceof String valor ? valor
				: UUID.randomUUID().toString();
		LocalDateTime inicio = request.getAttribute(TransaccionFilter.INICIO) instanceof LocalDateTime valor ? valor
				: fin;
		return new RespuestaApi<>(id, new ProcesoResponse(tipoProceso, inicio, fin), resultado, mensaje, errores,
				datos);
	}
}
