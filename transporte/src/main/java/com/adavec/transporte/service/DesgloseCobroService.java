package com.adavec.transporte.service;

import com.adavec.transporte.dto.DatosImportacion;
import com.adavec.transporte.dto.ResultadoDesglose;
import com.adavec.transporte.exception.BusinessValidationException;
import com.adavec.transporte.model.CobroDetalle;
import com.adavec.transporte.model.ConceptoCobro;
import com.adavec.transporte.model.TarifaConcepto;
import com.adavec.transporte.model.Unidad;
import com.adavec.transporte.repository.CobroDetalleRepository;
import com.adavec.transporte.repository.ConceptoCobroRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
// Versión mejorada de tu DesgloseCobroService para referencia

@Service
@Transactional
public class DesgloseCobroService {

    @Autowired
    private TarifaConceptoService tarifaService;

    @Autowired
    private ConceptoCobroRepository conceptoRepository;

    @Autowired
    private CobroDetalleRepository cobroDetalleRepository;

    /**
     * Desglosar montos del archivo según tarifas vigentes
     * VERSIÓN MEJORADA: No falla si faltan campos, aplica lo que puede
     */
    public ResultadoDesglose desglosarCobros(DatosImportacion datos) {
        ResultadoDesglose resultado = new ResultadoDesglose();
        LocalDate fechaCobro = datos.getFechaTraslado();

        try {
            List<CobroDetalle> detalles = new ArrayList<>();

            // 1. SIEMPRE aplicar seguros básicos (calculados como porcentaje)
            detalles.addAll(aplicarSegurosBasicos(datos, fechaCobro));

            // 2. Aplicar tarifa única SI EXISTE
            if (datos.getTarifaUnica() != null && datos.getTarifaUnica() > 0) {
                CobroDetalle tarifaUnicaDetalle = aplicarTarifaUnica(datos, fechaCobro);
                if (tarifaUnicaDetalle != null) {
                    detalles.add(tarifaUnicaDetalle);
                }
            } else {
                resultado.addAdvertencia("Sin tarifa única - Unidad exenta de cobros de traslado");
            }

            // 3. Aplicar desglose o cuota asociación según corresponda
            if (datos.getCuotaAsociacion() != null && datos.getCuotaAsociacion() > 0) {
                detalles.addAll(aplicarCuotasAsociacion(datos, fechaCobro));
            } else {
                resultado.addAdvertencia("Sin cuota de asociación");
            }

            // 4. Aplicar fondo estrella SI EXISTE
            if (datos.getFondoEstrella() != null && datos.getFondoEstrella() > 0) {
                CobroDetalle fondoDetalle = aplicarFondoEstrella(datos, fechaCobro);
                if (fondoDetalle != null) {
                    detalles.add(fondoDetalle);
                }
            }

            // 5. Guardar todos los detalles
            if (!detalles.isEmpty()) {
                cobroDetalleRepository.saveAll(detalles);

                // Calcular total
                Double totalCalculado = detalles.stream()
                        .mapToDouble(CobroDetalle::getMontoAplicado)
                        .sum();

                resultado.setDetalles(detalles);
                resultado.setTotalDesglosado(totalCalculado);
                resultado.setExitoso(true);

                System.out.println("✅ " + detalles.size() + " conceptos aplicados, total: $" +
                        String.format("%.2f", totalCalculado));
            } else {
                // Si no hay conceptos, marcar como exento pero exitoso
                resultado.setExento(true);
                resultado.setMotivo("No se aplicaron conceptos - Todos los valores son 0 o nulos");
                resultado.setExitoso(true);
            }

        } catch (Exception e) {
            resultado.setExitoso(false);
            resultado.setError("Error al desglosar: " + e.getMessage());
            System.err.println("❌ Error en desglose: " + e.getMessage());
        }

        return resultado;
    }

    /**
     * Aplica seguros básicos SIEMPRE (calculados como porcentaje del valor de la unidad)
     */
    private List<CobroDetalle> aplicarSegurosBasicos(DatosImportacion datos, LocalDate fecha) {
        List<CobroDetalle> detalles = new ArrayList<>();

        if (datos.getValorUnidad() == null || datos.getValorUnidad() <= 0) {
            System.err.println("⚠️ Sin valor de unidad, no se pueden calcular seguros");
            return detalles;
        }

        try {
            // Seguro Broker (1.34%)
            Double seguroBroker = datos.getValorUnidad() * 0.0134;
            CobroDetalle detalleBroker = crearDetalle(datos.getUnidad(), "SEGURO_BROKER",
                    seguroBroker, fecha, datos.getArchivoOrigen());
            if (detalleBroker != null) {
                detalles.add(detalleBroker);
            }

            // Seguro ADAVEC (3.24%)
            Double seguroAdavec = datos.getValorUnidad() * 0.0324;
            CobroDetalle detalleAdavec = crearDetalle(datos.getUnidad(), "SEGURO_ADAVEC",
                    seguroAdavec, fecha, datos.getArchivoOrigen());
            if (detalleAdavec != null) {
                detalles.add(detalleAdavec);
            }

        } catch (Exception e) {
            System.err.println("❌ Error aplicando seguros básicos: " + e.getMessage());
        }

        return detalles;
    }

    /**
     * Aplica tarifa única si existe
     */
    private CobroDetalle aplicarTarifaUnica(DatosImportacion datos, LocalDate fecha) {
        try {
            Double tarifa = datos.getTarifaUnica();

            // Si es 26,564 (tarifa con IVA), usar la base 22,900
            if (Math.abs(tarifa - 26564.0) < 0.01) {
                tarifa = 22900.0; // Base sin IVA
            }

            return crearDetalle(datos.getUnidad(), "TARIFA_UNICA",
                    tarifa, fecha, datos.getArchivoOrigen());

        } catch (Exception e) {
            System.err.println("❌ Error aplicando tarifa única: " + e.getMessage());
            return null;
        }
    }

    /**
     * Aplica cuotas de asociación (desglose completo o normal)
     */
    private List<CobroDetalle> aplicarCuotasAsociacion(DatosImportacion datos, LocalDate fecha) {
        List<CobroDetalle> detalles = new ArrayList<>();

        try {
            Double cuotaAsociacion = datos.getCuotaAsociacion();

            // Detectar desglose completo (17,883)
            if (Math.abs(cuotaAsociacion - 17883.0) < 0.01) {
                detalles.addAll(aplicarDesgloseCompleto(datos, fecha));
            } else {
                // Aplicar cuota normal
                CobroDetalle detalle = crearDetalle(datos.getUnidad(), "ADAVEC_ASOCIACION",
                        cuotaAsociacion, fecha, datos.getArchivoOrigen());
                if (detalle != null) {
                    detalles.add(detalle);
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Error aplicando cuotas de asociación: " + e.getMessage());
        }

        return detalles;
    }

    /**
     * Aplica el desglose completo de 17,883
     */
    private List<CobroDetalle> aplicarDesgloseCompleto(DatosImportacion datos, LocalDate fecha) {
        List<CobroDetalle> detalles = new ArrayList<>();

        try {
            // Conceptos ADAVEC (sin IVA)
            detalles.add(crearDetalle(datos.getUnidad(), "ADAVEC_ASOCIACION", 1200.0, fecha, datos.getArchivoOrigen()));
            detalles.add(crearDetalle(datos.getUnidad(), "ADAVEC_CONVENCION", 1500.0, fecha, datos.getArchivoOrigen()));
            detalles.add(crearDetalle(datos.getUnidad(), "ADAVEC_AMDA", 103.0, fecha, datos.getArchivoOrigen()));

            // Conceptos ASOBENS (sin IVA, se aplicará automáticamente)
            detalles.add(crearDetalle(datos.getUnidad(), "ASOBENS_PUBLICIDAD", 8000.0, fecha, datos.getArchivoOrigen()));
            detalles.add(crearDetalle(datos.getUnidad(), "ASOBENS_CAPACITACION", 5000.0, fecha, datos.getArchivoOrigen()));

            // Remover nulos
            detalles.removeIf(Objects::isNull);

        } catch (Exception e) {
            System.err.println("❌ Error aplicando desglose completo: " + e.getMessage());
        }

        return detalles;
    }

    /**
     * Aplica fondo estrella si existe
     */
    private CobroDetalle aplicarFondoEstrella(DatosImportacion datos, LocalDate fecha) {
        try {
            return crearDetalle(datos.getUnidad(), "FONDO_ESTRELLA",
                    datos.getFondoEstrella(), fecha, datos.getArchivoOrigen());
        } catch (Exception e) {
            System.err.println("❌ Error aplicando fondo estrella: " + e.getMessage());
            return null;
        }
    }

    /**
     * Crea un detalle de cobro
     */
    private CobroDetalle crearDetalle(Unidad unidad, String nombreConcepto,
                                      Double monto, LocalDate fecha, String archivo) {
        try {
            ConceptoCobro concepto = conceptoRepository.findByNombre(nombreConcepto)
                    .orElseThrow(() -> new RuntimeException("Concepto no encontrado: " + nombreConcepto));

            CobroDetalle detalle = new CobroDetalle();
            detalle.setUnidad(unidad);
            detalle.setConcepto(concepto);
            detalle.setMontoAplicado(monto);

            detalle.setArchivoOrigen(archivo);

            return detalle;
        } catch (Exception e) {
            System.err.println("❌ Error creando detalle para " + nombreConcepto + ": " + e.getMessage());
            return null;
        }
    }
}