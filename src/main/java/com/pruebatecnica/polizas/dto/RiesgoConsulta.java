package com.pruebatecnica.polizas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.pruebatecnica.polizas.domain.EstadoRiesgo;
import com.pruebatecnica.polizas.domain.Riesgo;

/** Un riesgo en su estado actual, para la consulta de riesgos de una póliza. */
public record RiesgoConsulta(Long id, Integer codigo, EstadoRiesgo estado, InmuebleDto inmueble,
		PersonaDto arrendatario, PersonaDto arrendador, BigDecimal canon, BigDecimal prima, LocalDate fechaInclusion,
		LocalDate fechaExclusion, Integer ultimoEndoso) {

	public static RiesgoConsulta de(Riesgo riesgo) {
		return new RiesgoConsulta(riesgo.getRiesgoId(), riesgo.getCodRiesgo(), riesgo.getEstado(),
				InmuebleDto.de(riesgo.getInmueble()), PersonaDto.de(riesgo.getArrendatario()),
				PersonaDto.de(riesgo.getArrendador()), riesgo.getCanon(), riesgo.getPrima(),
				riesgo.getFechaInclusion(), riesgo.getFechaExclusion(), riesgo.getNumEndoso());
	}
}
