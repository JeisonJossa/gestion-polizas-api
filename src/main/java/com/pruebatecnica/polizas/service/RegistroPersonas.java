package com.pruebatecnica.polizas.service;

import org.springframework.stereotype.Component;

import com.pruebatecnica.polizas.domain.DatosRiesgo;
import com.pruebatecnica.polizas.domain.Persona;
import com.pruebatecnica.polizas.dto.PersonaDto;
import com.pruebatecnica.polizas.dto.RiesgoRequest;
import com.pruebatecnica.polizas.repository.PersonaRepository;

/**
 * Registra en PERSONA a quienes llegan en una petición. Si la persona ya existe (mismo tipo y número de documento) se
 * reutiliza y se actualizan su nombre y sus datos de contacto; si no, se crea.
 */
@Component
class RegistroPersonas {

	private final PersonaRepository personas;

	RegistroPersonas(PersonaRepository personas) {
		this.personas = personas;
	}

	Persona registrar(PersonaDto datos) {
		Persona recibida = datos.aDominio();
		return personas.findByTipoDocumentoAndNumeroDocumento(recibida.getTipoDocumento(), recibida.getNumeroDocumento())
				.map(existente -> {
					existente.actualizarDatos(recibida);
					return existente;
				})
				.orElseGet(() -> personas.save(recibida));
	}

	/** Datos del riesgo con su arrendatario y su arrendador ya registrados. */
	DatosRiesgo datosRiesgo(RiesgoRequest riesgo) {
		return riesgo.aDominio(registrar(riesgo.arrendatario()), registrar(riesgo.arrendador()));
	}
}
