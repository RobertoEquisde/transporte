package com.adavec.transporte.controller;

import com.adavec.transporte.dto.ActualizarCobroRequest;
import com.adavec.transporte.dto.CobroDTO;
import com.adavec.transporte.dto.CrearCobroRequest;
import com.adavec.transporte.dto.HistorialCobroDTO;
import com.adavec.transporte.model.Cobros;
import com.adavec.transporte.service.CobrosService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cobros")
public class CobrosController {

    private final CobrosService cobrosService;

    public CobrosController(CobrosService cobrosService) {
        this.cobrosService = cobrosService;
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody CrearCobroRequest request) {
        Cobros nuevo = cobrosService.registrarCobro(request);
        return ResponseEntity.status(HttpStatus.CREATED).body("Cobro registrado para la unidad: " + nuevo.getUnidad().getNoSerie());
    }
    @GetMapping("/historial/{unidadId}")
    public ResponseEntity<List<HistorialCobroDTO>> getHistorialCobros(@PathVariable Long unidadId) {
        List<HistorialCobroDTO> historial = cobrosService.getHistorialCobros(unidadId);

        // Si no hay historial, devuelve 204 No Content
        if (historial == null || historial.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        // Si hay historial, devuelve 200 OK con los datos
        return ResponseEntity.ok(historial);
    }
//    @GetMapping("/unidad/{unidadId}")
//    public List<CobroDTO> listarPorUnidad(@PathVariable Integer unidadId) {
//        return cobrosService.obtenerPorUnidad(unidadId);
//    }
    // Método para actualizar un cobro
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(
            @PathVariable Integer id,
            @RequestBody CrearCobroRequest request) {
        Cobros actualizado = cobrosService.actualizarCobro(id, request);
        return ResponseEntity.ok("Cobro actualizado para la unidad: " + actualizado.getUnidad().getNoSerie());
    }
    // Nuevo método para actualizar por ID de unidad
    @PutMapping("/unidad/{unidadId}")
    public ResponseEntity<?> actualizarPorUnidad(
            @PathVariable Integer unidadId,
            @RequestBody ActualizarCobroRequest request) {
        Cobros actualizado = cobrosService.actualizarCobroPorUnidad(unidadId, request);
        return ResponseEntity.ok("Cobro actualizado para la unidad: " + actualizado.getUnidad().getNoSerie());
    }
    // Método para eliminar un cobro
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        cobrosService.eliminarCobro(id);
        return ResponseEntity.ok("Cobro con ID " + id + " eliminado exitosamente");
    }
}
