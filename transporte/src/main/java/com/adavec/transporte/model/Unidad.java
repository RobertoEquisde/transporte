package com.adavec.transporte.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "unidad")
@Data
public class Unidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "NoSerie")
    private String noSerie;

    @ManyToOne
    @JoinColumn(name = "ModeloID")
    private Modelo modelo;

    @Column(name = "Comentario")
    private String comentario;

    @Column(name = "Origen")
    private String origen;

    @Column(name = "DebisFecha")
    private LocalDate debisFecha;

    @ManyToOne
    @JoinColumn(name = "DistribuidoraID")
    private Distribuidor distribuidor;

    @Column(name = "Reportado_A")
    private String reportadoA;

    @Column(name = "PagoDistribuidora")
    private LocalDate pagoDistribuidora;

    @Column(name = "Valor_unidad")
    private Double valorUnidad;

    @OneToMany(mappedBy = "unidad", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Seguro> seguros;
    @OneToMany(mappedBy = "unidad", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Cobros> cobros;


}
