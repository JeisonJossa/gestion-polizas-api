package com.pruebatecnica.polizas.domain;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Variación anual del IPC, en porcentaje. */
@Entity
@Table(name = "ipc")
public class Ipc {

	@Id
	private Integer anio;

	private BigDecimal porcentaje;

	protected Ipc() {
	}

	public BigDecimal getPorcentaje() {
		return porcentaje;
	}
}
