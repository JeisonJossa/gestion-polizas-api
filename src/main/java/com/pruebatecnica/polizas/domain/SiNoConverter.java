package com.pruebatecnica.polizas.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Guarda un booleano como 'S' o 'N', igual que las marcas del CORE. */
@Converter
public class SiNoConverter implements AttributeConverter<Boolean, String> {

	@Override
	public String convertToDatabaseColumn(Boolean valor) {
		return Boolean.TRUE.equals(valor) ? "S" : "N";
	}

	@Override
	public Boolean convertToEntityAttribute(String valor) {
		return "S".equals(valor);
	}
}
