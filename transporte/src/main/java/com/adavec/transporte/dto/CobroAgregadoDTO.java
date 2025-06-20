package com.adavec.transporte.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
@Data
@Builder
public class CobroAgregadoDTO {
    private Integer unidadId;
    private String unidadNoSerie;

    // Campos agregados para compatibilidad
    private Double tarifaUnica;
    private Double cuotaAsociacion;
    private Double fondoEstrella;
    private Double seguroBroker;
    private Double seguroAdavec;

    // Totales
    private Double totalSinIva;
    private Double totalConIva;
    private Double totalGeneral;

    // Metadatos
    private Integer cantidadConceptos;
    private LocalDate fechaUltimoCobro;
    private List<String> conceptosAplicados;

    // Para el DTO original
    private Integer dias; // Si lo necesitas
    private String fechaTraslado;
}
