package com.adavec.transporte.controller;

import com.adavec.transporte.dto.ActualizarTarifaRequest;
import com.adavec.transporte.dto.TarifaHistorialDTO;
import com.adavec.transporte.model.TarifaConcepto;
import com.adavec.transporte.model.Unidad;
import com.adavec.transporte.service.TarifaConceptoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tarifas")
public class TarifaController {

    @Autowired
    private TarifaConceptoService tarifaService;

    /**
     * Listar todas las tarifas vigentes
     */
    @GetMapping("/vigentes")
    public ResponseEntity<Map<String, Double>> obtenerTarifasVigentes() {
        return ResponseEntity.ok(tarifaService.obtenerTarifasVigentes());
    }
    @PostMapping("/{nombreConcepto}")
    public ResponseEntity<?> crearTarifa(
            @PathVariable String nombreConcepto,
            @RequestBody ActualizarTarifaRequest request) {

        if (request.getNuevoValor() == null || request.getNuevoValor() <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "El valor debe ser mayor a 0"));
        }

        LocalDate fechaInicio = request.getFechaInicio() != null
                ? request.getFechaInicio()
                : LocalDate.now();

        try {
            TarifaConcepto nueva = tarifaService.crearTarifa(
                    nombreConcepto,
                    request.getNuevoValor(),
                    fechaInicio
            );

            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Tarifa creada exitosamente");
            response.put("id", nueva.getId());
            response.put("concepto", nombreConcepto);
            response.put("valor", nueva.getValor());
            response.put("vigenciaDesde", nueva.getFechaInicio());
            response.put("tipoCalculo", nueva.getConcepto().getTipoCalculo().toString());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Ver historial de un concepto
     */
    @GetMapping("/{nombreConcepto}/historial")
    public ResponseEntity<List<TarifaHistorialDTO>> verHistorial(
            @PathVariable String nombreConcepto) {
        return ResponseEntity.ok(tarifaService.obtenerHistorial(nombreConcepto));
    }

}