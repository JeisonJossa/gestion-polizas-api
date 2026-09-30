package com.pruebatecnica.polizas.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.pruebatecnica.polizas.dto.MovimientoRiesgoResponse;
import com.pruebatecnica.polizas.dto.RiesgoRequest;
import com.pruebatecnica.polizas.dto.RiesgoResponse;
import com.pruebatecnica.polizas.service.RiesgoService;

import jakarta.validation.Valid;

@RestController
public class RiesgoController {

	private final RiesgoService servicio;

	public RiesgoController(RiesgoService servicio) {
		this.servicio = servicio;
	}

	/** Riesgos de la póliza, activos y cancelados, en su estado actual. */
	@GetMapping("/polizas/{id}/riesgos")
	public List<RiesgoResponse> listar(@PathVariable long id) {
		return servicio.listar(id);
	}

	/** Agrega un riesgo; solo aplica a pólizas colectivas. */
	@PostMapping("/polizas/{id}/riesgos")
	@ResponseStatus(HttpStatus.CREATED)
	public MovimientoRiesgoResponse agregar(@PathVariable long id, @Valid @RequestBody RiesgoRequest solicitud) {
		return servicio.agregar(id, solicitud);
	}

	/** Cancela un riesgo de una póliza colectiva. */
	@PostMapping("/riesgos/{id}/cancelar")
	public MovimientoRiesgoResponse cancelar(@PathVariable long id) {
		return servicio.cancelar(id);
	}
}
