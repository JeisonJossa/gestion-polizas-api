package com.pruebatecnica.polizas.domain;

import java.io.Serializable;
import java.util.Objects;

/** Llave de RIESGO: la fila de un riesgo escrita en un endoso. */
public class RiesgoId implements Serializable {

	private Long polizaId;
	private Integer numEndoso;
	private Integer codRiesgo;

	protected RiesgoId() {
	}

	public RiesgoId(Long polizaId, Integer numEndoso, Integer codRiesgo) {
		this.polizaId = polizaId;
		this.numEndoso = numEndoso;
		this.codRiesgo = codRiesgo;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof RiesgoId otro && Objects.equals(polizaId, otro.polizaId)
				&& Objects.equals(numEndoso, otro.numEndoso) && Objects.equals(codRiesgo, otro.codRiesgo);
	}

	@Override
	public int hashCode() {
		return Objects.hash(polizaId, numEndoso, codRiesgo);
	}
}
