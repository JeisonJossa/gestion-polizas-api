package com.pruebatecnica.polizas.core;

/** El CORE recibió el aviso y lo rechazó (respuesta 4xx): el aviso queda RECHAZADO y no se reintenta. */
public class CoreRechazoException extends RuntimeException {

	public CoreRechazoException(String mensaje) {
		super(mensaje);
	}
}
