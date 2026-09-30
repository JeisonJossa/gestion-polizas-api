package com.pruebatecnica.polizas.domain;

/** Qué cambio registra un endoso. El endoso 0 siempre es la emisión. */
public enum TipoEndoso {
	EMISION,
	INCLUSION,
	EXCLUSION,
	MODIFICACION,
	RENOVACION,
	CANCELACION
}
