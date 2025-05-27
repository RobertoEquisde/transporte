package com.adavec.transporte.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

// dto para creaciond e unidades
@Data
public class CrearUnidadRequest {
    @NotBlank(message = "El número de serie es obligatorio")
    @Size(max = 50, message = "El número de serie no puede tener más de 50 caracteres")
    private String noSerie;

    @NotNull(message = "El ID del modelo es obligatorio")
    @Positive(message = "El ID del modelo debe ser un número positivo")
    private Long modeloId;

    @NotNull(message = "El ID del distribuidor es obligatorio")
    @Positive(message = "El ID del distribuidor debe ser un número positivo")
    private Long distribuidorId;

    @Valid
    @NotNull(message = "Los datos del seguro son obligatorios")
    private CrearSeguroRequest seguro;

    // Campos opcionales
    @Size(max = 500, message = "El comentario no puede tener más de 500 caracteres")
    private String comentario;

    @Size(max = 100, message = "El origen no puede tener más de 100 caracteres")
    private String origen;

    // Fechas como String para validación de formato si es necesario
    private String debisFecha;

    @Size(max = 100, message = "Reportado a no puede tener más de 100 caracteres")
    private String reportadoA;

    private String pagoDistribuidora;

    @DecimalMin(value = "0.01", message = "El valor de la unidad debe ser mayor a 0")
    @Digits(integer = 10, fraction = 2, message = "El valor debe tener máximo 10 dígitos enteros y 2 decimales")
    private BigDecimal valorUnidad;
}
