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

    @GetMapping
    public List<UnidadDTO> listar() {
        return unidadService.obtenerTodas().stream().map(unidad -> {
            UnidadDTO dto = new UnidadDTO();
            dto.setId(unidad.getId());
            dto.setNoSerie(unidad.getNoSerie());
            dto.setComentario(unidad.getComentario());
            dto.setOrigen(unidad.getOrigen());
            dto.setDebisFecha(unidad.getDebisFecha() != null ? unidad.getDebisFecha().toString() : null);
            dto.setPagoDistribuidora(unidad.getPagoDistribuidora() != null ? unidad.getPagoDistribuidora().toString() : null);
            dto.setReportadoA(unidad.getReportadoA());
            dto.setValorUnidad(unidad.getValorUnidad());

            ModeloDTO modeloDTO = new ModeloDTO();
            modeloDTO.setId(unidad.getModelo().getId());
            modeloDTO.setNombre(unidad.getModelo().getNombre());
            modeloDTO.setUso(unidad.getModelo().getUso());
            dto.setModelo(modeloDTO);

            DistribuidoraInfoDTO distDTO = new DistribuidoraInfoDTO();
            distDTO.setId(unidad.getDistribuidor().getId());
            distDTO.setNombreDistribuidora(unidad.getDistribuidor().getNombreDistribuidora());
            distDTO.setClaveDistribuidora(unidad.getDistribuidor().getClaveDistribuidora());
            dto.setDistribuidor(distDTO);
            Seguro seguro = seguroRepository.findByUnidadId(unidad.getId());
            SeguroResumenDTO seguroDTO = new SeguroResumenDTO();
            seguroDTO.setFactura(seguro != null ? seguro.getFactura() : null);
            seguroDTO.setValorSeguro(seguro != null ? seguro.getValorSeguro() : 0.0);
            seguroDTO.setSeguroDistribuidor(seguro != null && seguro.getSeguroDistribuidor() != null ? seguro.getSeguroDistribuidor() : 0.0);
            dto.setSeguro(seguroDTO);

            return dto;
        }).collect(Collectors.toList());
    }
    @GetMapping("/buscar")
    public UnidadDTO obtenerPorNumeroSerie(@RequestParam("serie") String serie) {
        return unidadService.obtenerPorNoSerie(serie)
                .map(unidad -> {
                    UnidadDTO dto = new UnidadDTO();
                    dto.setId(unidad.getId());
                    dto.setNoSerie(unidad.getNoSerie());
                    dto.setComentario(unidad.getComentario());
                    dto.setOrigen(unidad.getOrigen());
                    dto.setDebisFecha(unidad.getDebisFecha() != null ? unidad.getDebisFecha().toString() : null);
                    dto.setPagoDistribuidora(unidad.getPagoDistribuidora() != null ? unidad.getPagoDistribuidora().toString() : null);
                    dto.setReportadoA(unidad.getReportadoA());
                    dto.setValorUnidad(unidad.getValorUnidad());

                    ModeloDTO modeloDTO = new ModeloDTO();
                    modeloDTO.setId(unidad.getModelo().getId());
                    modeloDTO.setNombre(unidad.getModelo().getNombre());
                    modeloDTO.setUso(unidad.getModelo().getUso());
                    dto.setModelo(modeloDTO);

                    DistribuidoraInfoDTO distDTO = new DistribuidoraInfoDTO();
                    distDTO.setId(unidad.getDistribuidor().getId());
                    distDTO.setNombreDistribuidora(unidad.getDistribuidor().getNombreDistribuidora());
                    distDTO.setClaveDistribuidora(unidad.getDistribuidor().getClaveDistribuidora());
                    dto.setDistribuidor(distDTO);
                    Seguro seguro = seguroRepository.findByUnidadId(unidad.getId());
                    SeguroResumenDTO seguroDTO = new SeguroResumenDTO();
                    seguroDTO.setFactura(seguro != null ? seguro.getFactura() : null);
                    seguroDTO.setValorSeguro(seguro != null ? seguro.getValorSeguro() : 0.0);
                    seguroDTO.setSeguroDistribuidor(seguro != null && seguro.getSeguroDistribuidor() != null ? seguro.getSeguroDistribuidor() : 0.0);
                    dto.setSeguro(seguroDTO);
                    return dto;
                })
                .orElseThrow(() -> new RuntimeException("Unidad no encontrada con número de serie: " + serie));
    }
    @GetMapping("/buscar-por-digitos")
    public List<UnidadDTO> buscarPorUltimosDigitos(@RequestParam("terminaEn") String ultimos6) {
        List<Unidad> unidades = unidadService.buscarPorUltimosDigitosSerie(ultimos6);

        if (unidades.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontraron unidades con esos dígitos");
        }

        return unidades.stream()
                .map(unidad -> {
                    UnidadDTO dto = new UnidadDTO();
                    dto.setId(unidad.getId());
                    dto.setNoSerie(unidad.getNoSerie());
                    dto.setComentario(unidad.getComentario());
                    dto.setOrigen(unidad.getOrigen());
                    dto.setDebisFecha(unidad.getDebisFecha() != null ? unidad.getDebisFecha().toString() : null);
                    dto.setPagoDistribuidora(unidad.getPagoDistribuidora() != null ? unidad.getPagoDistribuidora().toString() : null);
                    dto.setReportadoA(unidad.getReportadoA());
                    dto.setValorUnidad(unidad.getValorUnidad());

                    ModeloDTO modeloDTO = new ModeloDTO();
                    modeloDTO.setId(unidad.getModelo().getId());
                    modeloDTO.setNombre(unidad.getModelo().getNombre());
                    modeloDTO.setUso(unidad.getModelo().getUso());
                    dto.setModelo(modeloDTO);

                    DistribuidoraInfoDTO distDTO = new DistribuidoraInfoDTO();
                    distDTO.setId(unidad.getDistribuidor().getId());
                    distDTO.setNombreDistribuidora(unidad.getDistribuidor().getNombreDistribuidora());
                    distDTO.setClaveDistribuidora(unidad.getDistribuidor().getClaveDistribuidora());
                    dto.setDistribuidor(distDTO);

                    Seguro seguro = seguroRepository.findByUnidadId(unidad.getId());
                    SeguroResumenDTO seguroDTO = new SeguroResumenDTO();
                    seguroDTO.setFactura(seguro != null ? seguro.getFactura() : null);
                    seguroDTO.setValorSeguro(seguro != null && seguro.getValorSeguro() != null ? seguro.getValorSeguro() : 0.0);
                    seguroDTO.setSeguroDistribuidor(seguro != null && seguro.getSeguroDistribuidor() != null ? seguro.getSeguroDistribuidor() : 0.0);
                    dto.setSeguro(seguroDTO);

                    return dto;
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/reportes/")
    public void exportExcelPorMesDistribuidora(
            @RequestParam("mes") @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes,
            HttpServletResponse response
    ) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=unidades_" + mes + ".xlsx");

        List<UnidadReporteDTO> datos = unidadService.obtenerDatosPorMes(mes);

        Map<String, List<UnidadReporteDTO>> agrupadoPorDistribuidora = datos.stream()
                .collect(Collectors.groupingBy(UnidadReporteDTO::getDistribuidora));

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Unidades");

        int rowIdx = 0;
        String[] columnas = {"Clave", "Factura", "Modelo", "Serie", "VIN", "Fecha Fondeo", "Tipo", "Valor Unidad", "Seguro GNP"};

        for (Map.Entry<String, List<UnidadReporteDTO>> entry : agrupadoPorDistribuidora.entrySet()) {
            String distribuidora = entry.getKey();
            List<UnidadReporteDTO> unidades = entry.getValue();

            Row titulo = sheet.createRow(rowIdx++);
            titulo.createCell(0).setCellValue("Distribuidora: " + distribuidora);

            Row encabezado = sheet.createRow(rowIdx++);
            for (int i = 0; i < columnas.length; i++) {
                encabezado.createCell(i).setCellValue(columnas[i]);
            }

            for (UnidadReporteDTO u : unidades) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(u.getClave());
                row.createCell(1).setCellValue(u.getFactura());
                row.createCell(2).setCellValue(u.getModelo());
                row.createCell(3).setCellValue(u.getSerie());
                row.createCell(4).setCellValue(u.getVin());
                row.createCell(5).setCellValue(u.getFechaFondeo());
                row.createCell(6).setCellValue(u.getTipo());
                row.createCell(7).setCellValue(u.getValorUnidad());
                row.createCell(8).setCellValue(u.getValorSeguro());

            }

            // Espacio entre grupos
            rowIdx++;
        }

        workbook.write(response.getOutputStream());
        workbook.close();
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

        // Puedes usar tu método mapUnidadToDTO aquí si ya lo tienes, o replicar el DTO como antes
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
