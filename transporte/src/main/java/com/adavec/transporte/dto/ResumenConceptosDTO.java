package com.adavec.transporte.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;
@Data
@Builder
public class ResumenConceptosDTO {
    private Integer unidadId;
    private String unidadNoSerie;
    private Map<String, Double> montosPorConcepto;
    private Map<String, Double> montosConIvaPorConcepto;
    private Double totalSinIva;
    private Double totalConIva;
    private Integer cantidadConceptos;
}

