package com.adavec.transporte.controller;

import com.adavec.transporte.model.*;
import com.adavec.transporte.service.CobrosService;
import com.adavec.transporte.service.SeguroService;
import com.adavec.transporte.service.UnidadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/importar")
public class TxtImportController {

    private final UnidadService unidadService;
    private final SeguroService seguroService;
    private final CobrosService cobrosService;

    public TxtImportController(UnidadService unidadService, SeguroService seguroService, CobrosService cobrosService) {
        this.unidadService = unidadService;
        this.seguroService = seguroService;
        this.cobrosService = cobrosService;
    }

    @PostMapping("/txt")
    public ResponseEntity<?> importarDesdeTxt(@RequestParam("archivo") MultipartFile archivo) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(archivo.getInputStream()))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                String[] col = linea.split("\\|");

                String claveDistribuidora = col[0];
                String factura = col[1];
                String modeloNombre = col[2];
                String noSerie = col[4];

                LocalDate fechaFondeo = parseFecha(col[5]);
                LocalDate fechaTraslado = parseFecha(col[6]);
                Integer dias = parseInt(col[7]);
                Double valorUnidad = parseDouble(col[8]);
                Double cuotaAsociacion = parseDouble(col[9]);
                Double valorSeguro = parseDouble(col[10]);
                Double tarifaunica = parseDouble(col[11]);
                Double fondoEstrella = convertirADouble(col[12]);




                LocalDate fechaInteres = col.length > 15 ? parseFecha(col[15]) : fechaFondeo;


                // Modelo y distribuidor
                Modelo modelo = unidadService.buscarOCrearModeloPorNombre(modeloNombre);
                Distribuidor distribuidor = unidadService.buscarDistribuidorPorClave(claveDistribuidora);

                // Guardar unidad
                Optional<Unidad> existente = unidadService.obtenerPorNoSerie(noSerie);
                Unidad unidad;

                if (existente.isPresent()) {
                    System.out.println("⚠️ Unidad duplicada encontrada con noSerie: " + noSerie + ". Registro omitido.");
                    continue;
                } else {
                    unidad = new Unidad();
                    unidad.setNoSerie(noSerie);
                    unidad.setModelo(modelo);
                    unidad.setDistribuidor(distribuidor);
                    unidad.setDebisFecha(fechaFondeo);
                    unidad.setValorUnidad(valorUnidad);

                    unidad = unidadService.guardar(unidad);
                }

                // Guardar seguro
                Seguro seguro = new Seguro();
                seguro.setUnidad(unidad);
                seguro.setDistribuidor(distribuidor);
                seguro.setFactura(factura);
                seguro.setValorSeguro(valorSeguro);
                seguro.setSeguroDistribuidor(cuotaAsociacion);
                seguro.setCuotaSeguro(valorUnidad * 0.0324);
                seguro.setFechaFactura(fechaFondeo);

                seguroService.guardar(seguro);

                // Guardar cobros
                Cobros cobro = new Cobros();
                cobro.setUnidad(unidad);
                cobro.setTarifaUnica(22900.0);
                cobro.setCuotaAsociacion(cuotaAsociacion);
                cobro.setFondoEstrella(fondoEstrella);
                cobro.setFechaTraslado(fechaTraslado);
                cobro.setFechaProceso(LocalDate.now());
                cobro.setFechaInteres(fechaInteres);
                cobro.setDias(dias); // o calcular con DAYS.between si prefieres
                cobro.setTarifaUnica(tarifaunica);
                cobro.setFondoEstrella(fondoEstrella);
                cobrosService.guardar(cobro);
            }

            return ResponseEntity.ok(Map.of("mensaje", "Importación completada con éxito."));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error durante la importación: " + e.getMessage());
        }
    }

    private Double convertirADouble(String raw) {
        try {
            return Double.parseDouble(raw.trim());
        } catch (Exception e) {
            System.out.println("⚠️ Valor no numérico en fondo estrella: '" + raw + "', se asigna 0.0");
            return 0.0;
        }
    }


    private LocalDate parseFecha(String raw) {
        try {
            if (raw != null && raw.length() >= 8) {
                return LocalDate.parse(raw, DateTimeFormatter.BASIC_ISO_DATE);
            }
        } catch (Exception e) {
            System.err.println("Fecha inválida: " + raw);
        }
        return null;
    }

    private Double parseDouble(String raw) {
        try {
            return Double.parseDouble(raw);
        } catch (Exception e) {
            return 0.0;
        }
    }

    private Integer parseInt(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (Exception e) {
            return 0;
        }
    }
}
