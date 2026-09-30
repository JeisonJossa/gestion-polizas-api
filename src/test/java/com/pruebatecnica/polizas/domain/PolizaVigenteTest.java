package com.pruebatecnica.polizas.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import com.pruebatecnica.polizas.exception.EstadoInvalidoException;
import com.pruebatecnica.polizas.exception.ReglaNegocioException;

/** Reglas de negocio sin Spring ni base de datos. */
class PolizaVigenteTest {

	private static final LocalDate ENERO_2026 = LocalDate.of(2026, 1, 1);
	private static final Persona INMOBILIARIA = new Persona("NIT", "900123456", "Inmobiliaria Andina S.A.S.", null, null);

	private final AtomicLong ids = new AtomicLong(1);

	@Test
	void reproduceElEjemploDeTotalizacionDelModulo1() {
		Endoso emision = emitirColectiva(1_000_000, 2_000_000);
		verificar(emision, 0, TipoEndoso.EMISION, ClaseMovimiento.COBRO, "36000000", "36000000", "3000000");

		Endoso inclusion = vigente(emision).agregarRiesgo(riesgo("3", 1_000_000), LocalDate.of(2026, 7, 10),
				ids::getAndIncrement);
		verificar(inclusion, 1, TipoEndoso.INCLUSION, ClaseMovimiento.COBRO, "6000000", "42000000", "4000000");

		long riesgo2 = emision.riesgosVigentes().get(1).getRiesgoId();
		Endoso exclusion = vigente(inclusion).cancelarRiesgo(riesgo2, LocalDate.of(2026, 9, 5));
		verificar(exclusion, 2, TipoEndoso.EXCLUSION, ClaseMovimiento.DEVOLUCION, "-8000000", "34000000", "2000000");
		assertThat(exclusion.riesgosEscritos().get(0).getPrima()).isEqualByComparingTo("16000000");

		Endoso renovacion = vigente(exclusion).renovar(new BigDecimal("5.20"));
		verificar(renovacion, 3, TipoEndoso.RENOVACION, ClaseMovimiento.COBRO, "25248000", "25248000", "2104000");
		assertThat(renovacion.poliza().getEstado()).isEqualTo(EstadoPoliza.RENOVADA);
		assertThat(renovacion.poliza().getInicioVigencia()).isEqualTo(LocalDate.of(2027, 1, 1));
		assertThat(renovacion.poliza().getFinVigencia()).isEqualTo(LocalDate.of(2027, 12, 31));
		assertThat(renovacion.riesgosEscritos()).as("el riesgo cancelado no se renueva").hasSize(2);

		Endoso cancelacion = vigente(renovacion).cancelar(LocalDate.of(2027, 5, 3));
		verificar(cancelacion, 4, TipoEndoso.CANCELACION, ClaseMovimiento.DEVOLUCION, "-16832000", "8416000", "0");
		assertThat(cancelacion.poliza().getEstado()).isEqualTo(EstadoPoliza.CANCELADA);
		assertThat(cancelacion.riesgosVigentes()).noneMatch(Riesgo::estaActivo);
	}

	@Test
	void unaIndividualSoloPuedeTenerUnRiesgo() {
		DatosPoliza individual = new DatosPoliza(TipoPoliza.INDIVIDUAL, arrendatario("10"), ENERO_2026, 12);
		assertThatThrownBy(() -> PolizaVigente.emitir(1, individual,
				List.of(riesgo("10", 1_000_000), riesgo("11", 1_000_000)), ids::getAndIncrement))
				.isInstanceOf(ReglaNegocioException.class)
				.hasMessageContaining("solo puede tener 1 riesgo");
	}

	@Test
	void enUnaIndividualElTomadorEsElArrendatario() {
		DatosPoliza individual = new DatosPoliza(TipoPoliza.INDIVIDUAL, arrendatario("10"), ENERO_2026, 12);
		assertThatThrownBy(() -> PolizaVigente.emitir(1, individual, List.of(riesgo("99", 1_000_000)),
				ids::getAndIncrement))
				.isInstanceOf(ReglaNegocioException.class)
				.hasMessageContaining("tomador debe ser el arrendatario");
	}

	@Test
	void agregarRiesgoExigeQueLaPolizaSeaColectiva() {
		DatosPoliza individual = new DatosPoliza(TipoPoliza.INDIVIDUAL, arrendatario("10"), ENERO_2026, 12);
		Endoso emision = PolizaVigente.emitir(1, individual, List.of(riesgo("10", 1_000_000)), ids::getAndIncrement);

		assertThatThrownBy(() -> vigente(emision).agregarRiesgo(riesgo("11", 1_000_000), LocalDate.of(2026, 3, 1),
				ids::getAndIncrement))
				.isInstanceOf(ReglaNegocioException.class)
				.hasMessageContaining("Solo se pueden agregar riesgos a pólizas colectivas");
	}

	@Test
	void agregarUnRiesgoNoReescribeLosDemas() {
		Endoso inclusion = vigente(emitirColectiva(1_000_000, 2_000_000))
				.agregarRiesgo(riesgo("3", 500_000), LocalDate.of(2026, 3, 1), ids::getAndIncrement);

		assertThat(inclusion.riesgosEscritos()).hasSize(1);
		assertThat(inclusion.riesgosEscritos().get(0).getCodRiesgo()).isEqualTo(3);
		assertThat(inclusion.riesgosVigentes()).hasSize(3);
	}

	@Test
	void noSeAgreganRiesgosDespuesDeTerminarLaVigencia() {
		PolizaVigente poliza = vigente(emitirColectiva(1_000_000));
		assertThatThrownBy(() -> poliza.agregarRiesgo(riesgo("2", 1_000_000), LocalDate.of(2027, 1, 15),
				ids::getAndIncrement))
				.isInstanceOf(ReglaNegocioException.class)
				.hasMessageContaining("renuévela");
	}

	@Test
	void cancelarUnRiesgoDejaSuFilaAnteriorNoVigente() {
		Endoso emision = emitirColectiva(1_000_000, 2_000_000);
		Riesgo anterior = emision.riesgosVigentes().get(0);

		Endoso exclusion = vigente(emision).cancelarRiesgo(anterior.getRiesgoId(), LocalDate.of(2026, 4, 1));

		assertThat(anterior.isVigente()).isFalse();
		Riesgo cancelado = exclusion.riesgosEscritos().get(0);
		assertThat(cancelado.isVigente()).isTrue();
		assertThat(cancelado.getEstado()).isEqualTo(EstadoRiesgo.CANCELADO);
		assertThat(cancelado.getFechaExclusion()).isEqualTo(LocalDate.of(2026, 4, 1));
	}

	@Test
	void unRiesgoCanceladoNoSeVuelveACancelar() {
		Endoso emision = emitirColectiva(1_000_000, 2_000_000);
		long riesgo1 = emision.riesgosVigentes().get(0).getRiesgoId();
		Endoso exclusion = vigente(emision).cancelarRiesgo(riesgo1, LocalDate.of(2026, 4, 1));

		assertThatThrownBy(() -> vigente(exclusion).cancelarRiesgo(riesgo1, LocalDate.of(2026, 5, 1)))
				.isInstanceOf(EstadoInvalidoException.class);
	}

	@Test
	void unaColectivaSinRiesgosSigueVigenteConPrimaCeroYSeRenueva() {
		Endoso emision = emitirColectiva(1_000_000);
		long unico = emision.riesgosVigentes().get(0).getRiesgoId();

		Endoso exclusion = vigente(emision).cancelarRiesgo(unico, LocalDate.of(2026, 1, 20));
		assertThat(exclusion.poliza().getEstado()).isEqualTo(EstadoPoliza.VIGENTE);
		assertThat(exclusion.poliza().getPrima()).isEqualByComparingTo("0");

		Endoso renovacion = vigente(exclusion).renovar(new BigDecimal("4.80"));
		assertThat(renovacion.poliza().getEstado()).isEqualTo(EstadoPoliza.RENOVADA);
		assertThat(renovacion.poliza().getClaseMovimiento()).isEqualTo(ClaseMovimiento.SIN_MOVIMIENTO);
		assertThat(renovacion.poliza().getPrima()).isEqualByComparingTo("0");
	}

	@Test
	void noSeRenuevaUnaPolizaCancelada() {
		Endoso cancelacion = vigente(emitirColectiva(1_000_000)).cancelar(LocalDate.of(2026, 2, 1));

		assertThatThrownBy(() -> vigente(cancelacion).renovar(new BigDecimal("5.20")))
				.isInstanceOf(EstadoInvalidoException.class)
				.hasMessageContaining("está cancelada");
	}

	@Test
	void cancelarLaPolizaCancelaTodosSusRiesgos() {
		Endoso cancelacion = vigente(emitirColectiva(1_000_000, 2_000_000, 3_000_000)).cancelar(LocalDate.of(2026, 7, 1));

		assertThat(cancelacion.riesgosEscritos()).hasSize(3).noneMatch(Riesgo::estaActivo);
		assertThat(cancelacion.poliza().getCanon()).isEqualByComparingTo("0");
		assertThat(cancelacion.poliza().getPrimaEndoso()).isEqualByComparingTo("-36000000");
	}

	@Test
	void laFechaDelMovimientoTieneQueEstarDentroDeLaVigencia() {
		PolizaVigente poliza = vigente(emitirColectiva(1_000_000));

		assertThatThrownBy(() -> poliza.cancelar(LocalDate.of(2025, 12, 31)))
				.isInstanceOf(ReglaNegocioException.class)
				.hasMessageContaining("anterior al inicio de la vigencia");
		assertThatThrownBy(() -> poliza.cancelar(LocalDate.of(2027, 1, 1)))
				.isInstanceOf(ReglaNegocioException.class)
				.hasMessageContaining("no quedan meses por devolver");
	}

	@Test
	void laRenovacionUsaElIpcDelAnioAnteriorAlInicioDeLaNuevaVigencia() {
		assertThat(vigente(emitirColectiva(1_000_000)).anioIpcParaRenovar()).isEqualTo(2026);
	}

	private Endoso emitirColectiva(long... canones) {
		List<DatosRiesgo> riesgos = Arrays.stream(canones)
				.mapToObj(canon -> riesgo(String.valueOf(ids.get() + 1000), canon))
				.toList();
		return PolizaVigente.emitir(1, new DatosPoliza(TipoPoliza.COLECTIVA, INMOBILIARIA, ENERO_2026, 12), riesgos,
				ids::getAndIncrement);
	}

	private static PolizaVigente vigente(Endoso endoso) {
		return new PolizaVigente(endoso.poliza(), endoso.riesgosVigentes());
	}

	private static Persona arrendatario(String documento) {
		return new Persona("CC", documento, "Arrendatario " + documento, null, null);
	}

	private static DatosRiesgo riesgo(String documentoArrendatario, long canon) {
		return new DatosRiesgo(new Inmueble("Calle 100 # 10-20", "Bogotá"), arrendatario(documentoArrendatario),
				new Persona("CC", "79000000", "Pedro Arrendador", null, null), BigDecimal.valueOf(canon));
	}

	/** Verifica el endoso y el control del Módulo 1: prima de la póliza = suma de las primas de sus riesgos. */
	private static void verificar(Endoso endoso, int numero, TipoEndoso tipo, ClaseMovimiento clase,
			String primaEndoso, String prima, String canon) {
		Poliza poliza = endoso.poliza();
		assertThat(poliza.getNumEndoso()).isEqualTo(numero);
		assertThat(poliza.getTipoEndoso()).isEqualTo(tipo);
		assertThat(poliza.getClaseMovimiento()).isEqualTo(clase);
		assertThat(poliza.getPrimaEndoso()).isEqualByComparingTo(primaEndoso);
		assertThat(poliza.getPrima()).isEqualByComparingTo(prima);
		assertThat(poliza.getCanon()).isEqualByComparingTo(canon);

		BigDecimal primaRiesgos = endoso.riesgosVigentes().stream()
				.filter(riesgo -> riesgo.getFechaExclusion() == null
						|| !riesgo.getFechaExclusion().isBefore(poliza.getInicioVigencia()))
				.map(Riesgo::getPrima)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		assertThat(primaRiesgos).as("control de prima").isEqualByComparingTo(poliza.getPrima());
	}
}
