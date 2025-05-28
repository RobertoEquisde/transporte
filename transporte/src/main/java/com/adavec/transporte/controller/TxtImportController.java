package com.adavec.transporte.controller;

import com.adavec.transporte.dto.CrearDistribuidorDTO;
import com.adavec.transporte.model.*;
import com.adavec.transporte.service.CobrosService;
import com.adavec.transporte.service.DistribuidorService;
import com.adavec.transporte.service.SeguroService;
import com.adavec.transporte.service.UnidadService;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/importar")
@EnableRetry
public class TxtImportController {

    private final UnidadService unidadService;
    private final SeguroService seguroService;
    private final CobrosService cobrosService;
    private final DistribuidorService distribuidorService;

    // Tamaño del lote para procesar
    private static final int BATCH_SIZE = 5;
    private static final int MIN_COLUMNS_REQUIRED = 13;

    public TxtImportController(UnidadService unidadService, SeguroService seguroService,
                               CobrosService cobrosService, DistribuidorService distribuidorService) {
        this.unidadService = unidadService;
        this.seguroService = seguroService;
        this.cobrosService = cobrosService;
        this.distribuidorService = distribuidorService;
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
        ERROR_PROCESAMIENTO("ERROR_PROCESAMIENTO");

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
                    Map<String, Object> resultadoLote = procesarLoteDeLineas(lote, lineaGlobal);

                    importados.addAndGet((int) resultadoLote.get("importados"));
                    distribuidoresCreados.addAndGet((int) resultadoLote.get("distribuidoresCreados"));

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

            // Construir respuesta
            resultado.put("unidadesImportadas", importados.get());
            resultado.put("distribuidoresCreados", distribuidoresCreados.get());
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
                            error.getTipoError() == TipoError.DISTRIBUIDOR_INVALIDO)
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

                // Incluir todos los errores para contexto completo
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
        LocalDate fechaFondeo = validarFecha(col[5], numeroLinea, "fechaFondeo", linea, errores);
        LocalDate fechaInteres = validarFecha(col[6], numeroLinea, "fechaInteres", linea, errores);

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

    /**
     * Procesa un lote de líneas con manejo de errores mejorado
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public Map<String, Object> procesarLoteDeLineas(List<String> lineas, int lineaInicial) {
        int importados = 0;
        int distribuidoresCreados = 0;
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

                // Continuar con el procesamiento si no hay errores
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

                // Guardar seguro y cobros
                Seguro seguro = crearSeguro(unidad, distribuidor, factura, valorSeguro,
                        cuotaAsociacion, valorUnidad, fechaFondeo);
                guardarSeguro(seguro);

                Cobros cobro = crearCobros(unidad, cuotaAsociacion, fechaTraslado,
                        fechaInteres, dias, tarifaUnica, fondoEstrella);
                guardarCobros(cobro);

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
        resultado.put("errores", errores);

        return resultado;
    }

    // Los métodos restantes (buscarOCrearModelo, buscarOCrearDistribuidor, etc.) permanecen igual
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

    public Cobros crearCobros(Unidad unidad, Double cuotaAsociacion, LocalDate fechaTraslado,
                              LocalDate fechaInteres, Integer dias, Double tarifaUnica, Double fondoEstrella) {
        Cobros cobro = new Cobros();
        cobro.setUnidad(unidad);
        cobro.setCuotaAsociacion(cuotaAsociacion);
        cobro.setFondoEstrella(fondoEstrella);
        cobro.setFechaTraslado(fechaTraslado);
        cobro.setFechaProceso(LocalDate.now());
        cobro.setFechaInteres(fechaInteres);
        cobro.setDias(dias);
        cobro.setTarifaUnica(tarifaUnica);
        cobro.setFondoEstrella(fondoEstrella);
        return cobro;
    }

    @Retryable(
            value = {PessimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2)
    )
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void guardarCobros(Cobros cobro) {
        cobrosService.guardar(cobro);
    }

    // Métodos de utilidad que permanecen igual
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