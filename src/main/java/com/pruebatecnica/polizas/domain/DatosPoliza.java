package com.pruebatecnica.polizas.domain;

import java.time.LocalDate;

/** Datos con los que se emite una póliza. */
public record DatosPoliza(TipoPoliza tipo, Persona tomador, LocalDate inicioVigencia, int mesesVigencia) {
}
