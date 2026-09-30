package com.pruebatecnica.polizas.core;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.pruebatecnica.polizas.service.EndosoRegistrado;

/**
 * Avisa al CORE solo después de que el endoso quedó guardado: si el guardado falla, el CORE nunca recibe el aviso
 * de algo que no existe.
 */
@Component
public class AvisoCoreListener {

	private final CoreNotifier core;

	public AvisoCoreListener(CoreNotifier core) {
		this.core = core;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void alRegistrarEndoso(EndosoRegistrado endoso) {
		core.notificarActualizacion(endoso.polizaId(), endoso.numEndoso(), endoso.tipoEndoso());
	}
}
