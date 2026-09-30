package com.pruebatecnica.polizas.domain;

import java.math.BigDecimal;

/** Efecto de un endoso sobre la prima. Sale del signo de la prima del endoso, nunca se escoge a mano. */
public enum ClaseMovimiento {
	COBRO,
	DEVOLUCION,
	SIN_MOVIMIENTO;

	public static ClaseMovimiento de(BigDecimal primaEndoso) {
		return switch (primaEndoso.signum()) {
			case 1 -> COBRO;
			case -1 -> DEVOLUCION;
			default -> SIN_MOVIMIENTO;
		};
	}
}
