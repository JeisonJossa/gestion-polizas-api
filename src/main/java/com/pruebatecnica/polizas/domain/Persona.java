package com.pruebatecnica.polizas.domain;

import jakarta.persistence.Embeddable;

/**
 * Tomador, arrendatario o arrendador. Va embebida en póliza y riesgo;
 * el Módulo 1 la modela como la tabla PERSONA.
 */
@Embeddable
public record Persona(String tipoDocumento, String numeroDocumento, String nombre, String correo, String celular) {

	public boolean esLaMismaQue(Persona otra) {
		return otra != null
				&& tipoDocumento.equalsIgnoreCase(otra.tipoDocumento())
				&& numeroDocumento.trim().equals(otra.numeroDocumento().trim());
	}
}
