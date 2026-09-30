package com.pruebatecnica.polizas.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Resultado de la renovación automática del día: cuántas pólizas vencidas se revisaron, cuáles se renovaron y
 * cuáles no, con el motivo (se vuelven a intentar en la siguiente corrida).
 */
public record RenovacionAutomaticaResponse(LocalDate fecha, int revisadas, List<PolizaResponse> renovadas,
		List<Omitida> omitidas) {

	public record Omitida(long polizaId, String motivo) {
	}
}
