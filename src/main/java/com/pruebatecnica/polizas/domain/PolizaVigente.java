package com.pruebatecnica.polizas.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.LongSupplier;

import com.pruebatecnica.polizas.exception.EstadoInvalidoException;
import com.pruebatecnica.polizas.exception.RecursoNoEncontradoException;
import com.pruebatecnica.polizas.exception.ReglaNegocioException;

/**
 * La póliza tal como está hoy: su último endoso y las filas vigentes de sus riesgos. Aquí viven las reglas de
 * negocio; cada operación devuelve el endoso nuevo sin tocar la base de datos.
 *
 * <p>Totalización de la prima (Módulo 1):
 * <ul>
 * <li>prima del endoso = suma de las primas del endoso de los riesgos escritos en él;</li>
 * <li>prima de la póliza = prima anterior + prima del endoso (en la renovación arranca con la del periodo nuevo);</li>
 * <li>canon de la póliza = suma de los cánones de los riesgos activos.</li>
 * </ul>
 */
public final class PolizaVigente {

	private final Poliza poliza;
	private final List<Riesgo> riesgos;

	public PolizaVigente(Poliza poliza, List<Riesgo> riesgosVigentes) {
		this.poliza = poliza;
		this.riesgos = List.copyOf(riesgosVigentes);
	}

	/** Endoso 0. La individual lleva exactamente un riesgo y su tomador es el arrendatario. */
	public static Endoso emitir(long polizaId, DatosPoliza datos, List<DatosRiesgo> riesgos, LongSupplier idRiesgo) {
		if (datos.tipo() == TipoPoliza.INDIVIDUAL && riesgos.size() != 1) {
			throw new ReglaNegocioException("Una póliza individual solo puede tener 1 riesgo; se recibieron "
					+ riesgos.size() + ".");
		}
		if (riesgos.isEmpty()) {
			throw new ReglaNegocioException("Una póliza colectiva debe nacer con al menos 1 riesgo.");
		}
		if (datos.tipo() == TipoPoliza.INDIVIDUAL && !riesgos.get(0).arrendatario().esLaMismaQue(datos.tomador())) {
			throw new ReglaNegocioException(
					"En una póliza individual el tomador debe ser el arrendatario del riesgo (mismo documento).");
		}
		List<Riesgo> escritos = new ArrayList<>();
		int codRiesgo = 1;
		for (DatosRiesgo datosRiesgo : riesgos) {
			escritos.add(Riesgo.incluir(idRiesgo.getAsLong(), polizaId, 0, codRiesgo++, datosRiesgo,
					datos.mesesVigencia(), datos.inicioVigencia()));
		}
		BigDecimal canon = Dinero.sumar(escritos, Riesgo::getCanon);
		BigDecimal prima = Dinero.sumar(escritos, Riesgo::getPrimaEndoso);
		return new Endoso(Poliza.emision(polizaId, datos, canon, prima), escritos, escritos);
	}

	/** Inclusión: solo en colectivas no canceladas; el riesgo paga los meses que faltan de la vigencia. */
	public Endoso agregarRiesgo(DatosRiesgo datos, LocalDate fecha, LongSupplier idRiesgo) {
		exigirNoCancelada("agregarle riesgos");
		if (poliza.getTipo() != TipoPoliza.COLECTIVA) {
			throw new ReglaNegocioException("Solo se pueden agregar riesgos a pólizas colectivas; la póliza "
					+ poliza.getPolizaId() + " es individual.");
		}
		int meses = mesesRestantes(fecha);
		if (meses == 0) {
			throw new ReglaNegocioException("La vigencia de la póliza " + poliza.getPolizaId()
					+ " terminó el " + poliza.getFinVigencia() + "; renuévela antes de agregar riesgos.");
		}
		int codRiesgo = riesgos.stream().mapToInt(Riesgo::getCodRiesgo).max().orElse(0) + 1;
		Riesgo nuevo = Riesgo.incluir(idRiesgo.getAsLong(), poliza.getPolizaId(), siguienteEndoso(), codRiesgo,
				datos, meses, fecha);
		return registrar(TipoEndoso.INCLUSION, fecha, poliza.getEstado(), poliza.getInicioVigencia(),
				poliza.getFinVigencia(), List.of(nuevo), List.of(), false);
	}

	/** Exclusión: solo en colectivas; se devuelven los meses que faltan. La póliza sigue vigente aunque quede vacía. */
	public Endoso cancelarRiesgo(long riesgoId, LocalDate fecha) {
		exigirNoCancelada("cancelarle riesgos");
		if (poliza.getTipo() != TipoPoliza.COLECTIVA) {
			throw new ReglaNegocioException(
					"Los riesgos de una póliza individual no se cancelan por separado; cancele la póliza.");
		}
		Riesgo actual = riesgos.stream()
				.filter(riesgo -> riesgo.getRiesgoId() == riesgoId)
				.findFirst()
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el riesgo " + riesgoId + "."));
		if (!actual.estaActivo()) {
			throw new EstadoInvalidoException("El riesgo " + riesgoId + " ya está cancelado.");
		}
		Riesgo cancelado = actual.cancelarEn(siguienteEndoso(), fecha, mesesRestantes(fecha));
		return registrar(TipoEndoso.EXCLUSION, fecha, poliza.getEstado(), poliza.getInicioVigencia(),
				poliza.getFinVigencia(), List.of(cancelado), List.of(actual), false);
	}

	public void exigirRenovable() {
		if (poliza.estaCancelada()) {
			throw new EstadoInvalidoException("No se puede renovar la póliza " + poliza.getPolizaId()
					+ " porque está cancelada.");
		}
	}

	/** Año del IPC que aplica: el anterior al inicio de la nueva vigencia. */
	public int anioIpcParaRenovar() {
		return poliza.getFinVigencia().plusDays(1).getYear() - 1;
	}

	/** Renovación: misma duración, canon + IPC en cada riesgo activo y la prima arranca con la del periodo nuevo. */
	public Endoso renovar(BigDecimal porcentajeIpc) {
		exigirRenovable();
		int meses = poliza.getMesesVigencia();
		LocalDate inicio = poliza.getFinVigencia().plusDays(1);
		BigDecimal factor = BigDecimal.ONE.add(porcentajeIpc.movePointLeft(2));
		int endoso = siguienteEndoso();
		List<Riesgo> activos = riesgos.stream().filter(Riesgo::estaActivo).toList();
		List<Riesgo> renovados = activos.stream().map(riesgo -> riesgo.renovarEn(endoso, factor, meses)).toList();
		return registrar(TipoEndoso.RENOVACION, inicio, EstadoPoliza.RENOVADA, inicio, Vigencia.fin(inicio, meses),
				renovados, activos, true);
	}

	/** Cancelación: cancela todos los riesgos activos y devuelve los meses que faltan. */
	public Endoso cancelar(LocalDate fecha) {
		if (poliza.estaCancelada()) {
			throw new EstadoInvalidoException("La póliza " + poliza.getPolizaId() + " ya está cancelada.");
		}
		int meses = mesesRestantes(fecha);
		int endoso = siguienteEndoso();
		List<Riesgo> activos = riesgos.stream().filter(Riesgo::estaActivo).toList();
		List<Riesgo> cancelados = activos.stream().map(riesgo -> riesgo.cancelarEn(endoso, fecha, meses)).toList();
		return registrar(TipoEndoso.CANCELACION, fecha, EstadoPoliza.CANCELADA, poliza.getInicioVigencia(),
				poliza.getFinVigencia(), cancelados, activos, false);
	}

	public Poliza poliza() {
		return poliza;
	}

	public List<Riesgo> riesgos() {
		return riesgos;
	}

	private Endoso registrar(TipoEndoso tipo, LocalDate fecha, EstadoPoliza estado, LocalDate inicio, LocalDate fin,
			List<Riesgo> escritos, List<Riesgo> reemplazados, boolean vigenciaNueva) {
		reemplazados.forEach(Riesgo::dejarDeSerVigente);
		List<Riesgo> vigentes = new ArrayList<>(riesgos);
		vigentes.removeAll(reemplazados);
		vigentes.addAll(escritos);
		vigentes.sort(Comparator.comparing(Riesgo::getCodRiesgo));

		BigDecimal primaEndoso = Dinero.sumar(escritos, Riesgo::getPrimaEndoso);
		BigDecimal canon = Dinero.sumar(vigentes.stream().filter(Riesgo::estaActivo).toList(), Riesgo::getCanon);
		BigDecimal prima = vigenciaNueva ? primaEndoso : Dinero.redondear(poliza.getPrima().add(primaEndoso));
		Poliza nueva = poliza.siguienteEndoso(tipo, fecha, estado, inicio, fin, canon, prima, primaEndoso);
		return new Endoso(nueva, escritos, List.copyOf(vigentes));
	}

	private void exigirNoCancelada(String accion) {
		if (poliza.estaCancelada()) {
			throw new EstadoInvalidoException("La póliza " + poliza.getPolizaId() + " está cancelada; no se puede "
					+ accion + ".");
		}
	}

	private int mesesRestantes(LocalDate fecha) {
		return Vigencia.mesesRestantes(poliza.getInicioVigencia(), poliza.getMesesVigencia(), fecha);
	}

	private int siguienteEndoso() {
		return poliza.getNumEndoso() + 1;
	}
}
