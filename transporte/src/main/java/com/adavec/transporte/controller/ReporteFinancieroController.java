package com.adavec.transporte.controller;

import com.adavec.transporte.dto.ReporteFinancieroDTO;
import com.adavec.transporte.service.ReporteFinancieroService;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.util.stream.Collectors;
import java.io.IOException;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
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

        List<ReporteFinancieroDTO> todasLasFilas = reporteFinancieroService.obtenerDatosFinancierosPorMes(mes);
        // Filtrar solo aquellos registros que tienen información en días y cuota asociación
        List<ReporteFinancieroDTO> filas = todasLasFilas.stream()
                .filter(dto -> dto.getDias() > 0 && dto.getCuotaAsociacion() != null && dto.getCuotaAsociacion() > 0)
                .collect(Collectors.toList());
        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Reporte Financiero");

        // Crear estilos base
        CellStyle headerStyle = crearEstiloEncabezado(workbook);
        CellStyle dateStyle = crearEstiloFecha(workbook);
        CellStyle numberStyle = crearEstiloNumero(workbook);
        CellStyle currencyStyle = crearEstiloMoneda(workbook);
        CellStyle textStyle = crearEstiloTexto(workbook);

        // Crear estilos específicos para las columnas de interés (con colores diferentes)
        CellStyle cuotaAsocStyle = crearEstiloEncabezadoEspecial(workbook, IndexedColors.YELLOW.getIndex());
        CellStyle cuotaSeguroStyle = crearEstiloEncabezadoEspecial(workbook, IndexedColors.LIGHT_BLUE.getIndex());
        CellStyle seguroStyle = crearEstiloEncabezadoEspecial(workbook, IndexedColors.LIGHT_BLUE.getIndex());
        CellStyle importeTrasladoStyle = crearEstiloEncabezadoEspecial(workbook, IndexedColors.BRIGHT_GREEN.getIndex());
        CellStyle fondoEstrellaStyle = crearEstiloEncabezadoEspecial(workbook, IndexedColors.GOLD.getIndex());

        // Título del reporte
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("REPORTE FINANCIERO - " + mes.format(DateTimeFormatter.ofPattern("MMMM yyyy")).toUpperCase());
        CellStyle titleStyle = crearEstiloTitulo(workbook);
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 20));

        // Fila de encabezados
        int rowIdx = 2;
        String[] columnas = {
                "Fecha Proceso", "Clave Distribuidor", "Número Factura", "Modelo", "No. Serie",
                "Fecha Factura", "Fecha Interés", "Días", "Importe Factura", "Cuota Asociación",
                "Cuota Seguro (3.24%)", "Seguro (1.34%)", "Importe Traslado", "Fondo Estrella"
        };

        Row encabezado = sheet.createRow(rowIdx);
        encabezado.setHeightInPoints(30); // Altura del encabezado

        // Agregar encabezados del reporte principal
        for (int i = 0; i < columnas.length; i++) {
            Cell cell = encabezado.createCell(i);
            cell.setCellValue(columnas[i]);

            // Aplicar estilos específicos a las columnas de interés
            if (i == 9) { // Cuota Asociación
                cell.setCellStyle(cuotaAsocStyle);
            } else if (i == 10) { // Cuota Seguro
                cell.setCellStyle(cuotaSeguroStyle);
            } else if (i == 11) { // Seguro
                cell.setCellStyle(seguroStyle);
            } else if (i == 12) { // Importe Traslado
                cell.setCellStyle(importeTrasladoStyle);
            } else if (i == 13) { // Fondo Estrella
                cell.setCellStyle(fondoEstrellaStyle);
            } else {
                cell.setCellStyle(headerStyle);
            }
        }

        // Agregar columna de separación
        Cell separatorCell = encabezado.createCell(14);
        separatorCell.setCellValue("");
        separatorCell.setCellStyle(textStyle);

        // Agregar encabezados del desglose al lado (como en la imagen)
        Cell desgloseHeader1 = encabezado.createCell(15);
        desgloseHeader1.setCellValue("CUOTA ASOCIACION");
        desgloseHeader1.setCellStyle(cuotaAsocStyle);
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 15, 16));

        Cell desgloseHeader2 = encabezado.createCell(17);
        desgloseHeader2.setCellValue("CUOTA SEGURO");
        desgloseHeader2.setCellStyle(cuotaSeguroStyle);
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 17, 18));

        Cell desgloseHeader3 = encabezado.createCell(19);
        desgloseHeader3.setCellValue("CUOTA TRASLADO");
        desgloseHeader3.setCellStyle(importeTrasladoStyle);

        Cell desgloseHeader4 = encabezado.createCell(20);
        desgloseHeader4.setCellValue("Fondo Estrella");
        desgloseHeader4.setCellStyle(fondoEstrellaStyle);

        // Crear todas las filas de datos del reporte principal primero
        for (int i = 0; i < filas.size(); i++) {
            ReporteFinancieroDTO dto = filas.get(i);
            rowIdx++;
            Row row = sheet.createRow(rowIdx);

            // Aplicar estilos según el tipo de dato
            Cell cell0 = row.createCell(0);
            cell0.setCellValue(dto.getFechaProceso());
            cell0.setCellStyle(dateStyle);

            Cell cell1 = row.createCell(1);
            cell1.setCellValue(dto.getClaveDistribuidor());
            cell1.setCellStyle(textStyle);

            Cell cell2 = row.createCell(2);
            cell2.setCellValue(dto.getNumeroFactura());
            cell2.setCellStyle(textStyle);

            Cell cell3 = row.createCell(3);
            cell3.setCellValue(dto.getModelo());
            cell3.setCellStyle(textStyle);

            Cell cell4 = row.createCell(4);
            cell4.setCellValue(dto.getNoSerie());
            cell4.setCellStyle(textStyle);

            Cell cell5 = row.createCell(5);
            cell5.setCellValue(dto.getFechaFactura());
            cell5.setCellStyle(dateStyle);

            Cell cell6 = row.createCell(6);
            cell6.setCellValue(dto.getFechaInteres());
            cell6.setCellStyle(dateStyle);

            Cell cell7 = row.createCell(7);
            cell7.setCellValue(dto.getDias());
            cell7.setCellStyle(numberStyle);

            Cell cell8 = row.createCell(8);
            cell8.setCellValue(dto.getValorUnidad());
            cell8.setCellStyle(currencyStyle);

            Cell cell9 = row.createCell(9);
            cell9.setCellValue(dto.getCuotaAsociacion());
            cell9.setCellStyle(currencyStyle);

            Cell cell10 = row.createCell(10);
            cell10.setCellValue(dto.getCuotaSeguro());
            cell10.setCellStyle(currencyStyle);

            Cell cell11 = row.createCell(11);
            cell11.setCellValue(dto.getSeguro());
            cell11.setCellStyle(currencyStyle);

            Cell cell12 = row.createCell(12);
            cell12.setCellValue(dto.getImporteTraslado());
            cell12.setCellStyle(currencyStyle);

            Cell cell13 = row.createCell(13);
            cell13.setCellValue(dto.getFondoEstrella());
            cell13.setCellStyle(currencyStyle);
        }

        // Ahora agregamos los datos del desglose en las filas adecuadas
        // AMDA - Primera fila de desglose (fila 3)
        Row amdaRow = getOrCreateRow(sheet, 3);

        Cell amdaName = amdaRow.createCell(14);
        amdaName.setCellValue("AMDA");
        amdaName.setCellStyle(textStyle);

        Cell amdaValue = amdaRow.createCell(15);
        amdaValue.setCellValue(103.00);
        amdaValue.setCellStyle(currencyStyle);

        Cell amdaIVA = amdaRow.createCell(16);
        amdaIVA.setCellValue("SIN I.V.A.");
        amdaIVA.setCellStyle(textStyle);

        Cell amdaPercent = amdaRow.createCell(17);
        amdaPercent.setCellValue("%");
        amdaPercent.setCellStyle(textStyle);

        Cell amdaPercentValue = amdaRow.createCell(18);
        amdaPercentValue.setCellValue(3.24);
        amdaPercentValue.setCellStyle(numberStyle);

        // ASOCIACION - Segunda fila de desglose (fila 4)
        Row asociacionRow = getOrCreateRow(sheet, 4);

        Cell asociacionName = asociacionRow.createCell(14);
        asociacionName.setCellValue("ASOCIACION");
        asociacionName.setCellStyle(textStyle);

        Cell asociacionValue = asociacionRow.createCell(15);
        asociacionValue.setCellValue(1200.00);
        asociacionValue.setCellStyle(currencyStyle);

        Cell asociacionIVA = asociacionRow.createCell(16);
        asociacionIVA.setCellValue("SIN I.V.A.");
        asociacionIVA.setCellStyle(textStyle);

        Cell asociacionPercentValue = asociacionRow.createCell(18);
        asociacionPercentValue.setCellValue(1000);
        asociacionPercentValue.setCellStyle(numberStyle);

        // CONVENCION - Tercera fila de desglose (fila 5)
        Row convencionRow = getOrCreateRow(sheet, 5);

        Cell convencionName = convencionRow.createCell(14);
        convencionName.setCellValue("CONVENCION");
        convencionName.setCellStyle(textStyle);

        Cell convencionValue = convencionRow.createCell(15);
        convencionValue.setCellValue(1500.00);
        convencionValue.setCellStyle(currencyStyle);

        Cell convencionIVA = convencionRow.createCell(16);
        convencionIVA.setCellValue("SIN I.V.A.");
        convencionIVA.setCellStyle(textStyle);

        Cell financieraName = convencionRow.createCell(17);
        financieraName.setCellValue("FINANCIERA");
        financieraName.setCellStyle(textStyle);

        Cell financieraValue = convencionRow.createCell(18);
        financieraValue.setCellValue(3855.50);
        financieraValue.setCellStyle(currencyStyle);

        // Valor adicional - Cuarta fila (fila 6)
        Row additionalRow = getOrCreateRow(sheet, 6);

        Cell additionalValue = additionalRow.createCell(15);
        additionalValue.setCellValue(2803.00);
        additionalValue.setCellStyle(currencyStyle);

        // CAPACITACION - Quinta fila (fila 7) - Saltamos una línea vacía
        Row capacitacionRow = getOrCreateRow(sheet, 8);

        Cell capacitacionName = capacitacionRow.createCell(14);
        capacitacionName.setCellValue("CAPACITACION");
        capacitacionName.setCellStyle(textStyle);

        Cell capacitacionValue = capacitacionRow.createCell(15);
        capacitacionValue.setCellValue(9280.00);
        capacitacionValue.setCellStyle(currencyStyle);

        Cell capacitacionIVA = capacitacionRow.createCell(16);
        capacitacionIVA.setCellValue("CON I.V.A.");
        capacitacionIVA.setCellStyle(textStyle);

        Cell capacitacionPercent = capacitacionRow.createCell(17);
        capacitacionPercent.setCellValue("%");
        capacitacionPercent.setCellStyle(textStyle);

        Cell capacitacionPercentValue = capacitacionRow.createCell(18);
        capacitacionPercentValue.setCellValue(1.34);
        capacitacionPercentValue.setCellStyle(numberStyle);

        Cell capacitacionTraslado = capacitacionRow.createCell(19);
        capacitacionTraslado.setCellValue(22900.00);
        capacitacionTraslado.setCellStyle(currencyStyle);

        // PUBLICIDAD - Sexta fila (fila 8)
        Row publicidadRow = getOrCreateRow(sheet, 9);

        Cell publicidadName = publicidadRow.createCell(14);
        publicidadName.setCellValue("PUBLICIDAD");
        publicidadName.setCellStyle(textStyle);

        Cell publicidadValue = publicidadRow.createCell(15);
        publicidadValue.setCellValue(5800.00);
        publicidadValue.setCellStyle(currencyStyle);

        Cell publicidadIVA = publicidadRow.createCell(16);
        publicidadIVA.setCellValue("CON I.V.A.");
        publicidadIVA.setCellStyle(textStyle);

        Cell publicidadPercentValue = publicidadRow.createCell(18);
        publicidadPercentValue.setCellValue(1000);
        publicidadPercentValue.setCellStyle(numberStyle);

        Cell publicidadTraslado = publicidadRow.createCell(19);
        publicidadTraslado.setCellValue(1.16);
        publicidadTraslado.setCellStyle(numberStyle);

        // Total parcial - Séptima fila (fila 9)
        Row subTotalRow = getOrCreateRow(sheet, 10);

        Cell subTotalValue = subTotalRow.createCell(15);
        subTotalValue.setCellValue(15080.00);
        subTotalValue.setCellStyle(currencyStyle);

        // Total final con todos los valores - Octava fila (fila 10)
        Row totalRow = getOrCreateRow(sheet, 11);

        // Estilos para valores coloreados
        CellStyle yellowTotalStyle = crearEstiloMoneda(workbook);
        yellowTotalStyle.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
        yellowTotalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Cell totalValue = totalRow.createCell(15);
        totalValue.setCellValue(17883.00);
        totalValue.setCellStyle(yellowTotalStyle);

        Cell aseguradoraName = totalRow.createCell(17);
        aseguradoraName.setCellValue("ASEGURADORA");
        aseguradoraName.setCellStyle(textStyle);

        CellStyle blueTotalStyle = crearEstiloMoneda(workbook);
        blueTotalStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        blueTotalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Cell aseguradoraValue = totalRow.createCell(18);
        aseguradoraValue.setCellValue(1594.56);
        aseguradoraValue.setCellStyle(blueTotalStyle);

        CellStyle greenTotalStyle = crearEstiloMoneda(workbook);
        greenTotalStyle.setFillForegroundColor(IndexedColors.BRIGHT_GREEN.getIndex());
        greenTotalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Cell trasladoTotal = totalRow.createCell(19);
        trasladoTotal.setCellValue(26564.00);
        trasladoTotal.setCellStyle(greenTotalStyle);

        CellStyle goldTotalStyle = crearEstiloMoneda(workbook);
        goldTotalStyle.setFillForegroundColor(IndexedColors.GOLD.getIndex());
        goldTotalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Cell estrellaTotal = totalRow.createCell(20);
        estrellaTotal.setCellValue(46162.58);
        estrellaTotal.setCellStyle(goldTotalStyle);

        // Ajustar el ancho de las columnas automáticamente
        for (int i = 0; i <= 20; i++) {
            sheet.autoSizeColumn(i);
        }

        // Congelar el panel para mantener visible la fila de encabezado
        sheet.createFreezePane(0, 3);

        // Agregar filtros en las columnas principales
        sheet.setAutoFilter(new CellRangeAddress(2, 2, 0, 13));

        workbook.write(response.getOutputStream());
        workbook.close();
    }

    // Método auxiliar para obtener una fila existente o crear una nueva si no existe
    private Row getOrCreateRow(Sheet sheet, int rowNum) {
        Row row = sheet.getRow(rowNum);
        if (row == null) {
            row = sheet.createRow(rowNum);
        }
        return row;
    }

    private CellStyle crearEstiloTitulo(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle crearEstiloEncabezado(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerFont.setFontHeightInPoints((short) 12);
        style.setFont(headerFont);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        style.setWrapText(true);

        return style;
    }

    private CellStyle crearEstiloEncabezadoEspecial(Workbook workbook, short colorIndex) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(colorIndex);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.BLACK.getIndex());
        headerFont.setFontHeightInPoints((short) 12);
        style.setFont(headerFont);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        style.setWrapText(true);

        return style;
    }

    private CellStyle crearEstiloFecha(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("dd/mm/yyyy"));

        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        style.setAlignment(HorizontalAlignment.CENTER);

        return style;
    }

    private CellStyle crearEstiloNumero(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));

        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        style.setAlignment(HorizontalAlignment.RIGHT);

        return style;
    }

    private CellStyle crearEstiloMoneda(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("$#,##0.00"));

        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        style.setAlignment(HorizontalAlignment.RIGHT);

        return style;
    }

    private CellStyle crearEstiloTexto(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        style.setAlignment(HorizontalAlignment.LEFT);

        return style;
    }
}