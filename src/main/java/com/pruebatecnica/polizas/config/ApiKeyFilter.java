package com.pruebatecnica.polizas.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.pruebatecnica.polizas.exception.NoAutorizadoException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Exige el encabezado api-key (el que pide el enunciado) en todas las rutas del API. También acepta x-api-key, la
 * forma habitual de nombrar este encabezado. La consola de H2 queda por fuera. El 401 lo arma el manejador de errores,
 * con la misma forma de las demás respuestas.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class ApiKeyFilter extends OncePerRequestFilter {

	public static final String HEADER = "api-key";
	public static final String HEADER_ALTERNO = "x-api-key";

	private final byte[] apiKey;
	private final HandlerExceptionResolver errores;

	public ApiKeyFilter(@Value("${polizas.api-key}") String apiKey,
			@Qualifier("handlerExceptionResolver") HandlerExceptionResolver errores) {
		this.apiKey = apiKey.getBytes(StandardCharsets.UTF_8);
		this.errores = errores;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return request.getRequestURI().startsWith("/h2-console");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String recibida = request.getHeader(HEADER) != null ? request.getHeader(HEADER)
				: request.getHeader(HEADER_ALTERNO);
		if (recibida != null && MessageDigest.isEqual(apiKey, recibida.getBytes(StandardCharsets.UTF_8))) {
			chain.doFilter(request, response);
			return;
		}
		errores.resolveException(request, response, null,
				new NoAutorizadoException("Falta el encabezado api-key o no es válido."));
	}
}
