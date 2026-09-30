package com.pruebatecnica.polizas.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** La fecha de los movimientos sale de este reloj; las pruebas lo reemplazan por uno fijo. */
@Configuration
public class RelojConfig {

	@Bean
	public Clock reloj() {
		return Clock.system(ZoneId.of("America/Bogota"));
	}
}
