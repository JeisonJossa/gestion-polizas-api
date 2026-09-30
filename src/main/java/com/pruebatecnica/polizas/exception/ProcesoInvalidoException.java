package com.pruebatecnica.polizas.exception;

/** El tipoProceso de la petición no corresponde a la operación que se llamó (422). */
public class ProcesoInvalidoException extends RuntimeException {

	public ProcesoInvalidoException(String mensaje) {
		super(mensaje);
	}
}
