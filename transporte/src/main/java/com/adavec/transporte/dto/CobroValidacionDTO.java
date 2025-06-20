package com.adavec.transporte.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CobroValidacionDTO {
    private boolean valido;
    private List<String> errores;
    private List<String> advertencias;
    private Double totalEsperado;
    private Double totalCalculado;
    private Double diferencia;

}
