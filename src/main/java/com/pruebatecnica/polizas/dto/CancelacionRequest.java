package com.pruebatecnica.polizas.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Cancelar un riesgo (EXCLUSION_RIESGO) o una póliza (CANCELACION): el proceso y el motivo. */
public record CancelacionRequest(
		@NotNull(message = "es obligatorio") @Valid ProcesoRequest proceso,
		@NotBlank(message = "es obligatorio") @Size(max = 200, message = "admite máximo 200 caracteres") String motivo) {
}
