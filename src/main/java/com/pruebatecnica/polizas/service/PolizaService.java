package com.pruebatecnica.polizas.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pruebatecnica.polizas.domain.Endoso;
import com.pruebatecnica.polizas.domain.EstadoPoliza;
import com.pruebatecnica.polizas.domain.Ipc;
import com.pruebatecnica.polizas.domain.PolizaVigente;
import com.pruebatecnica.polizas.domain.TipoPoliza;
import com.pruebatecnica.polizas.dto.CrearPolizaRequest;
import com.pruebatecnica.polizas.dto.PolizaResponse;
import com.pruebatecnica.polizas.dto.RiesgoRequest;
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
	private final Clock reloj;

	public PolizaService(PolizaRepository polizas, RiesgoRepository riesgos, IpcRepository ipcs,
			RegistroEndosos registro, Clock reloj) {
		this.polizas = polizas;
		this.riesgos = riesgos;
		this.ipcs = ipcs;
		this.registro = registro;
		this.reloj = reloj;
	}

	@Transactional(readOnly = true)
	public List<PolizaResponse> listar(TipoPoliza tipo, EstadoPoliza estado) {
		return polizas.buscarVigentes(tipo, estado).stream().map(PolizaResponse::de).toList();
	}

	@Transactional
	public PolizaResponse crear(CrearPolizaRequest solicitud) {
		Endoso emision = PolizaVigente.emitir(polizas.siguienteId(), solicitud.aDominio(),
				solicitud.riesgos().stream().map(RiesgoRequest::aDominio).toList(), riesgos::siguienteId);
		registro.guardar(emision);
		return PolizaResponse.de(emision.poliza(), emision.riesgosVigentes());
	}

	@Transactional
	public PolizaResponse renovar(long polizaId) {
		PolizaVigente poliza = registro.cargar(polizaId);
		poliza.exigirRenovable();
		int anio = poliza.anioIpcParaRenovar();
		Ipc ipc = ipcs.findById(anio).orElseThrow(() -> new ReglaNegocioException(
				"No hay IPC cargado para " + anio + "; la póliza " + polizaId + " se renueva cuando se publique."));
		Endoso renovacion = registro.guardar(poliza.renovar(ipc.getPorcentaje()));
		return PolizaResponse.de(renovacion.poliza(), renovacion.riesgosVigentes());
	}

	@Transactional
	public PolizaResponse cancelar(long polizaId) {
		Endoso cancelacion = registro.guardar(registro.cargar(polizaId).cancelar(LocalDate.now(reloj)));
		return PolizaResponse.de(cancelacion.poliza(), cancelacion.riesgosVigentes());
	}
}
