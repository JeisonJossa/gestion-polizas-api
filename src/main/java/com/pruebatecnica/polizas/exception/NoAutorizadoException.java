package com.pruebatecnica.polizas.exception;

/** Falta el encabezado api-key o no coincide (401). */
public class NoAutorizadoException extends RuntimeException {

	public NoAutorizadoException(String mensaje) {
		super(mensaje);
	}
}
