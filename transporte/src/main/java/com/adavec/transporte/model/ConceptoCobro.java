package com.adavec.transporte.model;

import jakarta.persistence.*;
import lombok.Data;


import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Table(name = "concepto_cobro")
@Data
public class ConceptoCobro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true , nullable = false)
    private String nombre;

    @Column(name = "descripcion_usuario")
    private String descripcion;

    @Column(name = "aplica_iva")
    private boolean aplicaIva;

    @Column(name="activo")
    private boolean activo = true;;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion = LocalDateTime.now();
    // 🆕 SOLO ESTE CAMPO - Sin default, lo defines tú
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_calculo", nullable = false)
    private TipoCalculo tipoCalculo;  // Sin = MONTO_FIJO

    public enum TipoCalculo {
        MONTO_FIJO,      // valor en tarifa es monto ($22,900)
        PORCENTAJE,      // valor en tarifa es porcentaje (3.24)
        MANUAL           // no usa tarifa, captura manual
    }

}
