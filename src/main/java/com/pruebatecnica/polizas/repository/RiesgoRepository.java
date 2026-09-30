package com.pruebatecnica.polizas.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.pruebatecnica.polizas.domain.Riesgo;
import com.pruebatecnica.polizas.domain.RiesgoId;

public interface RiesgoRepository extends JpaRepository<Riesgo, RiesgoId> {

	/** Filas vigentes de los riesgos de una póliza (con vigente = true), una por riesgo. */
	List<Riesgo> findByPolizaIdAndVigenteOrderByCodRiesgo(Long polizaId, Boolean vigente);

	Optional<Riesgo> findFirstByRiesgoIdAndVigente(Long riesgoId, Boolean vigente);

	@Query(value = "SELECT NEXT VALUE FOR seq_riesgo", nativeQuery = true)
	Long siguienteId();
}
