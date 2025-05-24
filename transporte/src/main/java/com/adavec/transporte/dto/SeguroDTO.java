package com.adavec.transporte.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class SeguroDTO {
    private Integer id;
    private String factura;
    private Double valorSeguro;
    private Double cuotaSeguro; // Nuevo campo
    private Double seguroDistribuidor;
    private LocalDate cuotaFactura; // Nuevo campo (tipo fecha)
    private Integer unidadId;
    private Integer distribuidorId;
}
