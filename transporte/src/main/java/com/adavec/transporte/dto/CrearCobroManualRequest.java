package com.adavec.transporte.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CrearCobroManualRequest {

    private Integer conceptoId;
    private Double monto;
    private LocalDate fechaTraslado;
}
