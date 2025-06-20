package com.adavec.transporte.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReporteFinancieroDTO {

    // ═══════════════════════════════════════
    // INFORMACIÓN BÁSICA DE LA UNIDAD
    // ═══════════════════════════════════════
    private LocalDate fechaProceso;
    private String claveDistribuidor;
    private String numeroFactura;
    private String modelo;
    private String noSerie;
    private LocalDate fechaTraslado;
    private LocalDate fechaInteres;
    private Integer dias;
    private Double valorUnidad; // Importe Factura

    // ═══════════════════════════════════════
    // CAMPOS PRINCIPALES DEL REPORTE
    // ═══════════════════════════════════════
    private Double cuotaAsociacion;    // Total del desglose
    private Double cuotaSeguro;        // 3.24%
    private Double seguro;             // 1.34%
    private Double importeTraslado;    // Tarifa única
    private Double fondoEstrella;      // Fondo estrella

    // ═══════════════════════════════════════
    // DESGLOSE DE CUOTA ASOCIACIÓN
    // ═══════════════════════════════════════
    private Double asociacion;         // ASOCIACION
    private Double convencion;         // CONVENCION
    private Double amda;              // AMDA
    private Double publicidad;        // PUBLICIDAD
    private Double capacitacion;      // CAPACITACION

    // ═══════════════════════════════════════
    // CAMPOS ADICIONALES PARA COMPATIBILIDAD
    // ═══════════════════════════════════════
    private String distribuidora;     // Para compatibilidad con código existente

    // ═══════════════════════════════════════
    // MÉTODOS DE UTILIDAD
    // ═══════════════════════════════════════

    /**
     * Calcula el total de cuota asociación sumando todos los componentes
     */
    public Double getCuotaAsociacionCalculada() {
        double total = 0.0;
        if (asociacion != null) total += asociacion;
        if (convencion != null) total += convencion;
        if (amda != null) total += amda;
        if (publicidad != null) total += publicidad;
        if (capacitacion != null) total += capacitacion;
        return total;
    }

    /**
     * Verifica si la unidad tiene información de cobros válida
     */
    public boolean tieneInformacionValida() {
        return (dias != null && dias > 0) &&
                (cuotaAsociacion != null && cuotaAsociacion > 0);
    }

    /**
     * Obtiene el total general de todos los conceptos
     */
    public Double getTotalGeneral() {
        double total = 0.0;
        if (cuotaAsociacion != null) total += cuotaAsociacion;
        if (cuotaSeguro != null) total += cuotaSeguro;
        if (seguro != null) total += seguro;
        if (importeTraslado != null) total += importeTraslado;
        if (fondoEstrella != null) total += fondoEstrella;
        return total;
    }
    private Double aplicarIVA(Double valor, boolean aplicaIva) {
        if (valor == null) return 0.0;
        return aplicaIva ? valor * 1.16 : valor;
    }

}