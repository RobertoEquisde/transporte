package com.adavec.transporte.service;

import com.adavec.transporte.dto.ReporteFinancieroDTO;

import java.time.YearMonth;
import java.util.List;

public interface ReporteFinancieroService {
    List<ReporteFinancieroDTO> obtenerDatosFinancierosPorMes(YearMonth mes);
}
