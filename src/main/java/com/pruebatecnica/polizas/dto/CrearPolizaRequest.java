package com.pruebatecnica.polizas.dto;

import java.time.LocalDate;
import java.util.List;

import com.pruebatecnica.polizas.domain.DatosPoliza;
import com.pruebatecnica.polizas.domain.TipoPoliza;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CrearPolizaRequest(
		@NotNull(message = "es obligatorio (INDIVIDUAL o COLECTIVA)") TipoPoliza tipo,
		@NotNull(message = "es obligatorio") @Valid PersonaDto tomador,
		@NotNull(message = "es obligatoria") LocalDate inicioVigencia,
		@NotNull(message = "es obligatorio") @Min(value = 1, message = "debe ser al menos 1")
		@Max(value = 36, message = "debe ser máximo 36") Integer mesesVigencia,
		@NotEmpty(message = "debe traer al menos un riesgo") List<@NotNull @Valid RiesgoRequest> riesgos) {

	public DatosPoliza aDominio() {
		return new DatosPoliza(tipo, tomador.aDominio(), inicioVigencia, mesesVigencia);
	}
}
