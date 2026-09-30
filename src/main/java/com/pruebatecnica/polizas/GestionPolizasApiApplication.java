package com.pruebatecnica.polizas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** La programación de tareas reintenta los avisos pendientes (ProcesadorAvisos). */
@SpringBootApplication
@EnableScheduling
public class GestionPolizasApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(GestionPolizasApiApplication.class, args);
	}

}
