package com.pruebatecnica.polizas.dto;

/** Resultado de agregar o cancelar un riesgo: la póliza con su endoso nuevo y el riesgo afectado. */
public record MovimientoRiesgoResponse(PolizaResponse poliza, RiesgoResponse riesgo) {
}
