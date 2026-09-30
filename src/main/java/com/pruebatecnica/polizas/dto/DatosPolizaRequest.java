package com.pruebatecnica.polizas.dto;

import java.time.LocalDate;

import com.pruebatecnica.polizas.domain.DatosPoliza;
import com.pruebatecnica.polizas.domain.Persona;
import com.pruebatecnica.polizas.domain.TipoPoliza;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Datos de la póliza que se emite: tipo, tomador y vigencia. */
public record DatosPolizaRequest(
		@NotNull(message = "es obligatorio (INDIVIDUAL o COLECTIVA)") TipoPoliza tipo,
		@NotNull(message = "es obligatorio") @Valid PersonaDto tomador,
		@NotNull(message = "es obligatoria") LocalDate inicioVigencia,
		@NotNull(message = "es obligatorio") @Min(value = 1, message = "debe ser al menos 1")
		@Max(value = 36, message = "debe ser máximo 36") Integer mesesVigencia) {

	/** Datos para emitir, con el tomador ya registrado en PERSONA. */
	public DatosPoliza aDominio(Persona tomadorRegistrado) {
		return new DatosPoliza(tipo, tomadorRegistrado, inicioVigencia, mesesVigencia);
	}
}
