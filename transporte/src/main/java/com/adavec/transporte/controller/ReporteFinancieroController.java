package com.adavec.transporte.controller;

import com.adavec.transporte.dto.ReporteFinancieroDTO;
import com.adavec.transporte.model.ConceptoCobro;
import com.adavec.transporte.repository.ConceptoCobroRepository;
import com.adavec.transporte.service.ReporteFinancieroService;
import com.adavec.transporte.dto.ReporteFinancieroDTO;
import com.adavec.transporte.service.ReporteFinancieroService;
import com.adavec.transporte.repository.ConceptoCobroRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.*;
import java.util.stream.Collectors;
import java.io.IOException;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/reportes")
public class ReporteFinancieroController {

    private final ReporteFinancieroService reporteFinancieroService;
    // ← AGREGAR ESTA LÍNEA - INYECTAR EL REPOSITORY
    private final ConceptoCobroRepository conceptoCobroRepository;

    public ReporteFinancieroController(ReporteFinancieroService reporteFinancieroService,
                                       ConceptoCobroRepository conceptoCobroRepository) {
        this.reporteFinancieroService = reporteFinancieroService;
        this.conceptoCobroRepository = conceptoCobroRepository;  // ← AGREGAR ESTA LÍNEA
    }

    // Enum para los tipos de reporte
    public enum TipoReporte {
        SEGUROS_COMPLETOS("Seguros Completos"),
        CUOTA_ASOCIACION("Cuota Asociación"),
        CUOTA_SEGURO("Cuota Seguro"),
        SEGURO("Seguro"),
        IMPORTE_TRASLADO("Importe Traslado"),
        FONDO_ESTRELLA("Fondo Estrella");

        private final String descripcion;

        TipoReporte(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }

    @GetMapping("/financiero")
    public void exportarReporteFinanciero(
            @RequestParam("mes") @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes,
            @RequestParam(value = "tipo", defaultValue = "SEGUROS_COMPLETOS") TipoReporte tipoReporte,
            HttpServletResponse response
    ) throws IOException {

        // Validación del mes
        validarMes(mes);

        // Obtener datos
        List<ReporteFinancieroDTO> todasLasFilas = reporteFinancieroService.obtenerDatosFinancierosPorMes(mes);

        // LOG: Debug información
        System.out.println("DEBUG: Total de filas encontradas: " + todasLasFilas.size());

        // Verificar si hay datos base
        if (todasLasFilas.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\": \"No se encontraron registros para el mes " +
                    mes.format(DateTimeFormatter.ofPattern("MMMM yyyy")) + "\"}");
            return;
        }

        // Filtrar solo aquellos registros que tienen información en días y cuota asociación
        List<ReporteFinancieroDTO> filas = todasLasFilas.stream()
                .filter(dto -> {
                    boolean tieneInformacion = dto.getDias() > 0 && dto.getCuotaAsociacion() != null && dto.getCuotaAsociacion() > 0;
                    System.out.println("DEBUG: Registro - Dias: " + dto.getDias() +
                            ", CuotaAsociacion: " + dto.getCuotaAsociacion() +
                            ", Válido: " + tieneInformacion);
                    return tieneInformacion;
                })
                .collect(Collectors.toList());

        System.out.println("DEBUG: Filas válidas después del filtro: " + filas.size());

        // VALIDACIÓN CORREGIDA: Verificar si hay datos válidos después del filtro
        if (filas.isEmpty()) {
            System.out.println("DEBUG: Entrando en caso 2 - Sin datos válidos");
            response.setStatus(HttpServletResponse.SC_NO_CONTENT); // 204 No Content
            System.out.println("DEBUG: Respuesta 204 enviada, finalizando método");
            return; // CRÍTICO: Parar la ejecución aquí
        }

        // Configurar respuesta
        String nombreArchivo = String.format("reporte_%s_%s.xlsx",
                tipoReporte.name().toLowerCase(), mes);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + nombreArchivo);

        // Generar el reporte según el tipo
        switch (tipoReporte) {
            case SEGUROS_COMPLETOS:
                generarReporteCompleto(filas, mes, response);
                break;
            case CUOTA_ASOCIACION:
                generarReporteEspecifico(filas, mes, response, tipoReporte);
                break;
            case CUOTA_SEGURO:
                generarReporteEspecifico(filas, mes, response, tipoReporte);
                break;
            case SEGURO:
                generarReporteEspecifico(filas, mes, response, tipoReporte);
                break;
            case IMPORTE_TRASLADO:
                generarReporteEspecifico(filas, mes, response, tipoReporte);
                break;
            case FONDO_ESTRELLA:
                generarReporteEspecifico(filas, mes, response, tipoReporte);
                break;
            default:
                throw new IllegalArgumentException("Tipo de reporte no válido: " + tipoReporte);
        }
    }

    private void validarMes(YearMonth mes) {
        if (mes.isAfter(YearMonth.now().plusMonths(0))) {
            throw new IllegalArgumentException("El mes no puede ser mayor a 12 meses en el futuro");
        }
        if (mes.isBefore(YearMonth.of(2000, 1))) {
            throw new IllegalArgumentException("El mes no puede ser anterior al año 2000");
        }
    }

    private void generarReporteEspecifico(List<ReporteFinancieroDTO> filas, YearMonth mes,
                                          HttpServletResponse response, TipoReporte tipoReporte) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(tipoReporte.getDescripcion());

        // Crear estilos
        CellStyle headerStyle = crearEstiloEncabezado(workbook);
        CellStyle currencyStyle = crearEstiloMoneda(workbook);
        CellStyle textStyle = crearEstiloTexto(workbook);

        // Título del reporte
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("REPORTE " + tipoReporte.getDescripcion().toUpperCase() + " - " +
                mes.format(DateTimeFormatter.ofPattern("MMMM yyyy")).toUpperCase());
        CellStyle titleStyle = crearEstiloTitulo(workbook);
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

        // Encabezados
        Row headerRow = sheet.createRow(2);
        String[] columnas = {"Clave Distribuidor", "Modelo", "No. Serie", getColumnaTitulo(tipoReporte)};

        for (int i = 0; i < columnas.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columnas[i]);

            // Aplicar color específico según el tipo de reporte
            if (i == 3) {
                cell.setCellStyle(crearEstiloEncabezadoEspecial(workbook, getColorPorTipo(tipoReporte)));
            } else {
                cell.setCellStyle(headerStyle);
            }
        }

        // Datos
        int rowIdx = 3;
        double total = 0;

        for (ReporteFinancieroDTO dto : filas) {
            Row row = sheet.createRow(rowIdx++);

            row.createCell(0).setCellValue(dto.getClaveDistribuidor());
            row.getCell(0).setCellStyle(textStyle);

            row.createCell(1).setCellValue(dto.getModelo());
            row.getCell(1).setCellStyle(textStyle);

            row.createCell(2).setCellValue(dto.getNoSerie());
            row.getCell(2).setCellStyle(textStyle);

            Cell valorCell = row.createCell(3);
            double valor = getValorPorTipo(dto, tipoReporte);
            valorCell.setCellValue(valor);
            valorCell.setCellStyle(currencyStyle);

            total += valor;
        }

        // Fila de total
        Row totalRow = sheet.createRow(rowIdx + 1);
        Cell totalLabelCell = totalRow.createCell(2);
        totalLabelCell.setCellValue("TOTAL:");
        CellStyle totalLabelStyle = crearEstiloTexto(workbook);
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        totalLabelStyle.setFont(boldFont);
        totalLabelCell.setCellStyle(totalLabelStyle);

        Cell totalValueCell = totalRow.createCell(3);
        totalValueCell.setCellValue(total);
        CellStyle totalStyle = crearEstiloMoneda(workbook);
        totalStyle.setFont(boldFont);
        totalStyle.setFillForegroundColor(getColorPorTipo(tipoReporte));
        totalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        totalValueCell.setCellStyle(totalStyle);

        // Ajustar columnas
        for (int i = 0; i < 4; i++) {
            sheet.autoSizeColumn(i);
        }

        // Congelar panel
        sheet.createFreezePane(0, 3);

        // Escribir y cerrar
        try {
            workbook.write(response.getOutputStream());
        } finally {
            workbook.close();
        }
    }

    private String getColumnaTitulo(TipoReporte tipo) {
        switch (tipo) {
            case CUOTA_ASOCIACION:
                return "Cuota Asociación";
            case CUOTA_SEGURO:
                return "Cuota Seguro (3.24%)";
            case SEGURO:
                return "Seguro (1.34%)";
            case IMPORTE_TRASLADO:
                return "Importe Traslado";
            case FONDO_ESTRELLA:
                return "Fondo Estrella";
            default:
                return "";
        }
    }

    private double getValorPorTipo(ReporteFinancieroDTO dto, TipoReporte tipo) {
        switch (tipo) {
            case CUOTA_ASOCIACION:
                return dto.getCuotaAsociacion() != null ? dto.getCuotaAsociacion() : 0;
            case CUOTA_SEGURO:
                return dto.getCuotaSeguro() != null ? dto.getCuotaSeguro() : 0;
            case SEGURO:
                return dto.getSeguro() != null ? dto.getSeguro() : 0;
            case IMPORTE_TRASLADO:
                return dto.getImporteTraslado() != null ? dto.getImporteTraslado() : 0;
            case FONDO_ESTRELLA:
                return dto.getFondoEstrella() != null ? dto.getFondoEstrella() : 0;
            default:
                return 0;
        }
    }

    private short getColorPorTipo(TipoReporte tipo) {
        switch (tipo) {
            case CUOTA_ASOCIACION:
                return IndexedColors.YELLOW.getIndex();
            case CUOTA_SEGURO:
                return IndexedColors.LIGHT_BLUE.getIndex();
            case SEGURO:
                return IndexedColors.LIGHT_BLUE.getIndex();
            case IMPORTE_TRASLADO:
                return IndexedColors.BRIGHT_GREEN.getIndex();
            case FONDO_ESTRELLA:
                return IndexedColors.GOLD.getIndex();
            default:
                return IndexedColors.GREY_25_PERCENT.getIndex();
        }
    }

    private void generarReporteCompleto(List<ReporteFinancieroDTO> filas, YearMonth mes,
                                        HttpServletResponse response) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Reporte Financiero");
        // Tasa de IVA (ej. 16%)
        final double tasaIVA = 0.16;

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
        CellStyle desgloseStyle = crearEstiloEncabezadoEspecial(workbook, IndexedColors.PALE_BLUE.getIndex());

        // Título del reporte
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("REPORTE FINANCIERO CON DESGLOSE - " + mes.format(DateTimeFormatter.ofPattern("MMMM yyyy")).toUpperCase());
        CellStyle titleStyle = crearEstiloTitulo(workbook);
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 24)); // Aumentamos el rango para más columnas

        // Fila de encabezados
        int rowIdx = 2;
        String[] columnas = {
                // INFORMACIÓN BÁSICA (0-8)
                "Fecha Proceso", "Clave Distribuidor", "Número Factura", "Modelo", "No. Serie",
                "Fecha Factura", "Fecha Interés", "Días", "Importe Factura",

                // TOTALES PRINCIPALES (9-13)
                "Cuota Asociación", "Cuota Seguro (3.24%)", "Seguro (1.34%)", "Importe Traslado", "Fondo Estrella",

                // SEPARADOR (14)
                "",

                // DESGLOSE DE CUOTA ASOCIACIÓN (15-19)
                "ASOCIACIÓN", "CONVENCIÓN", "AMDA", "PUBLICIDAD", "CAPACITACIÓN"
        };

        Row encabezado = sheet.createRow(rowIdx);
        encabezado.setHeightInPoints(30); // Altura del encabezado

        // Agregar encabezados
        for (int i = 0; i < columnas.length; i++) {
            Cell cell = encabezado.createCell(i);
            cell.setCellValue(columnas[i]);

            // Aplicar estilos específicos
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
            } else if (i >= 15 && i <= 19) { // Desglose
                cell.setCellStyle(desgloseStyle);
            } else if (i == 14) { // Separador
                cell.setCellStyle(textStyle);
            } else {
                cell.setCellStyle(headerStyle);
            }
        }

        // Variables para calcular totales
        double totalCuotaAsociacion = 0;
        double totalCuotaSeguro = 0;
        double totalSeguro = 0;
        double totalImporteTraslado = 0;
        double totalFondoEstrella = 0;

        // Totales del desglose
        double totalAsociacion = 0;
        double totalConvencion = 0;
        double totalAmda = 0;
        double totalPublicidad = 0;
        double totalCapacitacion = 0;

        // Crear todas las filas de datos
        for (int i = 0; i < filas.size(); i++) {
            ReporteFinancieroDTO dto = filas.get(i);
            rowIdx++;
            Row row = sheet.createRow(rowIdx);

            // ═══════════════════════════════════════
            // INFORMACIÓN BÁSICA (Columnas 0-8)
            // ═══════════════════════════════════════
            row.createCell(0).setCellValue(dto.getFechaProceso());
            row.getCell(0).setCellStyle(dateStyle);

            row.createCell(1).setCellValue(dto.getClaveDistribuidor());
            row.getCell(1).setCellStyle(textStyle);

            row.createCell(2).setCellValue(dto.getNumeroFactura());
            row.getCell(2).setCellStyle(textStyle);

            row.createCell(3).setCellValue(dto.getModelo());
            row.getCell(3).setCellStyle(textStyle);

            row.createCell(4).setCellValue(dto.getNoSerie());
            row.getCell(4).setCellStyle(textStyle);

            row.createCell(5).setCellValue(dto.getFechaTraslado());
            row.getCell(5).setCellStyle(dateStyle);

            row.createCell(6).setCellValue(dto.getFechaInteres());
            row.getCell(6).setCellStyle(dateStyle);

            row.createCell(7).setCellValue(dto.getDias() != null ? dto.getDias() : 0);
            row.getCell(7).setCellStyle(numberStyle);

            row.createCell(8).setCellValue(dto.getValorUnidad() != null ? dto.getValorUnidad() : 0);
            row.getCell(8).setCellStyle(currencyStyle);
            List<Object[]> data = conceptoCobroRepository.findAplicaIvaData();
            Map<Integer, Boolean> aplicaIvaMap = data.stream()
                    .collect(Collectors.toMap(
                            result -> (Integer) result[0],  // c.id
                            result -> (Boolean) result[1]   // c.aplicaIva
                    ));
            // ═══════════════════════════════════════
            // TOTALES PRINCIPALES (Columnas 9-13)
            // ═══════════════════════════════════════
            double cuotaAsociacion = dto.getCuotaAsociacion() != null ? dto.getCuotaAsociacion() : 0;
            double cuotaSeguro = dto.getCuotaSeguro() != null ? dto.getCuotaSeguro() : 0;
            double seguro = dto.getSeguro() != null ? dto.getSeguro() : 0;
            double importeTraslado = dto.getImporteTraslado() != null ? dto.getImporteTraslado() : 0;
            double asociacion = dto.getAsociacion() != null ? dto.getAsociacion() : 0;
            double fondoEstrella = dto.getFondoEstrella() != null ? dto.getFondoEstrella() : 0;
            double convencion = dto.getConvencion() != null ? dto.getConvencion() : 0;
            double amda = dto.getAmda() != null ? dto.getAmda() : 0;
            double publicidad = dto.getPublicidad() != null ? dto.getPublicidad() : 0;
            double capacitacion = dto.getCapacitacion() != null ? dto.getCapacitacion() : 0;
            // Cuota Asociación (Total)

            row.createCell(9).setCellValue(cuotaAsociacion);
            row.getCell(9).setCellStyle(currencyStyle);
            totalCuotaAsociacion += cuotaAsociacion;

            // Cuota Seguro

            row.createCell(10).setCellValue(cuotaSeguro);
            row.getCell(10).setCellStyle(currencyStyle);
            totalCuotaSeguro += cuotaSeguro;

            // Seguro

            row.createCell(11).setCellValue(seguro);
            row.getCell(11).setCellStyle(currencyStyle);
            totalSeguro += seguro;

            // Importe Traslado

            row.createCell(12).setCellValue(importeTraslado);
            row.getCell(12).setCellStyle(currencyStyle);
            totalImporteTraslado += importeTraslado;

            // Fondo Estrella

            row.createCell(13).setCellValue(fondoEstrella);
            row.getCell(13).setCellStyle(currencyStyle);
            totalFondoEstrella += fondoEstrella;

            // Separador (Columna 14)
            row.createCell(14).setCellValue("");
            row.getCell(14).setCellStyle(textStyle);

            // ═══════════════════════════════════════
            // DESGLOSE DE CUOTA ASOCIACIÓN (Columnas 15-19)
            // ═══════════════════════════════════════

            // ASOCIACIÓN

            row.createCell(15).setCellValue(asociacion);
            row.getCell(15).setCellStyle(currencyStyle);
            totalAsociacion += asociacion;

            // CONVENCIÓN

            row.createCell(16).setCellValue(convencion);
            row.getCell(16).setCellStyle(currencyStyle);
            totalConvencion += convencion;

            // AMDA

            row.createCell(17).setCellValue(amda);
            row.getCell(17).setCellStyle(currencyStyle);
            totalAmda += amda;

            // PUBLICIDAD
            row.createCell(18).setCellValue(publicidad);
            row.getCell(18).setCellStyle(currencyStyle);
            totalPublicidad += publicidad;

            // CAPACITACIÓN

            row.createCell(19).setCellValue(capacitacion);
            row.getCell(19).setCellStyle(currencyStyle);
            totalCapacitacion += capacitacion;
        }

        // ═══════════════════════════════════════
        // FILA DE TOTALES
        // ═══════════════════════════════════════
        rowIdx += 2; // Dejar una fila en blanco
        Row totalRow = sheet.createRow(rowIdx);

        // Crear estilos para los totales con colores de fondo
        CellStyle totalLabelStyle = crearEstiloTexto(workbook);
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        boldFont.setFontHeightInPoints((short) 12);
        totalLabelStyle.setFont(boldFont);

        // Etiqueta "TOTAL"
        Cell totalLabel = totalRow.createCell(8);
        totalLabel.setCellValue("TOTAL:");
        totalLabel.setCellStyle(totalLabelStyle);

        // Estilos para totales con colores
        CellStyle[] totalStyles = {
                crearEstiloMonedaColoreado(workbook, boldFont, IndexedColors.YELLOW.getIndex()),      // Cuota Asociación
                crearEstiloMonedaColoreado(workbook, boldFont, IndexedColors.LIGHT_BLUE.getIndex()),  // Cuota Seguro
                crearEstiloMonedaColoreado(workbook, boldFont, IndexedColors.LIGHT_BLUE.getIndex()),  // Seguro
                crearEstiloMonedaColoreado(workbook, boldFont, IndexedColors.BRIGHT_GREEN.getIndex()),// Importe Traslado
                crearEstiloMonedaColoreado(workbook, boldFont, IndexedColors.GOLD.getIndex()),        // Fondo Estrella
                crearEstiloMonedaColoreado(workbook, boldFont, IndexedColors.PALE_BLUE.getIndex())    // Desglose
        };

        // Totales principales
        double[] totales = {totalCuotaAsociacion, totalCuotaSeguro, totalSeguro, totalImporteTraslado, totalFondoEstrella};
        for (int i = 0; i < totales.length; i++) {
            Cell totalCell = totalRow.createCell(9 + i);
            totalCell.setCellValue(totales[i]);
            totalCell.setCellStyle(totalStyles[i]);
        }

        // Totales del desglose
        double[] totalesDesglose = {totalAsociacion, totalConvencion, totalAmda, totalPublicidad, totalCapacitacion};
        for (int i = 0; i < totalesDesglose.length; i++) {
            Cell totalCell = totalRow.createCell(15 + i);
            totalCell.setCellValue(totalesDesglose[i]);
            totalCell.setCellStyle(totalStyles[5]); // Usar estilo de desglose
        }

        // ═══════════════════════════════════════
        // VALIDACIÓN DEL TOTAL 17883
        // ═══════════════════════════════════════
        double totalDesgloseCalculado = totalAsociacion + totalConvencion + totalAmda + totalPublicidad + totalCapacitacion;

        // Agregar fila de verificación
        Row verificacionRow = sheet.createRow(rowIdx + 2);
        Cell verificacionLabel = verificacionRow.createCell(15);
        verificacionLabel.setCellValue("VERIFICACIÓN TOTAL DESGLOSE:");
        verificacionLabel.setCellStyle(totalLabelStyle);

        Cell verificacionValue = verificacionRow.createCell(16);
        verificacionValue.setCellValue(totalDesgloseCalculado);
        verificacionValue.setCellStyle(totalStyles[0]); // Amarillo

        Cell esperadoLabel = verificacionRow.createCell(17);
        esperadoLabel.setCellValue("(Esperado: 17,883)");
        esperadoLabel.setCellStyle(textStyle);

        // Log para debugging
        System.out.println("🔍 VERIFICACIÓN DE TOTALES:");
        System.out.println("   Cuota Asociación Total: " + String.format("%.2f", totalCuotaAsociacion));
        System.out.println("   Desglose Calculado: " + String.format("%.2f", totalDesgloseCalculado));
        System.out.println("   Diferencia: " + String.format("%.2f", Math.abs(totalCuotaAsociacion - totalDesgloseCalculado)));

        // Ajustar el ancho de las columnas automáticamente
        for (int i = 0; i <= 19; i++) {
            sheet.autoSizeColumn(i);
        }

        // Congelar el panel para mantener visible la fila de encabezado
        sheet.createFreezePane(0, 3);

        // Agregar filtros en las columnas principales
        sheet.setAutoFilter(new CellRangeAddress(2, 2, 0, 19));

        try {
            workbook.write(response.getOutputStream());
        } finally {
            workbook.close();
        }
    }

    // MÉTODO HELPER PARA CREAR ESTILOS COLOREADOS
    private CellStyle crearEstiloMonedaColoreado(Workbook workbook, Font font, short colorIndex) {
        CellStyle style = crearEstiloMoneda(workbook);
        style.setFont(font);
        style.setFillForegroundColor(colorIndex);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }
    private void agregarDesgloseLateral(Sheet sheet, Workbook workbook) {
        CellStyle textStyle = crearEstiloTexto(workbook);
        CellStyle currencyStyle = crearEstiloMoneda(workbook);
        CellStyle numberStyle = crearEstiloNumero(workbook);

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
        amdaPercentValue.setCellValue(03.24);
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

        // CAPACITACION - Quinta fila (fila 8)
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

        // PUBLICIDAD - Sexta fila (fila 9)
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

        // Total parcial - Séptima fila (fila 10)
        Row subTotalRow = getOrCreateRow(sheet, 10);
        Cell subTotalValue = subTotalRow.createCell(15);
        subTotalValue.setCellValue(15080.00);
        subTotalValue.setCellStyle(currencyStyle);

        // Total final con todos los valores - Octava fila (fila 11)
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
    }

    /**
     * Manejador de excepciones para errores de conversión de parámetros
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex) {

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", java.time.LocalDateTime.now());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("error", "Bad Request");

        if (ex.getName().equals("mes")) {
            errorResponse.put("message", "Formato de mes inválido. El formato esperado es YYYY-MM (ejemplo: 2025-05)");
            errorResponse.put("ejemplo", "2025-05");
        } else if (ex.getName().equals("tipo")) {
            errorResponse.put("message", "Tipo de reporte inválido. Los tipos válidos son: SEGUROS_COMPLETOS, CUOTA_ASOCIACION, CUOTA_SEGURO, SEGURO, IMPORTE_TRASLADO, FONDO_ESTRELLA");
        } else {
            errorResponse.put("message", "Parámetro inválido: " + ex.getName());
        }

        errorResponse.put("path", "/api/reportes/financiero");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Manejador de excepciones para argumentos ilegales
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", java.time.LocalDateTime.now());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("error", "Bad Request");
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("path", "/api/reportes/financiero");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Manejador de excepciones para errores de E/O
     */
    @ExceptionHandler(IOException.class)
    public ResponseEntity<Map<String, Object>> handleIOException(IOException ex) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", java.time.LocalDateTime.now());
        errorResponse.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorResponse.put("error", "Internal Server Error");
        errorResponse.put("message", "Error al generar el archivo Excel");
        errorResponse.put("path", "/api/reportes/financiero");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Manejador de excepciones genéricas
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", java.time.LocalDateTime.now());
        errorResponse.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorResponse.put("error", "Internal Server Error");
        errorResponse.put("message", "Error inesperado en el servidor");
        errorResponse.put("path", "/api/reportes/financiero");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
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
    @GetMapping("/verificar-conceptos")
    public ResponseEntity<?> verificarConceptos() {
        try {
            Map<String, Object> resultado = new HashMap<>();

            // ═══════════════════════════════════════
            // MAPEO DE IDs A NOMBRES PARA VERIFICACIÓN
            // ═══════════════════════════════════════
            Map<Integer, String> conceptosEsenciales = Map.of(
                    1, "Tarifa única de traslado",
                    2, "Seguro broker (1.34%)",
                    3, "Seguro ADAVEC (3.24%)",
                    4, "Cuota asociación ADAVEC",
                    5, "Cuota convención ADAVEC",
                    6, "Cuota AMDA",
                    7, "Publicidad ASOBENS",
                    8, "Capacitación ASOBENS",
                    9, "Fondo estrella"
            );

            Map<String, Object> estadoConceptos = new HashMap<>();
            boolean todosExisten = true;

            // Verificar cada concepto por ID (más eficiente)
            for (Map.Entry<Integer, String> entry : conceptosEsenciales.entrySet()) {
                Integer id = entry.getKey();
                String descripcion = entry.getValue();

                boolean existe = conceptoCobroRepository.findById(id).isPresent();
                estadoConceptos.put("ID_" + id + "_" + descripcion, existe ? "✅ EXISTE" : "❌ FALTA");

                if (!existe) {
                    todosExisten = false;
                    System.err.println("❌ Falta concepto ID " + id + ": " + descripcion);
                } else {
                    System.out.println("✅ Concepto ID " + id + " encontrado: " + descripcion);
                }
            }

            // Verificación adicional usando el método optimizado
            Long conceptosEncontrados = conceptoCobroRepository.countConceptosEsenciales();
            boolean verificacionOptimizada = conceptosEncontrados == 9;

            resultado.put("conceptos", estadoConceptos);
            resultado.put("configuracionCompleta", todosExisten);
            resultado.put("verificacionOptimizada", verificacionOptimizada);
            resultado.put("conceptosEncontrados", conceptosEncontrados);
            resultado.put("conceptosEsperados", 9);

            if (!todosExisten) {
                // Obtener IDs faltantes
                List<Integer> faltantes = conceptoCobroRepository.findConceptosFaltantes();
                resultado.put("idsFaltantes", faltantes);
                resultado.put("solucion", "Insertar los conceptos faltantes con IDs: " + faltantes);
            }

            // Información adicional para debugging
            List<Object[]> resumenConceptos = conceptoCobroRepository.findResumenConceptos();
            Map<String, Object> resumen = new HashMap<>();

            for (Object[] concepto : resumenConceptos) {
                Integer id = (Integer) concepto[0];
                String nombre = (String) concepto[1];
                String categoria = (String) concepto[2];

                resumen.put("ID_" + id, Map.of(
                        "nombre", nombre,
                        "categoria", categoria
                ));
            }

            resultado.put("resumenDetallado", resumen);

            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            System.err.println("💥 Error verificando conceptos: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Test específico para el desglose usando IDs
     */
    @GetMapping("/test-desglose-ids")
    public ResponseEntity<?> testDesgloseConIds(@RequestParam("mes") @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes) {
        try {
            Map<String, Object> resultado = new HashMap<>();

            System.out.println("🧪 PRUEBA DE DESGLOSE CON IDs PARA: " + mes);
            System.out.println("═══════════════════════════════════════");

            // Obtener datos del reporte
            List<ReporteFinancieroDTO> datos = reporteFinancieroService.obtenerDatosFinancierosPorMes(mes);

            if (datos.isEmpty()) {
                resultado.put("message", "No hay datos para el mes " + mes);
                return ResponseEntity.ok(resultado);
            }

            // Verificar que los conceptos del desglose existen
            List<ConceptoCobro> conceptosDesglose = conceptoCobroRepository.findConceptosDesglose();
            Map<String, Object> conceptosEncontrados = new HashMap<>();

            for (ConceptoCobro concepto : conceptosDesglose) {
                conceptosEncontrados.put("ID_" + concepto.getId(), concepto.getNombre());
            }

            // Estadísticas generales
            resultado.put("totalUnidades", datos.size());
            resultado.put("unidadesConCobros", datos.stream().filter(dto -> dto.getCuotaAsociacion() > 0).count());
            resultado.put("conceptosDesgloseEncontrados", conceptosEncontrados);

            // Verificar desglose de cuota asociación
            Map<String, Double> totalesDesglose = new HashMap<>();
            totalesDesglose.put("asociacion_ID4", datos.stream().mapToDouble(dto -> dto.getAsociacion() != null ? dto.getAsociacion() : 0).sum());
            totalesDesglose.put("convencion_ID5", datos.stream().mapToDouble(dto -> dto.getConvencion() != null ? dto.getConvencion() : 0).sum());
            totalesDesglose.put("amda_ID6", datos.stream().mapToDouble(dto -> dto.getAmda() != null ? dto.getAmda() : 0).sum());
            totalesDesglose.put("publicidad_ID7", datos.stream().mapToDouble(dto -> dto.getPublicidad() != null ? dto.getPublicidad() : 0).sum());
            totalesDesglose.put("capacitacion_ID8", datos.stream().mapToDouble(dto -> dto.getCapacitacion() != null ? dto.getCapacitacion() : 0).sum());

            double totalDesglose = totalesDesglose.values().stream().mapToDouble(Double::doubleValue).sum();
            double totalCuotaAsociacion = datos.stream().mapToDouble(dto -> dto.getCuotaAsociacion() != null ? dto.getCuotaAsociacion() : 0).sum();

            resultado.put("desglosePorId", totalesDesglose);
            resultado.put("totalDesglose", totalDesglose);
            resultado.put("totalCuotaAsociacion", totalCuotaAsociacion);
            resultado.put("diferencia", Math.abs(totalDesglose - totalCuotaAsociacion));
            resultado.put("target17883Correcto", Math.abs(totalDesglose - 17883.0) < 0.01);

            // Totales de conceptos principales (por ID)
            Map<String, Double> conceptosPrincipales = new HashMap<>();
            conceptosPrincipales.put("cuotaSeguro_ID3", datos.stream().mapToDouble(dto -> dto.getCuotaSeguro() != null ? dto.getCuotaSeguro() : 0).sum());
            conceptosPrincipales.put("seguro_ID2", datos.stream().mapToDouble(dto -> dto.getSeguro() != null ? dto.getSeguro() : 0).sum());
            conceptosPrincipales.put("importeTraslado_ID1", datos.stream().mapToDouble(dto -> dto.getImporteTraslado() != null ? dto.getImporteTraslado() : 0).sum());
            conceptosPrincipales.put("fondoEstrella_ID9", datos.stream().mapToDouble(dto -> dto.getFondoEstrella() != null ? dto.getFondoEstrella() : 0).sum());

            resultado.put("conceptosPrincipalesPorId", conceptosPrincipales);

            // Log detallado
            System.out.println("📊 RESULTADOS CON IDs:");
            System.out.println("   Total Unidades: " + datos.size());
            System.out.println("   Cuota Asociación Total: " + String.format("%.2f", totalCuotaAsociacion));
            System.out.println("   Desglose Total: " + String.format("%.2f", totalDesglose));
            System.out.println("   Target 17,883: " + (Math.abs(totalDesglose - 17883.0) < 0.01 ? "✅ CORRECTO" : "❌ INCORRECTO"));
            System.out.println("   Conceptos desglose encontrados: " + conceptosDesglose.size() + "/5");
            System.out.println("═══════════════════════════════════════");

            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            System.err.println("💥 Error en prueba de desglose con IDs: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    /**
     * Muestra el mapeo completo de IDs a columnas del Excel
     */
    @GetMapping("/mapeo-conceptos")
    public ResponseEntity<?> mostrarMapeoConceptos() {
        try {
            Map<String, Object> resultado = new HashMap<>();

            // ═══════════════════════════════════════
            // MAPEO DE IDs A COLUMNAS EXCEL
            // ═══════════════════════════════════════
            Map<String, Object> mapeoColumnas = new LinkedHashMap<>();

            // Conceptos principales
            mapeoColumnas.put("Cuota Seguro (3.24%)", Map.of(
                    "id", 3,
                    "descripcion", "Seguro ADAVEC (3.24% del valor)",
                    "tipo", "PORCENTAJE",
                    "categoria", "SEGURO"
            ));

            mapeoColumnas.put("Seguro (1.34%)", Map.of(
                    "id", 2,
                    "descripcion", "Seguro broker (1.34% del valor)",
                    "tipo", "PORCENTAJE",
                    "categoria", "SEGURO"
            ));

            mapeoColumnas.put("Importe Traslado", Map.of(
                    "id", 1,
                    "descripcion", "Tarifa única de traslado",
                    "tipo", "MONTO_FIJO",
                    "categoria", "TRASLADO"
            ));

            mapeoColumnas.put("Fondo Estrella", Map.of(
                    "id", 9,
                    "descripcion", "Fondo estrella - Monto variable",
                    "tipo", "MANUAL",
                    "categoria", "FONDO"
            ));

            // ═══════════════════════════════════════
            // DESGLOSE DE CUOTA ASOCIACIÓN (Target: 17,883)
            // ═══════════════════════════════════════
            Map<String, Object> desgloseAsociacion = new LinkedHashMap<>();

            desgloseAsociacion.put("ASOCIACIÓN", Map.of(
                    "id", 4,
                    "descripcion", "Cuota de asociación ADAVEC",
                    "tipo", "MONTO_FIJO"
            ));

            desgloseAsociacion.put("CONVENCIÓN", Map.of(
                    "id", 5,
                    "descripcion", "Cuota de convención ADAVEC",
                    "tipo", "MONTO_FIJO"
            ));

            desgloseAsociacion.put("AMDA", Map.of(
                    "id", 6,
                    "descripcion", "Cuota AMDA",
                    "tipo", "MONTO_FIJO"
            ));

            desgloseAsociacion.put("PUBLICIDAD", Map.of(
                    "id", 7,
                    "descripcion", "Publicidad ASOBENS",
                    "tipo", "MONTO_FIJO"
            ));

            desgloseAsociacion.put("CAPACITACIÓN", Map.of(
                    "id", 8,
                    "descripcion", "Capacitación ASOBENS",
                    "tipo", "MONTO_FIJO"
            ));

            // ═══════════════════════════════════════
            // INFORMACIÓN DE LA BASE DE DATOS
            // ═══════════════════════════════════════
            List<Object[]> conceptosActuales = conceptoCobroRepository.findAllConceptosInfo();
            Map<String, Object> conceptosEnBD = new LinkedHashMap<>();

            for (Object[] concepto : conceptosActuales) {
                Integer id = (Integer) concepto[0];
                String nombre = (String) concepto[1];
                String descripcion = (String) concepto[2];

                conceptosEnBD.put("ID_" + id, Map.of(
                        "nombre", nombre,
                        "descripcion", descripcion != null ? descripcion : "Sin descripción"
                ));
            }

            // ═══════════════════════════════════════
            // VALIDACIÓN DEL MAPEO
            // ═══════════════════════════════════════
            List<Integer> idsEsperados = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9);
            List<Integer> idsFaltantes = new ArrayList<>();

            for (Integer id : idsEsperados) {
                if (!conceptoCobroRepository.findById(id).isPresent()) {
                    idsFaltantes.add(id);
                }
            }

            boolean mapeoCompleto = idsFaltantes.isEmpty();

            // ═══════════════════════════════════════
            // CONSTRUCCIÓN DE LA RESPUESTA
            // ═══════════════════════════════════════
            resultado.put("columnasPrincipales", mapeoColumnas);
            resultado.put("desgloseAsociacion", desgloseAsociacion);
            resultado.put("targetDesgloseTotal", 17883.0);
            resultado.put("conceptosEnBaseDatos", conceptosEnBD);
            resultado.put("mapeoCompleto", mapeoCompleto);
            resultado.put("idsFaltantes", idsFaltantes);

            if (!mapeoCompleto) {
                resultado.put("advertencia", "Faltan conceptos en la base de datos");
                resultado.put("solucion", "Insertar los conceptos con IDs: " + idsFaltantes);
            }

            // Información adicional
            resultado.put("totalConceptosEsperados", 9);
            resultado.put("totalConceptosEncontrados", conceptosActuales.size());
            resultado.put("estructuraExcel", Map.of(
                    "columnas0_8", "Información básica de la unidad",
                    "columnas9_13", "Totales principales (Cuota Asociación, Cuota Seguro, Seguro, Importe Traslado, Fondo Estrella)",
                    "columnas15_19", "Desglose de Cuota Asociación (ASOCIACIÓN + CONVENCIÓN + AMDA + PUBLICIDAD + CAPACITACIÓN = 17,883)"
            ));

            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            System.err.println("💥 Error mostrando mapeo: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

}