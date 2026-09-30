package com.pruebatecnica.polizas.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pruebatecnica.polizas.dto.RenovacionAutomaticaResultado;
import com.pruebatecnica.polizas.dto.RenovacionRequest;
import com.pruebatecnica.polizas.dto.RespuestaApi;
import com.pruebatecnica.polizas.dto.TipoProceso;
import com.pruebatecnica.polizas.service.RenovacionAutomaticaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/renovaciones")
public class RenovacionController {

	private final RenovacionAutomaticaService servicio;
	private final Respuestas respuestas;

	public RenovacionController(RenovacionAutomaticaService servicio, Respuestas respuestas) {
		this.servicio = servicio;
		this.respuestas = respuestas;
	}

	/** RENOVACION_AUTOMATICA: la llama el programador de tareas una vez al día. */
	@PostMapping
	public RespuestaApi<RenovacionAutomaticaResultado> renovarLasQueVencieron(
			@Valid @RequestBody RenovacionRequest solicitud) {
		solicitud.proceso().exigirTipo(TipoProceso.RENOVACION_AUTOMATICA);
		RenovacionAutomaticaResultado resultado = servicio.renovarLasQueVencieron(solicitud.proceso());
		return respuestas.exito(TipoProceso.RENOVACION_AUTOMATICA, resultado.renovadas().size() + " pólizas renovadas y "
				+ resultado.omitidas().size() + " omitidas de " + resultado.revisadas() + " vencidas al "
				+ resultado.fechaCorte() + ".", resultado);
	}
}
