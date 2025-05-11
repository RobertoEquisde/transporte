package com.adavec.transporte.dto;

import lombok.Data;

@Data
public class UnidadDTO {
    private Integer id;
    private String noSerie;
    private String comentario;
    private String origen;
    private String debisFecha;
    private String reportadoA;
    private String pagoDistribuidora;
    private Double valorUnidad;

    private ModeloDTO modelo;
    private DistribuidoraInfoDTO distribuidor;
    private SeguroResumenDTO seguro;

}
