package com.pruebatecnica.polizas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Contrato del servicio agnóstico de edición: {"evento": "ACTUALIZACION", "polizaId": 555}. */
public record EventoCoreRequest(
		@NotBlank(message = "es obligatorio") String evento,
		@NotNull(message = "es obligatorio") Long polizaId) {

	public static final String ACTUALIZACION = "ACTUALIZACION";

	public static EventoCoreRequest actualizacion(long polizaId) {
		return new EventoCoreRequest(ACTUALIZACION, polizaId);
	}
}
