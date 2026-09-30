package com.pruebatecnica.polizas.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/** POST /polizas: proceso EMISION, la póliza y sus riesgos. */
public record EmisionRequest(
		@NotNull(message = "es obligatorio") @Valid ProcesoRequest proceso,
		@NotNull(message = "es obligatoria") @Valid DatosPolizaRequest poliza,
		@NotEmpty(message = "debe traer al menos un riesgo") List<@NotNull @Valid RiesgoRequest> riesgos) {
}
