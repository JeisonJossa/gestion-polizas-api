package com.pruebatecnica.polizas.core;

/**
 * Puerto hacia el servicio agnóstico de edición, que mantiene actualizado el CORE. Lanza CoreRechazoException si el
 * CORE rechaza la operación y CoreNoDisponibleException si no responde; en ese caso el aviso se reintenta con el
 * mismo id para que el CORE no lo aplique dos veces.
 */
public interface CoreNotifier {

	void enviar(long avisoId, long polizaId, int numEndoso);
}
