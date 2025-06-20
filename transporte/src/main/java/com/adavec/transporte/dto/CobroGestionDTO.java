package com.adavec.transporte.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class CobroGestionDTO {
    private Integer id;
    private Integer conceptoId;
    private String conceptoNombre;
    private String conceptoDescripcion;
    private Double monto;
    private LocalDate fechaCobro;
    private boolean isEdit;
}
