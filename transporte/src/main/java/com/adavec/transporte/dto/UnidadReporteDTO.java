package com.adavec.transporte.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UnidadReporteDTO {
    private String clave;
    private String distribuidora;
    private String modelo;
    private String serie;
    private String vin;
    private String fechaFondeo;
    private String tipo;
    private Double valorUnidad;
    private String factura;
    private Double valorSeguro;
}
