package com.pruebatecnica.polizas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.pruebatecnica.polizas.domain.ClaseMovimiento;
import com.pruebatecnica.polizas.domain.Poliza;
import com.pruebatecnica.polizas.domain.TipoEndoso;

/** Último endoso de la póliza: qué cambió y cuánto movió la prima. */
public record EndosoResponse(Integer numero, TipoEndoso tipo, ClaseMovimiento claseMovimiento, LocalDate fecha,
		BigDecimal prima) {

	public static EndosoResponse de(Poliza poliza) {
		return new EndosoResponse(poliza.getNumEndoso(), poliza.getTipoEndoso(), poliza.getClaseMovimiento(),
				poliza.getFechaEndoso(), poliza.getPrimaEndoso());
	}
}
