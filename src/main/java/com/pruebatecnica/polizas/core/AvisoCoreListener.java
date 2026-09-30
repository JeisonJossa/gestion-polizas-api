package com.pruebatecnica.polizas.core;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.pruebatecnica.polizas.service.EndosoRegistrado;

/**
 * Después de que el endoso y sus avisos quedaron guardados, le pide al procesador que los entregue de una vez. Si el
 * guardado falla, no hay avisos: el CORE nunca recibe el aviso de algo que no existe.
 */
@Component
public class AvisoCoreListener {

	private final ProcesadorAvisos procesador;

	public AvisoCoreListener(ProcesadorAvisos procesador) {
		this.procesador = procesador;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void alRegistrarEndoso(EndosoRegistrado endoso) {
		procesador.procesarPoliza(endoso.polizaId());
	}
}
