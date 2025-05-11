package com.adavec.transporte.dto;

import lombok.Data;

@Data
public class ReporteFinancieroDTO {
    private String noSerie;
    private String modelo;
    private String uso;
    private String distribuidora;
    private Double valorUnidad;
    private Double tarifaUnica;
    private Double cuotaSeguro;
    private Double valorSeguro;
    private Double fondoEstrella;
    private String fechaFactura;
}
