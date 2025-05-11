package com.adavec.transporte.controller;

import com.adavec.transporte.dto.CrearDistribuidorDTO;
import com.adavec.transporte.dto.DistribuidoraInfoDTO;
import com.adavec.transporte.model.Distribuidor;
import com.adavec.transporte.service.DistribuidorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/distribuidores")
public class DistribuidorController {

    private final DistribuidorService distribuidorService;

    public DistribuidorController(DistribuidorService distribuidorService) {
        this.distribuidorService = distribuidorService;
    }

    @GetMapping
    public List<DistribuidoraInfoDTO> listar() {
        return distribuidorService.obtenerTodos().stream().map(d -> {
            DistribuidoraInfoDTO dto = new DistribuidoraInfoDTO();
            dto.setId(d.getId());
            dto.setNombreDistribuidora(d.getNombreDistribuidora());
            dto.setClaveDistribuidora(d.getClaveDistribuidora());
            return dto;
        }).collect(Collectors.toList());
    }
    @GetMapping("/buscar")
    public List<DistribuidoraInfoDTO> buscarPorClave(@RequestParam("clave") String clave) {
        List<Distribuidor> distribuidores = distribuidorService.buscarPorClave(clave);

        if (distribuidores.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró ningún distribuidor con esa clave");
        }

        return distribuidores.stream().map(d -> {
            DistribuidoraInfoDTO dto = new DistribuidoraInfoDTO();
            dto.setId(d.getId());
            dto.setNombreDistribuidora(d.getNombreDistribuidora());
            dto.setClaveDistribuidora(d.getClaveDistribuidora());
            return dto;
        }).collect(Collectors.toList());
    }
    //post
    @PostMapping
    public ResponseEntity<Distribuidor> crear(@RequestBody CrearDistribuidorDTO dto) {
        Distribuidor creado = distribuidorService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    //delete
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        distribuidorService.eliminarPorId(id);
        return ResponseEntity.noContent().build();
    }

}
