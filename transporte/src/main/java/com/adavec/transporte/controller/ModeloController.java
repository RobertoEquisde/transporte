package com.adavec.transporte.controller;

import com.adavec.transporte.dto.CrearModeloRequest;
import com.adavec.transporte.dto.ModeloDTO;
import com.adavec.transporte.model.Modelo;
import com.adavec.transporte.service.ModeloService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/modelos")
public class ModeloController {

    private final ModeloService modeloService;

    public ModeloController(ModeloService modeloService) {
        this.modeloService = modeloService;
    }

    @GetMapping
    public List<ModeloDTO> listar() {
        return modeloService.obtenerTodos().stream().map(modelo -> {
            ModeloDTO dto = new ModeloDTO();
            dto.setId(modelo.getId());
            dto.setNombre(modelo.getNombre());
            dto.setUso(modelo.getUso());
            return dto;
        }).collect(Collectors.toList());
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody CrearModeloRequest request) {
        Modelo nuevo = modeloService.crearModelo(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        modeloService.eliminarModelo(id);
        return ResponseEntity.noContent().build();
    }


}
