package com.adavec.transporte.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name = "tarifa_concepto")
@Data
public class TarifaConcepto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name="concepto_id",nullable = false)
    private ConceptoCobro concepto;

    @Column(name = "valor")
    private Double valor;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(name = "activo")
    private Boolean activo;

    public boolean estaVigente() {
        LocalDate hoy = LocalDate.now();
        return activo &&
                !hoy.isBefore(fechaInicio) &&
                (fechaFin == null || !hoy.isAfter(fechaFin));
    }


}
