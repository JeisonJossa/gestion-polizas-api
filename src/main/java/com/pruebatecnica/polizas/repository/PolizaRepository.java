package com.pruebatecnica.polizas.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pruebatecnica.polizas.domain.EstadoPoliza;
import com.pruebatecnica.polizas.domain.Poliza;
import com.pruebatecnica.polizas.domain.PolizaId;
import com.pruebatecnica.polizas.domain.TipoPoliza;

public interface PolizaRepository extends JpaRepository<Poliza, PolizaId> {

	/** Último endoso (la póliza vigente) de cada póliza, filtrado por tipo y estado cuando vienen. */
	@Query("""
			select p from Poliza p join fetch p.tomador
			where p.numEndoso = (select max(x.numEndoso) from Poliza x where x.polizaId = p.polizaId)
			  and (:tipo is null or p.tipo = :tipo)
			  and (:estado is null or p.estado = :estado)
			order by p.polizaId
			""")
	List<Poliza> buscarVigentes(@Param("tipo") TipoPoliza tipo, @Param("estado") EstadoPoliza estado);

	/** Pólizas por renovar: su último endoso no está cancelado y su vigencia termina a más tardar en la fecha dada. */
	@Query("""
			select p.polizaId from Poliza p
			where p.numEndoso = (select max(x.numEndoso) from Poliza x where x.polizaId = p.polizaId)
			  and p.estado <> com.pruebatecnica.polizas.domain.EstadoPoliza.CANCELADA
			  and p.finVigencia <= :fecha
			order by p.polizaId
			""")
	List<Long> buscarPorRenovar(@Param("fecha") LocalDate fecha);

	Optional<Poliza> findFirstByPolizaIdOrderByNumEndosoDesc(Long polizaId);

	@Query(value = "SELECT NEXT VALUE FOR seq_poliza", nativeQuery = true)
	Long siguienteId();
}
