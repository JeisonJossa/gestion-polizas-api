package com.pruebatecnica.polizas.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Tabla PERSONA del Módulo 1: el tomador de una póliza y el arrendatario (asegurado) o el arrendador (beneficiario)
 * de un riesgo. Una persona se identifica por su tipo y número de documento; sus datos de contacto se usan en las
 * notificaciones.
 */
@Entity
@Table(name = "persona")
public class Persona {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String tipoDocumento;

	private String numeroDocumento;

	private String nombre;

	private String correo;

	private String celular;

	protected Persona() {
	}

	public Persona(String tipoDocumento, String numeroDocumento, String nombre, String correo, String celular) {
		this.tipoDocumento = tipoDocumento;
		this.numeroDocumento = numeroDocumento;
		this.nombre = nombre;
		this.correo = correo;
		this.celular = celular;
	}

	public boolean esLaMismaQue(Persona otra) {
		return otra != null
				&& tipoDocumento.equalsIgnoreCase(otra.tipoDocumento)
				&& numeroDocumento.trim().equals(otra.numeroDocumento.trim());
	}

	/** Actualiza el nombre y los datos de contacto que llegaron en una petición nueva. */
	public void actualizarDatos(Persona recibida) {
		this.nombre = recibida.nombre;
		if (recibida.correo != null) {
			this.correo = recibida.correo;
		}
		if (recibida.celular != null) {
			this.celular = recibida.celular;
		}
	}

	public Long getId() {
		return id;
	}

	public String getTipoDocumento() {
		return tipoDocumento;
	}

	public String getNumeroDocumento() {
		return numeroDocumento;
	}

	public String getNombre() {
		return nombre;
	}

	public String getCorreo() {
		return correo;
	}

	public String getCelular() {
		return celular;
	}
}
