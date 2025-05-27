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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/importar")
@EnableRetry
public class TxtImportController {

    private final UnidadService unidadService;
    private final SeguroService seguroService;
    private final CobrosService cobrosService;
    private final DistribuidorService distribuidorService;

    // Tamaño del lote para procesar - ajustable según la carga de tu servidor
    private static final int BATCH_SIZE = 5;

    public TxtImportController(UnidadService unidadService, SeguroService seguroService,
                               CobrosService cobrosService, DistribuidorService distribuidorService) {
        this.unidadService = unidadService;
        this.seguroService = seguroService;
        this.cobrosService = cobrosService;
        this.distribuidorService = distribuidorService;
    }

    @PostMapping("/txt")
    public ResponseEntity<?> importarDesdeTxt(@RequestParam("archivo") MultipartFile archivo) {
        try {
            // Leer todas las líneas del archivo
            List<String> lineas = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(archivo.getInputStream()))) {
                String linea;
                while ((linea = reader.readLine()) != null) {
                    lineas.add(linea);
                }
            }

            // Estadísticas de importación
            Map<String, Object> resultado = new HashMap<>();
            AtomicInteger importados = new AtomicInteger(0);
            AtomicInteger omitidos = new AtomicInteger(0);
            AtomicInteger distribuidoresCreados = new AtomicInteger(0);
            AtomicInteger errores = new AtomicInteger(0);
            List<String> erroresLog = new ArrayList<>();
            List<String> duplicados = new ArrayList<>(); // Nueva lista para duplicados

            // Dividir en lotes más pequeños para procesar
            List<List<String>> lotes = dividirEnLotes(lineas, BATCH_SIZE);

            System.out.println("Iniciando importación de " + lineas.size() +
                    " líneas divididas en " + lotes.size() + " lotes");

            // Procesar cada lote en una transacción separada
            for (int i = 0; i < lotes.size(); i++) {
                List<String> lote = lotes.get(i);
                try {
                    System.out.println("Procesando lote " + (i+1) + " de " + lotes.size());
                    Map<String, Object> resultadoLote = procesarLoteDeLineas(lote);

                    importados.addAndGet((int) resultadoLote.get("importados"));
                    omitidos.addAndGet((int) resultadoLote.get("omitidos"));
                    distribuidoresCreados.addAndGet((int) resultadoLote.get("distribuidoresCreados"));

                    if (resultadoLote.containsKey("erroresLog")) {
                        @SuppressWarnings("unchecked")
                        List<String> lotesErrores = (List<String>) resultadoLote.get("erroresLog");
                        erroresLog.addAll(lotesErrores);
                    }

                    // Agregar duplicados encontrados en este lote
                    if (resultadoLote.containsKey("duplicados")) {
                        @SuppressWarnings("unchecked")
                        List<String> lotesDuplicados = (List<String>) resultadoLote.get("duplicados");
                        duplicados.addAll(lotesDuplicados);
                    }
                } catch (Exception e) {
                    errores.incrementAndGet();
                    String mensajeError = "Error al procesar lote " + (i+1) + ": " + e.getMessage();
                    System.err.println(mensajeError);
                    erroresLog.add(mensajeError);
                    e.printStackTrace();
                }
            }

            // Construir respuesta con resultados
            resultado.put("unidadesImportadas", importados.get());
            resultado.put("unidadesOmitidas", omitidos.get());
            resultado.put("distribuidoresCreados", distribuidoresCreados.get());
            resultado.put("lotesConErrores", errores.get());

            // Solo incluir log de errores si hay errores
            if (!erroresLog.isEmpty()) {
                resultado.put("erroresLog", erroresLog);
            }

            // Verificar si hay duplicados y manejar como error
            if (!duplicados.isEmpty()) {
                // Mostrar duplicados en consola
                System.err.println("==========================================");
                System.err.println("⚠️  UNIDADES DUPLICADAS ENCONTRADAS ⚠️");
                System.err.println("==========================================");
                for (String duplicado : duplicados) {
                    System.err.println("🔄 " + duplicado);
                }
                System.err.println("==========================================");
                System.err.println("Total de duplicados: " + duplicados.size());
                System.err.println("==========================================");

                // Retornar error con información de duplicados
                resultado.put("mensaje", "Importación fallida: Se encontraron unidades duplicadas");
                resultado.put("duplicadosEncontrados", duplicados.size());
                resultado.put("duplicados", duplicados);

                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(resultado);
            }

            // Si no hay duplicados, retornar éxito
            resultado.put("mensaje", "Importación completada exitosamente.");
            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "mensaje", "Error durante la importación",
                            "error", e.getMessage()
                    ));
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
     * Procesa un lote de líneas con manejo de errores
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public Map<String, Object> procesarLoteDeLineas(List<String> lineas) {
        int importados = 0;
        int omitidos = 0;
        int distribuidoresCreados = 0;
        List<String> erroresLog = new ArrayList<>();
        List<String> duplicados = new ArrayList<>(); // Nueva lista para duplicados del lote

        for (String linea : lineas) {
            try {
                String[] col = linea.split("\\|");

                if (col.length < 13) {
                    String mensajeError = "⚠️ Línea con formato incorrecto: " + linea;
                    System.out.println(mensajeError);
                    erroresLog.add(mensajeError);
                    omitidos++;
                    continue;
                }

                String claveDistribuidora = col[0];
                String factura = col[1];
                String modeloNombre = col[2];
                String noSerie = col[4];

                // Validar datos obligatorios
                if (noSerie == null || noSerie.trim().isEmpty()) {
                    String mensajeError = "⚠️ Número de serie vacío en línea: " + linea;
                    System.out.println(mensajeError);
                    erroresLog.add(mensajeError);
                    omitidos++;
                    continue;
                }

                LocalDate fechaFondeo = parseFecha(col[5]);
                LocalDate fechaInteres = parseFecha(col[6]);
                Integer dias = parseInt(col[7]);
                Double valorUnidad = parseDouble(col[8]);
                Double cuotaAsociacion = parseDouble(col[9]);
                Double valorSeguro = parseDouble(col[10]);
                Double tarifaUnica = parseDouble(col[11]);

                // Manejar el campo de fondo estrella que puede ser texto
                Double fondoEstrella = 0.0;
                if (col.length > 12) {
                    fondoEstrella = convertirADouble(col[12]);
                }

                // Fecha de interés puede estar en posición 15 o usar fecha fondeo por defecto
                LocalDate fechaTraslado = fechaFondeo;
                if (col.length > 15) {
                    LocalDate fechaTemp = parseFecha(col[15]);
                    if (fechaTemp != null) {
                        fechaTraslado = fechaTemp;
                    }
                }

                // Verificar existencia de unidad primero para evitar trabajo innecesario
                Optional<Unidad> existente = unidadService.obtenerPorNoSerie(noSerie);
                if (existente.isPresent()) {
                    String mensajeDuplicado = "Número de serie: " + noSerie +
                            " | Distribuidor: " + claveDistribuidora +
                            " | Modelo: " + modeloNombre;
                    duplicados.add(mensajeDuplicado);
                    omitidos++;
                    continue; // Continúa para recopilar todos los duplicados
                }

                // Procesar modelo y distribuidor con reintentos
                Modelo modelo = buscarOCrearModelo(modeloNombre);

                boolean esNuevoDistribuidor = false;
                Distribuidor distribuidor = buscarOCrearDistribuidor(claveDistribuidora);
                if (distribuidor != null && (distribuidor.getId() == null || distribuidor.getId() == 0)) {
                    esNuevoDistribuidor = true;
                }

                // Crear y guardar unidad
                Unidad unidad = new Unidad();
                unidad.setNoSerie(noSerie);
                unidad.setModelo(modelo);
                unidad.setDistribuidor(distribuidor);
                unidad.setDebisFecha(fechaFondeo);
                unidad.setValorUnidad(valorUnidad);

                // Guardar la unidad con reintentos
                unidad = guardarUnidad(unidad);
                importados++;

                if (esNuevoDistribuidor) {
                    distribuidoresCreados++;
                }

                // Guardar seguro con reintentos
                Seguro seguro = crearSeguro(unidad, distribuidor, factura, valorSeguro,
                        cuotaAsociacion, valorUnidad, fechaFondeo);
                guardarSeguro(seguro);

                // Guardar cobros con reintentos
                Cobros cobro = crearCobros(unidad, cuotaAsociacion, fechaTraslado,
                        fechaInteres, dias, tarifaUnica, fondoEstrella);
                guardarCobros(cobro);

            } catch (Exception e) {
                String mensajeError = "Error procesando línea: " + linea + ". Causa: " + e.getMessage();
                System.err.println(mensajeError);
                erroresLog.add(mensajeError);
                omitidos++;
            }
        }

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("importados", importados);
        resultado.put("omitidos", omitidos);
        resultado.put("distribuidoresCreados", distribuidoresCreados);
        resultado.put("erroresLog", erroresLog);
        resultado.put("duplicados", duplicados); // Agregar duplicados al resultado

        return resultado;
    }

    /**
     * Método con reintento para buscar o crear modelo
     */
    @Retryable(
            value = {PessimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2)
    )
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Modelo buscarOCrearModelo(String nombreModelo) {
        return unidadService.buscarOCrearModeloPorNombre(nombreModelo);
    }

    /**
     * Método con reintento para buscar o crear distribuidor
     */
    @Retryable(
            value = {PessimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2)
    )
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Distribuidor buscarOCrearDistribuidor(String claveDistribuidora) {
        // Intentar encontrar por clave exacta primero
        Optional<Distribuidor> distribuidorOpt = distribuidorService.buscarPorClaveExacta(claveDistribuidora);

        if (distribuidorOpt.isPresent()) {
            return distribuidorOpt.get();
        } else {
            // Crear nuevo distribuidor con información básica
            CrearDistribuidorDTO nuevoDistDTO = new CrearDistribuidorDTO();
            nuevoDistDTO.setClaveDistribuidora(claveDistribuidora);
            nuevoDistDTO.setNombreDistribuidora("Distribuidor " + claveDistribuidora);

            // Valores por defecto
            nuevoDistDTO.setContacto("Pendiente");
            nuevoDistDTO.setCorreo("pendiente@example.com");
            nuevoDistDTO.setSucursal("Principal");

            System.out.println("➕ Creando nuevo distribuidor con clave: " + claveDistribuidora);
            return distribuidorService.crear(nuevoDistDTO);
        }
    }

    /**
     * Método con reintento para guardar unidad
     */
    @Retryable(
            value = {PessimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2)
    )
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Unidad guardarUnidad(Unidad unidad) {
        return unidadService.guardar(unidad);
    }

    /**
     * Método para crear objeto Seguro
     */
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

    /**
     * Método con reintento para guardar seguro
     */
    @Retryable(
            value = {PessimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2)
    )
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void guardarSeguro(Seguro seguro) {
        seguroService.guardar(seguro);
    }

    /**
     * Método para crear objeto Cobros
     */
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

    /**
     * Método con reintento para guardar cobros
     */
    @Retryable(
            value = {PessimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2)
    )
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void guardarCobros(Cobros cobro) {
        cobrosService.guardar(cobro);
    }

    private Double convertirADouble(String raw) {
        if (raw == null || raw.trim().isEmpty() ||
                raw.contains("SIN CVE") || raw.contains("N/A")) {
            return 0.0;
        }

        try {
            return Double.parseDouble(raw.trim());
        } catch (Exception e) {
            System.out.println("⚠️ Valor no numérico: '" + raw + "', se asigna 0.0");
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