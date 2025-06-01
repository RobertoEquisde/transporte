package com.adavec.transporte.repository;

import com.adavec.transporte.model.Cobros;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CobrosRepository extends JpaRepository<Cobros, Integer> {
    List<Cobros> findByUnidadId(Integer unidadId);
    @Query(value = """
        SELECT 
            DATE_FORMAT(fechaProceso, '%d/%m/%Y') as fecha,
            tarifaUnica as monto,
            'Tarifa Única' as tipo_cobro
        FROM cobros 
        WHERE UnidadID = :unidadId
        UNION ALL
        SELECT 
            DATE_FORMAT(fechaProceso, '%d/%m/%Y') as fecha,
            cuotaAsociacion as monto,
            'Cuota Asociación' as tipo_cobro
        FROM cobros 
        WHERE UnidadID = :unidadId
        UNION ALL
        SELECT 
            DATE_FORMAT(fechaProceso, '%d/%m/%Y') as fecha,
            fondoEstrella as monto,
            'Fondo Estrella' as tipo_cobro
        FROM cobros 
        WHERE UnidadID = :unidadId
        ORDER BY fecha DESC
        """, nativeQuery = true)
    List<Object[]> getHistorialCobros(@Param("unidadId") Long unidadId);
}
