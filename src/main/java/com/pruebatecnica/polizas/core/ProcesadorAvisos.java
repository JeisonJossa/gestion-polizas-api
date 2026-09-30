package com.pruebatecnica.polizas.core;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.pruebatecnica.polizas.domain.Aviso;
import com.pruebatecnica.polizas.domain.DestinoAviso;
import com.pruebatecnica.polizas.domain.EstadoAviso;
import com.pruebatecnica.polizas.repository.AvisoRepository;

/**
 * Procesador de avisos del Módulo 1 (outbox). Toma los avisos PENDIENTES de la tabla AVISO y los entrega: los del
 * CORE por el adaptador del servicio agnóstico de edición y los de notificación al publicador de eventos.
 * <ul>
 * <li>Se ejecuta apenas se guarda un endoso (después del commit) y, además, cada cierto tiempo reintenta los que
 * quedaron pendientes.</li>
 * <li>Los avisos al CORE de una misma póliza se envían en orden de endoso: si uno falla, los siguientes esperan.</li>
 * <li>Si el CORE no responde, el aviso sigue PENDIENTE y se reintenta con el mismo id; si lo rechaza, queda
 * RECHAZADO, no se reintenta y se genera una alerta. Cada aviso se procesa en su propia transacción.</li>
 * </ul>
 */
@Component
public class ProcesadorAvisos {

	private static final Logger log = LoggerFactory.getLogger(ProcesadorAvisos.class);
	private static final int INTENTOS_PARA_ALERTA = 5;

	private final AvisoRepository avisos;
	private final CoreNotifier core;
	private final PublicadorEventos publicador;
	private final TransactionTemplate transaccion;

	public ProcesadorAvisos(AvisoRepository avisos, CoreNotifier core, PublicadorEventos publicador,
			PlatformTransactionManager transacciones) {
		this.avisos = avisos;
		this.core = core;
		this.publicador = publicador;
		this.transaccion = new TransactionTemplate(transacciones);
		this.transaccion.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	}

	/** Entrega los avisos pendientes de una póliza; lo llama el listener después del commit de cada endoso. */
	public void procesarPoliza(long polizaId) {
		entregarEnOrden(transaccion.execute(estado -> ids(
				avisos.findByPolizaIdAndEstadoOrderByNumEndosoAscIdAsc(polizaId, EstadoAviso.PENDIENTE))));
	}

	/** Reintenta todos los avisos pendientes. */
	@Scheduled(initialDelayString = "${polizas.avisos.reintento-ms}", fixedDelayString = "${polizas.avisos.reintento-ms}")
	public void reintentarPendientes() {
		List<Long> pendientes = transaccion.execute(estado -> ids(
				avisos.findByEstadoOrderByPolizaIdAscNumEndosoAscIdAsc(EstadoAviso.PENDIENTE)));
		if (!pendientes.isEmpty()) {
			log.info("Reintentando {} avisos pendientes", pendientes.size());
		}
		entregarEnOrden(pendientes);
	}

	private void entregarEnOrden(List<Long> ids) {
		Set<Long> polizasEnEspera = new HashSet<>();
		for (Long id : ids) {
			transaccion.executeWithoutResult(estado -> avisos.findById(id)
					.filter(aviso -> aviso.getEstado() == EstadoAviso.PENDIENTE)
					.filter(aviso -> aviso.getDestino() != DestinoAviso.CORE
							|| !polizasEnEspera.contains(aviso.getPolizaId()))
					.ifPresent(aviso -> {
						if (!entregar(aviso) && aviso.getDestino() == DestinoAviso.CORE) {
							polizasEnEspera.add(aviso.getPolizaId());
						}
					}));
		}
	}

	/** Devuelve false si el aviso quedó pendiente para reintentar. */
	private boolean entregar(Aviso aviso) {
		try {
			if (aviso.getDestino() == DestinoAviso.CORE) {
				core.enviar(aviso.getId(), aviso.getPolizaId(), aviso.getNumEndoso());
			} else {
				publicador.publicar(aviso);
			}
			aviso.marcarSincronizado();
			return true;
		}
		catch (CoreRechazoException e) {
			aviso.marcarRechazado(e.getMessage());
			log.error("ALERTA: el CORE rechazo el aviso {} de la poliza {}, endoso {}: {}", aviso.getId(),
					aviso.getPolizaId(), aviso.getNumEndoso(), e.getMessage());
			return true;
		}
		catch (RuntimeException e) {
			aviso.registrarFallo(e.getMessage());
			if (aviso.getIntentos() % INTENTOS_PARA_ALERTA == 0) {
				log.error("ALERTA: el aviso {} de la poliza {} lleva {} intentos fallidos: {}", aviso.getId(),
						aviso.getPolizaId(), aviso.getIntentos(), e.getMessage());
			} else {
				log.warn("El aviso {} de la poliza {} quedo pendiente (intento {}): {}", aviso.getId(),
						aviso.getPolizaId(), aviso.getIntentos(), e.getMessage());
			}
			return false;
		}
	}

	private static List<Long> ids(List<Aviso> lista) {
		return lista.stream().map(Aviso::getId).toList();
	}
}
