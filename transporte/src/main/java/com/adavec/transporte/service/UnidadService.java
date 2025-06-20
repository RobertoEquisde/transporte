package com.adavec.transporte.service;

import com.adavec.transporte.dto.UnidadDTO;
import com.adavec.transporte.dto.UnidadReporteDTO;
import com.adavec.transporte.model.Distribuidor;
import com.adavec.transporte.model.Modelo;
import com.adavec.transporte.model.Unidad;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface UnidadService {
    List<Unidad> obtenerTodas();
    Optional<Unidad> obtenerPorId(Integer id);
    Unidad guardar(Unidad unidad);
    List<UnidadReporteDTO> obtenerDatosPorMes(YearMonth mes);
    Optional<Unidad> obtenerPorNoSerie(String noSerie);
    List<Unidad> buscarPorUltimosDigitosSerie(String ultimos6);
    void eliminarPorNoSerie(String noSerie);
    Modelo buscarOCrearModeloPorNombre(String nombre);

    // Updated method signature for updating Unidad using UnidadDTO as request
    Unidad actualizarUnidad(Integer id, UnidadDTO requestDTO);

    Distribuidor buscarDistribuidorPorClave(String clave);


    boolean existePorNoSerie(String trim);
}
