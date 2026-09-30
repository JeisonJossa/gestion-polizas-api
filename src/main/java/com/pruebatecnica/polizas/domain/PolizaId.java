package com.pruebatecnica.polizas.domain;

import java.io.Serializable;
import java.util.Objects;

/** Llave de POLIZA: una fila por endoso. */
public class PolizaId implements Serializable {

	private Long polizaId;
	private Integer numEndoso;

	protected PolizaId() {
	}

	public PolizaId(Long polizaId, Integer numEndoso) {
		this.polizaId = polizaId;
		this.numEndoso = numEndoso;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof PolizaId otro && Objects.equals(polizaId, otro.polizaId) && Objects.equals(numEndoso, otro.numEndoso);
	}

	@Override
	public int hashCode() {
		return Objects.hash(polizaId, numEndoso);
	}
}
