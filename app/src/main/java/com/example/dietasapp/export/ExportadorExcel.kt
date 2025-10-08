package com.example.dietasapp.export

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.dietasapp.calculation.methane.MethaneCalculator
import com.example.dietasapp.domain.Dieta
import com.example.dietasapp.data.Insumo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

/**
 * Exportador de dietas a formato Excel (.xlsx)
 * Utiliza Apache POI para generar archivos Excel completos
 */
class ExportadorExcel(private val context: Context) {

    companion object {
        private const val SHEET_RESUMEN = "Resumen"
        private const val SHEET_COMPOSICION = "Composición"
        private const val SHEET_NUTRIENTES = "Nutrientes"
        private const val SHEET_METANO = "Análisis de Metano"
    }

    /**
     * Exporta una dieta a un archivo Excel
     */
    suspend fun exportar(
        dieta: Dieta,
        nombreArchivo: String,
        insumos: List<Insumo>,
        methaneResult: MethaneCalculator.MethaneResult? = null
    ): ExportResult = withContext(Dispatchers.IO) {
        try {
            println("Iniciando exportación a Excel: $nombreArchivo")

            val workbook = XSSFWorkbook()
            val styles = createStyles(workbook)

            createResumenSheet(workbook, dieta, styles)
            createComposicionSheet(workbook, dieta, insumos, styles)
            createNutrientesSheet(workbook, dieta, styles)
            if (methaneResult != null) {
                createMetanoSheet(workbook, dieta, methaneResult, styles)
            }

            val fileName = "$nombreArchivo.xlsx"
            val outputStream = createOutputStream(fileName)
                ?: return@withContext ExportResult.Error("No se pudo crear el archivo de salida")

            workbook.write(outputStream)
            outputStream.close()
            workbook.close()

            val filePath = getFilePath(fileName)
            println("✓ Archivo Excel exportado: $filePath")

            ExportResult.Success(filePath)
        } catch (e: Exception) {
            println("Error al exportar a Excel: ${e.message}")
            e.printStackTrace()
            ExportResult.Error(e.message ?: "Error desconocido")
        }
    }

    // ============= CREACIÓN DE HOJAS =============

    private fun createResumenSheet(
        workbook: Workbook,
        dieta: Dieta,
        styles: ExcelStyles
    ) {
        val sheet = workbook.createSheet(SHEET_RESUMEN)
        var rowNum = 0

        // Título
        sheet.createRow(rowNum).also { row ->
            row.createCell(0).apply {
                setCellValue("REPORTE DE DIETA ÓPTIMA")
                cellStyle = styles.titleStyle
            }
        }
        sheet.addMergedRegion(CellRangeAddress(0, 0, 0, 3))
        rowNum += 2 // línea en blanco

        // Fecha
        sheet.createRow(rowNum++).apply {
            createCell(0).setCellValue("Fecha de generación:")
            createCell(1).setCellValue(formatDate(System.currentTimeMillis()))
        }

        rowNum++ // línea en blanco

        // Información del animal
        sheet.createRow(rowNum).apply {
            createCell(0).apply {
                setCellValue("INFORMACIÓN DEL ANIMAL")
                cellStyle = styles.headerStyle
            }
        }
        sheet.addMergedRegion(CellRangeAddress(rowNum, rowNum, 0, 1))
        rowNum++

        createDataRow(sheet, rowNum++, "Nombre:", dieta.animal.nombre, styles)
        createDataRow(sheet, rowNum++, "Peso corporal:", "${dieta.animal.pesoKg} kg", styles)
        createDataRow(sheet, rowNum++, "Consumo DMI objetivo:", "${dieta.animal.consumoDMI} kg/día", styles)
        createDataRow(sheet, rowNum++, "Tipo de dieta:", dieta.animal.tipo.descripcion, styles)

        rowNum++ // línea en blanco

        // Resultados económicos
        sheet.createRow(rowNum).apply {
            createCell(0).apply {
                setCellValue("RESULTADOS ECONÓMICOS")
                cellStyle = styles.headerStyle
            }
        }
        sheet.addMergedRegion(CellRangeAddress(rowNum, rowNum, 0, 1))
        rowNum++

        sheet.createRow(rowNum++).apply {
            createCell(0).setCellValue("Costo total:")
            createCell(1).apply {
                setCellValue("\$${String.format("%.2f", dieta.costoTotal)}/día")
            }
        }
        sheet.createRow(rowNum++).apply {
            createCell(0).setCellValue("Costo por kg MS:")
            createCell(1).apply {
                setCellValue("\$${String.format("%.4f", dieta.costoPorKgMS())}/kg")
            }
        }

        val totalKg = dieta.composicion.values.sum()
        sheet.createRow(rowNum++).apply {
            createCell(0).setCellValue("Total MS suministrado:")
            createCell(1).setCellValue(String.format("%.2f", totalKg) + " kg/día")
        }

        rowNum++ // línea en blanco

        // Impacto ambiental
        sheet.createRow(rowNum).apply {
            createCell(0).apply {
                setCellValue("IMPACTO AMBIENTAL")
                cellStyle = styles.headerStyle
            }
        }
        sheet.addMergedRegion(CellRangeAddress(rowNum, rowNum, 0, 1))
        rowNum++

        createDataRow(sheet, rowNum++, "Metano producido:", "${String.format("%.2f", dieta.metanoProducidoGramos)} g/día", styles)
        createDataRow(sheet, rowNum++, "Metano por kg DMI:", "${String.format("%.2f", dieta.metanoPorKgDMI())} g/kg", styles)

        sheet.setColumnWidth(0, 8000)
        sheet.setColumnWidth(1, 6000)
    }

    private fun createComposicionSheet(
        workbook: Workbook,
        dieta: Dieta,
        insumos: List<Insumo>,
        styles: ExcelStyles
    ) {
        val sheet = workbook.createSheet(SHEET_COMPOSICION)
        var rowNum = 0

        // Título
        sheet.createRow(rowNum).apply {
            createCell(0).apply {
                setCellValue("COMPOSICIÓN DE LA DIETA")
                cellStyle = styles.titleStyle
            }
        }
        sheet.addMergedRegion(CellRangeAddress(0, 0, 0, 4))
        rowNum += 2

        // Encabezados
        val headerRow = sheet.createRow(rowNum++)
        val headers = listOf("Ingrediente", "Cantidad (kg/día)", "Porcentaje (%)", "Costo Unitario ($/kg)", "Costo Total ($/día)")
        headers.forEachIndexed { idx, h ->
            headerRow.createCell(idx).apply {
                setCellValue(h)
                cellStyle = styles.tableHeaderStyle
            }
        }

        val totalKg = dieta.composicion.values.sum()
        var totalCosto = 0.0

        dieta.composicion.entries
            .sortedByDescending { it.value }
            .forEach { (ingrediente, kg) ->
                val row = sheet.createRow(rowNum++)

                // Porcentaje como fracción para usar 0.00%
                val porcentajeFraccion = if (totalKg > 0.0) kg / totalKg else 0.0

                // Costos
                val insumo = insumos.find { it.nombre == ingrediente }
                val costoUnitario = insumo?.costo ?: 0.0
                val costoIngrediente = kg * costoUnitario
                totalCosto += costoIngrediente

                row.createCell(0).setCellValue(ingrediente)

                row.createCell(1).apply {
                    setCellValue(kg)
                    cellStyle = styles.numberStyle
                }

                row.createCell(2).apply {
                    setCellValue(porcentajeFraccion)     // fracción (ej. 0.27)
                    cellStyle = styles.percentStyle      // se verá 27.00%
                }

                row.createCell(3).apply {
                    setCellValue(costoUnitario)
                    cellStyle = styles.currencyStyle
                }

                row.createCell(4).apply {
                    setCellValue(costoIngrediente)
                    cellStyle = styles.currencyStyle
                }
            }

        // Fila de totales
        val totalRow = sheet.createRow(rowNum++)
        totalRow.createCell(0).apply {
            setCellValue("TOTAL")
            cellStyle = styles.totalStyle
        }

        totalRow.createCell(1).apply {
            setCellValue(totalKg)
            cellStyle = styles.totalNumberStyle
        }

        totalRow.createCell(2).apply {
            setCellValue(if (totalKg > 0.0) 1.0 else 0.0) // 100% como fracción
            cellStyle = styles.totalPercentStyle
        }

        totalRow.createCell(3) // vacío

        totalRow.createCell(4).apply {
            setCellValue(totalCosto)                 // suma mostrada en la tabla
            cellStyle = styles.totalCurrencyStyle
        }

        // Anchos
        sheet.setColumnWidth(0, 25 * 256)
        sheet.setColumnWidth(1, 15 * 256)
        sheet.setColumnWidth(2, 15 * 256)
        sheet.setColumnWidth(3, 20 * 256)
        sheet.setColumnWidth(4, 20 * 256)
    }

    private fun createNutrientesSheet(
        workbook: Workbook,
        dieta: Dieta,
        styles: ExcelStyles
    ) {
        val sheet = workbook.createSheet(SHEET_NUTRIENTES)
        var rowNum = 0

        // Título
        sheet.createRow(rowNum).apply {
            createCell(0).apply {
                setCellValue("APORTE NUTRICIONAL TOTAL")
                cellStyle = styles.titleStyle
            }
        }
        sheet.addMergedRegion(CellRangeAddress(0, 0, 0, 2))
        rowNum += 2

        // Encabezados
        val headerRow = sheet.createRow(rowNum++)
        listOf("Nutriente", "Valor Total", "Unidad").forEachIndexed { index, header ->
            headerRow.createCell(index).apply {
                setCellValue(header)
                cellStyle = styles.tableHeaderStyle
            }
        }

        val nutrientUnits = mapOf(
            "GE" to "Mcal/día",
            "CP" to "kg/día",
            "TDN" to "kg/día",
            "NEm" to "Mcal/día",
            "Ca" to "kg/día",
            "P" to "kg/día",
            "NDF" to "kg/día",
            "Starch" to "kg/día",
            "Fat" to "kg/día"
        )

        dieta.nutrientesTotales.entries
            .sortedBy { it.key }
            .forEach { (nutriente, valor) ->
                val row = sheet.createRow(rowNum++)
                row.createCell(0).setCellValue(nutriente)
                row.createCell(1).apply {
                    setCellValue(valor)
                    cellStyle = styles.numberStyle
                }
                row.createCell(2).setCellValue(nutrientUnits[nutriente] ?: "")
            }

        rowNum++ // línea en blanco

        // Requerimientos
        sheet.createRow(rowNum).apply {
            createCell(0).apply {
                setCellValue("REQUERIMIENTOS DEL ANIMAL")
                cellStyle = styles.headerStyle
            }
        }
        sheet.addMergedRegion(CellRangeAddress(rowNum, rowNum, 0, 2))
        rowNum++

        // Mínimos
        sheet.createRow(rowNum++).apply { createCell(0).setCellValue("Mínimos:") }
        dieta.animal.requerimientosMinimos.forEach { (nutriente, valor) ->
            val row = sheet.createRow(rowNum++)
            row.createCell(0).setCellValue("  $nutriente")
            row.createCell(1).apply {
                setCellValue(valor)
                cellStyle = styles.numberStyle
            }
            row.createCell(2).setCellValue(getRequirementUnit(nutriente))
        }

        rowNum++ // línea en blanco

        // Máximos
        sheet.createRow(rowNum++).apply { createCell(0).setCellValue("Máximos:") }
        dieta.animal.requerimientosMaximos.forEach { (nutriente, valor) ->
            val row = sheet.createRow(rowNum++)
            row.createCell(0).setCellValue("  $nutriente")
            row.createCell(1).apply {
                setCellValue(valor)
                cellStyle = styles.numberStyle
            }
            row.createCell(2).setCellValue(getRequirementUnit(nutriente))
        }

        rowNum++ // línea en blanco

        // ================== COMPARATIVO ==================
        sheet.createRow(rowNum).apply {
            createCell(0).apply {
                setCellValue("COMPARATIVO DIETA vs REQUERIMIENTOS")
                cellStyle = styles.headerStyle
            }
        }
        sheet.addMergedRegion(CellRangeAddress(rowNum, rowNum, 0, 5))
        rowNum++

        // Encabezados comparativo
        val cmpHeader = sheet.createRow(rowNum++)
        listOf(
            "Nutriente",
            "Alcanzado",
            "Unidad",
            "Req. mínimo",
            "Req. máximo",
            "Cobertura vs Mín",
            "Estado"
        ).forEachIndexed { idx, h ->
            cmpHeader.createCell(idx).apply {
                setCellValue(h)
                cellStyle = styles.tableHeaderStyle
            }
        }

        val totalMS = dieta.composicion.values.sum()

        // Conjunto de todos los nutrientes que tienen al menos un requerimiento
        val allReqKeys = (dieta.animal.requerimientosMinimos.keys + dieta.animal.requerimientosMaximos.keys)
            .toSortedSet()

        allReqKeys.forEach { nutr ->
            val reqUnit = getRequirementUnit(nutr)
            val totalValor = dieta.nutrientesTotales[nutr] ?: 0.0

            // Convertimos "alcanzado" a las unidades del requerimiento
            val (alcanzado, usePercentStyle) = when (reqUnit) {
                "%" -> {
                    // kg/día -> %MS (fracción)
                    val frac = if (totalMS > 0.0) (totalValor / totalMS) else 0.0
                    frac to true // usar estilo porcentaje
                }
                "Mcal/kg" -> {
                    // Mcal/día -> Mcal/kg
                    val valPerKg = if (totalMS > 0.0) (totalValor / totalMS) else 0.0
                    valPerKg to false
                }
                else -> {
                    // Sin conversión; usamos el total como está
                    totalValor to false
                }
            }

            val minReq = dieta.animal.requerimientosMinimos[nutr]
            val maxReq = dieta.animal.requerimientosMaximos[nutr]

            val estado = when {
                minReq != null && alcanzado < minReq -> "Bajo"
                maxReq != null && alcanzado > maxReq -> "Excede"
                else -> "OK"
            }

            val row = sheet.createRow(rowNum++)
            var c = 0

            row.createCell(c++).setCellValue(nutr)

            row.createCell(c).apply {
                if (usePercentStyle) {
                    setCellValue(alcanzado)            // fracción
                    cellStyle = styles.percentStyle
                } else {
                    setCellValue(alcanzado)
                    cellStyle = styles.numberStyle
                }
            }
            c++

            row.createCell(c++).setCellValue(if (reqUnit.isEmpty()) reqUnit else (nutrientUnits[nutr] ?: ""))

            row.createCell(c).apply {
                if (minReq != null) {
                    if (reqUnit == "%") {
                        setCellValue(minReq)          // minReq viene ya en porcentaje absoluto (ej. 12), pero...
                        // OJO: como mostramos unidad "%", lo correcto es escribir 12.00 sin formato %,
                        // o bien convertirlo a fracción y usar percentStyle.
                        // Preferimos percentStyle con fracción:
                        setCellValue(minReq / 100.0)
                        cellStyle = styles.percentStyle
                    } else {
                        setCellValue(minReq)
                        cellStyle = styles.numberStyle
                    }
                } else setCellValue("-")
            }
            c++

            row.createCell(c).apply {
                if (maxReq != null) {
                    if (reqUnit == "%") {
                        setCellValue(maxReq / 100.0)
                        cellStyle = styles.percentStyle
                    } else {
                        setCellValue(maxReq)
                        cellStyle = styles.numberStyle
                    }
                } else setCellValue("-")
            }
            c++

            row.createCell(c).apply {
                if (minReq != null && minReq > 0.0) {
                    // Cobertura como fracción
                    val cobertura = if (reqUnit == "%") {
                        // ambos en fracción: alcanzado(fracción) / (min/100)
                        alcanzado / (minReq / 100.0)
                    } else {
                        alcanzado / minReq
                    }
                    setCellValue(cobertura)
                    cellStyle = styles.percentStyle
                } else {
                    setCellValue("-")
                }
            }
            c++

            row.createCell(c).setCellValue(estado)
        }

        // Ajustes de ancho
        sheet.setColumnWidth(0, 18 * 256)
        sheet.setColumnWidth(1, 14 * 256)
        sheet.setColumnWidth(2, 10 * 256)
        sheet.setColumnWidth(3, 14 * 256)
        sheet.setColumnWidth(4, 14 * 256)
        sheet.setColumnWidth(5, 18 * 256)
        sheet.setColumnWidth(6, 12 * 256)
    }

    private fun createMetanoSheet(
        workbook: Workbook,
        dieta: Dieta,
        methaneResult: MethaneCalculator.MethaneResult,
        styles: ExcelStyles
    ) {
        val sheet = workbook.createSheet(SHEET_METANO)
        var rowNum = 0

        // Título
        sheet.createRow(rowNum).apply {
            createCell(0).apply {
                setCellValue("ANÁLISIS DE METANO ENTÉRICO")
                cellStyle = styles.titleStyle
            }
        }
        sheet.addMergedRegion(CellRangeAddress(0, 0, 0, 2))
        rowNum += 2

        // Info general
        createDataRow(sheet, rowNum++, "Tipo de dieta:", methaneResult.tipoDieta.descripcion, styles)
        createDataRow(sheet, rowNum++, "Ecuaciones utilizadas:", "${methaneResult.predictions.size}", styles)

        rowNum++ // línea en blanco

        // Estadísticas
        sheet.createRow(rowNum).apply {
            createCell(0).apply {
                setCellValue("ESTADÍSTICAS DE PRODUCCIÓN")
                cellStyle = styles.headerStyle
            }
        }
        sheet.addMergedRegion(CellRangeAddress(rowNum, rowNum, 0, 2))
        rowNum++

        createDataRow(sheet, rowNum++, "Media:", "${String.format("%.2f", methaneResult.mean)} g/día", styles)
        createDataRow(sheet, rowNum++, "Mediana:", "${String.format("%.2f", methaneResult.median)} g/día", styles)
        createDataRow(sheet, rowNum++, "Mínimo:", "${String.format("%.2f", methaneResult.min)} g/día", styles)
        createDataRow(sheet, rowNum++, "Máximo:", "${String.format("%.2f", methaneResult.max)} g/día", styles)
        createDataRow(sheet, rowNum++, "Desviación estándar:", "${String.format("%.2f", methaneResult.standardDeviation)} g/día", styles)
        createDataRow(sheet, rowNum++, "Rango de incertidumbre:", "${String.format("%.2f", methaneResult.getUncertaintyRange())} g/día", styles)
        createDataRow(sheet, rowNum++, "Coeficiente de variación:", "${String.format("%.1f", methaneResult.getCoefficientOfVariation())}%", styles)

        rowNum++ // línea en blanco

        // Predicciones
        sheet.createRow(rowNum).apply {
            createCell(0).apply {
                setCellValue("PREDICCIONES POR ECUACIÓN")
                cellStyle = styles.headerStyle
            }
        }
        sheet.addMergedRegion(CellRangeAddress(rowNum, rowNum, 0, 2))
        rowNum++

        val tableHeader = sheet.createRow(rowNum++)
        listOf("Ecuación", "Valor", "Unidad").forEachIndexed { index, header ->
            tableHeader.createCell(index).apply {
                setCellValue(header)
                cellStyle = styles.tableHeaderStyle
            }
        }

        methaneResult.predictions
            .sortedByDescending { it.value }
            .forEach { prediction ->
                val row = sheet.createRow(rowNum++)
                row.createCell(0).setCellValue(prediction.equation)
                row.createCell(1).apply {
                    setCellValue(prediction.value)
                    cellStyle = styles.numberStyle
                }
                row.createCell(2).setCellValue(prediction.unit)
            }

        rowNum++ // línea en blanco

        // Interpretación
        sheet.createRow(rowNum).apply {
            createCell(0).apply {
                setCellValue(
                    buildString {
                        appendLine("La producción media de metano es de ${String.format("%.2f", methaneResult.mean)} g/día.")
                        appendLine("El rango de incertidumbre de ${String.format("%.2f", methaneResult.getUncertaintyRange())} g/día indica")
                        appendLine("la variabilidad entre las diferentes ecuaciones empíricas utilizadas.")
                        appendLine()
                        appendLine("Un coeficiente de variación de ${String.format("%.1f", methaneResult.getCoefficientOfVariation())}% " +
                                when {
                                    methaneResult.getCoefficientOfVariation() < 10.0 -> "indica alta consistencia entre las predicciones."
                                    methaneResult.getCoefficientOfVariation() < 20.0 -> "indica consistencia moderada entre las predicciones."
                                    else -> "indica mayor incertidumbre en las predicciones."
                                }
                        )
                    }
                )
                cellStyle = styles.wrapTextStyle
            }
        }
        sheet.addMergedRegion(CellRangeAddress(rowNum, rowNum + 5, 0, 2))

        sheet.setColumnWidth(0, 10000)
        sheet.setColumnWidth(1, 4000)
        sheet.setColumnWidth(2, 3000)
    }

    // ============= ESTILOS =============

    private fun createStyles(workbook: Workbook): ExcelStyles {
        val titleStyle = workbook.createCellStyle().apply {
            val font = workbook.createFont().apply {
                bold = true
                fontHeightInPoints = 16
            }
            setFont(font)
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
        }

        val headerStyle = workbook.createCellStyle().apply {
            val font = workbook.createFont().apply {
                bold = true
                fontHeightInPoints = 12
            }
            setFont(font)
            fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
        }

        val tableHeaderStyle = workbook.createCellStyle().apply {
            val font = workbook.createFont().apply { bold = true }
            setFont(font)
            fillForegroundColor = IndexedColors.LIGHT_BLUE.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            alignment = HorizontalAlignment.CENTER
            borderBottom = BorderStyle.MEDIUM
        }

        val numberStyle = workbook.createCellStyle().apply {
            dataFormat = workbook.createDataFormat().getFormat("0.00")
        }

        val percentStyle = workbook.createCellStyle().apply {
            dataFormat = workbook.createDataFormat().getFormat("0.00%")
        }

        val currencyStyle = workbook.createCellStyle().apply {
            dataFormat = workbook.createDataFormat().getFormat("\$#,##0.00")
        }

        val totalStyle = workbook.createCellStyle().apply {
            val font = workbook.createFont().apply { bold = true }
            setFont(font)
            borderTop = BorderStyle.DOUBLE
        }

        val totalNumberStyle = workbook.createCellStyle().apply {
            val font = workbook.createFont().apply { bold = true }
            setFont(font)
            dataFormat = workbook.createDataFormat().getFormat("0.00")
            borderTop = BorderStyle.DOUBLE
        }

        val totalCurrencyStyle = workbook.createCellStyle().apply {
            val font = workbook.createFont().apply { bold = true }
            setFont(font)
            dataFormat = workbook.createDataFormat().getFormat("\$#,##0.00")
            borderTop = BorderStyle.DOUBLE
        }

        val totalPercentStyle = workbook.createCellStyle().apply {
            val font = workbook.createFont().apply { bold = true }
            setFont(font)
            dataFormat = workbook.createDataFormat().getFormat("0.00%")
            borderTop = BorderStyle.DOUBLE
        }

        val labelBoldStyle = workbook.createCellStyle().apply {
            val font = workbook.createFont().apply { bold = true }
            setFont(font)
        }

        val wrapTextStyle = workbook.createCellStyle().apply {
            wrapText = true
            verticalAlignment = VerticalAlignment.TOP
        }

        return ExcelStyles(
            titleStyle = titleStyle,
            headerStyle = headerStyle,
            tableHeaderStyle = tableHeaderStyle,
            numberStyle = numberStyle,
            percentStyle = percentStyle,
            currencyStyle = currencyStyle,
            totalStyle = totalStyle,
            totalNumberStyle = totalNumberStyle,
            totalCurrencyStyle = totalCurrencyStyle,
            totalPercentStyle = totalPercentStyle,
            labelBoldStyle = labelBoldStyle,
            wrapTextStyle = wrapTextStyle
        )
    }

    // ============= UTILIDADES =============

    private fun createDataRow(
        sheet: Sheet,
        rowNum: Int,
        label: String,
        value: String,
        styles: ExcelStyles
    ) {
        val row = sheet.createRow(rowNum)
        row.createCell(0).apply {
            setCellValue(label)
            cellStyle = styles.labelBoldStyle
        }
        row.createCell(1).setCellValue(value)
    }

    private fun getRequirementUnit(nutrient: String): String {
        return when (nutrient) {
            "CP", "NDF" -> "%"
            "NEm", "NEg" -> "Mcal/kg"
            "Ca", "P" -> "%"
            else -> ""
        }
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun createOutputStream(fileName: String): OutputStream? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val uri = context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                contentValues
            )

            uri?.let { context.contentResolver.openOutputStream(it) }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)
            FileOutputStream(file)
        }
    }

    private fun getFilePath(fileName: String): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "Downloads/$fileName"
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            File(downloadsDir, fileName).absolutePath
        }
    }

    // ============= CLASES DE DATOS =============

    private data class ExcelStyles(
        val titleStyle: CellStyle,
        val headerStyle: CellStyle,
        val tableHeaderStyle: CellStyle,
        val numberStyle: CellStyle,
        val percentStyle: CellStyle,
        val currencyStyle: CellStyle,
        val totalStyle: CellStyle,
        val totalNumberStyle: CellStyle,
        val totalCurrencyStyle: CellStyle,
        val totalPercentStyle: CellStyle,
        val labelBoldStyle: CellStyle,
        val wrapTextStyle: CellStyle
    )

    sealed class ExportResult {
        data class Success(val filePath: String) : ExportResult()
        data class Error(val message: String) : ExportResult()
    }
}
