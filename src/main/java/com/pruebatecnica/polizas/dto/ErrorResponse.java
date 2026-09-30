package com.pruebatecnica.polizas.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(String codigo, String mensaje, List<String> detalles) {

	public ErrorResponse(String codigo, String mensaje) {
		this(codigo, mensaje, List.of());
	}
}
