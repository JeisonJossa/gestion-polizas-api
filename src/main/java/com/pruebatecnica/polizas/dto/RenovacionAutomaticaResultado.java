package com.pruebatecnica.polizas.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Datos de la renovación automática: cuántas pólizas vencidas se revisaron a la fecha de corte, el endoso de cada una
 * que se renovó y las que no, con el motivo (se vuelven a intentar en la siguiente corrida).
 */
public record RenovacionAutomaticaResultado(LocalDate fechaCorte, int revisadas, List<EndosoResultado> renovadas,
		List<Omitida> omitidas) {

	public record Omitida(long polizaId, String motivo) {
	}
}
