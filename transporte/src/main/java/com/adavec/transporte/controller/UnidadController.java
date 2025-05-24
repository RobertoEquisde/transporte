package com.adavec.transporte.controller;

import com.adavec.transporte.dto.*;
import com.adavec.transporte.model.Distribuidor;
import com.adavec.transporte.model.Modelo;
import com.adavec.transporte.model.Seguro;
import com.adavec.transporte.model.Unidad;
import com.adavec.transporte.repository.SeguroRepository;
import com.adavec.transporte.service.UnidadService;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/unidades")
public class UnidadController {

    private final UnidadService unidadService;
    private final SeguroRepository seguroRepository;
    public UnidadController(UnidadService unidadService, SeguroRepository seguroRepository) {
        this.unidadService = unidadService;
        this.seguroRepository = seguroRepository;
    }
    private UnidadDTO mapUnidadToUnidadDTO(Unidad unidad) {
        UnidadDTO dto = new UnidadDTO();
        dto.setId(unidad.getId());
        dto.setNoSerie(unidad.getNoSerie());
        dto.setComentario(unidad.getComentario());
        dto.setOrigen(unidad.getOrigen());
        dto.setDebisFecha(unidad.getDebisFecha() != null ? unidad.getDebisFecha().toString() : null);
        dto.setPagoDistribuidora(unidad.getPagoDistribuidora() != null ? unidad.getPagoDistribuidora().toString() : null);
        dto.setReportadoA(unidad.getReportadoA());
        dto.setValorUnidad(unidad.getValorUnidad());

        if (unidad.getModelo() != null) {
            ModeloDTO modeloDTO = new ModeloDTO();
            modeloDTO.setId(unidad.getModelo().getId());
            modeloDTO.setNombre(unidad.getModelo().getNombre());
            modeloDTO.setUso(unidad.getModelo().getUso());
            dto.setModelo(modeloDTO);
        }

        if (unidad.getDistribuidor() != null) {
            DistribuidoraInfoDTO distDTO = new DistribuidoraInfoDTO();
            distDTO.setId(unidad.getDistribuidor().getId());
            distDTO.setNombreDistribuidora(unidad.getDistribuidor().getNombreDistribuidora());
            distDTO.setClaveDistribuidora(unidad.getDistribuidor().getClaveDistribuidora());
            dto.setDistribuidor(distDTO);
        }

        Seguro seguro = seguroRepository.findByUnidadId(unidad.getId());
        SeguroResumenDTO seguroDTO = new SeguroResumenDTO();
        seguroDTO.setFactura(seguro != null ? seguro.getFactura() : null);
        seguroDTO.setValorSeguro(seguro != null && seguro.getValorSeguro() != null ? seguro.getValorSeguro() : 0.0);
        seguroDTO.setSeguroDistribuidor(seguro != null && seguro.getSeguroDistribuidor() != null ? seguro.getSeguroDistribuidor() : 0.0);
        dto.setSeguro(seguroDTO);

        return dto;
    }
    /*@GetMapping
    public List<UnidadDTO> listar() {
        return unidadService.obtenerTodas().stream()
                .map(this::mapUnidadToUnidadDTO) // Use helper method
                .collect(Collectors.toList());
    }*/

    @GetMapping("/buscar-por-digitos")
    public UnidadDTO buscarPorUltimosDigitos(@RequestParam("terminaEn") String terminaEn) {
        if (terminaEn == null || terminaEn.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe proporcionar los caracteres a buscar");
        }
        terminaEn = terminaEn.trim();
        if (terminaEn.length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe proporcionar al menos 6 caracteres. Proporcionados: " + terminaEn.length());
        }

        List<Unidad> unidades = unidadService.buscarPorUltimosDigitosSerie(terminaEn);

        if (unidades.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró ninguna unidad que termine con: " + terminaEn);
        }
        if (unidades.size() > 1) {
            String seriesEncontradas = unidades.stream().map(Unidad::getNoSerie).collect(Collectors.joining(", "));
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Se encontraron " + unidades.size() + " unidades que terminan con '" + terminaEn + "'. Series completas: " + seriesEncontradas);
        }
        return mapUnidadToUnidadDTO(unidades.get(0)); // Use helper method
    }

//post
    @PostMapping
    public ResponseEntity<UnidadDTO> crearUnidad(@RequestBody CrearUnidadRequest request) {
        Unidad unidad = new Unidad();

        unidad.setNoSerie(request.getNoSerie());
        unidad.setComentario(request.getComentario());
        unidad.setOrigen(request.getOrigen());
        unidad.setDebisFecha(request.getDebisFecha() != null ? LocalDate.parse(request.getDebisFecha()) : null);
        unidad.setReportadoA(request.getReportadoA());
        unidad.setPagoDistribuidora(request.getPagoDistribuidora() != null ? LocalDate.parse(request.getPagoDistribuidora()) : null);
        unidad.setValorUnidad(request.getValorUnidad());

        Modelo modelo = new Modelo();
        modelo.setId(request.getModeloId());
        unidad.setModelo(modelo);

        Distribuidor distribuidor = new Distribuidor();
        distribuidor.setId(request.getDistribuidorId());
        unidad.setDistribuidor(distribuidor);

        Unidad unidadGuardada = unidadService.guardar(unidad);

        UnidadDTO dto = new UnidadDTO();
        dto.setId(unidadGuardada.getId());
        dto.setNoSerie(unidadGuardada.getNoSerie());
        dto.setComentario(unidadGuardada.getComentario());
        dto.setOrigen(unidadGuardada.getOrigen());
        dto.setDebisFecha(unidadGuardada.getDebisFecha() != null ? unidadGuardada.getDebisFecha().toString() : null);
        dto.setPagoDistribuidora(unidadGuardada.getPagoDistribuidora() != null ? unidadGuardada.getPagoDistribuidora().toString() : null);
        dto.setReportadoA(unidadGuardada.getReportadoA());
        dto.setValorUnidad(unidadGuardada.getValorUnidad());

        ModeloDTO modeloDTO = new ModeloDTO();
        modeloDTO.setId(request.getModeloId());
        dto.setModelo(modeloDTO);

        DistribuidoraInfoDTO distDTO = new DistribuidoraInfoDTO();
        distDTO.setId(request.getDistribuidorId());
        dto.setDistribuidor(distDTO);

        return new ResponseEntity<>(dto, HttpStatus.CREATED);
    }
    @PutMapping("/{id}")
    public ResponseEntity<UnidadDTO> actualizarUnidad(@PathVariable Integer id, @RequestBody UnidadDTO request) {
        Unidad unidadActualizada = unidadService.actualizarUnidad(id, request);
        return ResponseEntity.ok(mapUnidadToUnidadDTO(unidadActualizada));
    }
    //delete
    @DeleteMapping("/eliminar")
    public ResponseEntity<String> eliminarUnidadPorSerie(@RequestParam("serie") String serie) {
        Optional<Unidad> unidad = unidadService.obtenerPorNoSerie(serie);

        if (unidad.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró unidad con número de serie: " + serie);
        }

        unidadService.eliminarPorNoSerie(serie);
        return ResponseEntity.ok("Unidad eliminada correctamente: " + serie);
    }



}
