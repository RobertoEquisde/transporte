package com.adavec.transporte.dto;

import lombok.Data;

@Data
public class CrearDistribuidorDTO {
    private String nombreDistribuidora;
    private String claveDistribuidora;
    private String contacto;
    private Integer telefono;
    private Integer extension;
    private String correo;
    private String sucursal;
}
