package com.adavec.transporte.repository;

import com.adavec.transporte.model.FechasCobros;
import com.adavec.transporte.model.Unidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FechasCobrosRepository extends JpaRepository<FechasCobros, Integer> {

    // Buscar por unidad
    List<FechasCobros> findByUnidadOrderByFechaProcesoDesc(Unidad unidad);

    // Buscar por unidad y período
    Optional<FechasCobros> findByUnidadAndPeriodo(Unidad unidad, String periodo);

    // Buscar por rango de fechas
    @Query("SELECT fc FROM FechasCobros fc WHERE fc.unidad = :unidad " +
            "AND fc.fechaTraslado BETWEEN :fechaInicio AND :fechaFin " +
            "ORDER BY fc.fechaTraslado DESC")
    List<FechasCobros> findByUnidadAndFechaTrasladoBetween(
            @Param("unidad") Unidad unidad,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin
    );

    // Buscar las más recientes por unidad
    @Query("SELECT fc FROM FechasCobros fc WHERE fc.unidad = :unidad " +
            "ORDER BY fc.fechaProceso DESC LIMIT 1")
    Optional<FechasCobros> findUltimasPorUnidad(@Param("unidad") Unidad unidad);
    // Agregar en FechasCobrosRepository
    @Query("SELECT fc FROM FechasCobros fc WHERE fc.fechaInteres BETWEEN :inicio AND :fin")
    List<FechasCobros> findByFechaInteresBetween(
            @Param("inicio") LocalDate inicio,
            @Param("fin") LocalDate fin
    );

    @Query("SELECT fc FROM FechasCobros fc WHERE fc.fechaTraslado BETWEEN :inicio AND :fin")
    List<FechasCobros> findByFechaTrasladoBetween(
            @Param("inicio") LocalDate inicio,
            @Param("fin") LocalDate fin
    );

    // Si esperas UN solo registro por unidad:
    Optional<FechasCobros> findByUnidadId(Integer unidadId);

}