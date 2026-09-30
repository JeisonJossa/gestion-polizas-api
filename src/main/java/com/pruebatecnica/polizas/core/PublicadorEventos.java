package com.pruebatecnica.polizas.core;

import com.pruebatecnica.polizas.domain.Aviso;

/** Puerto hacia la cola de eventos de la que consume el Servicio de Notificaciones (correo y SMS). */
public interface PublicadorEventos {

	void publicar(Aviso aviso);
}
