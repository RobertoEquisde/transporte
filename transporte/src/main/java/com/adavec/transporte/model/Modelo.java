package com.adavec.transporte.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "modelo")
@Data
public class Modelo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "Nombre")
    private String nombre;

    @Column(name = "Uso")
    private String uso;
}
