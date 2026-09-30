package com.pruebatecnica.polizas.dto;

import java.math.BigDecimal;

import com.pruebatecnica.polizas.domain.DatosRiesgo;
import com.pruebatecnica.polizas.domain.Persona;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RiesgoRequest(
		@NotNull(message = "es obligatorio") @Valid InmuebleDto inmueble,
		@NotNull(message = "es obligatorio") @Valid PersonaDto arrendatario,
		@NotNull(message = "es obligatorio") @Valid PersonaDto arrendador,
		@NotNull(message = "es obligatorio") @Positive(message = "debe ser mayor que cero")
		@Digits(integer = 13, fraction = 2, message = "admite hasta 13 enteros y 2 decimales") BigDecimal canon) {

	/** Datos del riesgo, con el arrendatario y el arrendador ya registrados en PERSONA. */
	public DatosRiesgo aDominio(Persona arrendatarioRegistrado, Persona arrendadorRegistrado) {
		return new DatosRiesgo(inmueble.aDominio(), arrendatarioRegistrado, arrendadorRegistrado, canon);
	}
}
