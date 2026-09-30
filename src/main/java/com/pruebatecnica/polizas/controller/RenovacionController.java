package com.pruebatecnica.polizas.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pruebatecnica.polizas.dto.RenovacionAutomaticaResponse;
import com.pruebatecnica.polizas.service.RenovacionAutomaticaService;

@RestController
@RequestMapping("/renovaciones")
public class RenovacionController {

	private final RenovacionAutomaticaService servicio;

	public RenovacionController(RenovacionAutomaticaService servicio) {
		this.servicio = servicio;
	}

	/** Renovación automática del día: la llama el programador de tareas una vez al día. */
	@PostMapping
	public RenovacionAutomaticaResponse renovarLasQueVencieron() {
		return servicio.renovarLasQueVencieron();
	}
}
