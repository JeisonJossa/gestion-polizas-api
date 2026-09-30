package com.pruebatecnica.polizas.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.function.Function;

/** Operaciones de dinero: siempre BigDecimal con 2 decimales y redondeo mitad hacia arriba. */
public final class Dinero {

	private Dinero() {
	}

	public static BigDecimal redondear(BigDecimal valor) {
		return valor.setScale(2, RoundingMode.HALF_UP);
	}

	public static BigDecimal porMeses(BigDecimal canon, int meses) {
		return redondear(canon.multiply(BigDecimal.valueOf(meses)));
	}

	public static <T> BigDecimal sumar(Collection<T> elementos, Function<T, BigDecimal> valor) {
		return redondear(elementos.stream().map(valor).reduce(BigDecimal.ZERO, BigDecimal::add));
	}
}
