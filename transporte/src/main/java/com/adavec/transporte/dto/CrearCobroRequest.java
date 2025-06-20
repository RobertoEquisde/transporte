package com.adavec.transporte.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CrearCobroRequest {
    private Integer unidadId;
    private Double tarifaUnica;
    private Double cuotaAsociacion;
    private Double fondoEstrella;
    private Integer dias;
    private LocalDate fechaTraslado; // formato: yyyy-MM-dd
}