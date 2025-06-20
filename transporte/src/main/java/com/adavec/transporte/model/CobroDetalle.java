package com.adavec.transporte.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name = "cobro_detalle")
@Data
public class CobroDetalle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "unidad_id", nullable = false)
    private Unidad unidad;

    @ManyToOne
    @JoinColumn(name = "concepto_id", nullable = false)
    private ConceptoCobro concepto;

    @Column(name = "monto_aplicado", nullable = false)
    private Double montoAplicado;



    @Column(name = "archivo_origen")
    private String archivoOrigen; // Para trazabilidad
}