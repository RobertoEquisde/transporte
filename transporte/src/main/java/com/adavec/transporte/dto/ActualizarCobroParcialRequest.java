package com.adavec.transporte.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ActualizarCobroParcialRequest {
    private Double monto;
    private LocalDate fechaTraslado;
}
