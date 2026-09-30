package com.pruebatecnica.polizas.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pruebatecnica.polizas.domain.Endoso;
import com.pruebatecnica.polizas.domain.Riesgo;
import com.pruebatecnica.polizas.dto.MovimientoRiesgoResponse;
import com.pruebatecnica.polizas.dto.PolizaResponse;
import com.pruebatecnica.polizas.dto.RiesgoRequest;
import com.pruebatecnica.polizas.dto.RiesgoResponse;
import com.pruebatecnica.polizas.exception.RecursoNoEncontradoException;
import com.pruebatecnica.polizas.repository.RiesgoRepository;

@Service
public class RiesgoService {

	private final RiesgoRepository riesgos;
	private final RegistroEndosos registro;
	private final Clock reloj;

	public RiesgoService(RiesgoRepository riesgos, RegistroEndosos registro, Clock reloj) {
		this.riesgos = riesgos;
		this.registro = registro;
		this.reloj = reloj;
	}

	@Transactional(readOnly = true)
	public List<RiesgoResponse> listar(long polizaId) {
		return registro.cargar(polizaId).riesgos().stream().map(RiesgoResponse::de).toList();
	}

	@Transactional
	public MovimientoRiesgoResponse agregar(long polizaId, RiesgoRequest solicitud) {
		Endoso inclusion = registro.guardar(registro.cargar(polizaId)
				.agregarRiesgo(solicitud.aDominio(), LocalDate.now(reloj), riesgos::siguienteId));
		return respuesta(inclusion);
	}

	@Transactional
	public MovimientoRiesgoResponse cancelar(long riesgoId) {
		Riesgo riesgo = riesgos.findFirstByRiesgoIdAndVigente(riesgoId, true)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el riesgo " + riesgoId + "."));
		Endoso exclusion = registro.guardar(registro.cargar(riesgo.getPolizaId())
				.cancelarRiesgo(riesgoId, LocalDate.now(reloj)));
		return respuesta(exclusion);
	}

	/** En la inclusión y en la exclusión se escribe exactamente un riesgo. */
	private MovimientoRiesgoResponse respuesta(Endoso endoso) {
		return new MovimientoRiesgoResponse(PolizaResponse.de(endoso.poliza()),
				RiesgoResponse.de(endoso.riesgosEscritos().get(0)));
	}
}
