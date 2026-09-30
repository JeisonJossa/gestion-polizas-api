package com.pruebatecnica.polizas.domain;

import jakarta.persistence.Embeddable;

@Embeddable
public record Inmueble(String direccion, String ciudad) {
}
