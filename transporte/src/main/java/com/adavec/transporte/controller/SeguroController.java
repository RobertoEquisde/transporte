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

    @GetMapping
    public List<SeguroDTO> listar() {
        return seguroService.obtenerTodos().stream().map(seguro -> {
            SeguroDTO dto = new SeguroDTO();
            dto.setId(seguro.getId());
            dto.setFactura(seguro.getFactura());
            dto.setValorSeguro(seguro.getValorSeguro());
            dto.setSeguroDistribuidor(seguro.getSeguroDistribuidor());
            dto.setUnidadId(seguro.getUnidad().getId());
            dto.setDistribuidorId(seguro.getDistribuidor().getId());
            return dto;
        }).collect(Collectors.toList());
    }
    @GetMapping("/buscar")
    public List<SeguroDTO> buscarPorFactura(@RequestParam("factura") String factura) {
        List<Seguro> seguros = seguroService.buscarPorFactura(factura);

        if (seguros.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontraron seguros con esa factura");
        }

        return seguros.stream().map(seguro -> {
            SeguroDTO dto = new SeguroDTO();
            dto.setId(seguro.getId());
            dto.setFactura(seguro.getFactura());
            dto.setValorSeguro(seguro.getValorSeguro());
            dto.setSeguroDistribuidor(seguro.getSeguroDistribuidor());
            dto.setUnidadId(seguro.getUnidad().getId());
            dto.setDistribuidorId(seguro.getDistribuidor().getId());
            return dto;
        }).collect(Collectors.toList());
    }
// post
    @PostMapping
    public ResponseEntity<SeguroDTO> crearSeguro(@RequestBody CrearSeguroRequest request) {
        Seguro seguro = seguroService.guardarSeguroDesdeDTO(request);

        SeguroDTO dto = new SeguroDTO();
        dto.setId(seguro.getId());
        dto.setFactura(seguro.getFactura());
        dto.setValorSeguro(seguro.getValorSeguro());
        dto.setSeguroDistribuidor(seguro.getSeguroDistribuidor());
        dto.setUnidadId(seguro.getUnidad().getId());
        dto.setDistribuidorId(seguro.getDistribuidor().getId());

        return new ResponseEntity<>(dto, HttpStatus.CREATED);
    }
    //delete
    @DeleteMapping("/factura/{factura}")
    public String eliminarPorFactura(@PathVariable String factura) {
        seguroService.eliminarPorFactura(factura);
        return "Seguro eliminado correctamente con factura: " + factura;
    }

}
