package com.adavec.transporte.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "seguros")
@Data
public class Seguro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "DistribuidoraID")
    private Distribuidor distribuidor;

    @ManyToOne
    @JoinColumn(name = "UnidadID")
    private Unidad unidad;

    @Column(name = "Factura")
    private String factura;

    @Column(name = "ValorSeguro")
    private Double valorSeguro;

    @Column(name = "SeguroDistribuidor")
    private Double seguroDistribuidor;
    @Column(name = "cuotaSeguro")
    private Double cuotaSeguro;

}
