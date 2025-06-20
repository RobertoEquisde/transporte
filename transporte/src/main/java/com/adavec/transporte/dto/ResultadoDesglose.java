package com.adavec.transporte.dto;

import com.adavec.transporte.model.CobroDetalle;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ResultadoDesglose {
    private boolean exitoso;
    private boolean exento;
    private String motivo;
    private List<CobroDetalle> detalles = new ArrayList<>();
    private Double totalDesglosado;
    private List<String> advertencias = new ArrayList<>();
    private String error;

    public void addAdvertencia(String advertencia) {
        this.advertencias.add(advertencia);
    }
}