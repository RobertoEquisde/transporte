package com.adavec.transporte.dto;

import lombok.Data;

@Data
public class ActualizarCobroRequest {
    private double tarifaUnica;
    private double cuotaAsociacion;
    private double fondoEstrella;
}
