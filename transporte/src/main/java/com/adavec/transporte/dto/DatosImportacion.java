package com.adavec.transporte.dto;

import com.adavec.transporte.model.Unidad;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DatosImportacion {
    private Unidad unidad;
    private Double tarifaUnica;
    private Double cuotaAsociacion;
    private Double fondoEstrella;
    private Double valorUnidad;
    private LocalDate fechaTraslado;
    private String archivoOrigen;
    // Campos adicionales para trazabilidad
    private  LocalDate  fechaInteres;
    private  LocalDate fechaFondeo;
    private  Integer dias;
    private String numeroFactura;
    private String claveDistribuidora;
    private String modeloNombre;
    private String noSerie;
}