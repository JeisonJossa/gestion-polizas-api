package com.pruebatecnica.polizas.dto;

import java.util.List;

/**
 * Forma común de todas las respuestas, al estilo de los servicios del CORE: identificador de la transacción, el proceso
 * ejecutado, resultado 0 (éxito) o -1 (error), un mensaje, la lista de errores y los datos.
 */
public record RespuestaApi<T>(String idTransaccion, ProcesoResponse proceso, int resultado, String mensaje,
		List<ErrorDetalle> errores, T datos) {

	public static final int EXITO = 0;
	public static final int ERROR = -1;
}
