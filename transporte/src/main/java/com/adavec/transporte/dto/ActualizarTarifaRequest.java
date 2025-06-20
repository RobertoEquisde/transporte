package com.adavec.transporte.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ActualizarTarifaRequest {
    private Double nuevoValor;
    private LocalDate fechaInicio; // Opcional, si es null usa fecha actual
}