package com.pruebatecnica.polizas.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.pruebatecnica.polizas.dto.EventoCoreRequest;

import jakarta.validation.Valid;

/** Simula el servicio agnóstico de edición: su único propósito es dejar en el log que se intentó avisar al CORE. */
@RestController
@RequestMapping("/core-mock")
public class CoreMockController {

	private static final Logger log = LoggerFactory.getLogger(CoreMockController.class);

	@PostMapping("/evento")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void registrar(@Valid @RequestBody EventoCoreRequest evento) {
		log.info("[CORE-MOCK] Operacion enviada al CORE: evento={}, polizaId={}", evento.evento(), evento.polizaId());
	}
}
