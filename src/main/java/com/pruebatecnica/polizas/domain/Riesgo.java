package com.pruebatecnica.polizas.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.domain.Persistable;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Fila de un riesgo (un inmueble arrendado) escrita en un endoso. En cada endoso solo se escriben los riesgos que
 * cambian: la fila nueva queda vigente y la anterior deja de serlo.
 */
@Entity
@Table(name = "riesgo")
@IdClass(RiesgoId.class)
public class Riesgo implements Persistable<RiesgoId> {

	@Id
	private Long polizaId;

	@Id
	private Integer numEndoso;

	@Id
	private Integer codRiesgo;

	/** Identificador del riesgo en el API; es el mismo en todas sus filas. */
	private Long riesgoId;

	@Embedded
	@AttributeOverride(name = "direccion", column = @Column(name = "inmueble_direccion"))
	@AttributeOverride(name = "ciudad", column = @Column(name = "inmueble_ciudad"))
	private Inmueble inmueble;

	@Embedded
	@AttributeOverride(name = "tipoDocumento", column = @Column(name = "arrendatario_tipo_documento"))
	@AttributeOverride(name = "numeroDocumento", column = @Column(name = "arrendatario_numero_documento"))
	@AttributeOverride(name = "nombre", column = @Column(name = "arrendatario_nombre"))
	@AttributeOverride(name = "correo", column = @Column(name = "arrendatario_correo"))
	@AttributeOverride(name = "celular", column = @Column(name = "arrendatario_celular"))
	private Persona arrendatario;

	@Embedded
	@AttributeOverride(name = "tipoDocumento", column = @Column(name = "arrendador_tipo_documento"))
	@AttributeOverride(name = "numeroDocumento", column = @Column(name = "arrendador_numero_documento"))
	@AttributeOverride(name = "nombre", column = @Column(name = "arrendador_nombre"))
	@AttributeOverride(name = "correo", column = @Column(name = "arrendador_correo"))
	@AttributeOverride(name = "celular", column = @Column(name = "arrendador_celular"))
	private Persona arrendador;

	private BigDecimal canon;

	/** Lo que paga el riesgo en la vigencia actual: su canon por los meses que cubre. */
	private BigDecimal prima;

	/** Lo que cambió su prima en este endoso. */
	private BigDecimal primaEndoso;

	private LocalDate fechaInclusion;

	private LocalDate fechaExclusion;

	@Enumerated(EnumType.STRING)
	private EstadoRiesgo estado;

	@Convert(converter = SiNoConverter.class)
	private Boolean vigente;

	@Transient
	private boolean nuevo;

	protected Riesgo() {
	}

	private Riesgo(Long polizaId, Integer numEndoso, Integer codRiesgo, Long riesgoId, Inmueble inmueble,
			Persona arrendatario, Persona arrendador, BigDecimal canon, BigDecimal prima, BigDecimal primaEndoso,
			LocalDate fechaInclusion, LocalDate fechaExclusion, EstadoRiesgo estado) {
		this.polizaId = polizaId;
		this.numEndoso = numEndoso;
		this.codRiesgo = codRiesgo;
		this.riesgoId = riesgoId;
		this.inmueble = inmueble;
		this.arrendatario = arrendatario;
		this.arrendador = arrendador;
		this.canon = canon;
		this.prima = prima;
		this.primaEndoso = primaEndoso;
		this.fechaInclusion = fechaInclusion;
		this.fechaExclusion = fechaExclusion;
		this.estado = estado;
		this.vigente = true;
		this.nuevo = true;
	}

	/** Riesgo nuevo: paga su canon por los meses que cubre. */
	static Riesgo incluir(long riesgoId, long polizaId, int numEndoso, int codRiesgo, DatosRiesgo datos, int meses,
			LocalDate fechaInclusion) {
		BigDecimal canon = Dinero.redondear(datos.canon());
		BigDecimal prima = Dinero.porMeses(canon, meses);
		return new Riesgo(polizaId, numEndoso, codRiesgo, riesgoId, datos.inmueble(), datos.arrendatario(),
				datos.arrendador(), canon, prima, prima, fechaInclusion, null, EstadoRiesgo.ACTIVO);
	}

	/** Fila de cancelación: se devuelven los meses que faltan de la vigencia. */
	Riesgo cancelarEn(int numEndoso, LocalDate fecha, int mesesRestantes) {
		BigDecimal devolucion = Dinero.porMeses(canon, mesesRestantes);
		return new Riesgo(polizaId, numEndoso, codRiesgo, riesgoId, inmueble, arrendatario, arrendador, canon,
				Dinero.redondear(prima.subtract(devolucion)), devolucion.negate(), fechaInclusion, fecha,
				EstadoRiesgo.CANCELADO);
	}

	/** Fila de renovación: canon ajustado con el IPC y prima de la nueva vigencia completa. */
	Riesgo renovarEn(int numEndoso, BigDecimal factorIpc, int meses) {
		BigDecimal nuevoCanon = Dinero.redondear(canon.multiply(factorIpc));
		BigDecimal nuevaPrima = Dinero.porMeses(nuevoCanon, meses);
		return new Riesgo(polizaId, numEndoso, codRiesgo, riesgoId, inmueble, arrendatario, arrendador, nuevoCanon,
				nuevaPrima, nuevaPrima, fechaInclusion, null, EstadoRiesgo.ACTIVO);
	}

	/** La fila anterior de un riesgo que cambió en un endoso nuevo. */
	void dejarDeSerVigente() {
		this.vigente = false;
	}

	@PostLoad
	@PostPersist
	void marcarGuardado() {
		this.nuevo = false;
	}

	@Override
	public RiesgoId getId() {
		return new RiesgoId(polizaId, numEndoso, codRiesgo);
	}

	@Override
	public boolean isNew() {
		return nuevo;
	}

	public boolean estaActivo() {
		return estado == EstadoRiesgo.ACTIVO;
	}

	public Long getPolizaId() {
		return polizaId;
	}

	public Integer getNumEndoso() {
		return numEndoso;
	}

	public Integer getCodRiesgo() {
		return codRiesgo;
	}

	public Long getRiesgoId() {
		return riesgoId;
	}

	public Inmueble getInmueble() {
		return inmueble;
	}

	public Persona getArrendatario() {
		return arrendatario;
	}

	public Persona getArrendador() {
		return arrendador;
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

	public LocalDate getFechaInclusion() {
		return fechaInclusion;
	}

	public LocalDate getFechaExclusion() {
		return fechaExclusion;
	}

	public EstadoRiesgo getEstado() {
		return estado;
	}

	public boolean isVigente() {
		return Boolean.TRUE.equals(vigente);
	}
}
