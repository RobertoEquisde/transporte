package com.adavec.transporte.controller;

import com.adavec.transporte.dto.CobroDTO;
import com.adavec.transporte.dto.CrearCobroRequest;
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

    @GetMapping("/unidad/{unidadId}")
    public List<CobroDTO> listarPorUnidad(@PathVariable Integer unidadId) {
        return cobrosService.obtenerPorUnidad(unidadId);
    }
}
