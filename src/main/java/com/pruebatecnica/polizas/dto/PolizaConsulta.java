package com.pruebatecnica.polizas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.pruebatecnica.polizas.domain.ClaseMovimiento;
import com.pruebatecnica.polizas.domain.EstadoPoliza;
import com.pruebatecnica.polizas.domain.Poliza;
import com.pruebatecnica.polizas.domain.TipoEndoso;
import com.pruebatecnica.polizas.domain.TipoPoliza;

/** La póliza según su último endoso, para las consultas. */
public record PolizaConsulta(Long polizaId, String numeroPoliza, TipoPoliza tipo, EstadoPoliza estado,
		LocalDate inicioVigencia, LocalDate finVigencia, Integer mesesVigencia, PersonaDto tomador,
		BigDecimal canonMensual, BigDecimal primaTotal, UltimoEndoso ultimoEndoso) {

	/** Qué cambió en el último endoso y cuánto movió. */
	public record UltimoEndoso(Integer numero, TipoEndoso tipo, LocalDate fecha, ClaseMovimiento claseMovimiento,
			BigDecimal valorMovimiento) {
	}

	public static PolizaConsulta de(Poliza poliza) {
		return new PolizaConsulta(poliza.getPolizaId(), poliza.getNumeroPoliza(), poliza.getTipo(), poliza.getEstado(),
				poliza.getInicioVigencia(), poliza.getFinVigencia(), poliza.getMesesVigencia(),
				PersonaDto.de(poliza.getTomador()), poliza.getCanon(), poliza.getPrima(),
				new UltimoEndoso(poliza.getNumEndoso(), poliza.getTipoEndoso(), poliza.getFechaEndoso(),
						poliza.getClaseMovimiento(), poliza.getPrimaEndoso()));
	}
}
