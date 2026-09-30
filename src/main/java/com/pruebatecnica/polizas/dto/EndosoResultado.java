package com.pruebatecnica.polizas.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import com.pruebatecnica.polizas.domain.ClaseMovimiento;
import com.pruebatecnica.polizas.domain.Endoso;
import com.pruebatecnica.polizas.domain.EstadoPoliza;
import com.pruebatecnica.polizas.domain.EstadoRiesgo;
import com.pruebatecnica.polizas.domain.Poliza;
import com.pruebatecnica.polizas.domain.Riesgo;
import com.pruebatecnica.polizas.domain.TipoEndoso;
import com.pruebatecnica.polizas.domain.TipoPoliza;

/**
 * Datos de toda operación que crea un endoso (emisión, inclusión, exclusión, renovación y cancelación). Separa lo que
 * mueve el endoso (valorMovimiento: positivo se cobra, negativo se devuelve) de los totales de la póliza.
 */
public record EndosoResultado(Long polizaId, String numeroPoliza, TipoPoliza tipoPoliza, EstadoPoliza estadoPoliza,
		Integer numeroEndoso, TipoEndoso tipoEndoso, LocalDate fechaEndoso, ClaseMovimiento claseMovimiento,
		BigDecimal valorMovimiento, List<RiesgoMovimiento> riesgos, LocalDate inicioVigencia, LocalDate finVigencia,
		BigDecimal canonMensualPoliza, BigDecimal primaTotalPoliza) {

	/** Riesgo escrito en el endoso: su canon, los meses que se cobran o devuelven y el valor que movió. */
	public record RiesgoMovimiento(Long id, Integer codigo, EstadoRiesgo estado, BigDecimal canon, Integer meses,
			BigDecimal valorMovimiento) {

		static RiesgoMovimiento de(Riesgo riesgo) {
			BigDecimal valor = riesgo.getPrimaEndoso();
			int meses = riesgo.getCanon().signum() == 0 ? 0
					: valor.abs().divide(riesgo.getCanon(), 0, RoundingMode.HALF_UP).intValue();
			return new RiesgoMovimiento(riesgo.getRiesgoId(), riesgo.getCodRiesgo(), riesgo.getEstado(),
					riesgo.getCanon(), meses, valor);
		}
	}

	public static EndosoResultado de(Endoso endoso) {
		Poliza poliza = endoso.poliza();
		return new EndosoResultado(poliza.getPolizaId(), poliza.getNumeroPoliza(), poliza.getTipo(), poliza.getEstado(),
				poliza.getNumEndoso(), poliza.getTipoEndoso(), poliza.getFechaEndoso(), poliza.getClaseMovimiento(),
				poliza.getPrimaEndoso(), endoso.riesgosEscritos().stream().map(RiesgoMovimiento::de).toList(),
				poliza.getInicioVigencia(), poliza.getFinVigencia(), poliza.getCanon(), poliza.getPrima());
	}
}
