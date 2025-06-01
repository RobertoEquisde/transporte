package com.adavec.transporte.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
@Data
@AllArgsConstructor
public class HistorialCobroDTO {
    private String fecha;
    private Double monto;
    private String tipoCobro;
}
