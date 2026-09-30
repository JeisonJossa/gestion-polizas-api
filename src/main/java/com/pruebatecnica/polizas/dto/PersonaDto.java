package com.pruebatecnica.polizas.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pruebatecnica.polizas.domain.Persona;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PersonaDto(
		@NotBlank(message = "es obligatorio") @Size(max = 5, message = "admite máximo 5 caracteres") String tipoDocumento,
		@NotBlank(message = "es obligatorio") @Size(max = 20, message = "admite máximo 20 caracteres") String numeroDocumento,
		@NotBlank(message = "es obligatorio") @Size(max = 120, message = "admite máximo 120 caracteres") String nombre,
		@Email(message = "no es un correo válido") @Size(max = 120, message = "admite máximo 120 caracteres") String correo,
		@Size(max = 20, message = "admite máximo 20 caracteres") String celular) {

	public Persona aDominio() {
		return new Persona(tipoDocumento.trim().toUpperCase(), numeroDocumento.trim(), nombre.trim(), correo, celular);
	}

	public static PersonaDto de(Persona persona) {
		return new PersonaDto(persona.tipoDocumento(), persona.numeroDocumento(), persona.nombre(), persona.correo(),
				persona.celular());
	}
}
