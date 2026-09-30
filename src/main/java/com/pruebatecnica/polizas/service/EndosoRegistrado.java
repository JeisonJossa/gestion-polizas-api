package com.pruebatecnica.polizas.service;

import com.pruebatecnica.polizas.domain.TipoEndoso;

/** Se publica al guardar un endoso; después del commit se avisa al CORE. */
public record EndosoRegistrado(long polizaId, int numEndoso, TipoEndoso tipoEndoso) {
}
