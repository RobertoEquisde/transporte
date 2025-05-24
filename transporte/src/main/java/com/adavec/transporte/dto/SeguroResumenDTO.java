package com.adavec.transporte.dto;

import lombok.Data;

@Data
public class SeguroResumenDTO {
    private Integer id;
    private String factura;
    private Double valorSeguro;
    private Double seguroDistribuidor;
}
