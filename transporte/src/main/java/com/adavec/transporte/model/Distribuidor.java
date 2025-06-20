package com.adavec.transporte.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "distribuidor")
@Data
@ToString(exclude = {"unidades"}) // ✅ Excluir la colección
@EqualsAndHashCode(exclude = {"unidades"})
public class Distribuidor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "NombreDistribuidora")
    private String nombreDistribuidora;

    @Column(name = "ClaveDistribuidora")
    private String claveDistribuidora;

    @Column(name = "Contacto")
    private String contacto;

    @Column(name = "Telefono")
    private Integer telefono;

    @Column(name = "Extension")
    private Integer extension;

    @Column(name = "Correo")
    private String correo;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;


    @Column(name = "Sucursal")
    private String sucursal;
    @OneToMany(mappedBy = "distribuidor", cascade = CascadeType.ALL, orphanRemoval = true)

    private List<Unidad> unidades;
    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        fechaModificacion = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        fechaModificacion = LocalDateTime.now();
    }
}
