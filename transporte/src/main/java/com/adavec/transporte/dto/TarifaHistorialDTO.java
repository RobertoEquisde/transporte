package com.adavec.transporte.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class TarifaHistorialDTO {
    private Integer id;
    private Double valor;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private boolean vigente;
}