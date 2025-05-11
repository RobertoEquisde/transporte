package com.adavec.transporte.dto;

import lombok.Data;
// dto para creaciond e unidades
@Data
public class CrearUnidadRequest {
    private String noSerie;
    private String comentario;
    private String origen;
    private String debisFecha; // en formato "yyyy-MM-dd"
    private String reportadoA;
    private String pagoDistribuidora; // "yyyy-MM-dd"
    private Double valorUnidad;

    private Integer modeloId;
    private Integer distribuidorId;
}
