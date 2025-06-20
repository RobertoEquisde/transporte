package com.adavec.transporte.controller;

import com.adavec.transporte.dto.CrearDistribuidorDTO;
import com.adavec.transporte.dto.DatosImportacion;
import com.adavec.transporte.dto.ResultadoDesglose;
import com.adavec.transporte.model.*;
import com.adavec.transporte.repository.CobroDetalleRepository;
import com.adavec.transporte.repository.ConceptoCobroRepository;
import com.adavec.transporte.service.*;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/importar")
@EnableRetry
public class TxtImportController {

    private final UnidadService unidadService;
    private final SeguroService seguroService;
    private final DistribuidorService distribuidorService;
    private final DesgloseCobroService desgloseCobroService;

    private final CobroDetalleRepository cobroDetalleRepository;
    private final ConceptoCobroRepository conceptoCobroRepository;

    private final TarifaConceptoService  tarifaConceptoService;

    // Tamaño del lote para procesar
    private static final int BATCH_SIZE = 5;
    private static final int MIN_COLUMNS_REQUIRED = 13;

    // Constantes para detección automática
    private static final double VALOR_DESGLOSE_COMPLETO = 17883.0;
    private static final double VALOR_TARIFA_UNICA_IVA = 26564.0;

    public TxtImportController(UnidadService unidadService,
                               SeguroService seguroService,
                               DistribuidorService distribuidorService,
                               DesgloseCobroService desgloseCobroService,
                               CobroDetalleRepository cobroDetalleRepository,
                               ConceptoCobroRepository conceptoCobroRepository,
                               TarifaConceptoService tarifaConceptoService) {  // ← AGREGAR ESTA LÍNEA
        this.unidadService = unidadService;
        this.seguroService = seguroService;
        this.distribuidorService = distribuidorService;
        this.desgloseCobroService = desgloseCobroService;
        this.cobroDetalleRepository = cobroDetalleRepository;
        this.conceptoCobroRepository = conceptoCobroRepository;
        this.tarifaConceptoService = tarifaConceptoService;  // ← AGREGAR ESTA LÍNEA
    }


    // Enum para tipos de errores
    public enum TipoError {
        DUPLICADO("DUPLICADO"),
        VIN_VACIO("VIN_VACIO"),
        FORMATO_INCORRECTO("FORMATO_INCORRECTO"),
        DATOS_OBLIGATORIOS_FALTANTES("DATOS_OBLIGATORIOS_FALTANTES"),
        FECHA_INVALIDA("FECHA_INVALIDA"),
        VALOR_NUMERICO_INVALIDO("VALOR_NUMERICO_INVALIDO"),
        DISTRIBUIDOR_INVALIDO("DISTRIBUIDOR_INVALIDO"),
        ERROR_BASE_DATOS("ERROR_BASE_DATOS"),
        ERROR_PROCESAMIENTO("ERROR_PROCESAMIENTO"),
        ERROR_DESGLOSE("ERROR_DESGLOSE");

        private final String descripcion;

        TipoError(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }

    // Clase para representar un error detallado
    public static class ErrorDetallado {
        private final int numeroLinea;
        private final TipoError tipoError;
        private final String mensaje;
        private final String lineaOriginal;
        private final String campo;
        private final String valor;

        public ErrorDetallado(int numeroLinea, TipoError tipoError, String mensaje, String lineaOriginal) {
            this(numeroLinea, tipoError, mensaje, lineaOriginal, null, null);
        }

        public ErrorDetallado(int numeroLinea, TipoError tipoError, String mensaje, String lineaOriginal, String campo, String valor) {
            this.numeroLinea = numeroLinea;
            this.tipoError = tipoError;
            this.mensaje = mensaje;
            this.lineaOriginal = lineaOriginal;
            this.campo = campo;
            this.valor = valor;
        }

        // Getters
        public int getNumeroLinea() { return numeroLinea; }
        public TipoError getTipoError() { return tipoError; }
        public String getMensaje() { return mensaje; }
        public String getLineaOriginal() { return lineaOriginal; }
        public String getCampo() { return campo; }
        public String getValor() { return valor; }

        @Override
        public String toString() {
            return String.format("Línea %d [%s]: %s", numeroLinea, tipoError.getDescripcion(), mensaje);
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("numeroLinea", numeroLinea);
            map.put("tipoError", tipoError.getDescripcion());
            map.put("message", mensaje);
            map.put("lineaOriginal", lineaOriginal);
            if (campo != null) map.put("campo", campo);
            if (valor != null) map.put("valor", valor);
            return map;
        }
    }

    // Resultado de validación
    public static class ResultadoValidacion {
        private final boolean esValido;
        private final List<ErrorDetallado> errores;

        public ResultadoValidacion(boolean esValido, List<ErrorDetallado> errores) {
            this.esValido = esValido;
            this.errores = errores;
        }

        public boolean isEsValido() { return esValido; }
        public List<ErrorDetallado> getErrores() { return errores; }
    }

    @PostMapping("/txt")
    public ResponseEntity<?> importarDesdeTxt(@RequestParam("archivo") MultipartFile archivo) {
        try {
            // Validar archivo
            if (archivo.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("mensaje", "El archivo está vacío", "tipoError", "ARCHIVO_VACIO"));
            }

            // Leer todas las líneas del archivo
            List<String> lineas = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(archivo.getInputStream()))) {
                String linea;
                while ((linea = reader.readLine()) != null) {
                    lineas.add(linea);
                }
            }

            if (lineas.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("mensaje", "El archivo no contiene datos", "tipoError", "ARCHIVO_SIN_DATOS"));
            }

            // Estadísticas de importación
            Map<String, Object> resultado = new HashMap<>();
            AtomicInteger importados = new AtomicInteger(0);
            AtomicInteger distribuidoresCreados = new AtomicInteger(0);
            AtomicInteger lotesConErrores = new AtomicInteger(0);
            AtomicInteger conceptosAplicados = new AtomicInteger(0);
            AtomicInteger desgloseCompleto = new AtomicInteger(0);
            AtomicInteger tarifaUnicaDetectada = new AtomicInteger(0);
            AtomicInteger unidadesExentas = new AtomicInteger(0);

            List<ErrorDetallado> todosLosErrores = new ArrayList<>();
            Map<TipoError, Integer> contadorErrores = new HashMap<>();

            // Dividir en lotes
            List<List<String>> lotes = dividirEnLotes(lineas, BATCH_SIZE);

            System.out.println("🚀 Iniciando importación de " + lineas.size() +
                    " líneas divididas en " + lotes.size() + " lotes");

            // Procesar cada lote
            int lineaGlobal = 1;
            for (int i = 0; i < lotes.size(); i++) {
                List<String> lote = lotes.get(i);
                try {
                    System.out.println("📦 Procesando lote " + (i+1) + " de " + lotes.size());
                    Map<String, Object> resultadoLote = procesarLoteDeLineas(lote, lineaGlobal, archivo.getOriginalFilename());

                    importados.addAndGet((int) resultadoLote.get("importados"));
                    distribuidoresCreados.addAndGet((int) resultadoLote.get("distribuidoresCreados"));
                    conceptosAplicados.addAndGet((int) resultadoLote.getOrDefault("conceptosAplicados", 0));
                    desgloseCompleto.addAndGet((int) resultadoLote.getOrDefault("desgloseCompleto", 0));
                    tarifaUnicaDetectada.addAndGet((int) resultadoLote.getOrDefault("tarifaUnicaDetectada", 0));
                    unidadesExentas.addAndGet((int) resultadoLote.getOrDefault("unidadesExentas", 0));

                    if (resultadoLote.containsKey("errores")) {
                        @SuppressWarnings("unchecked")
                        List<ErrorDetallado> erroresLote = (List<ErrorDetallado>) resultadoLote.get("errores");
                        todosLosErrores.addAll(erroresLote);
                    }

                    lineaGlobal += lote.size();
                } catch (Exception e) {
                    lotesConErrores.incrementAndGet();
                    String mensajeError = "Error crítico en lote " + (i+1) + ": " + e.getMessage();
                    System.err.println("💥 " + mensajeError);
                    todosLosErrores.add(new ErrorDetallado(lineaGlobal, TipoError.ERROR_PROCESAMIENTO,
                            mensajeError, "Lote completo"));
                    e.printStackTrace();
                    lineaGlobal += lote.size();
                }
            }

            // Contar errores por tipo
            for (ErrorDetallado error : todosLosErrores) {
                contadorErrores.merge(error.getTipoError(), 1, Integer::sum);
            }

            // Construir respuesta con estadísticas mejoradas
            resultado.put("unidadesImportadas", importados.get());
            resultado.put("distribuidoresCreados", distribuidoresCreados.get());
            resultado.put("conceptosAplicados", conceptosAplicados.get());
            resultado.put("desgloseCompletoDetectado", desgloseCompleto.get());
            resultado.put("tarifaUnicaIvaDetectada", tarifaUnicaDetectada.get());
            resultado.put("unidadesExentas", unidadesExentas.get());
            resultado.put("lotesConErrores", lotesConErrores.get());
            resultado.put("totalLineasProcesadas", lineas.size());
            resultado.put("totalErrores", todosLosErrores.size());

            // Añadir resumen de errores por tipo
            if (!contadorErrores.isEmpty()) {
                Map<String, Integer> resumenErrores = new HashMap<>();
                contadorErrores.forEach((tipo, cantidad) ->
                        resumenErrores.put(tipo.getDescripcion(), cantidad));
                resultado.put("resumenErrores", resumenErrores);
            }

            // Separar errores por tipo para códigos de estado específicos
            List<ErrorDetallado> erroresDuplicados = todosLosErrores.stream()
                    .filter(error -> error.getTipoError() == TipoError.DUPLICADO)
                    .toList();

            List<ErrorDetallado> erroresValidacion = todosLosErrores.stream()
                    .filter(error -> error.getTipoError() == TipoError.VIN_VACIO ||
                            error.getTipoError() == TipoError.DATOS_OBLIGATORIOS_FALTANTES ||
                            error.getTipoError() == TipoError.FORMATO_INCORRECTO ||
                            error.getTipoError() == TipoError.FECHA_INVALIDA ||
                            error.getTipoError() == TipoError.VALOR_NUMERICO_INVALIDO)
                    .toList();

            List<ErrorDetallado> erroresSistema = todosLosErrores.stream()
                    .filter(error -> error.getTipoError() == TipoError.ERROR_BASE_DATOS ||
                            error.getTipoError() == TipoError.ERROR_PROCESAMIENTO ||
                            error.getTipoError() == TipoError.DISTRIBUIDOR_INVALIDO ||
                            error.getTipoError() == TipoError.ERROR_DESGLOSE)
                    .toList();

            // Manejo específico para duplicados (HTTP 409 - Conflict)
            if (!erroresDuplicados.isEmpty()) {
                System.err.println("🔄 ==========================================");
                System.err.println("🔄  UNIDADES DUPLICADAS ENCONTRADAS");
                System.err.println("🔄 ==========================================");
                erroresDuplicados.forEach(error -> System.err.println("🔄 " + error.toString()));
                System.err.println("🔄 ==========================================");
                System.err.println("🔄 Total de duplicados: " + erroresDuplicados.size());
                System.err.println("🔄 ==========================================");

                List<Map<String, Object>> duplicadosDetallados = erroresDuplicados.stream()
                        .map(ErrorDetallado::toMap)
                        .toList();

                resultado.put("message", "Importación rechazada: Se detectaron unidades duplicadas que ya existen en el sistema");
                resultado.put("tipoError", "CONFLICTO_DUPLICADOS");
                resultado.put("duplicados", duplicadosDetallados);
                resultado.put("cantidadDuplicados", erroresDuplicados.size());
                resultado.put("solucion", "Revise los números de serie (VIN) y elimine las unidades duplicadas del archivo");

                if (!todosLosErrores.isEmpty()) {
                    resultado.put("todosLosErrores", todosLosErrores.stream().map(ErrorDetallado::toMap).toList());
                }

                return ResponseEntity.status(HttpStatus.CONFLICT).body(resultado);
            }

            // Manejo para errores de validación (HTTP 422 - Unprocessable Entity)
            if (!erroresValidacion.isEmpty()) {
                System.err.println("⚠️ ==========================================");
                System.err.println("⚠️  ERRORES DE VALIDACIÓN ENCONTRADOS");
                System.err.println("⚠️ ==========================================");
                erroresValidacion.forEach(error -> System.err.println("⚠️ " + error.toString()));
                System.err.println("⚠️ ==========================================");
                System.err.println("⚠️ Total de errores de validación: " + erroresValidacion.size());
                System.err.println("⚠️ ==========================================");

                List<Map<String, Object>> validacionDetallada = erroresValidacion.stream()
                        .map(ErrorDetallado::toMap)
                        .toList();

                resultado.put("message", "Importación rechazada: Los datos contienen errores de validación que deben corregirse");
                resultado.put("tipoError", "ERROR_VALIDACION");
                resultado.put("erroresValidacion", validacionDetallada);
                resultado.put("cantidadErroresValidacion", erroresValidacion.size());
                resultado.put("solucion", "Corrija los campos obligatorios faltantes, formatos de fecha y valores numéricos inválidos");

                if (!todosLosErrores.isEmpty()) {
                    resultado.put("todosLosErrores", todosLosErrores.stream().map(ErrorDetallado::toMap).toList());
                }

                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(resultado);
            }

            // Manejo para errores de sistema (HTTP 500 - Internal Server Error)
            if (!erroresSistema.isEmpty()) {
                System.err.println("💥 ==========================================");
                System.err.println("💥  ERRORES DE SISTEMA ENCONTRADOS");
                System.err.println("💥 ==========================================");
                erroresSistema.forEach(error -> System.err.println("💥 " + error.toString()));
                System.err.println("💥 ==========================================");
                System.err.println("💥 Total de errores de sistema: " + erroresSistema.size());
                System.err.println("💥 ==========================================");

                List<Map<String, Object>> sistemaDetallado = erroresSistema.stream()
                        .map(ErrorDetallado::toMap)
                        .toList();

                resultado.put("message", "Importación fallida: Se produjeron errores internos del sistema");
                resultado.put("tipoError", "ERROR_SISTEMA");
                resultado.put("erroresSistema", sistemaDetallado);
                resultado.put("cantidadErroresSistema", erroresSistema.size());
                resultado.put("solucion", "Contacte al administrador del sistema. Los errores han sido registrados para revisión");

                if (!todosLosErrores.isEmpty()) {
                    resultado.put("todosLosErrores", todosLosErrores.stream().map(ErrorDetallado::toMap).toList());
                }

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resultado);
            }

            // Si hay errores no críticos, incluirlos pero continuar
            if (!todosLosErrores.isEmpty()) {
                List<Map<String, Object>> erroresDetallados = todosLosErrores.stream()
                        .map(ErrorDetallado::toMap)
                        .toList();
                resultado.put("advertencias", erroresDetallados);
                resultado.put("message", "Importación completada con advertencias");
            } else {
                resultado.put("message", "Importación completada exitosamente");
            }

            // Añadir resumen ejecutivo
            Map<String, Object> resumenEjecutivo = new HashMap<>();
            resumenEjecutivo.put("mensaje", "Resumen de detección automática");
            resumenEjecutivo.put("desgloseCompleto17883", desgloseCompleto.get());
            resumenEjecutivo.put("tarifaUnicaIva26564", tarifaUnicaDetectada.get());
            resumenEjecutivo.put("cobrosNormales", importados.get() - desgloseCompleto.get() - tarifaUnicaDetectada.get() - unidadesExentas.get());
            resumenEjecutivo.put("unidadesExentas", unidadesExentas.get());
            resultado.put("resumenEjecutivo", resumenEjecutivo);

            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            System.err.println("💥 Error crítico durante la importación: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "message", "Error crítico durante la importación",
                            "tipoError", "ERROR_SISTEMA",
                            "error", e.getMessage()
                    ));
        }
    }

    /**
     * Procesa un lote de líneas con manejo de errores mejorado y desglose de conceptos
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public Map<String, Object> procesarLoteDeLineas(List<String> lineas, int lineaInicial, String nombreArchivo) {
        int importados = 0;
        int distribuidoresCreados = 0;
        int conceptosAplicados = 0;
        int desgloseCompleto = 0;
        int tarifaUnicaDetectada = 0;
        int unidadesExentas = 0;
        List<ErrorDetallado> errores = new ArrayList<>();

        for (int i = 0; i < lineas.size(); i++) {
            int numeroLinea = lineaInicial + i;
            String linea = lineas.get(i);

            try {
                // Validar línea antes de procesar
                ResultadoValidacion validacion = validarLinea(linea, numeroLinea);
                if (!validacion.isEsValido()) {
                    errores.addAll(validacion.getErrores());
                    continue;
                }

                String[] col = linea.split("\\|");
                String claveDistribuidora = col[0];
                String factura = col[1];
                String modeloNombre = col[2];
                String noSerie = col[4].trim();

                // Verificar duplicados
                Optional<Unidad> existente = unidadService.obtenerPorNoSerie(noSerie);
                if (existente.isPresent()) {
                    errores.add(new ErrorDetallado(numeroLinea, TipoError.DUPLICADO,
                            String.format("Unidad duplicada - VIN: %s ya existe en el sistema (Distribuidor: %s, Modelo: %s)",
                                    noSerie, claveDistribuidora, modeloNombre),
                            linea, "noSerie", noSerie));
                    continue;
                }

                // Procesar datos de la línea
                LocalDate fechaFondeo = parseFecha(col[5]);
                LocalDate fechaInteres = parseFecha(col[6]);
                Integer dias = parseInt(col[7]);
                Double valorUnidad = parseDouble(col[8]);
                Double cuotaAsociacion = parseDouble(col[9]);
                Double valorSeguro = parseDouble(col[10]);
                Double tarifaUnica = parseDouble(col[11]);

                Double fondoEstrella = 0.0;
                if (col.length > 12) {
                    fondoEstrella = convertirADouble(col[12]);
                }

                // Detectar tarifa única con IVA en penúltima columna
                if (col.length > 15) {
                    Double valorPenultimaColumna = convertirADouble(col[col.length - 2]);
                    if (Math.abs(valorPenultimaColumna - VALOR_TARIFA_UNICA_IVA) < 0.01) {
                        tarifaUnica = valorPenultimaColumna;
                        System.out.println("🔍 Detectada tarifa única con IVA: " + valorPenultimaColumna +
                                " para VIN: " + noSerie);
                    }
                }

                LocalDate fechaTraslado = fechaFondeo;
                if (col.length > 15) {
                    LocalDate fechaTemp = parseFecha(col[15]);
                    if (fechaTemp != null) {
                        fechaTraslado = fechaTemp;
                    }
                }

                // Procesar modelo y distribuidor
                Modelo modelo = buscarOCrearModelo(modeloNombre);
                if (modelo == null) {
                    errores.add(new ErrorDetallado(numeroLinea, TipoError.ERROR_BASE_DATOS,
                            "No se pudo crear o encontrar el modelo: " + modeloNombre,
                            linea, "modeloNombre", modeloNombre));
                    continue;
                }

                boolean esNuevoDistribuidor = false;
                Distribuidor distribuidor = buscarOCrearDistribuidor(claveDistribuidora);
                if (distribuidor == null) {
                    errores.add(new ErrorDetallado(numeroLinea, TipoError.DISTRIBUIDOR_INVALIDO,
                            "No se pudo crear o encontrar el distribuidor: " + claveDistribuidora,
                            linea, "claveDistribuidora", claveDistribuidora));
                    continue;
                }
                if (distribuidor.getId() == null || distribuidor.getId() == 0) {
                    esNuevoDistribuidor = true;
                }

                // Crear y guardar unidad
                Unidad unidad = new Unidad();
                unidad.setNoSerie(noSerie);
                unidad.setModelo(modelo);
                unidad.setDistribuidor(distribuidor);
                unidad.setDebisFecha(fechaFondeo);
                unidad.setValorUnidad(valorUnidad);

                unidad = guardarUnidad(unidad);
                importados++;

                if (esNuevoDistribuidor) {
                    distribuidoresCreados++;
                    System.out.println("✅ Nuevo distribuidor creado: " + claveDistribuidora);
                }

                // *** PROCESAR COBROS USANDO TU SISTEMA DE CONCEPTOS ***
                // *** PROCESAR COBROS CON MANEJO GRANULAR DE CONCEPTOS ***
                try {
                    DatosImportacion datosImportacion = new DatosImportacion();
                    datosImportacion.setUnidad(unidad);
                    datosImportacion.setTarifaUnica(tarifaUnica);
                    datosImportacion.setCuotaAsociacion(cuotaAsociacion);
                    datosImportacion.setFondoEstrella(fondoEstrella);
                    datosImportacion.setValorUnidad(valorUnidad);
                    datosImportacion.setFechaTraslado(fechaTraslado);
                    datosImportacion.setFechaInteres(fechaInteres);
                    datosImportacion.setFechaFondeo(fechaFondeo);
                    datosImportacion.setArchivoOrigen(nombreArchivo);
                    datosImportacion.setDias(dias);
                    datosImportacion.setNumeroFactura(factura);
                    datosImportacion.setClaveDistribuidora(claveDistribuidora);
                    datosImportacion.setModeloNombre(modeloNombre);
                    datosImportacion.setNoSerie(noSerie);

                    // Intentar usar el servicio completo primero
                    ResultadoDesglose resultadoDesglose = desgloseCobroService.desglosarCobros(datosImportacion);

                    if (resultadoDesglose.isExitoso()) {
                        // El servicio funcionó correctamente
                        conceptosAplicados += resultadoDesglose.getDetalles().size();

                        // Detectar tipo de desglose para estadísticas
                        if (Math.abs(cuotaAsociacion - VALOR_DESGLOSE_COMPLETO) < 0.01) {
                            desgloseCompleto++;
                            System.out.println("🔍 Desglose completo 17,883 aplicado para VIN: " + noSerie);
                        }

                        if (Math.abs(tarifaUnica - VALOR_TARIFA_UNICA_IVA) < 0.01) {
                            tarifaUnicaDetectada++;
                            System.out.println("🔍 Tarifa única con IVA 26,564 detectada para VIN: " + noSerie);
                        }

                        // Log advertencias del servicio
                        if (!resultadoDesglose.getAdvertencias().isEmpty()) {
                            System.out.println("⚠️ Advertencias para VIN " + noSerie + ":");
                            resultadoDesglose.getAdvertencias().forEach(adv ->
                                    System.out.println("   - " + adv));
                        }

                        System.out.println("✅ Cobros procesados - VIN: " + noSerie +
                                " - " + resultadoDesglose.getDetalles().size() + " conceptos" +
                                " - Total: $" + String.format("%.2f", resultadoDesglose.getTotalDesglosado()));

                    } else if (resultadoDesglose.isExento()) {
                        // Unidad exenta pero aún aplicar seguros básicos
                        unidadesExentas++;
                        System.out.println("ℹ️ Unidad exenta de tarifa única - VIN: " + noSerie +
                                " - " + resultadoDesglose.getMotivo());

                        // Aplicar solo seguros básicos para unidades exentas
                        int conceptosBasicos = aplicarConceptosBasicos(unidad, valorUnidad, fechaTraslado, nombreArchivo);
                        conceptosAplicados += conceptosBasicos;

                        if (conceptosBasicos > 0) {
                            System.out.println("✅ Aplicados " + conceptosBasicos + " conceptos básicos para VIN exenta: " + noSerie);
                        }

                    } else {
                        // Error en el servicio, intentar aplicación manual de conceptos
                        System.err.println("⚠️ Error en servicio de desglose para VIN " + noSerie +
                                ": " + resultadoDesglose.getError());
                        System.out.println("🔄 Intentando aplicación manual de conceptos...");

                        int conceptosAplicadosManual = aplicarConceptosManualmente(
                                unidad, tarifaUnica, cuotaAsociacion, fondoEstrella, valorUnidad,
                                fechaTraslado, nombreArchivo
                        );

                        if (conceptosAplicadosManual > 0) {
                            conceptosAplicados += conceptosAplicadosManual;

                            // Detectar tipo de desglose para estadísticas
                            if (Math.abs(cuotaAsociacion - VALOR_DESGLOSE_COMPLETO) < 0.01) {
                                desgloseCompleto++;
                                System.out.println("🔍 Desglose completo 17,883 aplicado manualmente para VIN: " + noSerie);
                            }

                            if (Math.abs(tarifaUnica - VALOR_TARIFA_UNICA_IVA) < 0.01) {
                                tarifaUnicaDetectada++;
                                System.out.println("🔍 Tarifa única con IVA 26,564 aplicada manualmente para VIN: " + noSerie);
                            }

                            System.out.println("✅ Aplicados " + conceptosAplicadosManual +
                                    " conceptos manualmente para VIN: " + noSerie);
                        } else {
                            errores.add(new ErrorDetallado(numeroLinea, TipoError.ERROR_DESGLOSE,
                                    "No se pudieron aplicar conceptos: " + resultadoDesglose.getError(),
                                    linea));
                        }
                    }

                } catch (Exception e) {
                    System.err.println("💥 Error inesperado en cobros para VIN " + noSerie + ": " + e.getMessage());

                    // Como último recurso, aplicar conceptos básicos
                    try {
                        int conceptosBasicos = aplicarConceptosBasicos(unidad, valorUnidad, fechaTraslado, nombreArchivo);
                        if (conceptosBasicos > 0) {
                            conceptosAplicados += conceptosBasicos;
                            System.out.println("🛡️ Aplicados " + conceptosBasicos + " conceptos básicos como respaldo para VIN: " + noSerie);
                        }
                    } catch (Exception ex) {
                        errores.add(new ErrorDetallado(numeroLinea, TipoError.ERROR_DESGLOSE,
                                "Error crítico procesando cobros: " + ex.getMessage(),
                                linea));
                        System.err.println("💥 Error crítico en conceptos básicos para VIN " + noSerie + ": " + ex.getMessage());
                    }
                }


                // Guardar seguro (mantener)
                Seguro seguro = crearSeguro(unidad, distribuidor, factura, valorSeguro,
                        cuotaAsociacion, valorUnidad, fechaFondeo);
                guardarSeguro(seguro);

                System.out.println("✅ Unidad procesada exitosamente - VIN: " + noSerie);

            } catch (Exception e) {
                errores.add(new ErrorDetallado(numeroLinea, TipoError.ERROR_PROCESAMIENTO,
                        "Error inesperado al procesar línea: " + e.getMessage(),
                        linea));
                System.err.println("💥 Error en línea " + numeroLinea + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("importados", importados);
        resultado.put("distribuidoresCreados", distribuidoresCreados);
        resultado.put("conceptosAplicados", conceptosAplicados);
        resultado.put("desgloseCompleto", desgloseCompleto);
        resultado.put("tarifaUnicaDetectada", tarifaUnicaDetectada);
        resultado.put("unidadesExentas", unidadesExentas);
        resultado.put("errores", errores);

        return resultado;
    }

    private int aplicarConceptosBasicos(Unidad unidad, Double valorUnidad, LocalDate fechaTraslado, String nombreArchivo) {
        int conceptosAplicados = 0;

        try {
            System.out.println("🔧 Aplicando conceptos básicos para VIN: " + unidad.getNoSerie() +
                    " - Valor: $" + String.format("%.2f", valorUnidad));

            // 1. SEGURO BÁSICO - DINÁMICO DESDE BD
            conceptosAplicados += aplicarConceptoSiExiste("Seguro Básico", "Seguro",
                    unidad, valorUnidad, fechaTraslado, nombreArchivo);

            // 2. ADMINISTRACIÓN - DINÁMICO DESDE BD
            conceptosAplicados += aplicarConceptoSiExiste("Gastos Administrativos", "Administración",
                    unidad, valorUnidad, fechaTraslado, nombreArchivo);

            // 3. MANEJO - DINÁMICO DESDE BD (OPCIONAL)
            conceptosAplicados += aplicarConceptoSiExiste("Manejo de Unidad", "Manejo",
                    unidad, valorUnidad, fechaTraslado, nombreArchivo);

            System.out.println("✅ Total conceptos básicos aplicados: " + conceptosAplicados);

        } catch (Exception e) {
            System.err.println("💥 Error aplicando conceptos básicos dinámicos: " + e.getMessage());
            e.printStackTrace();
        }

        return conceptosAplicados;
    }
// 3. MÉTODO HELPER PARA APLICAR CONCEPTOS CON FALLBACKS:
    /**
     * Intenta aplicar un concepto buscando por nombre principal y alternativo
     */
    private int aplicarConceptoSiExiste(String nombrePrincipal, String nombreAlternativo,
                                        Unidad unidad, Double valorUnidad, LocalDate fechaTraslado, String nombreArchivo) {
        try {
            // Intentar con nombre principal
            ConceptoCobro concepto = conceptoCobroRepository.findByNombre(nombrePrincipal).orElse(null);

            // Si no existe, intentar con nombre alternativo
            if (concepto == null) {
                concepto = conceptoCobroRepository.findByNombre(nombreAlternativo).orElse(null);
            }

            if (concepto != null) {
                return aplicarConceptoConTarifa(concepto, unidad, valorUnidad, fechaTraslado, nombreArchivo);
            } else {
                System.out.println("⚠️ No se encontró concepto: " + nombrePrincipal + " ni " + nombreAlternativo);
                return 0;
            }

        } catch (Exception e) {
            System.err.println("💥 Error aplicando concepto " + nombrePrincipal + ": " + e.getMessage());
            return 0;
        }
    }
// 4. MÉTODO PARA APLICAR CONCEPTO CON TARIFA DE BD:
    /**
     * Aplica un concepto consultando su tarifa en BD
     */
    private int aplicarConceptoConTarifa(ConceptoCobro concepto, Unidad unidad, Double valorUnidad,
                                         LocalDate fechaTraslado, String nombreArchivo) {
        try {
            // Calcular monto usando el servicio dinámico
            Double monto = tarifaConceptoService.calcularMonto(concepto.getId(), valorUnidad, fechaTraslado);

            if (monto != null && monto > 0) {
                // Aplicar el concepto
                aplicarConcepto(unidad, concepto, monto, fechaTraslado, nombreArchivo,
                        "Aplicado desde tarifa_concepto");

                System.out.println("✅ " + concepto.getNombre() + " aplicado: $" + String.format("%.2f", monto));
                return 1;

            } else {
                // Intentar fallback si es seguro básico
                if (concepto.getNombre().toLowerCase().contains("seguro")) {
                    Double montoFallback = valorUnidad * 0.03; // 3% por defecto
                    aplicarConcepto(unidad, concepto, montoFallback, fechaTraslado, nombreArchivo,
                            "Fallback 3% - Sin tarifa en BD");

                    System.out.println("⚠️ " + concepto.getNombre() + " aplicado con fallback: $" +
                            String.format("%.2f", montoFallback));
                    return 1;
                }

                System.out.println("❌ No se pudo calcular " + concepto.getNombre() + " - Sin tarifa configurada");
                return 0;
            }

        } catch (Exception e) {
            System.err.println("💥 Error aplicando concepto " + concepto.getNombre() + ": " + e.getMessage());
            return 0;
        }
    }

// 5. MÉTODO DE VALIDACIÓN AL INICIO DE LA IMPORTACIÓN:
    /**
     * Valida que las tarifas estén configuradas antes de importar
     */
    private void validarConfiguracionTarifas() {
        System.out.println("🔧 VALIDANDO CONFIGURACIÓN DE TARIFAS...");

        String[] conceptosEsenciales = {"Seguro Básico", "Seguro"};
        boolean configuracionOK = true;

        for (String nombreConcepto : conceptosEsenciales) {
            if (!tarifaConceptoService.tieneTarifaConfiguradaPorNombre(nombreConcepto, LocalDate.now())) {
                System.err.println("⚠️ ADVERTENCIA: No hay tarifa configurada para " + nombreConcepto);
                configuracionOK = false;
            } else {
                System.out.println("✅ " + nombreConcepto + " tiene tarifa configurada");
            }
        }

        if (!configuracionOK) {
            System.err.println("💡 SOLUCIÓN: Ejecutar el SQL de configuración de tarifas");
            System.err.println("💡 O usar el endpoint /diagnostico-tarifas para más detalles");
        } else {
            System.out.println("🎉 Configuración de tarifas OK - Listo para importar");
        }
    }
    /**
     * Aplica conceptos manualmente cuando el servicio automático falla
     */
    private int aplicarConceptosManualmente(Unidad unidad, Double tarifaUnica, Double cuotaAsociacion,
                                            Double fondoEstrella, Double valorUnidad, LocalDate fechaTraslado,
                                            String nombreArchivo) {
        int conceptosAplicados = 0;

        try {
            // 1. Aplicar tarifa única si existe
            if (tarifaUnica != null && tarifaUnica > 0) {
                ConceptoCobro conceptoTarifa = conceptoCobroRepository.findByNombre("Tarifa Única")
                        .orElse(conceptoCobroRepository.findByNombre("Tarifas")
                                .orElse(null));

                if (conceptoTarifa != null) {
                    aplicarConcepto(unidad, conceptoTarifa, tarifaUnica, fechaTraslado, nombreArchivo,
                            "Tarifa única manual - $" + tarifaUnica);
                    conceptosAplicados++;
                    System.out.println("✅ Tarifa única aplicada manualmente: $" + String.format("%.2f", tarifaUnica));
                }
            }

            // 2. Aplicar cuota de asociación si existe
            if (cuotaAsociacion != null && cuotaAsociacion > 0) {
                ConceptoCobro conceptoCuota = conceptoCobroRepository.findByNombre("Cuota de Asociación")
                        .orElse(conceptoCobroRepository.findByNombre("Asociación")
                                .orElse(null));

                if (conceptoCuota != null) {
                    aplicarConcepto(unidad, conceptoCuota, cuotaAsociacion, fechaTraslado, nombreArchivo,
                            "Cuota asociación manual - $" + cuotaAsociacion);
                    conceptosAplicados++;
                    System.out.println("✅ Cuota asociación aplicada manualmente: $" + String.format("%.2f", cuotaAsociacion));
                }
            }

            // 3. Aplicar fondo estrella si existe
            if (fondoEstrella != null && fondoEstrella > 0) {
                ConceptoCobro conceptoFondo = conceptoCobroRepository.findByNombre("Fondo Estrella")
                        .orElse(conceptoCobroRepository.findByNombre("Fondo")
                                .orElse(null));

                if (conceptoFondo != null) {
                    aplicarConcepto(unidad, conceptoFondo, fondoEstrella, fechaTraslado, nombreArchivo,
                            "Fondo estrella manual - $" + fondoEstrella);
                    conceptosAplicados++;
                    System.out.println("✅ Fondo estrella aplicado manualmente: $" + String.format("%.2f", fondoEstrella));
                }
            }

            // 4. Siempre aplicar conceptos básicos como respaldo
            int conceptosBasicos = aplicarConceptosBasicos(unidad, valorUnidad, fechaTraslado, nombreArchivo);
            conceptosAplicados += conceptosBasicos;

        } catch (Exception e) {
            System.err.println("Error en aplicación manual de conceptos: " + e.getMessage());
        }

        return conceptosAplicados;
    }

    /**
     * Aplica un concepto específico a una unidad
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void aplicarConcepto(Unidad unidad, ConceptoCobro concepto, Double monto,
                                 LocalDate fecha, String archivoOrigen, String observaciones) {
        try {
            CobroDetalle detalle = new CobroDetalle();
            detalle.setUnidad(unidad);
            detalle.setConcepto(concepto);
            detalle.setMontoAplicado(monto);  // ← Cambiado de setMonto a setMontoAplicado

            detalle.setArchivoOrigen(archivoOrigen);


            cobroDetalleRepository.save(detalle);

        } catch (Exception e) {
            System.err.println("Error guardando concepto " + concepto.getNombre() +
                    " para unidad " + unidad.getNoSerie() + ": " + e.getMessage());
            throw e;
        }
    }
    /**
     * Valida una línea del archivo
     */
    private ResultadoValidacion validarLinea(String linea, int numeroLinea) {
        List<ErrorDetallado> errores = new ArrayList<>();

        // Validar que la línea no esté vacía
        if (linea == null || linea.trim().isEmpty()) {
            errores.add(new ErrorDetallado(numeroLinea, TipoError.FORMATO_INCORRECTO,
                    "Línea vacía", linea));
            return new ResultadoValidacion(false, errores);
        }

        String[] col = linea.split("\\|");

        // Validar número mínimo de columnas
        if (col.length < MIN_COLUMNS_REQUIRED) {
            errores.add(new ErrorDetallado(numeroLinea, TipoError.FORMATO_INCORRECTO,
                    String.format("Se esperaban al menos %d columnas, se encontraron %d",
                            MIN_COLUMNS_REQUIRED, col.length), linea));
            return new ResultadoValidacion(false, errores);
        }

        // Validar campos obligatorios
        String claveDistribuidora = col[0];
        String factura = col[1];
        String modeloNombre = col[2];
        String noSerie = col[4];

        // Validar clave distribuidora
        if (esVacioONulo(claveDistribuidora)) {
            errores.add(new ErrorDetallado(numeroLinea, TipoError.DATOS_OBLIGATORIOS_FALTANTES,
                    "Clave de distribuidora es obligatoria", linea, "claveDistribuidora", claveDistribuidora));
        }

        // Validar número de serie (VIN)
        if (esVacioONulo(noSerie)) {
            errores.add(new ErrorDetallado(numeroLinea, TipoError.VIN_VACIO,
                    "Número de serie (VIN) es obligatorio y no puede estar vacío", linea, "noSerie", noSerie));
        } else if (noSerie.trim().length() < 3) {
            errores.add(new ErrorDetallado(numeroLinea, TipoError.VIN_VACIO,
                    "Número de serie (VIN) debe tener al menos 3 caracteres", linea, "noSerie", noSerie));
        }

        // Validar modelo
        if (esVacioONulo(modeloNombre)) {
            errores.add(new ErrorDetallado(numeroLinea, TipoError.DATOS_OBLIGATORIOS_FALTANTES,
                    "Nombre del modelo es obligatorio", linea, "modeloNombre", modeloNombre));
        }

        // Validar factura
        if (esVacioONulo(factura)) {
            errores.add(new ErrorDetallado(numeroLinea, TipoError.DATOS_OBLIGATORIOS_FALTANTES,
                    "Número de factura es obligatorio", linea, "factura", factura));
        }

        // Validar fechas
        validarFecha(col[5], numeroLinea, "fechaFondeo", linea, errores);
        validarFecha(col[6], numeroLinea, "fechaInteres", linea, errores);

        // Validar valores numéricos
        validarDouble(col[8], numeroLinea, "valorUnidad", linea, errores, true);
        validarDouble(col[9], numeroLinea, "cuotaAsociacion", linea, errores, false);
        validarDouble(col[10], numeroLinea, "valorSeguro", linea, errores, false);
        validarDouble(col[11], numeroLinea, "tarifaUnica", linea, errores, false);
        validarInteger(col[7], numeroLinea, "dias", linea, errores, false);

        return new ResultadoValidacion(errores.isEmpty(), errores);
    }

    private boolean esVacioONulo(String valor) {
        return valor == null || valor.trim().isEmpty();
    }

    private LocalDate validarFecha(String fechaStr, int numeroLinea, String nombreCampo, String lineaOriginal, List<ErrorDetallado> errores) {
        if (esVacioONulo(fechaStr)) {
            errores.add(new ErrorDetallado(numeroLinea, TipoError.FECHA_INVALIDA,
                    String.format("Fecha %s es obligatoria", nombreCampo), lineaOriginal, nombreCampo, fechaStr));
            return null;
        }

        try {
            if (fechaStr.length() >= 8) {
                return LocalDate.parse(fechaStr, DateTimeFormatter.BASIC_ISO_DATE);
            } else {
                errores.add(new ErrorDetallado(numeroLinea, TipoError.FECHA_INVALIDA,
                        String.format("Formato de fecha inválido para %s: %s", nombreCampo, fechaStr),
                        lineaOriginal, nombreCampo, fechaStr));
                return null;
            }
        } catch (Exception e) {
            errores.add(new ErrorDetallado(numeroLinea, TipoError.FECHA_INVALIDA,
                    String.format("Fecha inválida para %s: %s - %s", nombreCampo, fechaStr, e.getMessage()),
                    lineaOriginal, nombreCampo, fechaStr));
            return null;
        }
    }

    private Double validarDouble(String valorStr, int numeroLinea, String nombreCampo, String lineaOriginal,
                                 List<ErrorDetallado> errores, boolean esObligatorio) {
        if (esVacioONulo(valorStr)) {
            if (esObligatorio) {
                errores.add(new ErrorDetallado(numeroLinea, TipoError.DATOS_OBLIGATORIOS_FALTANTES,
                        String.format("Campo %s es obligatorio", nombreCampo), lineaOriginal, nombreCampo, valorStr));
            }
            return 0.0;
        }

        try {
            Double valor = Double.parseDouble(valorStr.trim());
            if (esObligatorio && valor <= 0) {
                errores.add(new ErrorDetallado(numeroLinea, TipoError.VALOR_NUMERICO_INVALIDO,
                        String.format("Campo %s debe ser mayor que 0", nombreCampo), lineaOriginal, nombreCampo, valorStr));
            }
            return valor;
        } catch (Exception e) {
            errores.add(new ErrorDetallado(numeroLinea, TipoError.VALOR_NUMERICO_INVALIDO,
                    String.format("Valor numérico inválido para %s: %s", nombreCampo, valorStr),
                    lineaOriginal, nombreCampo, valorStr));
            return 0.0;
        }
    }

    private Integer validarInteger(String valorStr, int numeroLinea, String nombreCampo, String lineaOriginal,
                                   List<ErrorDetallado> errores, boolean esObligatorio) {
        if (esVacioONulo(valorStr)) {
            if (esObligatorio) {
                errores.add(new ErrorDetallado(numeroLinea, TipoError.DATOS_OBLIGATORIOS_FALTANTES,
                        String.format("Campo %s es obligatorio", nombreCampo), lineaOriginal, nombreCampo, valorStr));
            }
            return 0;
        }

        try {
            return Integer.parseInt(valorStr.trim());
        } catch (Exception e) {
            errores.add(new ErrorDetallado(numeroLinea, TipoError.VALOR_NUMERICO_INVALIDO,
                    String.format("Valor entero inválido para %s: %s", nombreCampo, valorStr),
                    lineaOriginal, nombreCampo, valorStr));
            return 0;
        }
    }

    /**
     * Divide una lista en lotes más pequeños
     */
    private List<List<String>> dividirEnLotes(List<String> lista, int tamanoLote) {
        List<List<String>> lotes = new ArrayList<>();
        for (int i = 0; i < lista.size(); i += tamanoLote) {
            lotes.add(new ArrayList<>(
                    lista.subList(i, Math.min(i + tamanoLote, lista.size()))
            ));
        }
        return lotes;
    }

    // *** MÉTODOS NECESARIOS ***
    @Retryable(
            value = {PessimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2)
    )
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Modelo buscarOCrearModelo(String nombreModelo) {
        try {
            return unidadService.buscarOCrearModeloPorNombre(nombreModelo);
        } catch (Exception e) {
            System.err.println("Error al buscar/crear modelo: " + nombreModelo + " - " + e.getMessage());
            return null;
        }
    }

    @Retryable(
            value = {PessimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2)
    )
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Distribuidor buscarOCrearDistribuidor(String claveDistribuidora) {
        try {
            Optional<Distribuidor> distribuidorOpt = distribuidorService.buscarPorClaveExacta(claveDistribuidora);

            if (distribuidorOpt.isPresent()) {
                return distribuidorOpt.get();
            } else {
                CrearDistribuidorDTO nuevoDistDTO = new CrearDistribuidorDTO();
                nuevoDistDTO.setClaveDistribuidora(claveDistribuidora);
                nuevoDistDTO.setNombreDistribuidora("Distribuidor " + claveDistribuidora);
                nuevoDistDTO.setContacto("Pendiente");
                nuevoDistDTO.setCorreo("pendiente@example.com");
                nuevoDistDTO.setSucursal("Principal");

                System.out.println("➕ Creando nuevo distribuidor con clave: " + claveDistribuidora);
                return distribuidorService.crear(nuevoDistDTO);
            }
        } catch (Exception e) {
            System.err.println("Error al buscar/crear distribuidor: " + claveDistribuidora + " - " + e.getMessage());
            return null;
        }
    }

    @Retryable(
            value = {PessimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2)
    )
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Unidad guardarUnidad(Unidad unidad) {
        return unidadService.guardar(unidad);
    }

    public Seguro crearSeguro(Unidad unidad, Distribuidor distribuidor, String factura,
                              Double valorSeguro, Double seguroDistribuidor, Double valorUnidad,
                              LocalDate fechaFactura) {
        Seguro seguro = new Seguro();
        seguro.setUnidad(unidad);
        seguro.setDistribuidor(distribuidor);
        seguro.setFactura(factura);
        seguro.setValorSeguro(valorSeguro);
        seguro.setSeguroDistribuidor(seguroDistribuidor);
        seguro.setCuotaSeguro(valorUnidad * 0.0324);
        seguro.setFechaFactura(fechaFactura);
        return seguro;
    }

    @Retryable(
            value = {PessimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2)
    )
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void guardarSeguro(Seguro seguro) {
        seguroService.guardar(seguro);
    }

    // *** MÉTODOS DE UTILIDAD ***
    private Double convertirADouble(String raw) {
        if (raw == null || raw.trim().isEmpty() ||
                raw.contains("SIN CVE") || raw.contains("N/A")) {
            return 0.0;
        }

        try {
            return Double.parseDouble(raw.trim());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private LocalDate parseFecha(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }

        try {
            if (raw.length() >= 8) {
                return LocalDate.parse(raw, DateTimeFormatter.BASIC_ISO_DATE);
            }
        } catch (Exception e) {
            System.err.println("Fecha inválida: " + raw);
        }
        return null;
    }

    private Double parseDouble(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return 0.0;
        }

        try {
            return Double.parseDouble(raw.trim());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private Integer parseInt(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return 0;
        }

        try {
            return Integer.parseInt(raw.trim());
        } catch (Exception e) {
            return 0;
        }
    }
}