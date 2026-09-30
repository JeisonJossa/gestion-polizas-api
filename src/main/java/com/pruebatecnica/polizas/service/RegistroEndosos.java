package com.pruebatecnica.polizas.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.pruebatecnica.polizas.domain.Endoso;
import com.pruebatecnica.polizas.domain.Poliza;
import com.pruebatecnica.polizas.domain.PolizaVigente;
import com.pruebatecnica.polizas.exception.RecursoNoEncontradoException;
import com.pruebatecnica.polizas.repository.PolizaRepository;
import com.pruebatecnica.polizas.repository.RiesgoRepository;

/** Carga la póliza vigente y guarda cada endoso nuevo. Lo usan los servicios de pólizas y de riesgos. */
@Component
class RegistroEndosos {

	private final PolizaRepository polizas;
	private final RiesgoRepository riesgos;
	private final ApplicationEventPublisher eventos;

	RegistroEndosos(PolizaRepository polizas, RiesgoRepository riesgos, ApplicationEventPublisher eventos) {
		this.polizas = polizas;
		this.riesgos = riesgos;
		this.eventos = eventos;
	}

	PolizaVigente cargar(long polizaId) {
		Poliza ultimo = polizas.findFirstByPolizaIdOrderByNumEndosoDesc(polizaId)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe la póliza " + polizaId + "."));
		return new PolizaVigente(ultimo, riesgos.findByPolizaIdAndVigenteOrderByCodRiesgo(polizaId, true));
	}

	/**
	 * Inserta la fila nueva de la póliza y las de los riesgos que cambiaron; las filas anteriores de esos riesgos
	 * ya quedaron con vigente = N. Si dos operaciones crean a la vez el mismo número de endoso, la llave de POLIZA
	 * rechaza la segunda.
	 */
	Endoso guardar(Endoso endoso) {
		Poliza poliza = endoso.poliza();
		polizas.saveAndFlush(poliza);
		riesgos.saveAll(endoso.riesgosEscritos());
		eventos.publishEvent(new EndosoRegistrado(poliza.getPolizaId(), poliza.getNumEndoso(), poliza.getTipoEndoso()));
		return endoso;
	}
}
