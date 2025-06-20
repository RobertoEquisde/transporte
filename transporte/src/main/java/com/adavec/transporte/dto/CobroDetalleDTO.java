package com.adavec.transporte.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class CobroDetalleDTO {
    private Integer id;
    private Integer unidadId;
    private String unidadNoSerie;
    private Integer conceptoId;
    private String conceptoNombre;
    private String conceptoDescripcion;
    private Double montoAplicado;
    private Double montoConIva; // Calculado si aplica IVA
    private boolean aplicaIva;
    private String tipoCalculo; // MONTO_FIJO, PORCENTAJE, MANUAL
    private LocalDate fechaCobro;
}
