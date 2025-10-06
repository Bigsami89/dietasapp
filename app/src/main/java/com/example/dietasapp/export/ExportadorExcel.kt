package com.example.dietasapp.export

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.dietasapp.calculation.methane.MethaneCalculator
import com.example.dietasapp.domain.Dieta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

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
     *
     * @param dieta Dieta a exportar
     * @param nombreArchivo Nombre del archivo (sin extensión)
     * @param methaneResult Resultado detallado del cálculo de metano (opcional)
     * @return Resultado de la exportación con ruta del archivo
     */
    suspend fun exportar(
        dieta: Dieta,
        nombreArchivo: String,
        methaneResult: MethaneCalculator.MethaneResult? = null
    ): ExportResult = withContext(Dispatchers.IO) {

        try {
            println("Iniciando exportación a Excel: $nombreArchivo")

            // 1. Crear workbook
            val workbook = XSSFWorkbook()

            // 2. Crear estilos
            val styles = createStyles(workbook)

            // 3. Crear hojas con contenido
            createResumenSheet(workbook, dieta, styles)
            createComposicionSheet(workbook, dieta, styles)
            createNutrientesSheet(workbook, dieta, styles)

            if (methaneResult != null) {
                createMetanoSheet(workbook, dieta, methaneResult, styles)
            }

            // 4. Guardar archivo
            val fileName = "${nombreArchivo}.xlsx"
            val outputStream = createOutputStream(fileName)

            if (outputStream == null) {
                return@withContext ExportResult.Error("No se pudo crear el archivo de salida")
            }

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

    /**
     * Crea la hoja de resumen general
     */
    private fun createResumenSheet(
        workbook: Workbook,
        dieta: Dieta,
        styles: ExcelStyles
    ) {
        val sheet = workbook.createSheet(SHEET_RESUMEN)
        var rowNum = 0

        // Título
        val titleRow = sheet.createRow(rowNum++)
        val titleCell = titleRow.createCell(0)
        titleCell.setCellValue("REPORTE DE DIETA ÓPTIMA")
        titleCell.cellStyle = styles.titleStyle
        sheet.addMergedRegion(org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 3))

        rowNum++ // Línea en blanco

        // Fecha de generación
        val dateRow = sheet.createRow(rowNum++)
        dateRow.createCell(0).setCellValue("Fecha de generación:")
        dateRow.createCell(1).setCellValue(formatDate(System.currentTimeMillis()))

        rowNum++ // Línea en blanco

        // Sección: Información del Animal
        val animalHeaderRow = sheet.createRow(rowNum++)
        val animalHeaderCell = animalHeaderRow.createCell(0)
        animalHeaderCell.setCellValue("INFORMACIÓN DEL ANIMAL")
        animalHeaderCell.cellStyle = styles.headerStyle
        sheet.addMergedRegion(org.apache.poi.ss.util.CellRangeAddress(rowNum - 1, rowNum - 1, 0, 1))

        createDataRow(sheet, rowNum++, "Nombre:", dieta.animal.nombre, styles)
        createDataRow(sheet, rowNum++, "Peso corporal:", "${dieta.animal.pesoKg} kg", styles)
        createDataRow(sheet, rowNum++, "Consumo DMI objetivo:", "${dieta.animal.consumoDMI} kg/día", styles)
        createDataRow(sheet, rowNum++, "Tipo de dieta:", dieta.animal.tipo.descripcion, styles)

        rowNum++ // Línea en blanco

        // Sección: Resultados Económicos
        val econHeaderRow = sheet.createRow(rowNum++)
        val econHeaderCell = econHeaderRow.createCell(0)
        econHeaderCell.setCellValue("RESULTADOS ECONÓMICOS")
        econHeaderCell.cellStyle = styles.headerStyle
        sheet.addMergedRegion(org.apache.poi.ss.util.CellRangeAddress(rowNum - 1, rowNum - 1, 0, 1))

        createDataRow(sheet, rowNum++, "Costo total:", "$${String.format("%.2f", dieta.costoTotal)}/día", styles)
        createDataRow(sheet, rowNum++, "Costo por kg MS:", "$${String.format("%.4f", dieta.costoPorKgMS())}/kg", styles)

        val totalKg = dieta.composicion.values.sum()
        createDataRow(sheet, rowNum++, "Total MS suministrado:", "${String.format("%.2f", totalKg)} kg/día", styles)

        rowNum++ // Línea en blanco

        // Sección: Impacto Ambiental
        val envHeaderRow = sheet.createRow(rowNum++)
        val envHeaderCell = envHeaderRow.createCell(0)
        envHeaderCell.setCellValue("IMPACTO AMBIENTAL")
        envHeaderCell.cellStyle = styles.headerStyle
        sheet.addMergedRegion(org.apache.poi.ss.util.CellRangeAddress(rowNum - 1, rowNum - 1, 0, 1))

        createDataRow(sheet, rowNum++, "Metano producido:", "${String.format("%.2f", dieta.metanoProducidoGramos)} g/día", styles)
        createDataRow(sheet, rowNum++, "Metano por kg DMI:", "${String.format("%.2f", dieta.metanoPorKgDMI())} g/kg", styles)

        // Ajustar anchos de columna
        sheet.setColumnWidth(0, 8000)
        sheet.setColumnWidth(1, 6000)
    }

    /**
     * Crea la hoja de composición de la dieta
     */
    private fun createComposicionSheet(
        workbook: Workbook,
        dieta: Dieta,
        styles: ExcelStyles
    ) {
        val sheet = workbook.createSheet(SHEET_COMPOSICION)
        var rowNum = 0

        // Título
        val titleRow = sheet.createRow(rowNum++)
        val titleCell = titleRow.createCell(0)
        titleCell.setCellValue("COMPOSICIÓN DE LA DIETA")
        titleCell.cellStyle = styles.titleStyle
        sheet.addMergedRegion(org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 4))

        rowNum++ // Línea en blanco

        // Encabezados de tabla
        val headerRow = sheet.createRow(rowNum++)
        val headers = listOf("Ingrediente", "Cantidad (kg/día)", "Porcentaje (%)", "Costo Unitario ($/kg)", "Costo Total ($/día)")
        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = styles.tableHeaderStyle
        }

        // Datos de composición
        val totalKg = dieta.composicion.values.sum()
        var totalCosto = 0.0

        dieta.composicion.entries
            .sortedByDescending { it.value }
            .forEach { (ingrediente, kg) ->
                val row = sheet.createRow(rowNum++)
                val porcentaje = (kg / totalKg) * 100.0

                // Obtener costo unitario (simplificado, idealmente vendría del Insumo)
                val costoUnitario = dieta.costoTotal / totalKg // Aproximación
                val costoIngrediente = kg * costoUnitario
                totalCosto += costoIngrediente

                row.createCell(0).setCellValue(ingrediente)

                val kgCell = row.createCell(1)
                kgCell.setCellValue(kg)
                kgCell.cellStyle = styles.numberStyle

                val pctCell = row.createCell(2)
                pctCell.setCellValue(porcentaje)
                pctCell.cellStyle = styles.percentStyle

                val costoUnitCell = row.createCell(3)
                costoUnitCell.setCellValue(costoUnitario)
                costoUnitCell.cellStyle = styles.currencyStyle

                val costoTotalCell = row.createCell(4)
                costoTotalCell.setCellValue(costoIngrediente)
                costoTotalCell.cellStyle = styles.currencyStyle
            }

        // Fila de totales
        val totalRow = sheet.createRow(rowNum++)
        val totalCell = totalRow.createCell(0)
        totalCell.setCellValue("TOTAL")
        totalCell.cellStyle = styles.totalStyle

        val totalKgCell = totalRow.createCell(1)
        totalKgCell.setCellValue(totalKg)
        totalKgCell.cellStyle = styles.totalNumberStyle

        val pct100Cell = totalRow.createCell(2)
        pct100Cell.setCellValue(100.0)
        pct100Cell.cellStyle = styles.totalNumberStyle

        totalRow.createCell(3) // Vacío

        val totalCostoCell = totalRow.createCell(4)
        totalCostoCell.setCellValue(dieta.costoTotal)
        totalCostoCell.cellStyle = styles.totalCurrencyStyle

        // Ajustar anchos de columna
        for (i in 0..4) {
            sheet.autoSizeColumn(i)
            sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000)
        }
    }

    /**
     * Crea la hoja de nutrientes totales
     */
    private fun createNutrientesSheet(
        workbook: Workbook,
        dieta: Dieta,
        styles: ExcelStyles
    ) {
        val sheet = workbook.createSheet(SHEET_NUTRIENTES)
        var rowNum = 0

        // Título
        val titleRow = sheet.createRow(rowNum++)
        val titleCell = titleRow.createCell(0)
        titleCell.setCellValue("APORTE NUTRICIONAL TOTAL")
        titleCell.cellStyle = styles.titleStyle
        sheet.addMergedRegion(org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 2))

        rowNum++ // Línea en blanco

        // Encabezados de tabla
        val headerRow = sheet.createRow(rowNum++)
        listOf("Nutriente", "Valor Total", "Unidad").forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = styles.tableHeaderStyle
        }

        // Datos de nutrientes
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

                val valorCell = row.createCell(1)
                valorCell.setCellValue(valor)
                valorCell.cellStyle = styles.numberStyle

                row.createCell(2).setCellValue(nutrientUnits[nutriente] ?: "")
            }

        rowNum++ // Línea en blanco

        // Sección de requerimientos
        val reqHeaderRow = sheet.createRow(rowNum++)
        val reqHeaderCell = reqHeaderRow.createCell(0)
        reqHeaderCell.setCellValue("REQUERIMIENTOS DEL ANIMAL")
        reqHeaderCell.cellStyle = styles.headerStyle
        sheet.addMergedRegion(org.apache.poi.ss.util.CellRangeAddress(rowNum - 1, rowNum - 1, 0, 2))

        // Requerimientos mínimos
        val minHeaderRow = sheet.createRow(rowNum++)
        minHeaderRow.createCell(0).setCellValue("Mínimos:")
        dieta.animal.requerimientosMinimos.forEach { (nutriente, valor) ->
            val row = sheet.createRow(rowNum++)
            row.createCell(0).setCellValue("  $nutriente")
            val valorCell = row.createCell(1)
            valorCell.setCellValue(valor)
            valorCell.cellStyle = styles.numberStyle
            row.createCell(2).setCellValue(getRequirementUnit(nutriente))
        }

        rowNum++ // Línea en blanco

        // Requerimientos máximos
        val maxHeaderRow = sheet.createRow(rowNum++)
        maxHeaderRow.createCell(0).setCellValue("Máximos:")
        dieta.animal.requerimientosMaximos.forEach { (nutriente, valor) ->
            val row = sheet.createRow(rowNum++)
            row.createCell(0).setCellValue("  $nutriente")
            val valorCell = row.createCell(1)
            valorCell.setCellValue(valor)
            valorCell.cellStyle = styles.numberStyle
            row.createCell(2).setCellValue(getRequirementUnit(nutriente))
        }

        // Ajustar anchos de columna
        sheet.setColumnWidth(0, 5000)
        sheet.setColumnWidth(1, 4000)
        sheet.setColumnWidth(2, 4000)
    }

    /**
     * Crea la hoja de análisis de metano
     */
    private fun createMetanoSheet(
        workbook: Workbook,
        dieta: Dieta,
        methaneResult: MethaneCalculator.MethaneResult,
        styles: ExcelStyles
    ) {
        val sheet = workbook.createSheet(SHEET_METANO)
        var rowNum = 0

        // Título
        val titleRow = sheet.createRow(rowNum++)
        val titleCell = titleRow.createCell(0)
        titleCell.setCellValue("ANÁLISIS DE METANO ENTÉRICO")
        titleCell.cellStyle = styles.titleStyle
        sheet.addMergedRegion(org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 2))

        rowNum++ // Línea en blanco

        // Información general
        createDataRow(sheet, rowNum++, "Tipo de dieta:", methaneResult.tipoDieta.descripcion, styles)
        createDataRow(sheet, rowNum++, "Ecuaciones utilizadas:", "${methaneResult.predictions.size}", styles)

        rowNum++ // Línea en blanco

        // Estadísticas
        val statsHeaderRow = sheet.createRow(rowNum++)
        val statsHeaderCell = statsHeaderRow.createCell(0)
        statsHeaderCell.setCellValue("ESTADÍSTICAS DE PRODUCCIÓN")
        statsHeaderCell.cellStyle = styles.headerStyle
        sheet.addMergedRegion(org.apache.poi.ss.util.CellRangeAddress(rowNum - 1, rowNum - 1, 0, 2))

        createDataRow(sheet, rowNum++, "Media:", "${String.format("%.2f", methaneResult.mean)} g/día", styles)
        createDataRow(sheet, rowNum++, "Mediana:", "${String.format("%.2f", methaneResult.median)} g/día", styles)
        createDataRow(sheet, rowNum++, "Mínimo:", "${String.format("%.2f", methaneResult.min)} g/día", styles)
        createDataRow(sheet, rowNum++, "Máximo:", "${String.format("%.2f", methaneResult.max)} g/día", styles)
        createDataRow(sheet, rowNum++, "Desviación estándar:", "${String.format("%.2f", methaneResult.standardDeviation)} g/día", styles)
        createDataRow(sheet, rowNum++, "Rango de incertidumbre:", "${String.format("%.2f", methaneResult.getUncertaintyRange())} g/día", styles)
        createDataRow(sheet, rowNum++, "Coeficiente de variación:", "${String.format("%.1f", methaneResult.getCoefficientOfVariation())}%", styles)

        rowNum++ // Línea en blanco

        // Predicciones individuales
        val predHeaderRow = sheet.createRow(rowNum++)
        val predHeaderCell = predHeaderRow.createCell(0)
        predHeaderCell.setCellValue("PREDICCIONES POR ECUACIÓN")
        predHeaderCell.cellStyle = styles.headerStyle
        sheet.addMergedRegion(org.apache.poi.ss.util.CellRangeAddress(rowNum - 1, rowNum - 1, 0, 2))

        // Encabezados de tabla
        val tableHeaderRow = sheet.createRow(rowNum++)
        listOf("Ecuación", "Valor", "Unidad").forEachIndexed { index, header ->
            val cell = tableHeaderRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = styles.tableHeaderStyle
        }

        // Datos de predicciones
        methaneResult.predictions
            .sortedByDescending { it.value }
            .forEach { prediction ->
                val row = sheet.createRow(rowNum++)

                row.createCell(0).setCellValue(prediction.equation)

                val valueCell = row.createCell(1)
                valueCell.setCellValue(prediction.value)
                valueCell.cellStyle = styles.numberStyle

                row.createCell(2).setCellValue(prediction.unit)
            }

        rowNum++ // Línea en blanco

        // Interpretación
        val interpHeaderRow = sheet.createRow(rowNum++)
        val interpHeaderCell = interpHeaderRow.createCell(0)
        interpHeaderCell.setCellValue("INTERPRETACIÓN")
        interpHeaderCell.cellStyle = styles.headerStyle
        sheet.addMergedRegion(org.apache.poi.ss.util.CellRangeAddress(rowNum - 1, rowNum - 1, 0, 2))

        val interpretation = buildString {
            appendLine("La producción media de metano es de ${String.format("%.2f", methaneResult.mean)} g/día.")
            appendLine("El rango de incertidumbre de ${String.format("%.2f", methaneResult.getUncertaintyRange())} g/día indica ")
            appendLine("la variabilidad entre las diferentes ecuaciones empíricas utilizadas.")
            appendLine("\nUn coeficiente de variación de ${String.format("%.1f", methaneResult.getCoefficientOfVariation())}% ")

            when {
                methaneResult.getCoefficientOfVariation() < 10.0 -> appendLine("indica alta consistencia entre las predicciones.")
                methaneResult.getCoefficientOfVariation() < 20.0 -> appendLine("indica consistencia moderada entre las predicciones.")
                else -> appendLine("indica mayor incertidumbre en las predicciones.")
            }
        }

        val interpretRow = sheet.createRow(rowNum++)
        val interpretCell = interpretRow.createCell(0)
        interpretCell.setCellValue(interpretation)
        sheet.addMergedRegion(org.apache.poi.ss.util.CellRangeAddress(rowNum - 1, rowNum + 5, 0, 2))

        // Ajustar anchos de columna
        sheet.setColumnWidth(0, 10000)
        sheet.setColumnWidth(1, 4000)
        sheet.setColumnWidth(2, 3000)
    }

    // ============= ESTILOS =============

    /**
     * Crea los estilos del Excel
     */
    private fun createStyles(workbook: Workbook): ExcelStyles {
        // Estilo de título
        val titleStyle = workbook.createCellStyle().apply {
            val font = workbook.createFont()
            font.bold = true
            font.fontHeightInPoints = 16
            setFont(font)
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
        }

        // Estilo de encabezado de sección
        val headerStyle = workbook.createCellStyle().apply {
            val font = workbook.createFont()
            font.bold = true
            font.fontHeightInPoints = 12
            setFont(font)
            fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
        }

        // Estilo de encabezado de tabla
        val tableHeaderStyle = workbook.createCellStyle().apply {
            val font = workbook.createFont()
            font.bold = true
            setFont(font)
            fillForegroundColor = IndexedColors.LIGHT_BLUE.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            alignment = HorizontalAlignment.CENTER
            borderBottom = BorderStyle.MEDIUM
        }

        // Estilo de número
        val numberStyle = workbook.createCellStyle().apply {
            dataFormat = workbook.createDataFormat().getFormat("0.00")
        }

        // Estilo de porcentaje
        val percentStyle = workbook.createCellStyle().apply {
            dataFormat = workbook.createDataFormat().getFormat("0.00%")
        }

        // Estilo de moneda
        val currencyStyle = workbook.createCellStyle().apply {
            dataFormat = workbook.createDataFormat().getFormat("$#,##0.00")
        }

        // Estilo de total
        val totalStyle = workbook.createCellStyle().apply {
            val font = workbook.createFont()
            font.bold = true
            setFont(font)
            borderTop = BorderStyle.DOUBLE
        }

        val totalNumberStyle = workbook.createCellStyle().apply {
            val font = workbook.createFont()
            font.bold = true
            setFont(font)
            dataFormat = workbook.createDataFormat().getFormat("0.00")
            borderTop = BorderStyle.DOUBLE
        }

        val totalCurrencyStyle = workbook.createCellStyle().apply {
            val font = workbook.createFont()
            font.bold = true
            setFont(font)
            dataFormat = workbook.createDataFormat().getFormat("$#,##0.00")
            borderTop = BorderStyle.DOUBLE
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
            totalCurrencyStyle = totalCurrencyStyle
        )
    }

    // ============= UTILIDADES =============

    /**
     * Crea una fila de datos con etiqueta y valor
     */
    private fun createDataRow(
        sheet: Sheet,
        rowNum: Int,
        label: String,
        value: String,
        styles: ExcelStyles
    ) {
        val row = sheet.createRow(rowNum)
        val labelCell = row.createCell(0)
        labelCell.setCellValue(label)

        val font = sheet.workbook.createFont()
        font.bold = true
        val labelStyle = sheet.workbook.createCellStyle()
        labelStyle.setFont(font)
        labelCell.cellStyle = labelStyle

        row.createCell(1).setCellValue(value)
    }

    /**
     * Obtiene la unidad de un requerimiento
     */
    private fun getRequirementUnit(nutrient: String): String {
        return when (nutrient) {
            "CP", "NDF" -> "%"
            "NEm", "NEg" -> "Mcal/kg"
            "Ca", "P" -> "%"
            else -> ""
        }
    }

    /**
     * Formatea una fecha
     */
    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    /**
     * Crea un OutputStream para guardar el archivo
     */
    private fun createOutputStream(fileName: String): OutputStream? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ - Usar MediaStore
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
            // Android 9 y anteriores - Usar almacenamiento externo
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)
            FileOutputStream(file)
        }
    }

    /**
     * Obtiene la ruta del archivo guardado
     */
    private fun getFilePath(fileName: String): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "Downloads/$fileName"
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            File(downloadsDir, fileName).absolutePath
        }
    }

    // ============= CLASES DE DATOS =============

    /**
     * Contenedor de estilos de Excel
     */
    private data class ExcelStyles(
        val titleStyle: CellStyle,
        val headerStyle: CellStyle,
        val tableHeaderStyle: CellStyle,
        val numberStyle: CellStyle,
        val percentStyle: CellStyle,
        val currencyStyle: CellStyle,
        val totalStyle: CellStyle,
        val totalNumberStyle: CellStyle,
        val totalCurrencyStyle: CellStyle
    )

    /**
     * Resultado de la exportación
     */
    sealed class ExportResult {
        data class Success(val filePath: String) : ExportResult()
        data class Error(val message: String) : ExportResult()
    }
}