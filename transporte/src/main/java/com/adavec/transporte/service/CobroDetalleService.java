package com.adavec.transporte.service;

import com.adavec.transporte.dto.*;
import com.adavec.transporte.exception.BusinessValidationException;
import com.adavec.transporte.model.*;
import com.adavec.transporte.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class CobroDetalleService {

    @Autowired
    private CobroDetalleRepository cobroDetalleRepository;

    @Autowired
    private ConceptoCobroRepository conceptoRepository;

    @Autowired
    private UnidadRepository unidadRepository;

    @Autowired
    private FechasCobrosRepository fechasCobrosRepository;

    @Autowired
    private TarifaConceptoService tarifaService;
    @Autowired
    private TarifaConceptoRepository tarifaConceptoRepository;

    // ==================================================
    // CREAR COBROS
    // ==================================================

    /**
     * Crear un cobro manual por concepto individual
     */
    public CobroDetalle crearCobroManual(Integer unidadId, Integer conceptoId,
                                         Double monto, LocalDate fechaTraslado) {


        // Validaciones básicas
        Unidad unidad = unidadRepository.findById(unidadId.intValue())
                .orElseThrow(() -> new BusinessValidationException("Unidad no encontrada: " + unidadId));

        ConceptoCobro concepto = conceptoRepository.findById(conceptoId)
                .orElseThrow(() -> new BusinessValidationException("Concepto no encontrado: " + conceptoId));


        // VALIDACIÓN NUEVA: Solo conceptos MANUAL pueden tener monto personalizado
        Double montoFinal;

        if (concepto.getTipoCalculo() == ConceptoCobro.TipoCalculo.MANUAL) {
            // Concepto manual - usar monto enviado
            if (monto == null || monto <= 0) {
                throw new BusinessValidationException("Para conceptos manuales el monto debe ser mayor a 0");
            }
            montoFinal = monto;

        } else {
            // Concepto con tarifa fija - obtener monto de tarifas
            TarifaConcepto tarifa = tarifaConceptoRepository.findByConceptoIdAndActivoTrue(conceptoId)
                    .orElseThrow(() -> new BusinessValidationException("No hay tarifa activa para el concepto: " + concepto.getDescripcion()));

            if (concepto.getTipoCalculo() == ConceptoCobro.TipoCalculo.MONTO_FIJO) {
                montoFinal = tarifa.getValor();

            } else if (concepto.getTipoCalculo() == ConceptoCobro.TipoCalculo.PORCENTAJE) {
                // Para porcentajes necesitas un valor base (ej: valor del vehículo)
                throw new BusinessValidationException("Los conceptos de porcentaje requieren un valor base. Use el endpoint de desglose automático.");

            } else {
                throw new BusinessValidationException("Tipo de cálculo no válido para el concepto");
            }

            // Advertir si enviaron monto pero se ignora
            if (monto != null && !monto.equals(montoFinal)) {
                // Log o advertencia: "Monto ignorado, usando tarifa fija: $" + montoFinal
            }
        }

        // Crear el detalle
        CobroDetalle detalle = new CobroDetalle();
        detalle.setUnidad(unidad);  // ← Verifica que unidad no sea null aquí
        System.out.println("🔍 Unidad asignada al detalle: " + detalle.getUnidad()); // ← Debug

        detalle.setConcepto(concepto);
        detalle.setMontoAplicado(montoFinal);
        detalle.setArchivoOrigen("MANUAL_" + System.currentTimeMillis());

        System.out.println("💾 Guardando detalle..."); // ← Debug

        return cobroDetalleRepository.save(detalle);
    }

    // Método auxiliar para obtener conceptos que permiten cobro manual
    public List<ConceptoCobro> obtenerConceptosPermitidosParaCobroManual() {
        return conceptoRepository.findByTipoCalculoAndActivoTrue(ConceptoCobro.TipoCalculo.MANUAL);
    }
    // Método auxiliar para manejar las fechas
    private void crearOActualizarFechaCobro(Integer unidadId, LocalDate fechaTraslado, String archivoOrigen, Unidad unidad) {
        // Buscar si ya existe un registro para esta unidad
        Optional<FechasCobros> fechaExistente = fechasCobrosRepository.findByUnidadId(unidadId);

        if (fechaExistente.isPresent()) {
            // Actualizar fecha existente
            FechasCobros fecha = fechaExistente.get();
            fecha.setFechaTraslado(fechaTraslado);
            fecha.setArchivoOrigen(archivoOrigen);
            fechasCobrosRepository.save(fecha);
        } else {
            // Crear nuevo registro de fecha
            FechasCobros nuevaFecha = new FechasCobros();
            nuevaFecha.setUnidad(unidad);  // ← Usar la entidad completa
            nuevaFecha.setFechaTraslado(fechaTraslado);
            nuevaFecha.setArchivoOrigen(archivoOrigen);
            nuevaFecha.setFechaProceso(LocalDate.now());
            fechasCobrosRepository.save(nuevaFecha);
        }
    }
    // ==================================================
    // CONSULTAR COBROS
    // ==================================================

    /**
     * Obtener resumen agregado por unidad (compatibilidad con DTO original)
     */
    public CobroAgregadoDTO obtenerResumenPorUnidad(Integer unidadId) {
        List<CobroDetalle> cobros = cobroDetalleRepository.findByUnidadId(unidadId.longValue());

        if (cobros.isEmpty()) {
            return null;
        }

        Unidad unidad = cobros.get(0).getUnidad();

        // Agrupar por concepto
        Map<String, Double> montosPorConcepto = cobros.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getConcepto().getNombre(),
                        Collectors.summingDouble(CobroDetalle::getMontoAplicado)
                ));

        // Extraer campos específicos para compatibilidad
        Double tarifaUnica = montosPorConcepto.getOrDefault("TARIFA_UNICA", 0.0);
        Double cuotaAsociacion = calcularCuotaAsociacionTotal(montosPorConcepto);
        Double fondoEstrella = montosPorConcepto.getOrDefault("FONDO_ESTRELLA", 0.0);
        Double seguroBroker = montosPorConcepto.getOrDefault("SEGURO_BROKER", 0.0);
        Double seguroAdavec = montosPorConcepto.getOrDefault("SEGURO_ADAVEC", 0.0);

        // Calcular totales
        Double totalSinIva = cobros.stream()
                .filter(c -> !c.getConcepto().isAplicaIva())
                .mapToDouble(CobroDetalle::getMontoAplicado)
                .sum();

        Double totalConIva = cobros.stream()
                .filter(c -> c.getConcepto().isAplicaIva())
                .mapToDouble(c -> c.getMontoAplicado() * 1.16)
                .sum();

        List<String> conceptosAplicados = cobros.stream()
                .map(c -> c.getConcepto().getDescripcion())
                .distinct()
                .collect(Collectors.toList());

        return CobroAgregadoDTO.builder()
                .unidadId(unidadId)
                .unidadNoSerie(unidad.getNoSerie())
                .tarifaUnica(tarifaUnica)
                .cuotaAsociacion(cuotaAsociacion)
                .fondoEstrella(fondoEstrella)
                .seguroBroker(seguroBroker)
                .seguroAdavec(seguroAdavec)
                .totalSinIva(totalSinIva)
                .totalConIva(totalConIva)
                .totalGeneral(totalSinIva + totalConIva)
                .cantidadConceptos(cobros.size())
                .conceptosAplicados(conceptosAplicados)
                .fechaTraslado(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .build();
    }

    /**
     * Obtener detalles individuales por unidad
     */
    public List<CobroDetalleDTO> obtenerDetallesPorUnidad(Integer unidadId) {
        List<CobroDetalle> cobros = cobroDetalleRepository.findByUnidadId(unidadId.longValue());

        return cobros.stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtener historial detallado
     */
    public List<HistorialCobroDTO> getHistorialDetallado(Integer unidadId) {
        List<CobroDetalle> cobros = cobroDetalleRepository.findByUnidadId(unidadId.longValue());

        return cobros.stream()
                .map(cobro -> HistorialCobroDTO.builder()
                        .fecha(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                        .monto(cobro.getMontoAplicado())
                        .tipoCobro(cobro.getConcepto().getNombre())

                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Obtener resumen por conceptos
     */
    public Map<String, Double> obtenerResumenPorConceptos(Integer unidadId) {
        List<CobroDetalle> cobros = cobroDetalleRepository.findByUnidadId(unidadId.longValue());

        return cobros.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getConcepto().getDescripcion(),
                        Collectors.summingDouble(CobroDetalle::getMontoAplicado)
                ));
    }

    // ==================================================
    // ACTUALIZAR COBROS
    // ==================================================

    /**
     * Actualizar monto de un cobro específico
     */
    public CobroDetalle actualizarCobro(Integer cobroId, Double nuevoMonto) {
        CobroDetalle cobro = cobroDetalleRepository.findById(cobroId)
                .orElseThrow(() -> new BusinessValidationException("Cobro no encontrado: " + cobroId));

        if (nuevoMonto <= 0) {
            throw new BusinessValidationException("El monto debe ser mayor a 0");
        }

        cobro.setMontoAplicado(nuevoMonto);
        return cobroDetalleRepository.save(cobro);
    }

    // ==================================================
    // ELIMINAR COBROS
    // ==================================================

    /**
     * Eliminar un cobro específico
     */
    public void eliminarCobro(Integer cobroId) {
        if (!cobroDetalleRepository.existsById(cobroId)) {
            throw new BusinessValidationException("Cobro no encontrado: " + cobroId);
        }
        cobroDetalleRepository.deleteById(cobroId);
    }

    /**
     * Eliminar todos los cobros de una unidad
     */
    public int eliminarCobrosPorUnidad(Integer unidadId) {
        List<CobroDetalle> cobros = cobroDetalleRepository.findByUnidadId(unidadId.longValue());
        int cantidad = cobros.size();

        if (cantidad > 0) {
            cobroDetalleRepository.deleteByUnidadId(unidadId);
        }

        return cantidad;
    }

    // ==================================================
    // MÉTODOS AUXILIARES
    // ==================================================

    /**
     * Obtener unidad por ID
     */
    public Unidad obtenerUnidadPorId(Integer unidadId) {
        return unidadRepository.findById(unidadId.intValue())
                .orElseThrow(() -> new BusinessValidationException("Unidad no encontrada: " + unidadId));
    }

    /**
     * Obtener datos para recálculo (simulando datos de importación)
     */
    public DatosImportacion obtenerDatosParaRecalculo(Integer unidadId) {
        Unidad unidad = obtenerUnidadPorId(unidadId);

        // Buscar la fecha de cobro más reciente
        Optional<FechasCobros> fechasCobros = fechasCobrosRepository.findUltimasPorUnidad(unidad);

        DatosImportacion datos = new DatosImportacion();
        datos.setUnidad(unidad);
        datos.setArchivoOrigen("RECALCULO_" + System.currentTimeMillis());

        if (fechasCobros.isPresent()) {
            FechasCobros fc = fechasCobros.get();
            datos.setFechaTraslado(fc.getFechaTraslado());
            datos.setFechaInteres(fc.getFechaInteres());

            // Los montos se calcularán según las tarifas vigentes
        } else {
            datos.setFechaTraslado(LocalDate.now());
        }

        // Establecer valores por defecto o desde configuración
        // Nota: Revisa el nombre del campo en tu modelo Unidad
        // Podría ser: unidad.getValor(), unidad.getValorUnidad(), etc.
        try {
            if (unidad.getValorUnidad() != null) {
                datos.setValorUnidad(unidad.getValorUnidad());
            }
        } catch (Exception e) {
            // Si el método no existe, intenta con otros nombres comunes
            System.err.println("⚠️ Revisar nombre del campo valor en Unidad: " + e.getMessage());
        }

        return datos;
    }

    /**
     * Convertir CobroDetalle a DTO
     */
    private CobroDetalleDTO convertirADTO(CobroDetalle cobro) {
        Double montoConIva = cobro.getConcepto().isAplicaIva()
                ? cobro.getMontoAplicado() * 1.16
                : cobro.getMontoAplicado();

        return CobroDetalleDTO.builder()
                .id(cobro.getId())
                .unidadId(cobro.getUnidad().getId().intValue())
                .unidadNoSerie(cobro.getUnidad().getNoSerie())
                .conceptoId(cobro.getConcepto().getId())
                .conceptoNombre(cobro.getConcepto().getNombre())
                .conceptoDescripcion(cobro.getConcepto().getDescripcion())
                .montoAplicado(cobro.getMontoAplicado())
                .montoConIva(montoConIva)
                .aplicaIva(cobro.getConcepto().isAplicaIva())
                .tipoCalculo(cobro.getConcepto().getTipoCalculo().toString())

                .build();
    }

    /**
     * Calcular cuota de asociación total (suma de conceptos ADAVEC + ASOBENS)
     */
    private Double calcularCuotaAsociacionTotal(Map<String, Double> montosPorConcepto) {
        double total = 0.0;

        // Conceptos ADAVEC (sin IVA)
        total += montosPorConcepto.getOrDefault("ADAVEC_ASOCIACION", 0.0);
        total += montosPorConcepto.getOrDefault("ADAVEC_CONVENCION", 0.0);
        total += montosPorConcepto.getOrDefault("ADAVEC_AMDA", 0.0);

        // Conceptos ASOBENS (con IVA aplicado)
        total += montosPorConcepto.getOrDefault("ASOBENS_PUBLICIDAD", 0.0) * 1.16;
        total += montosPorConcepto.getOrDefault("ASOBENS_CAPACITACION", 0.0) * 1.16;

        return total;
    }

    /**
     * Validar que un cobro puede ser creado/actualizado
     */
    public CobroValidacionDTO validarCobro(Integer unidadId, Integer conceptoId, Double monto) {
        List<String> errores = new ArrayList<>();
        List<String> advertencias = new ArrayList<>();

        // Validar unidad
        if (!unidadRepository.existsById(unidadId.intValue())) {
            errores.add("Unidad no encontrada");
        }

        // Validar concepto
        Optional<ConceptoCobro> concepto = conceptoRepository.findById(conceptoId);
        if (!concepto.isPresent()) {
            errores.add("Concepto no encontrado");
        } else if (!concepto.get().isActivo()) {
            errores.add("Concepto inactivo");
        }

        // Validar monto
        if (monto <= 0) {
            errores.add("El monto debe ser mayor a 0");
        }

        return CobroValidacionDTO.builder()
                .valido(errores.isEmpty())
                .errores(errores)
                .advertencias(advertencias)
                .build();
    }

    // ==================================================
    // MÉTODOS ADICIONALES PARA FUNCIONALIDADES AVANZADAS
    // ==================================================

    /**
     * Obtener estadísticas de cobros por unidad
     */
    public Map<String, Object> obtenerEstadisticas(Integer unidadId) {
        List<CobroDetalle> cobros = cobroDetalleRepository.findByUnidadId(unidadId.longValue());

        if (cobros.isEmpty()) {
            return Map.of("sinCobros", true);
        }

        Double total = cobros.stream().mapToDouble(CobroDetalle::getMontoAplicado).sum();
        Double promedio = total / cobros.size();

        Map<String, Long> conceptosCantidad = cobros.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getConcepto().getNombre(),
                        Collectors.counting()
                ));

        return Map.of(
                "totalCobros", cobros.size(),
                "montoTotal", total,
                "montoPromedio", promedio,
                "conceptosUnicos", conceptosCantidad.size(),
                "distribuccionConceptos", conceptosCantidad
        );
    }

    /**
     * Verificar si una unidad tiene cobros pendientes
     */
    public boolean tieneCobros(Integer unidadId) {
        return cobroDetalleRepository.findByUnidadId(unidadId.longValue()).size() > 0;
    }

    /**
     * Obtener el último cobro de una unidad
     */
    public Optional<CobroDetalle> obtenerUltimoCobro(Integer unidadId) {
        List<CobroDetalle> cobros = cobroDetalleRepository.findByUnidadId(unidadId.longValue());

        return cobros.stream()
                .max(Comparator.comparing(CobroDetalle::getId));
    }

    public List<CobroGestionDTO> obtenerCobrosSimplesPorUnidad(Integer unidadId) {
        List<CobroDetalle> cobros = cobroDetalleRepository.findByUnidadId(unidadId.longValue());
        Unidad unidad = obtenerUnidadPorId(unidadId);
        Optional<FechasCobros> fechasCobros = fechasCobrosRepository.findUltimasPorUnidad(unidad);

        LocalDate fechaTraslado = fechasCobros.map(FechasCobros::getFechaTraslado).orElse(null);
        return cobros.stream()
                .map(cobro -> convertirASimpleDTO(cobro, fechaTraslado))  // ✅ Lambda en lugar de method reference
                .collect(Collectors.toList());
    }
    /**
     * Convertir CobroDetalle a CobroSimpleDTO
     */
    private CobroGestionDTO convertirASimpleDTO(CobroDetalle cobro,  LocalDate fechaTraslado) {
        Double montoConIva = cobro.getConcepto().isAplicaIva()
                ? cobro.getMontoAplicado() * 1.16
                : cobro.getMontoAplicado();
        boolean esEditable = cobro.getConcepto().getTipoCalculo() == ConceptoCobro.TipoCalculo.MANUAL;
        return CobroGestionDTO.builder()
                .id(cobro.getId())
                .conceptoId(cobro.getConcepto().getId())
                .conceptoNombre(cobro.getConcepto().getNombre())
                .conceptoDescripcion(cobro.getConcepto().getDescripcion())
                .monto(montoConIva)
                .fechaCobro(fechaTraslado) // O usar una fecha real si la
                .isEdit(esEditable)
                .build();
    }
    private List<String> obtenerCamposActualizados(ActualizarCobroParcialRequest request) {
        List<String> campos = new ArrayList<>();
        if (request.getMonto() != null) campos.add("monto");
        if (request.getFechaTraslado() != null) campos.add("fechaTraslado");
        return campos;
    }
    public CobroDetalle actualizarCobroParcial(Integer id, ActualizarCobroParcialRequest request) {
        CobroDetalle cobro = cobroDetalleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cobro no encontrado con ID: " + id));

        // Actualizar monto en cobro_detalle
        if (request.getMonto() != null) {
            cobro.setMontoAplicado(request.getMonto());
            cobro = cobroDetalleRepository.save(cobro);
        }

        // Actualizar fecha usando findBy y save (NO query directa)
        if (request.getFechaTraslado() != null) {
            FechasCobros fechaCobro = fechasCobrosRepository
                    .findByUnidadId(cobro.getUnidad().getId()) // ← Ajusta según tu getter
                    .orElseThrow(() -> new RuntimeException("Fecha no encontrada para la unidad"));

            fechaCobro.setFechaTraslado(request.getFechaTraslado());
            fechasCobrosRepository.save(fechaCobro);
        }

        return cobro;
    }
}