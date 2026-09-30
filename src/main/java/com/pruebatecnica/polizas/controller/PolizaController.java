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
import com.pruebatecnica.polizas.dto.CancelacionRequest;
import com.pruebatecnica.polizas.dto.EmisionRequest;
import com.pruebatecnica.polizas.dto.EndosoResultado;
import com.pruebatecnica.polizas.dto.PolizaConsulta;
import com.pruebatecnica.polizas.dto.RenovacionRequest;
import com.pruebatecnica.polizas.dto.RespuestaApi;
import com.pruebatecnica.polizas.dto.TipoProceso;
import com.pruebatecnica.polizas.service.PolizaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/polizas")
public class PolizaController {

	private final PolizaService servicio;
	private final Respuestas respuestas;

	public PolizaController(PolizaService servicio, Respuestas respuestas) {
		this.servicio = servicio;
		this.respuestas = respuestas;
	}

	/** CONSULTA_POLIZAS: las pólizas según su último endoso; tipo y estado son opcionales. */
	@GetMapping
	public RespuestaApi<List<PolizaConsulta>> listar(@RequestParam(required = false) TipoPoliza tipo,
			@RequestParam(required = false) EstadoPoliza estado) {
		List<PolizaConsulta> polizas = servicio.listar(tipo, estado);
		return respuestas.exito(TipoProceso.CONSULTA_POLIZAS, polizas.size() + " pólizas encontradas.", polizas);
	}

	/** EMISION: emite una póliza individual o colectiva (endoso 0). */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public RespuestaApi<EndosoResultado> emitir(@Valid @RequestBody EmisionRequest solicitud) {
		solicitud.proceso().exigirTipo(TipoProceso.EMISION);
		EndosoResultado emision = servicio.emitir(solicitud);
		return respuestas.exito(TipoProceso.EMISION, "Póliza " + emision.polizaId() + " emitida.", emision);
	}

	/** RENOVACION: renueva por el mismo periodo con el IPC del año anterior; la póliza pasa a RENOVADA. */
	@PostMapping("/{id}/renovar")
	public RespuestaApi<EndosoResultado> renovar(@PathVariable long id, @Valid @RequestBody RenovacionRequest solicitud) {
		solicitud.proceso().exigirTipo(TipoProceso.RENOVACION);
		EndosoResultado renovacion = servicio.renovar(id, solicitud.proceso(), null);
		return respuestas.exito(TipoProceso.RENOVACION,
				"Póliza " + id + " renovada hasta el " + renovacion.finVigencia() + ".", renovacion);
	}

	/** CANCELACION: cancela la póliza y todos sus riesgos. */
	@PostMapping("/{id}/cancelar")
	public RespuestaApi<EndosoResultado> cancelar(@PathVariable long id,
			@Valid @RequestBody CancelacionRequest solicitud) {
		solicitud.proceso().exigirTipo(TipoProceso.CANCELACION);
		EndosoResultado cancelacion = servicio.cancelar(id, solicitud);
		return respuestas.exito(TipoProceso.CANCELACION, "Póliza " + id + " cancelada con sus riesgos.", cancelacion);
	}
}
