package com.pruebatecnica.polizas.dto;

import java.time.LocalDate;

import com.pruebatecnica.polizas.exception.ProcesoInvalidoException;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Bloque "proceso" que llega en toda petición que cambia una póliza: qué proceso se ejecuta, quién lo origina y la
 * fecha del movimiento (si no viene, es hoy).
 */
public record ProcesoRequest(
		@NotNull(message = "es obligatorio") TipoProceso tipoProceso,
		@NotBlank(message = "es obligatorio") @Size(max = 30, message = "admite máximo 30 caracteres") String canal,
		@Size(max = 60, message = "admite máximo 60 caracteres") String usuario,
		LocalDate fechaMovimiento) {

	/** El tipo de proceso tiene que ser el de la ruta que se llamó. */
	public void exigirTipo(TipoProceso esperado) {
		if (tipoProceso != esperado) {
			throw new ProcesoInvalidoException("El tipoProceso " + tipoProceso + " no corresponde a esta operación; "
					+ "se esperaba " + esperado + ".");
		}
	}

	public LocalDate fechaMovimientoO(LocalDate hoy) {
		return fechaMovimiento != null ? fechaMovimiento : hoy;
	}
}
