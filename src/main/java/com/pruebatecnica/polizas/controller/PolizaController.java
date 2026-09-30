package com.pruebatecnica.polizas.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.pruebatecnica.polizas.domain.EstadoPoliza;
import com.pruebatecnica.polizas.domain.TipoPoliza;
import com.pruebatecnica.polizas.dto.CrearPolizaRequest;
import com.pruebatecnica.polizas.dto.PolizaResponse;
import com.pruebatecnica.polizas.service.PolizaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/polizas")
public class PolizaController {

	private final PolizaService servicio;

	public PolizaController(PolizaService servicio) {
		this.servicio = servicio;
	}

	/** Lista las pólizas según su último endoso; tipo y estado son opcionales. */
	@GetMapping
	public List<PolizaResponse> listar(@RequestParam(required = false) TipoPoliza tipo,
			@RequestParam(required = false) EstadoPoliza estado) {
		return servicio.listar(tipo, estado);
	}

	/** Emite una póliza individual o colectiva (endoso 0). */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public PolizaResponse crear(@Valid @RequestBody CrearPolizaRequest solicitud) {
		return servicio.crear(solicitud);
	}

	/** Renueva por el mismo periodo con el IPC del año anterior; la póliza pasa a RENOVADA. */
	@PostMapping("/{id}/renovar")
	public PolizaResponse renovar(@PathVariable long id) {
		return servicio.renovar(id);
	}

	/** Cancela la póliza y todos sus riesgos. */
	@PostMapping("/{id}/cancelar")
	public PolizaResponse cancelar(@PathVariable long id) {
		return servicio.cancelar(id);
	}
}
