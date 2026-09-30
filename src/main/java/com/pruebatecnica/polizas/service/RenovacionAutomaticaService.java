package com.pruebatecnica.polizas.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.pruebatecnica.polizas.dto.EndosoResultado;
import com.pruebatecnica.polizas.dto.ProcesoRequest;
import com.pruebatecnica.polizas.dto.RenovacionAutomaticaResultado;
import com.pruebatecnica.polizas.exception.EstadoInvalidoException;
import com.pruebatecnica.polizas.exception.ReglaNegocioException;
import com.pruebatecnica.polizas.repository.PolizaRepository;

/**
 * Renovación automática: renueva las pólizas no canceladas cuya vigencia terminó a más tardar en la fecha de corte
 * (la fecha del movimiento, o hoy). La llama una vez al día un programador de tareas externo, para que con varias
 * copias de la API corra una sola vez. Cada póliza se renueva en su propia transacción: si una no se puede renovar
 * (por ejemplo, porque aún no hay IPC), las demás siguen y esa se intenta de nuevo en la corrida siguiente. Repetir la
 * corrida no duplica nada: una póliza renovada ya no está vencida.
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

	public RenovacionAutomaticaResultado renovarLasQueVencieron(ProcesoRequest proceso) {
		LocalDate corte = proceso.fechaMovimientoO(LocalDate.now(reloj));
		List<Long> porRenovar = polizas.buscarPorRenovar(corte);
		List<EndosoResultado> renovadas = new ArrayList<>();
		List<RenovacionAutomaticaResultado.Omitida> omitidas = new ArrayList<>();
		for (long polizaId : porRenovar) {
			try {
				renovadas.add(servicio.renovar(polizaId, proceso, "Renovacion automatica con corte " + corte));
			} catch (ReglaNegocioException | EstadoInvalidoException | DataIntegrityViolationException e) {
				log.warn("Renovacion automatica: la poliza {} no se renovo: {}", polizaId, e.getMessage());
				omitidas.add(new RenovacionAutomaticaResultado.Omitida(polizaId, e.getMessage()));
			}
		}
		log.info("Renovacion automatica del {}: {} vencidas, {} renovadas, {} omitidas", corte, porRenovar.size(),
				renovadas.size(), omitidas.size());
		return new RenovacionAutomaticaResultado(corte, porRenovar.size(), renovadas, omitidas);
	}
}
