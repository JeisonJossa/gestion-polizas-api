package com.pruebatecnica.polizas.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.pruebatecnica.polizas.domain.Aviso;
import com.pruebatecnica.polizas.domain.Endoso;
import com.pruebatecnica.polizas.domain.Poliza;
import com.pruebatecnica.polizas.domain.PolizaVigente;
import com.pruebatecnica.polizas.dto.ProcesoRequest;
import com.pruebatecnica.polizas.exception.RecursoNoEncontradoException;
import com.pruebatecnica.polizas.repository.AvisoRepository;
import com.pruebatecnica.polizas.repository.PolizaRepository;
import com.pruebatecnica.polizas.repository.RiesgoRepository;

/** Carga la póliza vigente y guarda cada endoso nuevo. Lo usan los servicios de pólizas y de riesgos. */
@Component
class RegistroEndosos {

	private final PolizaRepository polizas;
	private final RiesgoRepository riesgos;
	private final AvisoRepository avisos;
	private final ApplicationEventPublisher eventos;
	private final Clock reloj;

	RegistroEndosos(PolizaRepository polizas, RiesgoRepository riesgos, AvisoRepository avisos,
			ApplicationEventPublisher eventos, Clock reloj) {
		this.polizas = polizas;
		this.riesgos = riesgos;
		this.avisos = avisos;
		this.eventos = eventos;
		this.reloj = reloj;
	}

	PolizaVigente cargar(long polizaId) {
		Poliza ultimo = polizas.findFirstByPolizaIdOrderByNumEndosoDesc(polizaId)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe la póliza " + polizaId + "."));
		return new PolizaVigente(ultimo, riesgos.findByPolizaIdAndVigenteOrderByCodRiesgo(polizaId, true));
	}

	/**
	 * Inserta la fila nueva de la póliza, con el canal, el usuario y el motivo que la originaron, las de los riesgos
	 * que cambiaron (sus filas anteriores ya quedaron con vigente = N) y los avisos del endoso para el CORE y las
	 * notificaciones, todo en la misma transacción (outbox). Si dos operaciones crean a la vez el mismo número de
	 * endoso, la llave de POLIZA rechaza la segunda.
	 */
	Endoso guardar(Endoso endoso, ProcesoRequest proceso, String motivo) {
		Poliza poliza = endoso.poliza();
		poliza.registrarOrigen(proceso.canal(), proceso.usuario(), motivo);
		polizas.saveAndFlush(poliza);
		riesgos.saveAll(endoso.riesgosEscritos());
		avisos.saveAll(Aviso.de(poliza, LocalDateTime.now(reloj)));
		eventos.publishEvent(new EndosoRegistrado(poliza.getPolizaId(), poliza.getNumEndoso(), poliza.getTipoEndoso()));
		return endoso;
	}
}
