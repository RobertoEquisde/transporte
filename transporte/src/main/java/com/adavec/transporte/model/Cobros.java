package com.adavec.transporte.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name = "cobros")
@Data
public class Cobros {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "UnidadID", nullable = false)
    private Unidad unidad;

    @Column(name = "tarifaUnica")
    private Double tarifaUnica;

    @Column(name = "cuotaAsociacion")
    private Double cuotaAsociacion;

    @Column(name = "fondoEstrella")
    private Double fondoEstrella;

    @Column(name = "dias")
    private Integer dias;

    @Column(name = "fechaTraslado")
    private LocalDate fechaTraslado;
}
