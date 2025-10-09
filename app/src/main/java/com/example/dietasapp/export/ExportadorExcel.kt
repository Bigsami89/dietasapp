package com.example.dietasapp.export

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.example.dietasapp.domain.Dieta
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
import kotlin.math.abs

/**
 * Exportador de dietas a formato Excel (.xlsx)
 * SOLO usa la Dieta proporcionada (y lo que está dentro de dieta.animal).
 * Base primaria de reporte: POR KG DE DMI.
 */
class ExportadorExcel(private val context: Context) {

    companion object {
        private const val TAG = "ExportadorExcel"

        private const val SHEET_RESUMEN = "Resumen"
        private const val SHEET_COMPOSICION = "Composición"
        private const val SHEET_NUTRIENTES = "Nutrientes"
        private const val SHEET_METANO = "Análisis de Metano"
        private const val SHEET_LOGS = "Registro"

        // Si true, cuando un nutriente en % luce 10× por debajo del mínimo,
        // se aplica una corrección visual ×10 (no altera la Dieta).
        private const val AUTO_FIX_PERCENTAGE_X10 = true

        // Advertir si la suma de la mezcla difiere del DMI más de 2%
        private const val WARN_DIFF_THRESHOLD = 0.02
    }

    /** Nutrientes almacenados en g/día en dieta.nutrientesTotales (por convención del proyecto). */
    private val nutrientsInGrams: Set<String> =
        setOf("CP", "NDF", "Ca", "P", "Starch", "Fat", "TDN")

    private fun isStoredInGrams(nutr: String) = nutr in nutrientsInGrams

    /** Exporta usando SOLO campos de Dieta. Emite logs a Logcat y hoja "Registro". */
    suspend fun exportar(
        dieta: Dieta,
        nombreArchivo: String
    ): ExportResult = withContext(Dispatchers.IO) {
        val logs = mutableListOf<String>()
        try {
            logs += "== INICIO EXPORTACIÓN =="
            logs += "Archivo: $nombreArchivo.xlsx"
            logs += "Fecha: ${formatDate(System.currentTimeMillis())}"

            val workbook = XSSFWorkbook()
            val styles = createStyles(workbook)

            createResumenSheet(workbook, dieta, styles, logs)
            createComposicionSheet(workbook, dieta, styles, logs)
            createNutrientesSheet(workbook, dieta, styles, logs)
            createMetanoSheet(workbook, dieta, styles, logs)
            createLogsSheet(workbook, styles, logs)

            val fileName = "$nombreArchivo.xlsx"
            val outputStream = createOutputStream(fileName)
                ?: return@withContext ExportResult.Error("No se pudo crear el archivo de salida")

            workbook.write(outputStream)
            outputStream.close()
            workbook.close()

            val path = getFilePath(fileName)
            logs += "✓ Archivo Excel exportado: $path"
            logs += "== FIN EXPORTACIÓN =="

            dumpLogs(logs)
            ExportResult.Success(path)
        } catch (e: Exception) {
            logs += "Error al exportar: ${e.message}"
            dumpLogs(logs)
            e.printStackTrace()
            ExportResult.Error(e.message ?: "Error desconocido")
        }
    }

    // ============= HELPERS SOLO-DIETA =============

    private fun totalKgMezcla(dieta: Dieta): Double =
        dieta.composicion.values.sum().coerceAtLeast(0.0)

    /**
     * Valor por kg de DMI según la unidad del requerimiento del nutriente.
     * - Para %: (g/día -> kg/día) / DMI => fracción (→ % en display)
     * - Para Mcal/kg: (Mcal/día) / DMI => Mcal/kg
     * - Fallback:
     *    * si viene en g/día -> g/kg DMI (conversión a kg si quieres % no aplica aquí)
     *    * si ya está en unidad base/día -> base/kg DMI
     */
    private fun perKgDMI(nutriente: String, totalCrudo: Double, dmi: Double): Double {
        if (dmi <= 0.0) return 0.0
        return when (getRequirementUnit(nutriente)) {
            "%" -> {
                // totalCrudo (g/día) -> kg/día -> / DMI = fracción
                val kgDia = if (isStoredInGrams(nutriente)) totalCrudo / 1000.0 else totalCrudo
                kgDia / dmi
            }
            "Mcal/kg" -> {
                // totalCrudo (Mcal/día) / DMI
                totalCrudo / dmi
            }
            else -> {
                // Fallback: si está en g/día => g/kg DMI; si no, base/día / DMI
                if (isStoredInGrams(nutriente)) (totalCrudo / dmi) / 1000.0 else totalCrudo / dmi
            }
        }
    }

    /**
     * Si el nutriente es % y luce 10× por debajo del mínimo, corrige visualmente x10.
     * Devuelve (valorPosibleCorregido, seCorrigió?).
     */
    private fun maybeFixPercentScale(
        nutrient: String,
        achievedFrac: Double, // fracción (0..1)
        minReqPct: Double?,   // % (por ej. 13.0)
        logs: MutableList<String>
    ): Pair<Double, Boolean> {
        if (!AUTO_FIX_PERCENTAGE_X10 || minReqPct == null || minReqPct <= 0.0) return achievedFrac to false
        val alcPct = achievedFrac * 100.0
        val tooLow = alcPct < (0.5 * minReqPct)
        val nearIfScaled = (alcPct * 10.0) in (0.6 * minReqPct)..(1.4 * minReqPct)
        return if (tooLow && nearIfScaled) {
            logs += "CORRECCIÓN ×10: $nutrient (alcanzado=${"%.2f".format(alcPct)}%, min=${"%.2f".format(minReqPct)}%)"
            (achievedFrac * 10.0) to true
        } else achievedFrac to false
    }

    // ============= HOJAS =============

    private fun createResumenSheet(
        workbook: Workbook,
        dieta: Dieta,
        styles: ExcelStyles,
        logs: MutableList<String>
    ) {
        val sheet = workbook.createSheet(SHEET_RESUMEN)
        var rowNum = 0

        sheet.createRow(rowNum).also { row ->
            row.createCell(0).apply {
                setCellValue("REPORTE DE DIETA (POR KG DMI)")
                cellStyle = styles.titleStyle
            }
        }
        sheet.addMergedRegion(CellRangeAddress(0, 0, 0, 3))
        rowNum += 2

        sheet.createRow(rowNum++).apply {
            createCell(0).setCellValue("Fecha de generación:")
            createCell(1).setCellValue(formatDate(System.currentTimeMillis()))
        }

        rowNum++

        // Info del animal (dentro de Dieta)
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

        rowNum++

        // Económicos por kg mezcla (referencial)
        sheet.createRow(rowNum).apply {
            createCell(0).apply {
                setCellValue("RESULTADOS ECONÓMICOS (POR KG)")
                cellStyle = styles.headerStyle
            }
        }
        sheet.addMergedRegion(CellRangeAddress(rowNum, rowNum, 0, 1))
        rowNum++

        val totalKg = totalKgMezcla(dieta)
        val dmi = dieta.animal.consumoDMI.coerceAtLeast(0.0)
        val costoPorKg = dieta.costoPorKgMS().coerceAtLeast(0.0)

        sheet.createRow(rowNum++).apply {
            createCell(0).setCellValue("Costo por kg mezcla (MS):")
            createCell(1).apply {
                setCellValue(costoPorKg)
                cellStyle = styles.currencyStyle
            }
        }

        // Diferencia mezcla vs DMI
        val diff = if (dmi > 0) abs(totalKg - dmi) / dmi else 0.0
        if (diff > WARN_DIFF_THRESHOLD) {
            sheet.createRow(rowNum++).apply {
                createCell(0).setCellValue("AVISO: La suma de mezcla difiere del DMI")
                createCell(1).setCellValue(String.format(Locale.US, "%.2f %%", diff * 100))
            }
            logs += "AVISO: totalMezcla=${"%.3f".format(totalKg)} kg/d, DMI=${"%.3f".format(dmi)} kg/d (diff=${"%.2f".format(diff * 100)}%)"
        } else {
            rowNum++
        }

        // Metano
        sheet.createRow(rowNum).apply {
            createCell(0).apply {
                setCellValue("IMPACTO AMBIENTAL")
                cellStyle = styles.headerStyle
            }
        }
        sheet.addMergedRegion(CellRangeAddress(rowNum, rowNum, 0, 1))
        rowNum++

        val ch4_g_dia = dieta.metanoProducidoGramos.coerceAtLeast(0.0)
        val ch4_g_kgDMI = if (dmi > 0) ch4_g_dia / dmi else 0.0
        val ch4_g_kgMezcla = if (totalKg > 0) ch4_g_dia / totalKg else 0.0

        createDataRow(sheet, rowNum++, "CH₄ total:", "${String.format("%.2f", ch4_g_dia)} g/día", styles)
        createDataRow(sheet, rowNum++, "CH₄ por kg DMI:", "${String.format("%.2f", ch4_g_kgDMI)} g/kg", styles)
        createDataRow(sheet, rowNum++, "CH₄ por kg mezcla:", "${String.format("%.2f", ch4_g_kgMezcla)} g/kg", styles)

        sheet.setColumnWidth(0, 9000)
        sheet.setColumnWidth(1, 7000)

        // Logs
        logs += "-- RESUMEN --"
        logs += "Animal: ${dieta.animal.nombre}, Peso=${dieta.animal.pesoKg} kg, DMI=${dmi} kg/día, Tipo=${dieta.animal.tipo.descripcion}"
        logs += "Total mezcla: $totalKg kg/día"
        logs += "Costo mezcla: $${String.format("%.4f", costoPorKg)} /kg"
        logs += "CH4: total=${String.format("%.2f", ch4_g_dia)} g/día, g/kgDMI=${String.format("%.2f", ch4_g_kgDMI)}, g/kgMezcla=${String.format("%.2f", ch4_g_kgMezcla)}"
    }

    private fun createComposicionSheet(
        workbook: Workbook,
        dieta: Dieta,
        styles: ExcelStyles,
        logs: MutableList<String>
    ) {
        val sheet = workbook.createSheet(SHEET_COMPOSICION)
        var rowNum = 0

        sheet.createRow(rowNum).apply {
            createCell(0).apply {
                setCellValue("COMPOSICIÓN DE LA DIETA (BASE POR KG MEZCLA)")
                cellStyle = styles.titleStyle
            }
        }
        sheet.addMergedRegion(CellRangeAddress(0, 0, 0, 3))
        rowNum += 2

        val headerRow = sheet.createRow(rowNum++)
        val headers = listOf(
            "Ingrediente",
            "Inclusión (kg/kg)",
            "Participación (%)",
            "Aporte prorrateado al costo ($/kg)"
        )
        headers.forEachIndexed { idx, h ->
            headerRow.createCell(idx).apply {
                setCellValue(h)
                cellStyle = styles.tableHeaderStyle
            }
        }

        val totalKg = totalKgMezcla(dieta)
        val costoPorKg = dieta.costoPorKgMS().coerceAtLeast(0.0)
        logs += "-- COMPOSICIÓN --"
        logs += "Total mezcla: $totalKg kg/día, Costo base: $${String.format("%.4f", costoPorKg)}/kg"

        dieta.composicion.entries
            .sortedByDescending { it.value }
            .forEach { (ingrediente, kgDia) ->
                val row = sheet.createRow(rowNum++)
                val inclusionKgKg = if (totalKg > 0.0) kgDia / totalKg else 0.0
                val aporteProrrateado = inclusionKgKg * costoPorKg

                row.createCell(0).setCellValue(ingrediente)

                row.createCell(1).apply {
                    setCellValue(inclusionKgKg)
                    cellStyle = styles.numberStyle
                }

                row.createCell(2).apply {
                    setCellValue(inclusionKgKg) // fracción -> se formatea como %
                    cellStyle = styles.percentStyle
                }

                row.createCell(3).apply {
                    setCellValue(aporteProrrateado)
                    cellStyle = styles.currencyStyle
                }

                logs += "COMPOS: $ingrediente, kgDia=${String.format("%.4f", kgDia)}, kg/kg=${String.format("%.6f", inclusionKgKg)}, %=${String.format("%.2f", inclusionKgKg * 100)}, costoProrr=$${String.format("%.4f", aporteProrrateado)}"
            }

        val totalRow = sheet.createRow(rowNum++)
        totalRow.createCell(0).apply {
            setCellValue("TOTAL")
            cellStyle = styles.totalStyle
        }

        totalRow.createCell(1).apply {
            setCellValue(if (totalKg > 0.0) 1.0 else 0.0)
            cellStyle = styles.totalNumberStyle
        }

        totalRow.createCell(2).apply {
            setCellValue(if (totalKg > 0.0) 1.0 else 0.0) // 100%
            cellStyle = styles.totalPercentStyle
        }

        totalRow.createCell(3).apply {
            setCellValue(costoPorKg) // costo de la mezcla por kg
            cellStyle = styles.totalCurrencyStyle
        }

        sheet.setColumnWidth(0, 25 * 256)
        sheet.setColumnWidth(1, 18 * 256)
        sheet.setColumnWidth(2, 16 * 256)
        sheet.setColumnWidth(3, 24 * 256)
    }

    private fun createNutrientesSheet(
        workbook: Workbook,
        dieta: Dieta,
        styles: ExcelStyles,
        logs: MutableList<String>
    ) {
        val sheet = workbook.createSheet(SHEET_NUTRIENTES)
        var rowNum = 0

        sheet.createRow(rowNum).apply {
            createCell(0).apply {
                setCellValue("NUTRIENTES (POR KG — UNIDADES NATIVAS)")
                cellStyle = styles.titleStyle
            }
        }
        sheet.addMergedRegion(CellRangeAddress(0, 0, 0, 2))
        rowNum += 2

        val headerRow = sheet.createRow(rowNum++)
        listOf("Nutriente", "Valor", "Unidad").forEachIndexed { index, header ->
            headerRow.createCell(index).apply {
                setCellValue(header)
                cellStyle = styles.tableHeaderStyle
            }
        }

        logs += "-- NUTRIENTES (POR KG, SIN CONVERSIONES) --"

        dieta.nutrientesTotales.entries
            .sortedBy { it.key }
            .forEach { (nutriente, valor) ->
                val unidad = displayUnit(nutriente)
                val row = sheet.createRow(rowNum++)
                row.createCell(0).setCellValue(nutriente)
                row.createCell(1).apply { setCellValue(valor); cellStyle = styles.numberStyle }
                row.createCell(2).setCellValue(unidad)
                logs += "RAW: $nutriente = ${String.format("%.4f", valor)} $unidad (por kg)"
            }

        sheet.setColumnWidth(0, 18 * 256)
        sheet.setColumnWidth(1, 16 * 256)
        sheet.setColumnWidth(2, 16 * 256)
    }

    private fun displayUnit(nutrient: String): String = when (nutrient) {
        "CP","NDF","Ca","P","TDN","Starch","Fat" -> "%"
        "NEm","NEg","GE" -> "Mcal/kg"
        else -> "g/kg"
    }


    /** Unidad del valor CRUDO almacenado en dieta.nutrientesTotales por convención del proyecto. */
    private fun getRawUnit(nutrient: String): String {
        return when {
            nutrient in setOf("GE", "NEm", "NEg") -> "Mcal/día"
            nutrient in nutrientsInGrams -> "g/día" // CP, NDF, Ca, P, Starch, Fat, TDN...
            else -> "u/día" // unidad genérica si no hay convención
        }
    }



    private fun createMetanoSheet(
        workbook: Workbook,
        dieta: Dieta,
        styles: ExcelStyles,
        logs: MutableList<String>
    ) {
        val sheet = workbook.createSheet(SHEET_METANO)
        var rowNum = 0

        sheet.createRow(rowNum).apply {
            createCell(0).apply {
                setCellValue("ANÁLISIS DE METANO (g/kg DMI)")
                cellStyle = styles.titleStyle
            }
        }
        sheet.addMergedRegion(CellRangeAddress(0, 0, 0, 2))
        rowNum += 2

        val dmi = dieta.animal.consumoDMI.coerceAtLeast(0.0)
        val totalKg = totalKgMezcla(dieta)
        val ch4 = dieta.metanoProducidoGramos.coerceAtLeast(0.0)

        createDataRow(sheet, rowNum++, "Tipo de dieta:", dieta.animal.tipo.descripcion, styles)
        createDataRow(sheet, rowNum++, "DMI (kg/día):", String.format("%.2f", dmi), styles)

        rowNum++

        sheet.createRow(rowNum).apply {
            createCell(0).apply {
                setCellValue("MÉTRICAS")
                cellStyle = styles.headerStyle
            }
        }
        sheet.addMergedRegion(CellRangeAddress(rowNum, rowNum, 0, 2))
        rowNum++

        fun safeDiv(a: Double, b: Double) = if (b > 0) a / b else 0.0

        val vTotal = ch4
        val vKgDMI = safeDiv(ch4, dmi)
        val vKgMix = safeDiv(ch4, totalKg)

        createDataRow(sheet, rowNum++, "CH₄ total (g/día):", String.format("%.2f", vTotal), styles)
        createDataRow(sheet, rowNum++, "CH₄ (g/kg DMI):", String.format("%.2f", vKgDMI), styles)
        createDataRow(sheet, rowNum++, "CH₄ (g/kg mezcla):", String.format("%.2f", vKgMix), styles)

        sheet.setColumnWidth(0, 10000)
        sheet.setColumnWidth(1, 4000)
        sheet.setColumnWidth(2, 3000)

        logs += "-- METANO --"
        logs += "CH4 total=${String.format("%.2f", vTotal)} g/día, g/kgDMI=${String.format("%.2f", vKgDMI)}, g/kgMezcla=${String.format("%.2f", vKgMix)}"
    }

    private fun createLogsSheet(
        workbook: Workbook,
        styles: ExcelStyles,
        logs: List<String>
    ) {
        val sheet = workbook.createSheet(SHEET_LOGS)
        var rowNum = 0

        sheet.createRow(rowNum).apply {
            createCell(0).apply {
                setCellValue("LOG DE EXPORTACIÓN")
                cellStyle = styles.titleStyle
            }
        }
        sheet.addMergedRegion(CellRangeAddress(0, 0, 0, 2))
        rowNum += 2

        val header = sheet.createRow(rowNum++)
        header.createCell(0).apply {
            setCellValue("#")
            cellStyle = styles.tableHeaderStyle
        }
        header.createCell(1).apply {
            setCellValue("Mensaje")
            cellStyle = styles.tableHeaderStyle
        }

        logs.forEachIndexed { idx, msg ->
            val row = sheet.createRow(rowNum++)
            row.createCell(0).setCellValue((idx + 1).toDouble())
            row.createCell(1).apply {
                setCellValue(msg)
                cellStyle = styles.wrapTextStyle
            }
        }

        sheet.setColumnWidth(0, 8 * 256)
        sheet.setColumnWidth(1, 120 * 256)
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

    /** Unidades de comparación esperadas por nutriente. */
    private fun getRequirementUnit(nutrient: String): String {
        return when (nutrient) {
            "CP", "NDF", "Ca", "P", "TDN", "Starch", "Fat" -> "%"
            "NEm", "NEg", "GE" -> "Mcal/kg"
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

    private fun dumpLogs(logs: List<String>) {
        for (line in logs) Log.d(TAG, line)
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
