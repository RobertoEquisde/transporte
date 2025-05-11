package com.adavec.transporte.dto;

import lombok.Data;
// dto para  creacion de un seguro
@Data
public class CrearSeguroRequest {
    private String factura;
    private Double valorSeguro;
    private Double seguroDistribuidor;
    private Integer unidadId; // <- obligatorio
}
