package com.adavec.transporte.repository;

import com.adavec.transporte.model.CobroDetalle;
import com.adavec.transporte.model.ConceptoCobro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CobroDetalleRepository extends JpaRepository<CobroDetalle, Integer> {

    /**
     * Buscar todos los cobros de una unidad
     */
    List<CobroDetalle> findByUnidadId(Integer unidadId);


    /**
     * Buscar cobros por concepto
     */
    List<CobroDetalle> findByConceptoId(Integer conceptoId);



    /**
     * Obtener total de cobros por unidad
     */
    @Query("""
        SELECT SUM(cd.montoAplicado) 
        FROM CobroDetalle cd 
        WHERE cd.unidad.id = :unidadId
        """)
    Optional<Double> sumMontosByUnidadId(@Param("unidadId") Integer unidadId);

    /**
     * Obtener cobros agrupados por concepto
     */
    @Query("""
        SELECT cd.concepto.nombre, SUM(cd.montoAplicado)
        FROM CobroDetalle cd
        WHERE cd.unidad.id = :unidadId
        GROUP BY cd.concepto.nombre
        ORDER BY cd.concepto.id
        """)
    List<Object[]> findCobrosAgrupadosPorConcepto(@Param("unidadId") Integer unidadId);

    /**
     * Obtener cobros con IVA aplicado donde corresponde
     */
    @Query("""
        SELECT cd.concepto.nombre,
               cd.montoAplicado,
               CASE 
                   WHEN cd.concepto.aplicaIva = true 
                   THEN cd.montoAplicado * 1.16 
                   ELSE cd.montoAplicado 
               END as montoConIva
        FROM CobroDetalle cd
        WHERE cd.unidad.id = :unidadId
        ORDER BY cd.concepto.id
        """)
    List<Object[]> findCobrosConIva(@Param("unidadId") Integer unidadId);

    /**
     * Buscar cobros por archivo origen (para trazabilidad)
     */
    List<CobroDetalle> findByArchivoOrigen(String archivoOrigen);

    /**
     * Obtener resumen agregado como en tabla original
     */
    @Query(value = """
        SELECT 
            u.id as unidad_id,
            SUM(CASE WHEN cc.nombre = 'TARIFA_UNICA' 
                THEN cd.monto_aplicado * 1.16 ELSE 0 END) as tarifa_unica,
            SUM(CASE WHEN cc.nombre IN ('ADAVEC_ASOCIACION', 'ADAVEC_CONVENCION', 'ADAVEC_AMDA') 
                THEN cd.monto_aplicado ELSE 0 END) +
            SUM(CASE WHEN cc.nombre IN ('ASOBENS_PUBLICIDAD', 'ASOBENS_CAPACITACION') 
                THEN cd.monto_aplicado * 1.16 ELSE 0 END) as cuota_asociacion,
            SUM(CASE WHEN cc.nombre = 'FONDO_ESTRELLA' 
                THEN cd.monto_aplicado ELSE 0 END) as fondo_estrella,
            SUM(CASE WHEN cc.nombre = 'SEGURO_BROKER' 
                THEN cd.monto_aplicado ELSE 0 END) as seguro_broker,
            SUM(CASE WHEN cc.nombre = 'SEGURO_ADAVEC' 
                THEN cd.monto_aplicado ELSE 0 END) as seguro_adavec
        FROM cobro_detalle cd
        JOIN concepto_cobro cc ON cd.concepto_id = cc.id
        JOIN unidad u ON cd.unidad_id = u.id
        WHERE u.id = :unidadId
        GROUP BY u.id
        """, nativeQuery = true)
    Optional<Object[]> findResumenAgregadoPorUnidad(@Param("unidadId") Integer unidadId);

    /**
     * Eliminar cobros de una unidad
     */
    void deleteByUnidadId(Integer unidadId);

    /**
     * Buscar cobros por rango de fechas
     */


    /**
     * Obtener historial de cobros formateado
     */
    @Query(value = """
        SELECT 
            DATE_FORMAT(cd.fecha_cobro, '%d/%m/%Y') as fecha,
            cd.monto_aplicado as monto,
            cc.nombre as tipo_cobro,
            cc.descripcion_usuario as descripcion
        FROM cobro_detalle cd
        JOIN concepto_cobro cc ON cd.concepto_id = cc.id
        WHERE cd.unidad_id = :unidadId
        ORDER BY cd.fecha_cobro DESC, cc.id
        """, nativeQuery = true)
    List<Object[]> getHistorialCobrosDetallado(@Param("unidadId") Integer unidadId);


    // Método que faltaba para el reporte
    List<CobroDetalle> findByUnidadId(Long unidadId);

    // Métodos adicionales útiles para reportes
    @Query("SELECT cd FROM CobroDetalle cd WHERE cd.unidad.noSerie = :noSerie")
    List<CobroDetalle> findByUnidadNoSerie(@Param("noSerie") String noSerie);

    @Query("SELECT SUM(cd.montoAplicado) FROM CobroDetalle cd WHERE cd.unidad.id = :unidadId")
    Double sumMontoByUnidadId(@Param("unidadId") Long unidadId);

    // Para filtrar por concepto específico
    @Query("SELECT cd FROM CobroDetalle cd WHERE cd.unidad.id = :unidadId AND cd.concepto.nombre = :nombreConcepto")
    List<CobroDetalle> findByUnidadIdAndConceptoNombre(@Param("unidadId") Long unidadId, @Param("nombreConcepto") String nombreConcepto);


}