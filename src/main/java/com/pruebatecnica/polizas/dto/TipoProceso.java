package com.pruebatecnica.polizas.dto;

import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * Proceso que ejecuta cada petición. Las que cambian una póliza lo envían en el bloque "proceso" y debe coincidir con
 * la ruta; en las consultas lo deduce el API. Cada proceso sabe a qué ruta corresponde.
 */
public enum TipoProceso {

	EMISION("POST", "/polizas"),
	INCLUSION_RIESGO("POST", "/polizas/[^/]+/riesgos"),
	EXCLUSION_RIESGO("POST", "/riesgos/[^/]+/cancelar"),
	RENOVACION("POST", "/polizas/[^/]+/renovar"),
	CANCELACION("POST", "/polizas/[^/]+/cancelar"),
	RENOVACION_AUTOMATICA("POST", "/renovaciones"),
	CONSULTA_POLIZAS("GET", "/polizas"),
	CONSULTA_RIESGOS("GET", "/polizas/[^/]+/riesgos");

	private final String metodo;
	private final Pattern ruta;

	TipoProceso(String metodo, String ruta) {
		this.metodo = metodo;
		this.ruta = Pattern.compile(ruta + "/?");
	}

	/** Proceso que corresponde a un método y una ruta, o null si la ruta no es de un proceso (por ejemplo, el mock). */
	public static TipoProceso deLaRuta(String metodo, String ruta) {
		return Arrays.stream(values())
				.filter(proceso -> proceso.metodo.equalsIgnoreCase(metodo) && proceso.ruta.matcher(ruta).matches())
				.findFirst()
				.orElse(null);
	}
}
