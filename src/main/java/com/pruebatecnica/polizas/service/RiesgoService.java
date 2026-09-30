package com.pruebatecnica.polizas.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pruebatecnica.polizas.domain.Endoso;
import com.pruebatecnica.polizas.domain.Riesgo;
import com.pruebatecnica.polizas.dto.CancelacionRequest;
import com.pruebatecnica.polizas.dto.EndosoResultado;
import com.pruebatecnica.polizas.dto.InclusionRiesgoRequest;
import com.pruebatecnica.polizas.dto.RiesgoConsulta;
import com.pruebatecnica.polizas.exception.RecursoNoEncontradoException;
import com.pruebatecnica.polizas.repository.RiesgoRepository;

@Service
public class RiesgoService {

	private final RiesgoRepository riesgos;
	private final RegistroEndosos registro;
	private final RegistroPersonas personas;
	private final Clock reloj;

	public RiesgoService(RiesgoRepository riesgos, RegistroEndosos registro, RegistroPersonas personas, Clock reloj) {
		this.riesgos = riesgos;
		this.registro = registro;
		this.personas = personas;
		this.reloj = reloj;
	}

	@Transactional(readOnly = true)
	public List<RiesgoConsulta> listar(long polizaId) {
		return registro.cargar(polizaId).riesgos().stream().map(RiesgoConsulta::de).toList();
	}

	@Transactional
	public EndosoResultado agregar(long polizaId, InclusionRiesgoRequest solicitud) {
		LocalDate fecha = solicitud.proceso().fechaMovimientoO(LocalDate.now(reloj));
		Endoso inclusion = registro.cargar(polizaId)
				.agregarRiesgo(personas.datosRiesgo(solicitud.riesgo()), fecha, riesgos::siguienteId);
		return EndosoResultado.de(registro.guardar(inclusion, solicitud.proceso(), null));
	}

	@Transactional
	public EndosoResultado cancelar(long riesgoId, CancelacionRequest solicitud) {
		Riesgo riesgo = riesgos.findFirstByRiesgoIdAndVigente(riesgoId, true)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el riesgo " + riesgoId + "."));
		LocalDate fecha = solicitud.proceso().fechaMovimientoO(LocalDate.now(reloj));
		Endoso exclusion = registro.cargar(riesgo.getPolizaId()).cancelarRiesgo(riesgoId, fecha);
		return EndosoResultado.de(registro.guardar(exclusion, solicitud.proceso(), solicitud.motivo()));
	}
}
