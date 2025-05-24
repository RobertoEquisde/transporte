package com.adavec.transporte.controller;

import com.adavec.transporte.dto.CrearSeguroRequest;
import com.adavec.transporte.dto.SeguroDTO;
import com.adavec.transporte.model.Seguro;
import com.adavec.transporte.service.SeguroService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/seguros")
public class SeguroController {

    private final SeguroService seguroService;

    public SeguroController(SeguroService seguroService) {
        this.seguroService = seguroService;
    }

    // Método helper para convertir Entidad a DTO
    private SeguroDTO convertToDTO(Seguro seguro) {
        SeguroDTO dto = new SeguroDTO();
        dto.setId(seguro.getId());
        dto.setFactura(seguro.getFactura());
        dto.setValorSeguro(seguro.getValorSeguro());
        dto.setSeguroDistribuidor(seguro.getSeguroDistribuidor());
        dto.setUnidadId(seguro.getUnidad().getId());
        dto.setDistribuidorId(seguro.getDistribuidor().getId());
        return dto;
    }

    /*@GetMapping
    public List<SeguroDTO> listar() {
        return seguroService.obtenerTodos().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }*/

    @GetMapping("/buscar")
    public List<SeguroDTO> buscarPorFactura(@RequestParam("factura") String factura) {
        List<Seguro> seguros = seguroService.buscarPorFactura(factura);

        if (seguros.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontraron seguros con esa factura");
        }

        return seguros.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @PostMapping
    public ResponseEntity<SeguroDTO> crearSeguro(@RequestBody CrearSeguroRequest request) {
        Seguro seguro = seguroService.guardarSeguroDesdeDTO(request);
        return new ResponseEntity<>(convertToDTO(seguro), HttpStatus.CREATED);
    }

    // Nuevo método para actualizar
    @PutMapping("/{id}")
    public ResponseEntity<SeguroDTO> actualizarSeguro(
            @PathVariable Integer id,
            @RequestBody SeguroDTO seguroDTO) {

        if (!id.equals(seguroDTO.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "ID en URL no coincide con ID en el cuerpo de la solicitud"
            );
        }

        Seguro seguroActualizado = seguroService.actualizarSeguro(seguroDTO);
        return ResponseEntity.ok(convertToDTO(seguroActualizado));
    }

    @DeleteMapping("/factura/{factura}")
    public ResponseEntity<String> eliminarPorFactura(@PathVariable String factura) {
        seguroService.eliminarPorFactura(factura);
        return ResponseEntity.ok("Seguro eliminado correctamente con factura: " + factura);
    }
}