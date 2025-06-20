package com.adavec.transporte.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name = "fechas_cobros")
@Data
public class FechasCobros {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "unidad_id", nullable = false)
    private Unidad unidad;

    @Column(name = "fecha_traslado", nullable = false)
    private LocalDate fechaTraslado;

    @Column(name = "fecha_interes", nullable = false)
    private LocalDate fechaInteres;

    @Column(name = "dias", nullable = false)
    private Integer dias;

    @Column(name = "fecha_proceso", nullable = false)
    private LocalDate fechaProceso;

    @Column(name = "archivo_origen")
    private String archivoOrigen; // Para trazabilidad del documento

    // Opcional: Si quieres relacionarlo con los cobros generados
    @Column(name = "periodo")
    private String periodo; // Ej: "2024-01" para identificar el período de cobro



}