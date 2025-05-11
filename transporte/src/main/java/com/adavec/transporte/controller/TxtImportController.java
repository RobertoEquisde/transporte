package com.adavec.transporte.controller;

import com.adavec.transporte.model.*;
import com.adavec.transporte.service.CobrosService;
import com.adavec.transporte.service.SeguroService;
import com.adavec.transporte.service.UnidadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/importar")
public class TxtImportController {

    private final UnidadService unidadService;
    private final SeguroService seguroService;
    private final CobrosService cobrosService;

    public TxtImportController(UnidadService unidadService,
                               SeguroService seguroService,
                               CobrosService cobrosService) {
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

                // Campos base
                String claveDistribuidora = col[0];
                String factura = col[1];
                String modeloNombre = col[2];
                String noSerie = col[4];
                LocalDate fechaFondeo = LocalDate.parse(col[5], DateTimeFormatter.BASIC_ISO_DATE);
                LocalDate fechaTraslado = LocalDate.parse(col[6], DateTimeFormatter.BASIC_ISO_DATE);
                Integer dias = Integer.parseInt(col[7]);
                Double valorUnidad = Double.parseDouble(col[8]);
                Double cuotaAsociacion = Double.parseDouble(col[9]);
                Double valorSeguro = Double.parseDouble(col[10]);
                Double pension = Double.parseDouble(col[11]);
                Double siniestro = Double.parseDouble(col[12]);
                String observaciones = col.length > 13 ? col[13] : null;

                // Buscar modelo y distribuidor
                Modelo modelo = unidadService.buscarOCrearModeloPorNombre(modeloNombre);
                Distribuidor distribuidor = unidadService.buscarDistribuidorPorClave(claveDistribuidora);

                // Registrar unidad
                Unidad unidad = new Unidad();
                unidad.setNoSerie(noSerie);
                unidad.setModelo(modelo);
                unidad.setDistribuidor(distribuidor);
                unidad.setDebisFecha(fechaFondeo);
                unidad.setValorUnidad(valorUnidad);
                unidad.setComentario(observaciones);
                unidad = unidadService.guardar(unidad);

                // Registrar seguro
                Seguro seguro = new Seguro();
                seguro.setUnidad(unidad);
                seguro.setDistribuidor(distribuidor);
                seguro.setFactura(factura);
                seguro.setValorSeguro(valorSeguro);
                seguro.setCuotaSeguro(valorUnidad * 0.0324); // 3.24%
                seguro.setSeguroDistribuidor(cuotaAsociacion); // o reemplazar con otro valor si aplica
                seguroService.guardar(seguro);

                // Registrar cobros
                Cobros cobro = new Cobros();
                cobro.setUnidad(unidad);
                cobro.setTarifaUnica(22900.0); // si es fija, cámbiala por valor real si viene del archivo
                cobro.setCuotaAsociacion(cuotaAsociacion);
                cobro.setFondoEstrella(0.0); // puedes extraerlo si está disponible
                cobro.setDias(dias);
                cobro.setFechaTraslado(fechaTraslado);
                cobrosService.guardar(cobro);
            }

            return ResponseEntity.ok("Importación completada con éxito.");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error durante la importación: " + e.getMessage());
        }
    }
}
