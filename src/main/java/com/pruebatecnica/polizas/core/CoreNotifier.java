package com.pruebatecnica.polizas.core;

import com.pruebatecnica.polizas.domain.TipoEndoso;

/** Puerto hacia el servicio agnóstico de edición, que mantiene actualizado el CORE. */
public interface CoreNotifier {

	void notificarActualizacion(long polizaId, int numEndoso, TipoEndoso tipoEndoso);
}
