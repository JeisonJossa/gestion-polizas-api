package com.pruebatecnica.polizas.domain;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Tabla AVISO del Módulo 1 (outbox). Cada endoso guarda, en la misma transacción, un aviso para el CORE y, en la
 * emisión y la renovación, otro para el Servicio de Notificaciones. El procesador de avisos los entrega después y los
 * reintenta con el mismo id si el destino no responde.
 */
@Entity
@Table(name = "aviso")
public class Aviso {

	public static final String EVENTO_CORE = "ACTUALIZACION";
	public static final String POLIZA_CREADA = "POLIZA_CREADA";
	public static final String POLIZA_RENOVADA = "POLIZA_RENOVADA";

	private static final int MAX_ERROR = 500;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long polizaId;

	private Integer numEndoso;

	@Enumerated(EnumType.STRING)
	private DestinoAviso destino;

	private String evento;

	@Enumerated(EnumType.STRING)
	private EstadoAviso estado;

	private Integer intentos;

	private String ultimoError;

	/** Cuándo se creó el aviso, junto con su endoso. */
	private LocalDateTime fecha;

	protected Aviso() {
	}

	private Aviso(Poliza endoso, DestinoAviso destino, String evento, LocalDateTime fecha) {
		this.polizaId = endoso.getPolizaId();
		this.numEndoso = endoso.getNumEndoso();
		this.destino = destino;
		this.evento = evento;
		this.estado = EstadoAviso.PENDIENTE;
		this.intentos = 0;
		this.fecha = fecha;
	}

	/** Avisos que genera un endoso: siempre uno al CORE, y en la emisión y la renovación uno para notificar. */
	public static List<Aviso> de(Poliza endoso, LocalDateTime fecha) {
		Aviso core = new Aviso(endoso, DestinoAviso.CORE, EVENTO_CORE, fecha);
		return switch (endoso.getTipoEndoso()) {
			case EMISION -> List.of(core, new Aviso(endoso, DestinoAviso.NOTIFICACION, POLIZA_CREADA, fecha));
			case RENOVACION -> List.of(core,
					new Aviso(endoso, DestinoAviso.NOTIFICACION, POLIZA_RENOVADA, fecha));
			default -> List.of(core);
		};
	}

	public void marcarSincronizado() {
		this.intentos++;
		this.estado = EstadoAviso.SINCRONIZADO;
		this.ultimoError = null;
	}

	/** El destino no respondió: queda pendiente para el siguiente reintento. */
	public void registrarFallo(String error) {
		this.intentos++;
		this.ultimoError = recortar(error);
	}

	/** El CORE rechazó la operación: no se reintenta y queda para revisión. */
	public void marcarRechazado(String error) {
		this.intentos++;
		this.estado = EstadoAviso.RECHAZADO;
		this.ultimoError = recortar(error);
	}

	private static String recortar(String error) {
		return error == null || error.length() <= MAX_ERROR ? error : error.substring(0, MAX_ERROR);
	}

	public Long getId() {
		return id;
	}

	public Long getPolizaId() {
		return polizaId;
	}

	public Integer getNumEndoso() {
		return numEndoso;
	}

	public DestinoAviso getDestino() {
		return destino;
	}

	public String getEvento() {
		return evento;
	}

	public EstadoAviso getEstado() {
		return estado;
	}

	public Integer getIntentos() {
		return intentos;
	}
}
