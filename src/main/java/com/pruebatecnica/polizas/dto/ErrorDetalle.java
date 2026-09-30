package com.pruebatecnica.polizas.dto;

/** Un error de la respuesta: código estable para el cliente y el detalle legible. */
public record ErrorDetalle(String codigo, String detalle) {
}
