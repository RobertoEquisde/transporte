package com.adavec.transporte.dto;

import lombok.Data;

@Data
public class ReporteFinancieroDTO {
    private String noSerie;
    private String modelo;
    private String distribuidora;
    private String claveDistribuidor;

    private String numeroFactura;
    private String fechaFactura;
    private String fechaInteres;
    private String fechaProceso;

    private Double valorUnidad;
    private Double importeTraslado;
    private Double fondoEstrella;
    private Double cuotaSeguro;       // 3.24%
    private Double seguro;            // 1.34%
    private Double cuotaAsociacion;
    private Integer dias;

}
