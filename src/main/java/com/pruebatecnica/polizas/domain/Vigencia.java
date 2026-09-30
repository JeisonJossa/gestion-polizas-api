package com.pruebatecnica.polizas.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Cálculos de fechas de la vigencia. */
public final class Vigencia {

	private Vigencia() {
	}

	/** Último día de una vigencia que empieza en {@code inicio} y dura {@code meses}. */
	public static LocalDate fin(LocalDate inicio, int meses) {
		return inicio.plusMonths(meses).minusDays(1);
	}

	/**
	 * Meses de la vigencia que faltan, contando completo el mes de la vigencia en que ocurre el movimiento.
	 * Ejemplo: vigencia de enero a diciembre y movimiento en julio = 6 meses (julio a diciembre).
	 */
	public static int mesesRestantes(LocalDate inicio, int meses, LocalDate fecha) {
		if (fecha.isBefore(inicio)) {
			return meses;
		}
		long transcurridos = ChronoUnit.MONTHS.between(inicio, fecha);
		return (int) Math.max(0, meses - transcurridos);
	}
}
