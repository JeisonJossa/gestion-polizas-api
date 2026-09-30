package com.pruebatecnica.polizas.dto;

import com.pruebatecnica.polizas.domain.Inmueble;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InmuebleDto(
		@NotBlank(message = "es obligatoria") @Size(max = 150, message = "admite máximo 150 caracteres") String direccion,
		@NotBlank(message = "es obligatoria") @Size(max = 60, message = "admite máximo 60 caracteres") String ciudad) {

	public Inmueble aDominio() {
		return new Inmueble(direccion.trim(), ciudad.trim());
	}

	public static InmuebleDto de(Inmueble inmueble) {
		return new InmuebleDto(inmueble.direccion(), inmueble.ciudad());
	}
}
