package com.pruebatecnica.polizas.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.domain.Persistable;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Una fila por endoso de la póliza. El endoso 0 es la emisión y cada cambio agrega una fila con el número
 * siguiente, así que las filas no se modifican: la de mayor número de endoso es la póliza vigente.
 */
@Entity
@Table(name = "poliza")
@IdClass(PolizaId.class)
public class Poliza implements Persistable<PolizaId> {

	@Id
	private Long polizaId;

	@Id
	private Integer numEndoso;

	private String numeroPoliza;

	/** Número de la póliza en el CORE. Lo asigna el CORE al registrarla; el mock del Módulo 2 no lo devuelve. */
	private String numeroCore;

	@Enumerated(EnumType.STRING)
	private TipoPoliza tipo;

	@Enumerated(EnumType.STRING)
	private EstadoPoliza estado;

	@Enumerated(EnumType.STRING)
	private TipoEndoso tipoEndoso;

	@Enumerated(EnumType.STRING)
	private ClaseMovimiento claseMovimiento;

	private LocalDate inicioVigencia;

	private LocalDate finVigencia;

	private Integer mesesVigencia;

	/** Desde cuándo aplica el cambio que registra este endoso. */
	private LocalDate fechaEndoso;

	@ManyToOne(optional = false)
	@JoinColumn(name = "tomador_id")
	private Persona tomador;

	/** Suma de los cánones de los riesgos activos. */
	private BigDecimal canon;

	/** Prima de la vigencia actual después de este endoso. */
	private BigDecimal prima;

	/** Lo que movió este endoso: positivo cobra, negativo devuelve. */
	private BigDecimal primaEndoso;

	/** Quién originó el endoso: canal, usuario y motivo que llegaron en la petición. */
	private String canal;

	private String usuario;

	private String motivo;

	@Transient
	private boolean nueva;

	protected Poliza() {
	}

	private Poliza(Long polizaId, Integer numEndoso, String numeroPoliza, TipoPoliza tipo, EstadoPoliza estado,
			TipoEndoso tipoEndoso, LocalDate inicioVigencia, LocalDate finVigencia, Integer mesesVigencia,
			LocalDate fechaEndoso, Persona tomador, BigDecimal canon, BigDecimal prima, BigDecimal primaEndoso) {
		this.polizaId = polizaId;
		this.numEndoso = numEndoso;
		this.numeroPoliza = numeroPoliza;
		this.tipo = tipo;
		this.estado = estado;
		this.tipoEndoso = tipoEndoso;
		this.claseMovimiento = ClaseMovimiento.de(primaEndoso);
		this.inicioVigencia = inicioVigencia;
		this.finVigencia = finVigencia;
		this.mesesVigencia = mesesVigencia;
		this.fechaEndoso = fechaEndoso;
		this.tomador = tomador;
		this.canon = canon;
		this.prima = prima;
		this.primaEndoso = primaEndoso;
		this.nueva = true;
	}

	/** Endoso 0: la emisión cobra la prima completa. */
	static Poliza emision(long polizaId, DatosPoliza datos, BigDecimal canon, BigDecimal prima) {
		LocalDate inicio = datos.inicioVigencia();
		return new Poliza(polizaId, 0, "POL-" + polizaId, datos.tipo(), EstadoPoliza.VIGENTE, TipoEndoso.EMISION,
				inicio, Vigencia.fin(inicio, datos.mesesVigencia()), datos.mesesVigencia(), inicio, datos.tomador(),
				canon, prima, prima);
	}

	/** Copia esta fila como el endoso siguiente, con lo que cambia. */
	Poliza siguienteEndoso(TipoEndoso tipoEndoso, LocalDate fecha, EstadoPoliza estado, LocalDate inicio,
			LocalDate fin, BigDecimal canon, BigDecimal prima, BigDecimal primaEndoso) {
		Poliza siguiente = new Poliza(polizaId, numEndoso + 1, numeroPoliza, tipo, estado, tipoEndoso, inicio, fin,
				mesesVigencia, fecha, tomador, canon, prima, primaEndoso);
		siguiente.numeroCore = numeroCore;
		return siguiente;
	}

	/** Registra de dónde vino el endoso. Solo aplica a la fila nueva, antes de guardarla. */
	public void registrarOrigen(String canal, String usuario, String motivo) {
		if (!nueva) {
			throw new IllegalStateException("El origen solo se registra en un endoso nuevo.");
		}
		this.canal = canal;
		this.usuario = usuario;
		this.motivo = motivo;
	}

	@PostLoad
	@PostPersist
	void marcarGuardada() {
		this.nueva = false;
	}

	@Override
	public PolizaId getId() {
		return new PolizaId(polizaId, numEndoso);
	}

	@Override
	public boolean isNew() {
		return nueva;
	}

	public boolean estaCancelada() {
		return estado == EstadoPoliza.CANCELADA;
	}

	public Long getPolizaId() {
		return polizaId;
	}

	public Integer getNumEndoso() {
		return numEndoso;
	}

	public String getNumeroPoliza() {
		return numeroPoliza;
	}

	public TipoPoliza getTipo() {
		return tipo;
	}

	public EstadoPoliza getEstado() {
		return estado;
	}

	public TipoEndoso getTipoEndoso() {
		return tipoEndoso;
	}

	public ClaseMovimiento getClaseMovimiento() {
		return claseMovimiento;
	}

	public LocalDate getInicioVigencia() {
		return inicioVigencia;
	}

	public LocalDate getFinVigencia() {
		return finVigencia;
	}

	public Integer getMesesVigencia() {
		return mesesVigencia;
	}

	public LocalDate getFechaEndoso() {
		return fechaEndoso;
	}

	public Persona getTomador() {
		return tomador;
	}

	public BigDecimal getCanon() {
		return canon;
	}

	public BigDecimal getPrima() {
		return prima;
	}

	public BigDecimal getPrimaEndoso() {
		return primaEndoso;
	}
}
