package com.adavec.transporte.controller;

import com.adavec.transporte.dto.*;
import com.adavec.transporte.model.CobroDetalle;
import com.adavec.transporte.model.Cobros;
import com.adavec.transporte.service.CobroDetalleService;
import com.adavec.transporte.service.CobrosService;
import com.adavec.transporte.service.DesgloseCobroService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cobros")
public class CobrosController {

    @Autowired
    private CobroDetalleService cobroDetalleService;

    @Autowired
    private DesgloseCobroService desgloseCobroService;

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody CrearCobroRequest request) {
        try {
            DatosImportacion datos = convertirRequest(request);
            ResultadoDesglose resultado = desgloseCobroService.desglosarCobros(datos);

            if (resultado.isExitoso()) {
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(Map.of(
                                "mensaje", "Cobro registrado exitosamente",
                                "unidad", request.getUnidadId(),
                                "conceptosAplicados", resultado.getDetalles().size(),
                                "totalDesglosado", resultado.getTotalDesglosado(),
                                "advertencias", resultado.getAdvertencias()
                        ));
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", resultado.getError()));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al registrar cobro: " + e.getMessage()));
        }
    }

    @PostMapping("/manual/{unidadId}")
    public ResponseEntity<?> crearManual(@PathVariable Integer unidadId,
                                         @RequestBody CrearCobroManualRequest request) {
        try {
            CobroDetalle detalle = cobroDetalleService.crearCobroManual(
                    unidadId, request.getConceptoId(),
                    request.getMonto(), request.getFechaTraslado()
            );

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("Cobro registrado con ID: " + detalle.getId());
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("Error: " + e.getMessage());
        }
    }
    @GetMapping("/historial/{unidadId}")
    public ResponseEntity<List<HistorialCobroDTO>> getHistorialCobros(@PathVariable Integer unidadId) {
        List<HistorialCobroDTO> historial = cobroDetalleService.getHistorialDetallado(unidadId);
        if (historial == null || historial.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(historial);
    }



    @GetMapping("/unidad/{unidadId}/detalles")
    public ResponseEntity<List<CobroDetalleDTO>> listarDetallesPorUnidad(@PathVariable Integer unidadId) {
        List<CobroDetalleDTO> detalles = cobroDetalleService.obtenerDetallesPorUnidad(unidadId);
        if (detalles.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(detalles);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Integer id, @RequestBody ActualizarCobroDetalleRequest request) {
        try {
            CobroDetalle actualizado = cobroDetalleService.actualizarCobro(id, request.getMonto());
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Cobro actualizado",
                    "id", actualizado.getId(),
                    "concepto", actualizado.getConcepto().getDescripcion(),
                    "montoAnterior", request.getMontoAnterior(),
                    "montoNuevo", actualizado.getMontoAplicado()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }




    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        try {
            cobroDetalleService.eliminarCobro(id);
            return ResponseEntity.ok(Map.of("mensaje", "Cobro eliminado exitosamente"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/unidad/{unidadId}")
    public ResponseEntity<?> eliminarPorUnidad(@PathVariable Integer unidadId) {
        try {
            int eliminados = cobroDetalleService.eliminarCobrosPorUnidad(unidadId);
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Cobros eliminados exitosamente",
                    "cantidadEliminada", eliminados
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/unidad/{unidadId}/resumen-conceptos")
    public ResponseEntity<Map<String, Double>> getResumenConceptos(@PathVariable Integer unidadId) {
        Map<String, Double> resumen = cobroDetalleService.obtenerResumenPorConceptos(unidadId);
        if (resumen.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(resumen);
    }

    private DatosImportacion convertirRequest(CrearCobroRequest request) {
        DatosImportacion datos = new DatosImportacion();
        datos.setUnidad(cobroDetalleService.obtenerUnidadPorId(request.getUnidadId()));
        datos.setTarifaUnica(request.getTarifaUnica());
        datos.setCuotaAsociacion(request.getCuotaAsociacion());
        datos.setFondoEstrella(request.getFondoEstrella());

        datos.setFechaTraslado(request.getFechaTraslado());
        datos.setArchivoOrigen("MANUAL_" + System.currentTimeMillis());
        return datos;
    }
    @GetMapping("/unidad/{unidadId}")
    public ResponseEntity<List<CobroGestionDTO>> listarCobrosSimples(@PathVariable Integer unidadId) {
        List<CobroGestionDTO> cobros = cobroDetalleService.obtenerCobrosSimplesPorUnidad(unidadId);

        if (cobros.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(cobros);
    }
    // 2. Endpoint PATCH en tu CobrosController
    @PatchMapping("/{id}")
    public ResponseEntity<?> actualizarParcial(@PathVariable Integer id,
                                               @RequestBody ActualizarCobroParcialRequest request) {
        try {
            CobroDetalle actualizado = cobroDetalleService.actualizarCobroParcial(id, request);

            return ResponseEntity.ok(Map.of(
                    "mensaje", "Cobro actualizado exitosamente",
                    "id", actualizado.getId(),
                    "concepto", actualizado.getConcepto().getDescripcion(),
                    "montoActual", actualizado.getMontoAplicado(),

                    "camposActualizados", obtenerCamposActualizados(request)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private List<String> obtenerCamposActualizados(ActualizarCobroParcialRequest request) {
        List<String> campos = new ArrayList<>();
        if (request.getMonto() != null) campos.add("monto");
        if (request.getFechaTraslado() != null) campos.add("fechaTraslado");
        return campos;
    }
}