package com.adavec.transporte.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
@Data
@AllArgsConstructor
@Builder
public class HistorialCobroDTO {
    private String fecha;
    private Double monto;
    private String tipoCobro;

}
