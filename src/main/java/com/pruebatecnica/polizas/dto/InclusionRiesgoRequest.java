package com.pruebatecnica.polizas.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** POST /polizas/{id}/riesgos: proceso INCLUSION_RIESGO y los datos del riesgo nuevo. */
public record InclusionRiesgoRequest(
		@NotNull(message = "es obligatorio") @Valid ProcesoRequest proceso,
		@NotNull(message = "es obligatorio") @Valid RiesgoRequest riesgo) {
}
