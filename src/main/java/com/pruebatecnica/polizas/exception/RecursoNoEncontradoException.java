package com.pruebatecnica.polizas.exception;

/** La póliza o el riesgo no existe (responde 404). */
public class RecursoNoEncontradoException extends RuntimeException {

	public RecursoNoEncontradoException(String mensaje) {
		super(mensaje);
	}
}
