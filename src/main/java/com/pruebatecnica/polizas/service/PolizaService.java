package com.pruebatecnica.polizas.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pruebatecnica.polizas.domain.DatosRiesgo;
import com.pruebatecnica.polizas.domain.Endoso;
import com.pruebatecnica.polizas.domain.EstadoPoliza;
import com.pruebatecnica.polizas.domain.Ipc;
import com.pruebatecnica.polizas.domain.Persona;
import com.pruebatecnica.polizas.domain.PolizaVigente;
import com.pruebatecnica.polizas.domain.TipoPoliza;
import com.pruebatecnica.polizas.dto.CancelacionRequest;
import com.pruebatecnica.polizas.dto.EmisionRequest;
import com.pruebatecnica.polizas.dto.EndosoResultado;
import com.pruebatecnica.polizas.dto.PolizaConsulta;
import com.pruebatecnica.polizas.dto.ProcesoRequest;
import com.pruebatecnica.polizas.exception.ReglaNegocioException;
import com.pruebatecnica.polizas.repository.IpcRepository;
import com.pruebatecnica.polizas.repository.PolizaRepository;
import com.pruebatecnica.polizas.repository.RiesgoRepository;

@Service
public class PolizaService {

	private final PolizaRepository polizas;
	private final RiesgoRepository riesgos;
	private final IpcRepository ipcs;
	private final RegistroEndosos registro;
	private final RegistroPersonas personas;
	private final Clock reloj;

	public PolizaService(PolizaRepository polizas, RiesgoRepository riesgos, IpcRepository ipcs,
			RegistroEndosos registro, RegistroPersonas personas, Clock reloj) {
		this.polizas = polizas;
		this.riesgos = riesgos;
		this.ipcs = ipcs;
		this.registro = registro;
		this.personas = personas;
		this.reloj = reloj;
	}

	@Transactional(readOnly = true)
	public List<PolizaConsulta> listar(TipoPoliza tipo, EstadoPoliza estado) {
		return polizas.buscarVigentes(tipo, estado).stream().map(PolizaConsulta::de).toList();
	}

	/** Endoso 0: la fecha es el inicio de la vigencia; el tomador y las personas de los riesgos quedan en PERSONA. */
	@Transactional
	public EndosoResultado emitir(EmisionRequest solicitud) {
		Persona tomador = personas.registrar(solicitud.poliza().tomador());
		List<DatosRiesgo> datosRiesgos = solicitud.riesgos().stream().map(personas::datosRiesgo).toList();
		Endoso emision = PolizaVigente.emitir(polizas.siguienteId(), solicitud.poliza().aDominio(tomador), datosRiesgos,
				riesgos::siguienteId);
		return EndosoResultado.de(registro.guardar(emision, solicitud.proceso(), null));
	}

	@Transactional
	public EndosoResultado renovar(long polizaId, ProcesoRequest proceso, String motivo) {
		PolizaVigente poliza = registro.cargar(polizaId);
		poliza.exigirRenovable();
		int anio = poliza.anioIpcParaRenovar();
		Ipc ipc = ipcs.findById(anio).orElseThrow(() -> new ReglaNegocioException(
				"No hay IPC cargado para " + anio + "; la póliza " + polizaId + " se renueva cuando se publique."));
		return EndosoResultado.de(registro.guardar(poliza.renovar(ipc.getPorcentaje()), proceso, motivo));
	}

	@Transactional
	public EndosoResultado cancelar(long polizaId, CancelacionRequest solicitud) {
		LocalDate fecha = solicitud.proceso().fechaMovimientoO(LocalDate.now(reloj));
		Endoso cancelacion = registro.cargar(polizaId).cancelar(fecha);
		return EndosoResultado.de(registro.guardar(cancelacion, solicitud.proceso(), solicitud.motivo()));
	}
}
