package com.adavec.transporte.model;

import com.adavec.transporte.model.Carroceras;
import com.adavec.transporte.model.Distribuidor;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ubicaciones")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Ubicacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoEmpresa tipo;

    @NotBlank(message = "El nombre de la sucursal es obligatorio")
    @Size(max = 255, message = "El nombre no puede exceder 255 caracteres")
    @Column(name = "nombre_sucursal", nullable = false)
    private String nombreSucursal;

   

    @DecimalMin(value = "0.0", message = "Los kilómetros deben ser positivos")
    @Column(name = "MONTERREY", precision = 8, scale = 2)
    private BigDecimal kmPlanta1;

    @DecimalMin(value = "0.0", message = "Los kilómetros deben ser positivos")
    @Column(name = "SANTIAGO", precision = 8, scale = 2)
    private BigDecimal kmPlanta2;

    @DecimalMin(value = "0.0", message = "Los kilómetros deben ser positivos")
    @Column(name = "SALTILLO", precision = 8, scale = 2)
    private BigDecimal kmPlanta3;

    @Column(name = "activo")
    private Boolean activo = true;

    @Column(name = "fecha_baja")
    private LocalDateTime fechaBaja;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    // Relación con carrocera
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carrocera_id")
    private Carroceras carrocera;

    // Relación con distribuidor (para ubicaciones tipo DISTRIBUIDORA)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "distribuidor_id")
    private Distribuidor distribuidor;

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


