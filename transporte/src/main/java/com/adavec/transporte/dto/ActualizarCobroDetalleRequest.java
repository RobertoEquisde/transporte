package com.adavec.transporte.dto;

import lombok.Data;

@Data
public class ActualizarCobroDetalleRequest {
    private Double monto;
    private Double montoAnterior; // Para auditoría

}
