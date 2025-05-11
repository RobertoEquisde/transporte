package com.adavec.transporte.dto;

import lombok.Data;

@Data
public class CrearCobroRequest {
    private Integer unidadId;
    private Double tarifaUnica;
    private Double cuotaAsociacion;
    private Double fondoEstrella;
    private Integer dias;
    private String fechaTraslado; // formato: yyyy-MM-dd
}