package com.pruebatecnica.polizas.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pruebatecnica.polizas.domain.Persona;

public interface PersonaRepository extends JpaRepository<Persona, Long> {

	Optional<Persona> findByTipoDocumentoAndNumeroDocumento(String tipoDocumento, String numeroDocumento);
}
