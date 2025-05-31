package com.adavec.transporte.dto;

import lombok.Data;

@Data
public class CobroResumenDTO {
    private Integer id;
    private Double tarifaUnica;
    private Double cuotaAsociacion;
    private Double fondoEstrella;
}