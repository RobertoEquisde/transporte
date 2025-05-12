package com.adavec.transporte.controller;

import com.adavec.transporte.dto.ReporteFinancieroDTO;
import com.adavec.transporte.service.ReporteFinancieroService;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/reportes")
public class ReporteFinancieroController {

    private final ReporteFinancieroService reporteFinancieroService;

    public ReporteFinancieroController(ReporteFinancieroService reporteFinancieroService) {
        this.reporteFinancieroService = reporteFinancieroService;
    }

    @GetMapping("/financiero")
    public void exportarReporteFinanciero(
            @RequestParam("mes") @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes,
            HttpServletResponse response
    ) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=reporte_financiero_" + mes + ".xlsx");

        List<ReporteFinancieroDTO> filas = reporteFinancieroService.obtenerDatosFinancierosPorMes(mes);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Reporte Financiero");

        int rowIdx = 0;
        String[] columnas = {
                "FechaProceso", "ClaveDistribuidor", "NumeroFactura", "Modelo", "NoSerie",
                "FechaFactura", "FechaInteres", "Dias", "ImporteFactura", "CuotaAsociacion",
                "CuotaSeguro (3.24%)", "Seguro (1.34%)", "FondoEstrella"
        };

        Row encabezado = sheet.createRow(rowIdx++);
        for (int i = 0; i < columnas.length; i++) {
            encabezado.createCell(i).setCellValue(columnas[i]);
        }

        for (ReporteFinancieroDTO dto : filas) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(dto.getFechaProceso());
            row.createCell(1).setCellValue(dto.getClaveDistribuidor());
            row.createCell(2).setCellValue(dto.getNumeroFactura());
            row.createCell(3).setCellValue(dto.getModelo());
            row.createCell(4).setCellValue(dto.getNoSerie());
            row.createCell(5).setCellValue(dto.getFechaFactura());
            row.createCell(6).setCellValue(dto.getFechaInteres());
            row.createCell(7).setCellValue(dto.getDias());
            row.createCell(8).setCellValue(dto.getValorUnidad());
            row.createCell(9).setCellValue(dto.getCuotaAsociacion());
            row.createCell(10).setCellValue(dto.getCuotaSeguro());
            row.createCell(11).setCellValue(dto.getSeguro());
            row.createCell(12).setCellValue(dto.getFondoEstrella());
        }

        workbook.write(response.getOutputStream());
        workbook.close();
    }
}
