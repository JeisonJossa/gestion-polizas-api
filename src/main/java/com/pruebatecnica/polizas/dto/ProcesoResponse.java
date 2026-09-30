package com.pruebatecnica.polizas.dto;

import java.time.LocalDateTime;

/** El proceso que se ejecutó y cuándo empezó y terminó. */
public record ProcesoResponse(TipoProceso tipoProceso, LocalDateTime fechaInicio, LocalDateTime fechaFin) {
}
