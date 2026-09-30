package com.pruebatecnica.polizas.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class VigenciaTest {

	private static final LocalDate ENERO = LocalDate.of(2026, 1, 1);

	@Test
	void laVigenciaTerminaElDiaAnteriorAlMismoDiaDelMesFinal() {
		assertThat(Vigencia.fin(ENERO, 12)).isEqualTo(LocalDate.of(2026, 12, 31));
		assertThat(Vigencia.fin(LocalDate.of(2026, 3, 15), 12)).isEqualTo(LocalDate.of(2027, 3, 14));
	}

	@Test
	void cuentaCompletoElMesDeLaVigenciaEnQueOcurreElMovimiento() {
		assertThat(Vigencia.mesesRestantes(ENERO, 12, LocalDate.of(2026, 7, 1))).isEqualTo(6);
		assertThat(Vigencia.mesesRestantes(ENERO, 12, LocalDate.of(2026, 7, 31))).isEqualTo(6);
		assertThat(Vigencia.mesesRestantes(ENERO, 12, LocalDate.of(2026, 12, 31))).isEqualTo(1);
	}

	@Test
	void antesDeEmpezarFaltaTodaLaVigenciaYDespuesDeTerminarNoFaltaNada() {
		assertThat(Vigencia.mesesRestantes(ENERO, 12, LocalDate.of(2025, 12, 15))).isEqualTo(12);
		assertThat(Vigencia.mesesRestantes(ENERO, 12, LocalDate.of(2027, 1, 1))).isZero();
	}

	@Test
	void conVigenciaAMitadDeMesLosMesesSeCuentanDesdeElDiaDeInicio() {
		LocalDate inicio = LocalDate.of(2026, 10, 15);
		assertThat(Vigencia.mesesRestantes(inicio, 12, LocalDate.of(2026, 11, 14))).isEqualTo(12);
		assertThat(Vigencia.mesesRestantes(inicio, 12, LocalDate.of(2026, 11, 15))).isEqualTo(11);
	}
}
