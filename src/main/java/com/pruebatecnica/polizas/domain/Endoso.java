package com.pruebatecnica.polizas.domain;

import java.util.List;

/**
 * Resultado de una operación: la fila nueva de la póliza, las filas de riesgo que se escribieron en el endoso y
 * cómo quedan los riesgos vigentes de la póliza.
 */
public record Endoso(Poliza poliza, List<Riesgo> riesgosEscritos, List<Riesgo> riesgosVigentes) {
}
