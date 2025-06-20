package com.adavec.transporte.repository;

import com.adavec.transporte.model.TarifaConcepto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TarifaConceptoRepository extends JpaRepository<TarifaConcepto, Integer> {

    @Query("""
        SELECT t FROM TarifaConcepto t
        WHERE t.concepto.nombre = :nombreConcepto
        AND t.activo = true
        AND t.fechaInicio <= :fecha
        AND (t.fechaFin IS NULL OR t.fechaFin >= :fecha)
        ORDER BY t.fechaInicio DESC
        LIMIT 1
        """)
    Optional<TarifaConcepto> findTarifaVigente(
            @Param("nombreConcepto") String nombreConcepto,
            @Param("fecha") LocalDate fecha
    );

    List<TarifaConcepto> findByConceptoIdOrderByFechaInicioDesc(Integer conceptoId);

    @Query("SELECT t FROM TarifaConcepto t WHERE t.concepto.id = :conceptoId AND t.fechaFin IS NULL")
    Optional<TarifaConcepto> findTarifaActualByConceptoId(@Param("conceptoId") Integer conceptoId);

    List<TarifaConcepto> findByConceptoIdAndActivoTrueAndFechaFinIsNull(Integer conceptoId);

    /**
     * Buscar tarifa actual por concepto ID
     */
    @Query("SELECT tc FROM TarifaConcepto tc " +
            "WHERE tc.concepto.id = :conceptoId " +
            "AND tc.activo = true " +
            "AND tc.fechaFin IS NULL " +
            "ORDER BY tc.fechaInicio DESC")
    Optional<TarifaConcepto> findTarifaActualByConceptoId(@Param("conceptoId") Long conceptoId);

    /**
     * Buscar por concepto ID ordenado por fecha
     */
    @Query("SELECT tc FROM TarifaConcepto tc " +
            "WHERE tc.concepto.id = :conceptoId " +
            "ORDER BY tc.fechaInicio DESC")
    List<TarifaConcepto> findByConceptoIdOrderByFechaInicioDesc(@Param("conceptoId") Long conceptoId);

    @Query("SELECT t FROM TarifaConcepto t " +
            "WHERE t.fechaInicio <= :fecha " +
            "AND (t.fechaFin IS NULL OR t.fechaFin >= :fecha) " +
            "AND t.activo = true")
    List<TarifaConcepto> findTarifasVigentes(@Param("fecha") LocalDate fecha);

    @Query("SELECT t.concepto.id, t.valor FROM TarifaConcepto t " +
            "WHERE t.fechaInicio <= :fecha " +
            "AND (t.fechaFin IS NULL OR t.fechaFin >= :fecha) " +
            "AND t.activo = true")
    Map<Integer, Double> findTarifasVigentesMap(@Param("fecha") LocalDate fecha);

    // Si esperas UNA tarifa por concepto:
    Optional<TarifaConcepto> findByConceptoIdAndActivoTrue(Integer conceptoId);
}