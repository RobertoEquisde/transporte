package com.adavec.transporte.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
@Data
@Builder
public class HistorialCobroDetalladoDTO {
    private Integer cobroId;
    private LocalDate fecha;
    private String concepto;
    private String descripcion;
    private Double monto;
    private Double montoConIva;
    private boolean aplicaIva;
    private String tipoCalculo;
    private String archivoOrigen;
}


