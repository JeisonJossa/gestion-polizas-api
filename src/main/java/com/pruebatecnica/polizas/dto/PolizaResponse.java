package com.pruebatecnica.polizas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pruebatecnica.polizas.domain.EstadoPoliza;
import com.pruebatecnica.polizas.domain.Poliza;
import com.pruebatecnica.polizas.domain.Riesgo;
import com.pruebatecnica.polizas.domain.TipoPoliza;

/** La póliza según su último endoso. Los riesgos solo vienen cuando se pide la póliza completa. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PolizaResponse(Long id, String numeroPoliza, TipoPoliza tipo, EstadoPoliza estado,
		LocalDate inicioVigencia, LocalDate finVigencia, Integer mesesVigencia, PersonaDto tomador, BigDecimal canon,
		BigDecimal prima, EndosoResponse endoso, List<RiesgoResponse> riesgos) {

	public static PolizaResponse de(Poliza poliza) {
		return de(poliza, null);
	}

	public static PolizaResponse de(Poliza poliza, List<Riesgo> riesgos) {
		return new PolizaResponse(poliza.getPolizaId(), poliza.getNumeroPoliza(), poliza.getTipo(),
				poliza.getEstado(), poliza.getInicioVigencia(), poliza.getFinVigencia(), poliza.getMesesVigencia(),
				PersonaDto.de(poliza.getTomador()), poliza.getCanon(), poliza.getPrima(), EndosoResponse.de(poliza),
				riesgos == null ? null : riesgos.stream().map(RiesgoResponse::de).toList());
	}

	/** La misma póliza sin el detalle de sus riesgos, para los listados. */
	public PolizaResponse sinRiesgos() {
		return new PolizaResponse(id, numeroPoliza, tipo, estado, inicioVigencia, finVigencia, mesesVigencia, tomador,
				canon, prima, endoso, null);
	}
}
