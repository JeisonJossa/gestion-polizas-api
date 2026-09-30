package com.pruebatecnica.polizas.core;

/** El CORE no respondió o respondió con error técnico: el aviso queda PENDIENTE y se reintenta. */
public class CoreNoDisponibleException extends RuntimeException {

	public CoreNoDisponibleException(String mensaje) {
		super(mensaje);
	}
}
