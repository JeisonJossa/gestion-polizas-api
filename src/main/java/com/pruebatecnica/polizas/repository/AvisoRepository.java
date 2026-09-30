package com.pruebatecnica.polizas.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pruebatecnica.polizas.domain.Aviso;
import com.pruebatecnica.polizas.domain.EstadoAviso;

public interface AvisoRepository extends JpaRepository<Aviso, Long> {

	/** Avisos de una póliza en un estado, en orden de endoso: al CORE se envían en ese orden. */
	List<Aviso> findByPolizaIdAndEstadoOrderByNumEndosoAscIdAsc(Long polizaId, EstadoAviso estado);

	/** Todos los avisos en un estado, agrupados por póliza y en orden de endoso. */
	List<Aviso> findByEstadoOrderByPolizaIdAscNumEndosoAscIdAsc(EstadoAviso estado);
}
