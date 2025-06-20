package com.adavec.transporte.repository;

import com.adavec.transporte.model.ConceptoCobro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface ConceptoCobroRepository extends JpaRepository<ConceptoCobro, Integer> {
    // iva
    @Query("SELECT c.id, c.aplicaIva FROM ConceptoCobro c")
    List<Object[]> findAplicaIvaData();
    // ═══════════════════════════════════════
    // MÉTODOS BÁSICOS ESENCIALES
    // ═══════════════════════════════════════

    /**
     * Buscar concepto por nombre (para compatibilidad)
     */
    Optional<ConceptoCobro> findByNombre(String nombre);

    /**
     * Buscar conceptos activos
     */
    List<ConceptoCobro> findByActivoTrue();

    /**
     * Buscar por múltiples IDs (más eficiente que por nombres)
     */
    List<ConceptoCobro> findByIdIn(List<Integer> ids);

    // ═══════════════════════════════════════
    // MÉTODOS ESPECÍFICOS PARA TUS IDs
    // ═══════════════════════════════════════

    /**
     * Obtener conceptos del desglose (IDs: 4, 5, 6, 7, 8)
     */
    @Query("SELECT cc FROM ConceptoCobro cc WHERE cc.id IN (4, 5, 6, 7, 8) AND cc.activo = true ORDER BY cc.id")
    List<ConceptoCobro> findConceptosDesglose();

    /**
     * Obtener conceptos de seguros (IDs: 2, 3)
     */
    @Query("SELECT cc FROM ConceptoCobro cc WHERE cc.id IN (2, 3) AND cc.activo = true ORDER BY cc.id")
    List<ConceptoCobro> findConceptosSeguros();

    /**
     * Obtener conceptos principales para reportes (IDs: 1, 2, 3, 9)
     */
    @Query("SELECT cc FROM ConceptoCobro cc WHERE cc.id IN (1, 2, 3, 9) AND cc.activo = true ORDER BY cc.id")
    List<ConceptoCobro> findConceptosPrincipales();

    /**
     * Verificar que todos los conceptos esenciales existen
     */
    @Query("SELECT COUNT(cc) FROM ConceptoCobro cc WHERE cc.id IN (1, 2, 3, 4, 5, 6, 7, 8, 9) AND cc.activo = true")
    Long countConceptosEsenciales();

    // ═══════════════════════════════════════
    // MÉTODOS PARA DEBUGGING Y VERIFICACIÓN
    // ═══════════════════════════════════════

    /**
     * Obtener información de todos los conceptos (para debugging)
     */
    @Query("SELECT cc.id, cc.nombre, cc.descripcion FROM ConceptoCobro cc WHERE cc.activo = true ORDER BY cc.id")
    List<Object[]> findAllConceptosInfo();

    /**
     * Verificar conceptos faltantes de la lista esencial
     */
    @Query("SELECT id FROM " +
            "(SELECT 1 as id UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 " +
            " UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) as expected " +
            "WHERE id NOT IN (SELECT cc.id FROM ConceptoCobro cc WHERE cc.activo = true)")
    List<Integer> findConceptosFaltantes();

    /**
     * Obtener resumen de conceptos para el reporte
     */
    @Query("SELECT " +
            "cc.id, " +
            "cc.nombre, " +
            "CASE " +
            "  WHEN cc.id IN (4, 5, 6, 7, 8) THEN 'DESGLOSE' " +
            "  WHEN cc.id IN (2, 3) THEN 'SEGURO' " +
            "  WHEN cc.id = 1 THEN 'TRASLADO' " +
            "  WHEN cc.id = 9 THEN 'FONDO' " +
            "  ELSE 'OTRO' " +
            "END as categoria " +
            "FROM ConceptoCobro cc WHERE cc.activo = true ORDER BY cc.id")
    List<Object[]> findResumenConceptos();

    List<ConceptoCobro> findByTipoCalculoAndActivoTrue(ConceptoCobro.TipoCalculo tipoCalculo);
}