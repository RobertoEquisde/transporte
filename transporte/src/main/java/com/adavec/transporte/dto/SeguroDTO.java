package com.adavec.transporte.dto;

import lombok.Data;

@Data
public class SeguroDTO {
    private Integer id;
    private String factura;
    private Double valorSeguro;
    private Double seguroDistribuidor;

    private Integer unidadId;
    private Integer distribuidorId;
}
