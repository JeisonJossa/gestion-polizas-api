package com.pruebatecnica.polizas.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Renovar una póliza (RENOVACION) o la renovación automática del día (RENOVACION_AUTOMATICA). Solo lleva el proceso:
 * el IPC lo toma el API; en la automática, la fecha del movimiento es la fecha de corte.
 */
public record RenovacionRequest(@NotNull(message = "es obligatorio") @Valid ProcesoRequest proceso) {
}
