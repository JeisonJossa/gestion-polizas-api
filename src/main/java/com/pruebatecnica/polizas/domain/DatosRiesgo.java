package com.pruebatecnica.polizas.domain;

import java.math.BigDecimal;

/** Datos con los que entra un riesgo nuevo. */
public record DatosRiesgo(Inmueble inmueble, Persona arrendatario, Persona arrendador, BigDecimal canon) {
}
