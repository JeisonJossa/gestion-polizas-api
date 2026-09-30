package com.pruebatecnica.polizas.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.pruebatecnica.polizas.dto.PolizaResponse;
import com.pruebatecnica.polizas.dto.RenovacionAutomaticaResponse;
import com.pruebatecnica.polizas.exception.EstadoInvalidoException;
import com.pruebatecnica.polizas.exception.ReglaNegocioException;
import com.pruebatecnica.polizas.repository.PolizaRepository;

/**
 * Renovación automática: renueva las pólizas no canceladas cuya vigencia ya terminó. La llama una vez al día un
 * programador de tareas externo, para que con varias copias de la API corra una sola vez. Cada póliza se renueva en
 * su propia transacción: si una no se puede renovar (por ejemplo, porque aún no hay IPC), las demás siguen y esa se
 * intenta de nuevo en la corrida siguiente. Repetir la corrida no duplica nada: una póliza renovada ya no está
 * vencida.
 */
@Service
public class RenovacionAutomaticaService {

	private static final Logger log = LoggerFactory.getLogger(RenovacionAutomaticaService.class);

	private final PolizaRepository polizas;
	private final PolizaService servicio;
	private final Clock reloj;

	public RenovacionAutomaticaService(PolizaRepository polizas, PolizaService servicio, Clock reloj) {
		this.polizas = polizas;
		this.servicio = servicio;
		this.reloj = reloj;
	}

	public RenovacionAutomaticaResponse renovarLasQueVencieron() {
		LocalDate hoy = LocalDate.now(reloj);
		List<Long> porRenovar = polizas.buscarPorRenovar(hoy);
		List<PolizaResponse> renovadas = new ArrayList<>();
		List<RenovacionAutomaticaResponse.Omitida> omitidas = new ArrayList<>();
		for (long polizaId : porRenovar) {
			try {
				renovadas.add(servicio.renovar(polizaId).sinRiesgos());
			} catch (ReglaNegocioException | EstadoInvalidoException | DataIntegrityViolationException e) {
				log.warn("Renovacion automatica: la poliza {} no se renovo: {}", polizaId, e.getMessage());
				omitidas.add(new RenovacionAutomaticaResponse.Omitida(polizaId, e.getMessage()));
			}
		}
		log.info("Renovacion automatica del {}: {} vencidas, {} renovadas, {} omitidas", hoy, porRenovar.size(),
				renovadas.size(), omitidas.size());
		return new RenovacionAutomaticaResponse(hoy, porRenovar.size(), renovadas, omitidas);
	}
}
