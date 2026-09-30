package com.pruebatecnica.polizas.exception;

/** La póliza o el riesgo no está en un estado que permita la operación (responde 409). */
public class EstadoInvalidoException extends RuntimeException {

	public EstadoInvalidoException(String mensaje) {
		super(mensaje);
	}
}
