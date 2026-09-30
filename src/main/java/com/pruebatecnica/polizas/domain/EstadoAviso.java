package com.pruebatecnica.polizas.domain;

/** PENDIENTE → SINCRONIZADO cuando el destino lo recibe, o RECHAZADO si el CORE lo rechaza (no se reintenta). */
public enum EstadoAviso {
	PENDIENTE, SINCRONIZADO, RECHAZADO
}
