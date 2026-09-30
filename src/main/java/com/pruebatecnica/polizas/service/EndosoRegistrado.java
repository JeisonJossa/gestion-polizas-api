package com.pruebatecnica.polizas.service;

import com.pruebatecnica.polizas.domain.TipoEndoso;

/** Se publica al guardar un endoso con sus avisos; después del commit el procesador de avisos los entrega. */
public record EndosoRegistrado(long polizaId, int numEndoso, TipoEndoso tipoEndoso) {
}
