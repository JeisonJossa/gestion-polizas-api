package com.pruebatecnica.polizas.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.pruebatecnica.polizas.domain.Aviso;

/**
 * El Módulo 2 no incluye la cola ni el Servicio de Notificaciones: el evento queda en el log. En producción este
 * adaptador publica en la cola (Módulo 1) y el resto del flujo no cambia.
 */
@Component
public class LogPublicadorEventos implements PublicadorEventos {

	private static final Logger log = LoggerFactory.getLogger(LogPublicadorEventos.class);

	@Override
	public void publicar(Aviso aviso) {
		log.info("[NOTIFICACION] Evento {} de la poliza {} (endoso {}) publicado para el Servicio de Notificaciones",
				aviso.getEvento(), aviso.getPolizaId(), aviso.getNumEndoso());
	}
}
