package com.pruebatecnica.polizas.config;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Abre la transacción de cada petición: le asigna un id y guarda la hora de inicio. El id viaja en la respuesta, en el
 * encabezado id-transaccion y en cada línea del log, para seguir la operación de punta a punta.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TransaccionFilter extends OncePerRequestFilter {

	public static final String ID = "idTransaccion";
	public static final String INICIO = "fechaInicioTransaccion";
	public static final String ENCABEZADO = "id-transaccion";

	private final Clock reloj;

	public TransaccionFilter(Clock reloj) {
		this.reloj = reloj;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String id = UUID.randomUUID().toString();
		request.setAttribute(ID, id);
		request.setAttribute(INICIO, LocalDateTime.now(reloj).truncatedTo(ChronoUnit.MILLIS));
		response.setHeader(ENCABEZADO, id);
		MDC.put(ID, id);
		try {
			chain.doFilter(request, response);
		} finally {
			MDC.remove(ID);
		}
	}
}
