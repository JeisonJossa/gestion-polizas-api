package com.pruebatecnica.polizas.exception;

/** La operación no cumple una regla de negocio (responde 422). */
public class ReglaNegocioException extends RuntimeException {

	public ReglaNegocioException(String mensaje) {
		super(mensaje);
	}
}
