package com.adavec.transporte.dto;
import lombok.Data;

@Data
public class CrearUnidadConSeguroRequest {
    // Datos de unidad
    private String noSerie;
    private String comentario;
    private String origen;
    private String debisFecha;
    private String reportadoA;
    private String pagoDistribuidora;
    private Double valorUnidad;
    private Integer modeloId;
    private Integer distribuidorId;

    // Datos de seguro (obligatorio)
    private CrearSeguroRequest seguro;
}