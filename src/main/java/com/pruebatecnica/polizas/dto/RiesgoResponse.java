package com.pruebatecnica.polizas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pruebatecnica.polizas.domain.EstadoRiesgo;
import com.pruebatecnica.polizas.domain.Riesgo;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RiesgoResponse(Long id, Integer codigo, EstadoRiesgo estado, InmuebleDto inmueble,
		PersonaDto arrendatario, PersonaDto arrendador, BigDecimal canon, BigDecimal prima, LocalDate fechaInclusion,
		LocalDate fechaExclusion, Integer ultimoEndoso) {

	public static RiesgoResponse de(Riesgo riesgo) {
		return new RiesgoResponse(riesgo.getRiesgoId(), riesgo.getCodRiesgo(), riesgo.getEstado(),
				InmuebleDto.de(riesgo.getInmueble()), PersonaDto.de(riesgo.getArrendatario()),
				PersonaDto.de(riesgo.getArrendador()), riesgo.getCanon(), riesgo.getPrima(),
				riesgo.getFechaInclusion(), riesgo.getFechaExclusion(), riesgo.getNumEndoso());
	}
}
