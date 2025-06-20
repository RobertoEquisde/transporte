package com.adavec.transporte.service;

import com.adavec.transporte.dto.ReporteFinancieroDTO;
import com.adavec.transporte.model.Unidad;
import com.adavec.transporte.model.Seguro;
import com.adavec.transporte.model.CobroDetalle;
import com.adavec.transporte.model.FechasCobros;
import com.adavec.transporte.repository.FechasCobrosRepository;
import com.adavec.transporte.repository.UnidadRepository;
import com.adavec.transporte.repository.SeguroRepository;
import com.adavec.transporte.repository.CobroDetalleRepository;
import com.adavec.transporte.repository.ConceptoCobroRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ReporteFinancieroServiceImpl implements ReporteFinancieroService {

    private final UnidadRepository unidadRepository;
    private final SeguroRepository seguroRepository;
    private final CobroDetalleRepository cobroDetalleRepository;
    private final FechasCobrosRepository fechasCobrosRepository;
    private final ConceptoCobroRepository conceptoCobroRepository;

    // ═══════════════════════════════════════
    // MAPEO DE IDs DE CONCEPTOS (SEGÚN TU BD)
    // ═══════════════════════════════════════
    // ID 1: TARIFA_UNICA - Tarifa única de traslado
    // ID 2: SEGURO_BROKER - Seguro broker (1.34%)
    // ID 3: SEGURO_ADAVEC - Seguro ADAVEC (3.24%)
    // ID 4: ADAVEC_ASOCIACION - Cuota de asociación ADAVEC
    // ID 5: ADAVEC_CONVENCION - Cuota de convención ADAVEC
    // ID 6: ADAVEC_AMDA - Cuota AMDA
    // ID 7: ASOBENS_PUBLICIDAD - Publicidad ASOBENS
    // ID 8: ASOBENS_CAPACITACION - Capacitación ASOBENS
    // ID 9: FONDO_ESTRELLA - Fondo estrella

    public ReporteFinancieroServiceImpl(UnidadRepository unidadRepository,
                                        SeguroRepository seguroRepository,
                                        CobroDetalleRepository cobroDetalleRepository,
                                        FechasCobrosRepository fechasCobrosRepository,
                                        ConceptoCobroRepository conceptoCobroRepository) {
        this.unidadRepository = unidadRepository;
        this.seguroRepository = seguroRepository;
        this.cobroDetalleRepository = cobroDetalleRepository;
        this.fechasCobrosRepository = fechasCobrosRepository;
        this.conceptoCobroRepository = conceptoCobroRepository;
    }
    public Map<Integer, Boolean> getAplicaIvaMap() {
        List<Object[]> data = conceptoCobroRepository.findAplicaIvaData();

        return data.stream()
                .collect(Collectors.toMap(
                        row -> (Integer) row[0],  // c.id
                        row -> (Boolean) row[1]   // c.aplicaIva
                ));
    }
    @Override
    public List<ReporteFinancieroDTO> obtenerDatosFinancierosPorMes(YearMonth mes) {
        LocalDate inicio = mes.atDay(1);
        LocalDate fin = mes.atEndOfMonth();

        // Filtrar por fecha_traslado (no fecha_interes) de fechas_cobros
        List<FechasCobros> fechasDelMes = fechasCobrosRepository.findByFechaTrasladoBetween(inicio, fin);
        Map<Integer, Boolean> aplicaIvaMap = getAplicaIvaMap();
        final double tasaIVA = 0.16;
        // Extraer las unidades únicas
        List<Unidad> unidadesDelMes = fechasDelMes.stream()
                .map(FechasCobros::getUnidad)
                .distinct()
                .collect(Collectors.toList());

        // Si no hay fechas_cobros para el mes, intentar con el método anterior
        if (unidadesDelMes.isEmpty()) {
            System.out.println("⚠️ No se encontraron registros en fechas_cobros para " + mes +
                    ". Usando fecha de unidad como fallback.");
            unidadesDelMes = unidadRepository.findByDebisFechaBetween(inicio, fin);
        }

        // Crear un mapa de unidad -> fechas para acceso rápido
        Map<Integer, FechasCobros> fechasPorUnidad = fechasDelMes.stream()
                .collect(Collectors.toMap(
                        fc -> fc.getUnidad().getId(),
                        fc -> fc,
                        (fc1, fc2) -> fc1.getFechaProceso().isAfter(fc2.getFechaProceso()) ? fc1 : fc2
                ));

        return unidadesDelMes.stream().map(unidad -> {
            ReporteFinancieroDTO dto = new ReporteFinancieroDTO();

            // INFORMACIÓN BÁSICA DE LA UNIDAD
            dto.setNoSerie(unidad.getNoSerie());
            dto.setModelo(unidad.getModelo() != null ? unidad.getModelo().getNombre() : "SIN MODELO");
            dto.setDistribuidora(unidad.getDistribuidor() != null ? unidad.getDistribuidor().getNombreDistribuidora() : "SIN DISTRIBUIDORA");
            dto.setClaveDistribuidor(unidad.getDistribuidor() != null ? unidad.getDistribuidor().getClaveDistribuidora() : "");

            // Valor unidad
            Double valorUnidad = unidad.getValorUnidad() != null ? unidad.getValorUnidad() : 0.0;
            dto.setValorUnidad(valorUnidad);

            // FECHAS DESDE FECHAS_COBROS - CORREGIDO
            FechasCobros fechasCobros = fechasPorUnidad.get(unidad.getId());

            if (fechasCobros != null) {
                // CORRECCIÓN: fecha_traslado para fecha factura
                dto.setFechaTraslado(fechasCobros.getFechaTraslado());

                // CORRECCIÓN: fecha_interes para fecha interés
                dto.setFechaInteres(fechasCobros.getFechaInteres());

                dto.setFechaProceso(
                        fechasCobros.getFechaProceso() != null
                                ? fechasCobros.getFechaProceso()
                                : LocalDate.now()
                );

                dto.setDias(fechasCobros.getDias() != null ? fechasCobros.getDias() : 0);

                // Log para debugging
                System.out.println(String.format(
                        "📅 Fechas para VIN %s: Traslado(Factura)=%s, Interés=%s, Días=%d",
                        unidad.getNoSerie(),
                        fechasCobros.getFechaTraslado(),
                        fechasCobros.getFechaInteres(),
                        fechasCobros.getDias()
                ));
            } else {
                // Fallback si no hay fechas_cobros
                System.out.println("⚠️ Sin fechas_cobros para VIN: " + unidad.getNoSerie());
                LocalDate fechaDebis = unidad.getDebisFecha();

                dto.setFechaTraslado(fechaDebis);
                dto.setFechaInteres(fechaDebis);
                dto.setFechaProceso(LocalDate.now());
                dto.setDias(0);
            }

            // INFORMACIÓN DE SEGURO
            Seguro seguro = seguroRepository.findByUnidadId(unidad.getId());
            dto.setNumeroFactura(seguro != null ? seguro.getFactura() : "");

            // COBROS DESDE COBRO_DETALLE POR ID
            List<CobroDetalle> cobrosDetalle = cobroDetalleRepository.findByUnidadId(unidad.getId());

            // Convertir lista a mapa por ID de concepto para máxima eficiencia
            Map<Integer, Double> conceptosPorId = cobrosDetalle.stream()
                    .collect(Collectors.toMap(
                            cobro -> cobro.getConcepto().getId(),
                            CobroDetalle::getMontoAplicado,
                            Double::sum // En caso de conceptos duplicados, sumar
                    ));
            // 1. CUOTA SEGURO (3.24%) - ID 3
            Double cuotaSeguro = aplicarIva(conceptosPorId.getOrDefault(3, 0.0), aplicaIvaMap.get(3), tasaIVA);
            dto.setCuotaSeguro(cuotaSeguro);

            // 2. SEGURO (1.34%) - ID 2
            Double seguroEspecifico = aplicarIva(conceptosPorId.getOrDefault(2, 0.0), aplicaIvaMap.get(2), tasaIVA);
            dto.setSeguro(seguroEspecifico);

            // 3. IMPORTE TRASLADO - ID 1
            Double importeTraslado = aplicarIva(conceptosPorId.getOrDefault(1, 0.0), aplicaIvaMap.get(1), tasaIVA);
            dto.setImporteTraslado(importeTraslado);

            // 4. FONDO ESTRELLA - ID 9
            Double fondoEstrella = aplicarIva(conceptosPorId.getOrDefault(9, 0.0), aplicaIvaMap.get(9), tasaIVA);
            dto.setFondoEstrella(fondoEstrella);

            // DESGLOSE DE CUOTA ASOCIACIÓN POR IDs

            // ID 4: Cuota asociación ADAVEC
            Double asociacion = aplicarIva(conceptosPorId.getOrDefault(4, 0.0), aplicaIvaMap.get(4), tasaIVA);
            dto.setAsociacion(asociacion);

            // ID 5: Cuota convención ADAVEC
            Double convencion = aplicarIva(conceptosPorId.getOrDefault(5, 0.0), aplicaIvaMap.get(5), tasaIVA);
            dto.setConvencion(convencion);

            // ID 6: Cuota AMDA
            Double amda = aplicarIva(conceptosPorId.getOrDefault(6, 0.0), aplicaIvaMap.get(6), tasaIVA);
            dto.setAmda(amda);

            // ID 7: Publicidad ASOBENS
            Double publicidad = aplicarIva(conceptosPorId.getOrDefault(7, 0.0), aplicaIvaMap.get(7), tasaIVA);
            dto.setPublicidad(publicidad);

            // ID 8: Capacitación ASOBENS
            Double capacitacion = aplicarIva(conceptosPorId.getOrDefault(8, 0.0), aplicaIvaMap.get(8), tasaIVA);
            dto.setCapacitacion(capacitacion);

            // Calcular total de cuota asociación (suma del desglose)
            Double totalCuotaAsociacion = asociacion + convencion + amda + publicidad + capacitacion;
            dto.setCuotaAsociacion(totalCuotaAsociacion);

            // LOG PARA DEBUGGING - VERIFICAR TOTAL 17,883
            if (totalCuotaAsociacion > 0) {
                System.out.println(String.format(
                        "🔍 DEBUG - VIN: %s | Desglose: ASOC=%.2f + CONV=%.2f + AMDA=%.2f + PUB=%.2f + CAP=%.2f = TOTAL: %.2f | DÍAS: %d",
                        unidad.getNoSerie(), asociacion, convencion, amda, publicidad, capacitacion, totalCuotaAsociacion, dto.getDias()
                ));

                // Verificar si llegamos al target de 17,883
                if (Math.abs(totalCuotaAsociacion - 17883.0) < 0.01) {
                    System.out.println("✅ PERFECTO! Total = 17,883 para VIN: " + unidad.getNoSerie());
                } else if (totalCuotaAsociacion > 0) {
                    System.out.println("⚠️ Total diferente de 17,883: " + totalCuotaAsociacion + " para VIN: " + unidad.getNoSerie());
                }
            }

            return dto;
        }).collect(Collectors.toList());
    }

    private double aplicarIva(double valorBase, Boolean aplicaIva, double tasa) {
        if (aplicaIva != null && aplicaIva) {
            return valorBase * (1 + tasa);
        }
        return valorBase;
    }
}