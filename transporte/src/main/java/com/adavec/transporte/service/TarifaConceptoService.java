package com.adavec.transporte.service;

import com.adavec.transporte.dto.TarifaHistorialDTO;
import com.adavec.transporte.model.ConceptoCobro;
import com.adavec.transporte.model.TarifaConcepto;
import com.adavec.transporte.repository.ConceptoCobroRepository;
import com.adavec.transporte.repository.TarifaConceptoRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class TarifaConceptoService {

    @Autowired
    private ConceptoCobroRepository conceptoRepository;

    @Autowired
    private TarifaConceptoRepository tarifaRepository;

    /**
     * Obtener todas las tarifas vigentes
     */
    public Map<String, Double> obtenerTarifasVigentes() {
        Map<String, Double> tarifas = new HashMap<>();
        LocalDate hoy = LocalDate.now();

        List<ConceptoCobro> conceptos = conceptoRepository.findByActivoTrue();

        for (ConceptoCobro concepto : conceptos) {
            tarifaRepository.findTarifaVigente(concepto.getNombre(), hoy)
                    .ifPresent(tarifa -> tarifas.put(concepto.getNombre(), tarifa.getValor()));
        }

        return tarifas;
    }

    /**
     * Actualizar tarifa (solo modifica el monto)
     */
    public TarifaConcepto actualizarTarifa(String nombreConcepto,
                                           Double nuevoValor,
                                           LocalDate fechaInicio) {

        // 1. Buscar concepto
        ConceptoCobro concepto = conceptoRepository.findByNombre(nombreConcepto)
                .orElseThrow(() -> new RuntimeException("Concepto no encontrado: " + nombreConcepto));

        // 2. Cerrar tarifa actual si existe
        tarifaRepository.findTarifaActualByConceptoId(concepto.getId())
                .ifPresent(tarifaActual -> {
                    tarifaActual.setFechaFin(fechaInicio.minusDays(1));
                    tarifaActual.setActivo(false);
                    tarifaRepository.save(tarifaActual);
                });

        // 3. Crear nueva tarifa
        TarifaConcepto nuevaTarifa = new TarifaConcepto();
        nuevaTarifa.setConcepto(concepto);
        nuevaTarifa.setValor(nuevoValor);
        nuevaTarifa.setFechaInicio(fechaInicio);
        nuevaTarifa.setActivo(true);

        return tarifaRepository.save(nuevaTarifa);
    }

    /**
     * Obtener historial de tarifas
     */
    public List<TarifaHistorialDTO> obtenerHistorial(String nombreConcepto) {
        ConceptoCobro concepto = conceptoRepository.findByNombre(nombreConcepto)
                .orElseThrow(() -> new RuntimeException("Concepto no encontrado"));

        return tarifaRepository.findByConceptoIdOrderByFechaInicioDesc(concepto.getId())
                .stream()
                .map(t -> TarifaHistorialDTO.builder()
                        .id(t.getId())
                        .valor(t.getValor())
                        .fechaInicio(t.getFechaInicio())
                        .fechaFin(t.getFechaFin())
                        .vigente(t.estaVigente())
                        .build())
                .collect(Collectors.toList());
    }
    /**
     * Calcular monto de un concepto para una unidad
     */
    public Double calcularMontoCobro(String nombreConcepto, Double valorUnidad, LocalDate fecha) {

        // 1. Obtener concepto
        ConceptoCobro concepto = conceptoRepository.findByNombre(nombreConcepto)
                .orElseThrow(() -> new RuntimeException("Concepto no encontrado: " + nombreConcepto));

        // 2. Calcular según tipo
        switch (concepto.getTipoCalculo()) {
            case MONTO_FIJO:
                TarifaConcepto tarifaFija = tarifaRepository.findTarifaVigente(nombreConcepto, fecha)
                        .orElseThrow(() -> new RuntimeException("No hay tarifa vigente para: " + nombreConcepto));
                return tarifaFija.getValor();

            case PORCENTAJE:
                TarifaConcepto tarifaPorcentaje = tarifaRepository.findTarifaVigente(nombreConcepto, fecha)
                        .orElseThrow(() -> new RuntimeException("No hay tarifa vigente para: " + nombreConcepto));
                return valorUnidad * (tarifaPorcentaje.getValor() / 100);

            case MANUAL:
                return null; // Usuario debe proporcionar el valor

            default:
                throw new IllegalStateException("Tipo de cálculo no soportado: " + concepto.getTipoCalculo());
        }
    }
    /**
     * Crear nueva tarifa sin cerrar las anteriores
     */
    public TarifaConcepto crearTarifa(String nombreConcepto,
                                      Double valor,
                                      LocalDate fechaInicio) {

        // 1. Buscar concepto
        ConceptoCobro concepto = conceptoRepository.findByNombre(nombreConcepto)
                .orElseThrow(() -> new RuntimeException("Concepto no encontrado: " + nombreConcepto));

        // 2. Validar que NO sea MANUAL
        if (concepto.getTipoCalculo() == ConceptoCobro.TipoCalculo.MANUAL) {
            throw new RuntimeException("Los conceptos tipo MANUAL no requieren tarifa");
        }

        // 3. Validar que no haya solapamiento de fechas
        validarSolapamientoFechas(concepto.getId(), fechaInicio);

        // 4. Crear nueva tarifa
        TarifaConcepto nuevaTarifa = new TarifaConcepto();
        nuevaTarifa.setConcepto(concepto);
        nuevaTarifa.setValor(valor);
        nuevaTarifa.setFechaInicio(fechaInicio);
        nuevaTarifa.setActivo(true);
        // fechaFin queda null (abierta)

        return tarifaRepository.save(nuevaTarifa);
    }

    /**
     * Validar que no haya tarifas activas en la misma fecha
     */
    private void validarSolapamientoFechas(Integer conceptoId, LocalDate fechaInicio) {
        List<TarifaConcepto> tarifasActivas = tarifaRepository
                .findByConceptoIdAndActivoTrueAndFechaFinIsNull(conceptoId);

        for (TarifaConcepto tarifa : tarifasActivas) {
            if (!fechaInicio.isAfter(tarifa.getFechaInicio())) {
                throw new RuntimeException(
                        "Ya existe una tarifa activa desde " + tarifa.getFechaInicio() +
                                ". La nueva tarifa debe iniciar después de esa fecha."
                );
            }
        }
    }

    /**
     * Obtener tarifa vigente mejorado
     */
    public TarifaConcepto obtenerTarifaVigente(String nombreConcepto, LocalDate fecha) {
        return tarifaRepository.findTarifaVigente(nombreConcepto, fecha)
                .orElseThrow(() -> new RuntimeException(
                        "No hay tarifa vigente para " + nombreConcepto + " en la fecha " + fecha
                ));
    }
    /**
     * Calcular todos los cobros para una unidad
     */
    public Map<String, Double> calcularTodosCobros(Double valorUnidad, LocalDate fecha) {
        Map<String, Double> cobros = new HashMap<>();

        List<ConceptoCobro> conceptos = conceptoRepository.findByActivoTrue();

        for (ConceptoCobro concepto : conceptos) {
            try {
                Double monto = calcularMontoCobro(concepto.getNombre(), valorUnidad, fecha);
                if (monto != null) {
                    cobros.put(concepto.getNombre(), monto);
                }
            } catch (Exception e) {
                // Log error pero continuar con otros conceptos
                System.err.println("Error calculando " + concepto.getNombre() + ": " + e.getMessage());
            }
        }

        return cobros;
    }
    // AGREGAR ESTE MÉTODO A TU TarifaConceptoService EXISTENTE:

    /**
     * Verifica si un concepto tiene tarifa configurada por nombre
     */
    public boolean tieneTarifaConfiguradaPorNombre(String nombreConcepto, LocalDate fecha) {
        try {
            // Verificar que el concepto existe
            ConceptoCobro concepto = conceptoRepository.findByNombre(nombreConcepto)
                    .orElse(null);

            if (concepto == null) {
                System.out.println("❌ Concepto no encontrado: " + nombreConcepto);
                return false;
            }

            // Si es tipo MANUAL, no necesita tarifa
            if (concepto.getTipoCalculo() == ConceptoCobro.TipoCalculo.MANUAL) {
                System.out.println("ℹ️ Concepto " + nombreConcepto + " es MANUAL - no requiere tarifa");
                return true; // Los manuales siempre están "configurados"
            }

            // Verificar que tiene tarifa vigente
            boolean tieneTarifa = tarifaRepository.findTarifaVigente(nombreConcepto, fecha).isPresent();

            if (tieneTarifa) {
                System.out.println("✅ Concepto " + nombreConcepto + " tiene tarifa configurada");
            } else {
                System.out.println("❌ Concepto " + nombreConcepto + " NO tiene tarifa vigente para " + fecha);
            }

            return tieneTarifa;

        } catch (Exception e) {
            System.err.println("💥 Error verificando tarifa para " + nombreConcepto + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Verifica si un concepto tiene tarifa configurada por ID
     */
    public boolean tieneTarifaConfigurada(Integer conceptoId, LocalDate fecha) {
        try {
            ConceptoCobro concepto = conceptoRepository.findById(conceptoId).orElse(null);

            if (concepto == null) {
                System.out.println("❌ Concepto con ID " + conceptoId + " no encontrado");
                return false;
            }

            return tieneTarifaConfiguradaPorNombre(concepto.getNombre(), fecha);

        } catch (Exception e) {
            System.err.println("💥 Error verificando tarifa para concepto ID " + conceptoId + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Método alternativo para obtener porcentaje vigente (compatible con tu implementación)
     */
    public Double obtenerPorcentajeVigente(Integer conceptoId, LocalDate fecha) {
        try {
            ConceptoCobro concepto = conceptoRepository.findById(conceptoId).orElse(null);

            if (concepto == null) {
                System.out.println("❌ Concepto con ID " + conceptoId + " no encontrado");
                return null;
            }

            TarifaConcepto tarifa = tarifaRepository.findTarifaVigente(concepto.getNombre(), fecha)
                    .orElse(null);

            if (tarifa != null) {
                Double valor = tarifa.getValor();
                System.out.println("✅ Porcentaje encontrado para " + concepto.getNombre() + ": " + valor + "%");
                return valor;
            }

            System.out.println("❌ No se encontró tarifa vigente para " + concepto.getNombre());
            return null;

        } catch (Exception e) {
            System.err.println("💥 Error obteniendo porcentaje para concepto ID " + conceptoId + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Calcular monto por ID de concepto (para compatibilidad)
     */
    public Double calcularMonto(Integer conceptoId, Double valorUnidad, LocalDate fecha) {
        try {
            ConceptoCobro concepto = conceptoRepository.findById(conceptoId).orElse(null);

            if (concepto == null) {
                System.out.println("❌ Concepto con ID " + conceptoId + " no encontrado");
                return null;
            }

            return calcularMontoCobro(concepto.getNombre(), valorUnidad, fecha);

        } catch (Exception e) {
            System.err.println("💥 Error calculando monto para concepto ID " + conceptoId + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Método de diagnóstico mejorado
     */
    public void diagnosticarConcepto(String nombreConcepto, Double valorUnidad, LocalDate fecha) {
        System.out.println("🔍 DIAGNÓSTICO CONCEPTO: " + nombreConcepto);
        System.out.println("═══════════════════════════════════════");

        try {
            // Verificar concepto
            ConceptoCobro concepto = conceptoRepository.findByNombre(nombreConcepto).orElse(null);

            if (concepto == null) {
                System.out.println("❌ Concepto no encontrado en BD");
                return;
            }

            System.out.println("✅ Concepto encontrado - ID: " + concepto.getId());
            System.out.println("📋 Tipo cálculo: " + concepto.getTipoCalculo());

            // Verificar tarifa
            boolean tieneTarifa = tieneTarifaConfiguradaPorNombre(nombreConcepto, fecha);

            if (!tieneTarifa) {
                System.out.println("❌ No hay tarifa configurada");
                return;
            }

            // Calcular monto
            if (valorUnidad != null && valorUnidad > 0) {
                try {
                    Double monto = calcularMontoCobro(nombreConcepto, valorUnidad, fecha);

                    if (monto != null) {
                        System.out.println("✅ Monto calculado: $" + String.format("%.2f", monto));

                        // Mostrar detalles del cálculo
                        TarifaConcepto tarifa = tarifaRepository.findTarifaVigente(nombreConcepto, fecha).orElse(null);
                        if (tarifa != null) {
                            if (concepto.getTipoCalculo() == ConceptoCobro.TipoCalculo.PORCENTAJE) {
                                System.out.println("🧮 Fórmula: " + valorUnidad + " × (" + tarifa.getValor() + "/100)");
                            } else {
                                System.out.println("🧮 Monto fijo: $" + tarifa.getValor());
                            }
                        }
                    } else {
                        System.out.println("❌ No se pudo calcular el monto");
                    }

                } catch (Exception e) {
                    System.out.println("❌ Error calculando: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("💥 Error en diagnóstico: " + e.getMessage());
        }

        System.out.println("═══════════════════════════════════════");
    }
}