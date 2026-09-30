package com.pruebatecnica.polizas.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.pruebatecnica.polizas.dto.CancelacionRequest;
import com.pruebatecnica.polizas.dto.EndosoResultado;
import com.pruebatecnica.polizas.dto.InclusionRiesgoRequest;
import com.pruebatecnica.polizas.dto.RespuestaApi;
import com.pruebatecnica.polizas.dto.RiesgoConsulta;
import com.pruebatecnica.polizas.dto.TipoProceso;
import com.pruebatecnica.polizas.service.RiesgoService;

import jakarta.validation.Valid;

@RestController
public class RiesgoController {

	private final RiesgoService servicio;
	private final Respuestas respuestas;

	public RiesgoController(RiesgoService servicio, Respuestas respuestas) {
		this.servicio = servicio;
		this.respuestas = respuestas;
	}

	/** CONSULTA_RIESGOS: los riesgos de la póliza, activos y cancelados, en su estado actual. */
	@GetMapping("/polizas/{id}/riesgos")
	public RespuestaApi<List<RiesgoConsulta>> listar(@PathVariable long id) {
		List<RiesgoConsulta> riesgos = servicio.listar(id);
		return respuestas.exito(TipoProceso.CONSULTA_RIESGOS, riesgos.size() + " riesgos de la póliza " + id + ".",
				riesgos);
	}

	/** INCLUSION_RIESGO: agrega un riesgo; solo aplica a pólizas colectivas. */
	@PostMapping("/polizas/{id}/riesgos")
	@ResponseStatus(HttpStatus.CREATED)
	public RespuestaApi<EndosoResultado> agregar(@PathVariable long id,
			@Valid @RequestBody InclusionRiesgoRequest solicitud) {
		solicitud.proceso().exigirTipo(TipoProceso.INCLUSION_RIESGO);
		EndosoResultado inclusion = servicio.agregar(id, solicitud);
		return respuestas.exito(TipoProceso.INCLUSION_RIESGO, "Riesgo " + inclusion.riesgos().get(0).codigo()
				+ " incluido en la póliza " + id + ".", inclusion);
	}

	/** EXCLUSION_RIESGO: cancela un riesgo de una póliza colectiva. */
	@PostMapping("/riesgos/{id}/cancelar")
	public RespuestaApi<EndosoResultado> cancelar(@PathVariable long id,
			@Valid @RequestBody CancelacionRequest solicitud) {
		solicitud.proceso().exigirTipo(TipoProceso.EXCLUSION_RIESGO);
		EndosoResultado exclusion = servicio.cancelar(id, solicitud);
		return respuestas.exito(TipoProceso.EXCLUSION_RIESGO, "Riesgo " + id + " excluido de la póliza "
				+ exclusion.polizaId() + ".", exclusion);
	}
}
