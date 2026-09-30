package com.pruebatecnica.polizas;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Verifica que la aplicación arranca con los datos precargados y que esos datos cumplen
 * las reglas de totalización del Módulo 1.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:datos;DB_CLOSE_DELAY=-1")
class DatosPrecargadosTest {

	/** Última fila (endoso vigente) de cada póliza. */
	private static final String ULTIMO_ENDOSO =
			"p.num_endoso = (SELECT MAX(x.num_endoso) FROM poliza x WHERE x.poliza_id = p.poliza_id)";

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void cargaPolizasRiesgosEIpc() {
		assertThat(contar("ipc")).isEqualTo(4);
		assertThat(contar("poliza")).as("filas de póliza, una por endoso").isEqualTo(11);
		assertThat(contar("riesgo")).as("filas de riesgo, solo las que cambian en cada endoso").isEqualTo(14);
		assertThat(jdbc.queryForObject("SELECT COUNT(DISTINCT poliza_id) FROM poliza", Integer.class)).isEqualTo(7);
	}

	@Test
	void laPrimaDeCadaPolizaEsLaSumaDeLasPrimasDeSusRiesgosEnLaVigenciaActual() {
		var descuadres = jdbc.queryForList("""
				SELECT p.poliza_id, p.prima, COALESCE(SUM(r.prima), 0) AS suma_riesgos
				FROM poliza p
				LEFT JOIN riesgo r ON r.poliza_id = p.poliza_id AND r.vigente = 'S'
				     AND (r.fecha_exclusion IS NULL OR r.fecha_exclusion >= p.inicio_vigencia)
				WHERE %s
				GROUP BY p.poliza_id, p.prima
				HAVING p.prima <> COALESCE(SUM(r.prima), 0)
				""".formatted(ULTIMO_ENDOSO));
		assertThat(descuadres).isEmpty();
	}

	@Test
	void elCanonDeCadaPolizaEsLaSumaDeLosCanonesDeSusRiesgosActivos() {
		var descuadres = jdbc.queryForList("""
				SELECT p.poliza_id, p.canon, COALESCE(SUM(r.canon), 0) AS suma_riesgos
				FROM poliza p
				LEFT JOIN riesgo r ON r.poliza_id = p.poliza_id AND r.vigente = 'S' AND r.estado = 'ACTIVO'
				WHERE %s
				GROUP BY p.poliza_id, p.canon
				HAVING p.canon <> COALESCE(SUM(r.canon), 0)
				""".formatted(ULTIMO_ENDOSO));
		assertThat(descuadres).isEmpty();
	}

	@Test
	void laPrimaDeCadaEndosoEsLaSumaDeLasPrimasDelEndosoDeSusRiesgos() {
		var descuadres = jdbc.queryForList("""
				SELECT p.poliza_id, p.num_endoso, p.prima_endoso, COALESCE(SUM(r.prima_endoso), 0) AS suma_riesgos
				FROM poliza p
				LEFT JOIN riesgo r ON r.poliza_id = p.poliza_id AND r.num_endoso = p.num_endoso
				GROUP BY p.poliza_id, p.num_endoso, p.prima_endoso
				HAVING p.prima_endoso <> COALESCE(SUM(r.prima_endoso), 0)
				""");
		assertThat(descuadres).isEmpty();
	}

	private int contar(String tabla) {
		return jdbc.queryForObject("SELECT COUNT(*) FROM " + tabla, Integer.class);
	}

}
