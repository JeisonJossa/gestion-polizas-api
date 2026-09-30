package com.pruebatecnica.polizas.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Exige el encabezado api-key (el que pide el enunciado) en todas las rutas del API. También acepta x-api-key, la
 * forma habitual de nombrar este encabezado. La consola de H2 queda por fuera.
 */
@Component
public class ApiKeyFilter extends OncePerRequestFilter {

	public static final String HEADER = "api-key";
	public static final String HEADER_ALTERNO = "x-api-key";

	private static final String RESPUESTA_401 =
			"{\"codigo\":\"NO_AUTORIZADO\",\"mensaje\":\"Falta el encabezado api-key o no es válido.\"}";

	private final byte[] apiKey;

	public ApiKeyFilter(@Value("${polizas.api-key}") String apiKey) {
		this.apiKey = apiKey.getBytes(StandardCharsets.UTF_8);
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
		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.getWriter().write(RESPUESTA_401);
	}
}
