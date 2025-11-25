// DietAPI.kt
package com.example.dietasapp.calculation.optimizer

import android.util.Log
import com.example.dietasapp.calculation.components.CostComponent
import com.example.dietasapp.calculation.components.IDietComponent
import com.example.dietasapp.calculation.components.InclusionConstraintsComponent
import com.example.dietasapp.calculation.components.MethaneComponent
import com.example.dietasapp.calculation.components.NutritionalComponent
import com.example.dietasapp.calculation.methane.MethaneCalculator
import com.example.dietasapp.data.Animal
import com.example.dietasapp.data.Insumo
import com.example.dietasapp.data.TipoDieta
import com.example.dietasapp.domain.Dieta
import com.example.dietasapp.domain.DietResult
import com.example.dietasapp.domain.Ingredient
import kotlin.math.max

/**
 * API principal para el cálculo de dietas óptimas
 * Fachada que coordina el optimizador y el cálculo de metano
 *
 * Proporciona una interfaz simplificada para:
 * - Optimizar dietas por costo mínimo
 * - Optimizar dietas por bajo metano
 * - Usar componentes personalizados
 * - Diagnosticar problemas de factibilidad
 */
class DietAPI {

    companion object {
        private const val TAG = "DietAPI"
    }

    private fun logI(msg: String) { Log.i(TAG, msg); println(msg) }
    private fun logW(msg: String) { Log.w(TAG, msg); println("W: $msg") }
    private fun logE(msg: String) { Log.e(TAG, msg); println("E: $msg") }

    private val methaneCalculator = MethaneCalculator()

    /**
     * Calcula la dieta óptima para un animal con los ingredientes disponibles
     *
     * @param animal Animal con sus requerimientos nutricionales
     * @param ingredientes Lista de insumos disponibles
     * @param allInsumos Lista completa de insumos (para recomendaciones)
     * @param optimizationMode Modo de optimización (COSTO o METANO)
     * @return Dieta óptima o dieta de error si no es factible
     */
    fun calculateOptimalDiet(
        animal: Animal,
        ingredientes: List<Insumo>,
        allInsumos: List<Insumo> = emptyList(),
        optimizationMode: OptimizationMode = OptimizationMode.COST
    ): Dieta {

        logI("\n╔════════════════════════════════════════╗")
        logI("║   CÁLCULO DE DIETA ÓPTIMA              ║")
        logI("╚════════════════════════════════════════╝")
        logI("Animal: ${animal.nombre}")
        logI("Modo: ${optimizationMode.name}")

        // 1. Convertir insumos a ingredientes de dominio
        val ingredients = ingredientes.map { Ingredient.fromInsumo(it) }

        // 2. Crear componentes según el modo de optimización
        val components = createComponents(optimizationMode)

        // 3. Crear y ejecutar optimizador
        val optimizer = DietOptimizer(components)
        val dietResult = optimizer.optimize(animal, ingredients)

        // 4. Verificar si la optimización fue exitosa
        if (!dietResult.isFeasible()) {
            val diag = diagnoseInfeasibility(animal, ingredientes, allInsumos)
            logE("❌ Optimización no factible. Diagnóstico:")
            diag.lines().forEach { logE("   $it") }
            return createErrorDiet(animal, dietResult, diag)
        }

        // 5. Determinar tipo de dieta real basado en composición
        // (puede diferir del tipo predefinido en el animal)
        val tipoDietaReal = determineTipoDieta(dietResult.composition, ingredientes)

        // 6. Calcular metano preciso con MethaneCalculator
        val methaneResult = calculatePreciseMethane(
            dietResult = dietResult,
            animal = animal,
            tipoDieta = tipoDietaReal
        )

        // 7. Calcular nutrientes totales de la dieta
        val nutrientesTotales = calculateTotalNutrients(dietResult.composition)

        // 8. Crear objeto Dieta final
        val composicionMap = dietResult.composition.mapKeys { it.key.name }

        val dieta = Dieta(
            animal = animal,
            composicion = composicionMap,
            costoTotal = dietResult.totalCost,
            metanoProducidoGramos = methaneResult.mean,
            nutrientesTotales = nutrientesTotales,
            observaciones = buildObservations(methaneResult, dietResult, tipoDietaReal)
        )

        // 9. Mostrar resumen final
        printFinalSummary(dieta, methaneResult)

        return dieta
    }

    /**
     * Calcula dieta óptima con componentes personalizados
     *
     * @param animal Animal con sus requerimientos
     * @param ingredientes Lista de insumos disponibles
     * @param components Lista de componentes a aplicar
     * @return Dieta óptima o dieta de error
     */
    fun calculateOptimalDietWithComponents(
        animal: Animal,
        ingredientes: List<Insumo>,
        components: List<IDietComponent>
    ): Dieta {
        logI("\n╔════════════════════════════════════════╗")
        logI("║   DIETA CON COMPONENTES PERSONALIZADOS ║")
        logI("╚════════════════════════════════════════╝")
        logI("Animal: ${animal.nombre}")
        logI("Componentes: ${components.size}")

        val ingredients = ingredientes.map { Ingredient.fromInsumo(it) }

        val optimizer = DietOptimizer(components)
        val dietResult = optimizer.optimize(animal, ingredients)

        if (!dietResult.isFeasible()) {
            val diag = diagnoseInfeasibility(animal, ingredientes, emptyList())
            logE("❌ Optimización no factible. Diagnóstico:")
            diag.lines().forEach { logE("   $it") }
            return createErrorDiet(animal, dietResult, diag)
        }

        val tipoDietaReal = determineTipoDieta(dietResult.composition, ingredientes)
        val methaneResult = calculatePreciseMethane(dietResult, animal, tipoDietaReal)
        val nutrientesTotales = calculateTotalNutrients(dietResult.composition)
        val composicionMap = dietResult.composition.mapKeys { it.key.name }

        val dieta = Dieta(
            animal = animal,
            composicion = composicionMap,
            costoTotal = dietResult.totalCost,
            metanoProducidoGramos = methaneResult.mean,
            nutrientesTotales = nutrientesTotales,
            observaciones = buildObservations(methaneResult, dietResult, tipoDietaReal)
        )

        printFinalSummary(dieta, methaneResult)

        return dieta
    }

    /**
     * Crea los componentes según el modo de optimización
     */
    private fun createComponents(mode: OptimizationMode): List<IDietComponent> {
        return when (mode) {
            OptimizationMode.COST -> listOf(
                NutritionalComponent(),
                InclusionConstraintsComponent(),
                CostComponent()
            )
            OptimizationMode.METHANE -> listOf(
                NutritionalComponent(),
                InclusionConstraintsComponent(),
                MethaneComponent()
            )
        }
    }

    /**
     * Determina el tipo de dieta real basado en la composición final
     * Esto puede diferir del tipo predefinido en el animal
     */
    private fun determineTipoDieta(
        composition: Map<Ingredient, Double>,
        insumos: List<Insumo>
    ): TipoDieta {
        var totalForraje = 0.0
        var totalDM = 0.0

        composition.forEach { (ingredient, kg) ->
            totalDM += kg
            val insumo = insumos.find { it.nombre == ingredient.name }
            if (insumo?.esForraje == true) {
                totalForraje += kg
            }
        }

        val foragePercentage = if (totalDM > 0) (totalForraje / totalDM) * 100.0 else 0.0

        logI("Composición real de forraje: ${"%.1f".format(foragePercentage)}%")

        return TipoDieta.fromForagePercentage(foragePercentage)
    }

    /**
     * Calcula metano preciso usando MethaneCalculator
     *
     * @param dietResult Resultado de la optimización
     * @param animal Animal para el cual se calculó la dieta
     * @param tipoDieta Tipo de dieta determinado por la composición real
     * @return Resultado del cálculo de metano con estadísticas
     */
    private fun calculatePreciseMethane(
        dietResult: DietResult,
        animal: Animal,
        tipoDieta: TipoDieta
    ): MethaneCalculator.MethaneResult {

        logI("\n========================================")
        logI("Calculando producción de metano...")
        logI("========================================")

        // Usar el tipo de dieta real (calculado) en lugar del predefinido
        val methaneResult = methaneCalculator.calculate(
            dietComposition = dietResult.composition,
            bodyWeightKg = animal.pesoKg,
            dmiKgDay = animal.consumoDMI,
            tipoDieta = tipoDieta
        )

        logI(methaneResult.getSummary())

        return methaneResult
    }

    /**
     * Calcula los nutrientes totales de la dieta como promedio ponderado
     */
    private fun calculateTotalNutrients(
        composition: Map<Ingredient, Double>
    ): Map<String, Double> {
        val totals = mutableMapOf<String, Double>()
        val sumKg = composition.values.sum().coerceAtLeast(0.0)

        if (sumKg == 0.0) return emptyMap()

        // Obtener todos los nutrientes únicos de todos los ingredientes
        val allKeys = composition.keys.flatMap { it.nutrients.keys }.distinct()

        allKeys.forEach { nutrient ->
            var weightedSum = 0.0
            composition.forEach { (ingredient, kgDia) ->
                val fraction = kgDia / sumKg
                val value = ingredient.getNutrient(nutrient)
                weightedSum += value * fraction
            }
            totals[nutrient] = weightedSum
        }

        return totals
    }

    /**
     * Construye observaciones sobre la dieta optimizada
     */
    private fun buildObservations(
        methaneResult: MethaneCalculator.MethaneResult,
        dietResult: DietResult,
        tipoDietaReal: TipoDieta
    ): String {
        return buildString {
            appendLine("Estado de optimización: ${dietResult.getStatusMessage()}")
            appendLine("Tipo de dieta (calculado): ${tipoDietaReal.descripcion}")
            appendLine("Metano (media): ${String.format("%.2f", methaneResult.mean)} g/día")
            appendLine("Rango de metano: ${String.format("%.2f", methaneResult.min)} - ${String.format("%.2f", methaneResult.max)} g/día")
            appendLine("Coeficiente de variación: ${String.format("%.1f", methaneResult.getCoefficientOfVariation())}%")
            appendLine("Ecuaciones utilizadas: ${methaneResult.predictions.size}")

            // Advertencia si el tipo de dieta real difiere del predefinido
            if (tipoDietaReal != methaneResult.tipoDieta) {
                appendLine("NOTA: El tipo de dieta calculado (${tipoDietaReal.descripcion}) difiere del esperado (${methaneResult.tipoDieta.descripcion})")
            }
        }
    }

    /**
     * Diagnóstico cuando la optimización NO es factible.
     *
     * Analiza:
     * - Disponibilidad de insumos
     * - Consistencia de requerimientos vs disponibilidad
     * - Datos faltantes en nutrientes críticos
     * - Precios anómalos
     * - RECOMENDACIONES DE INSUMOS (Gap Analysis)
     */
    private fun diagnoseInfeasibility(
        animal: Animal,
        insumos: List<Insumo>,
        allInsumos: List<Insumo>
    ): String = buildString {
        val dmi = max(0.0, animal.consumoDMI)

        // 1. Validaciones básicas
        if (insumos.isEmpty()) {
            appendLine("❌ No hay insumos disponibles.")
            return@buildString
        }

        if (dmi <= 0.0) {
            appendLine("❌ El DMI del animal es 0 o negativo (${animal.consumoDMI}).")
            return@buildString
        }

        appendLine("Analizando factibilidad de la dieta...")
        appendLine()

        // 2. Funciones helper para densidades
        fun maxDensity(key: String): Double? =
            insumos.mapNotNull { it.nutrientes[key] }.maxOrNull()

        fun minDensity(key: String): Double? =
            insumos.mapNotNull { it.nutrientes[key] }.minOrNull()

        var problemasDetectados = 0

        // 3. Verificar requerimientos mínimos
        appendLine("=== REQUERIMIENTOS MÍNIMOS ===")
        if (animal.requerimientosMinimos.isEmpty()) {
            appendLine("✓ No hay requerimientos mínimos definidos")
        } else {
            animal.requerimientosMinimos.forEach { (k, minReq) ->
                val maxD = maxDensity(k)
                when {
                    maxD == null -> {
                        appendLine("⚠ Nutriente '$k': no hay datos en ningún insumo")
                        problemasDetectados++
                    }
                    minReq > maxD -> {
                        appendLine("❌ Nutriente '$k': requisito mínimo ${"%.2f".format(minReq)} > máximo posible ${"%.2f".format(maxD)}")
                        problemasDetectados++
                    }
                    else -> {
                        appendLine("✓ Nutriente '$k': factible (req: ${"%.2f".format(minReq)}, max: ${"%.2f".format(maxD)})")
                    }
                }
            }
        }
        appendLine()

        // 3b. RECOMENDACIONES (Gap Analysis)
        if (problemasDetectados > 0 && allInsumos.isNotEmpty()) {
             appendLine("=== RECOMENDACIONES DE INSUMOS ===")
             animal.requerimientosMinimos.forEach { (k, minReq) ->
                 val maxD = maxDensity(k)
                 if (maxD != null && minReq > maxD) {
                     // Encontramos un déficit. Buscar en el catálogo completo.
                     val topInsumos = allInsumos
                        .filter { it.nutrientes[k] != null }
                        .sortedByDescending { it.nutrientes[k] }
                        .take(3)
                     
                     if (topInsumos.isNotEmpty()) {
                         appendLine("💡 Para mejorar '$k' (req: ${"%.2f".format(minReq)}), considera agregar:")
                         topInsumos.forEach { insumo ->
                             val valNut = insumo.nutrientes[k] ?: 0.0
                             appendLine("   • ${insumo.nombre} (${"%.2f".format(valNut)})")
                         }
                         appendLine()
                     }
                 }
             }
        }

        // 4. Verificar requerimientos máximos
        appendLine("=== REQUERIMIENTOS MÁXIMOS ===")
        if (animal.requerimientosMaximos.isEmpty()) {
            appendLine("✓ No hay requerimientos máximos definidos")
        } else {
            animal.requerimientosMaximos.forEach { (k, maxReq) ->
                val minD = minDensity(k)
                when {
                    minD == null -> {
                        appendLine("⚠ Nutriente '$k': no hay datos en ningún insumo")
                    }
                    maxReq < minD -> {
                        appendLine("❌ Nutriente '$k': tope máximo ${"%.2f".format(maxReq)} < mínimo posible ${"%.2f".format(minD)}")
                        problemasDetectados++
                    }
                    else -> {
                        appendLine("✓ Nutriente '$k': factible (max: ${"%.2f".format(maxReq)}, min: ${"%.2f".format(minD)})")
                    }
                }
            }
        }
        appendLine()

        // 5. Insumos con datos faltantes
        val nutrientesCriticos = (animal.requerimientosMinimos.keys + animal.requerimientosMaximos.keys).toSet()
        val insumosConHuecos = insumos.filter { i ->
            nutrientesCriticos.any { k -> i.nutrientes[k] == null }
        }

        if (insumosConHuecos.isNotEmpty()) {
            appendLine("=== INSUMOS CON DATOS FALTANTES ===")
            insumosConHuecos.take(5).forEach { i ->
                val faltan = nutrientesCriticos.filter { k -> i.nutrientes[k] == null }
                appendLine("⚠ ${i.nombre}: faltan ${faltan.joinToString()}")
            }
            if (insumosConHuecos.size > 5) {
                appendLine("  ... y ${insumosConHuecos.size - 5} insumos más")
            }
            appendLine()
        }

        // 6. Precios anómalos
        val negativos = insumos.filter { it.costo < 0.0 }
        if (negativos.isNotEmpty()) {
            appendLine("=== PRECIOS ANÓMALOS ===")
            appendLine("❌ Insumos con costo negativo:")
            negativos.forEach {
                appendLine("   • ${it.nombre}: $${"%.2f".format(it.costo)}")
            }
            problemasDetectados += negativos.size
            appendLine()
        }

        // 7. Resumen
        appendLine("=== RESUMEN ===")
        if (problemasDetectados == 0) {
            appendLine("⚠ No se detectaron problemas obvios en los datos")
            appendLine("Posibles causas:")
            appendLine("  • Combinación de restricciones demasiado restrictiva")
            appendLine("  • Límites de inclusión min/max conflictivos")
            appendLine("  • DMI objetivo inalcanzable con ingredientes disponibles")
        } else {
            appendLine("❌ Se detectaron $problemasDetectados problemas")
            appendLine("Corrija los datos y vuelva a intentar")
        }
    }

    /**
     * Crea una dieta de error cuando la optimización falla
     */
    private fun createErrorDiet(
        animal: Animal,
        dietResult: DietResult,
        diagnostics: String
    ): Dieta {
        val obs = buildString {
            appendLine("╔════════════════════════════════════════╗")
            appendLine("║           ERROR DE OPTIMIZACIÓN        ║")
            appendLine("╚════════════════════════════════════════╝")
            appendLine()
            appendLine("Estado: ${dietResult.getStatusMessage()}")
            appendLine()
            appendLine("No se pudo encontrar una dieta factible con los")
            appendLine("ingredientes y restricciones proporcionados.")
            appendLine()
            if (diagnostics.isNotBlank()) {
                appendLine("═══ DIAGNÓSTICO ═══")
                append(diagnostics.trim())
            }
        }

        return Dieta(
            animal = animal,
            composicion = emptyMap(),
            costoTotal = 0.0,
            metanoProducidoGramos = 0.0,
            nutrientesTotales = emptyMap(),
            observaciones = obs
        )
    }

    /**
     * Imprime resumen final de la dieta optimizada
     */
    private fun printFinalSummary(
        dieta: Dieta,
        methaneResult: MethaneCalculator.MethaneResult
    ) {
        logI("\n╔════════════════════════════════════════╗")
        logI("║   RESUMEN FINAL DE LA DIETA            ║")
        logI("╚════════════════════════════════════════╝")
        logI("Animal: ${dieta.animal.nombre}")
        logI("ID: ${dieta.animal.id}")
        logI("Tipo de dieta predefinida: ${dieta.animal.tipo.descripcion}")
        logI("Peso: ${dieta.animal.pesoKg} kg")
        logI("DMI objetivo: ${dieta.animal.consumoDMI} kg/día")
        logI("----------------------------------------")
        logI("Costo total: $${String.format("%.2f", dieta.costoTotal)}/día")
        logI("Costo por kg MS: $${String.format("%.4f", dieta.costoPorKgMS())}/kg")
        logI("----------------------------------------")
        logI("Metano producido (media): ${String.format("%.2f", methaneResult.mean)} g/día")
        logI("Metano por kg DMI: ${String.format("%.2f", dieta.metanoPorKgDMI())} g/kg")
        logI("Rango de incertidumbre: ±${String.format("%.2f", methaneResult.getUncertaintyRange() / 2)} g/día")
        logI("Coef. de variación: ${String.format("%.1f", methaneResult.getCoefficientOfVariation())}%")
        logI("----------------------------------------")
        logI("Ingredientes en la dieta: ${dieta.composicion.size}")

        // Mostrar composición resumida
        if (dieta.composicion.isNotEmpty()) {
            logI("\nComposición:")
            val totalDM = dieta.composicion.values.sum()
            dieta.composicion.entries
                .sortedByDescending { it.value }
                .take(5)
                .forEach { (nombre, kg) ->
                    val pct = (kg / totalDM) * 100.0
                    logI("  • ${nombre.padEnd(25)} ${"%6.2f".format(kg)} kg/d (${"%5.1f".format(pct)}%)")
                }
            if (dieta.composicion.size > 5) {
                logI("  ... y ${dieta.composicion.size - 5} ingredientes más")
            }
        }

        logI("╚════════════════════════════════════════╝\n")

        printNutrientSummary(dieta)
    }

    /**
     * Imprime resumen de nutrientes alcanzados en la dieta
     */
    private fun printNutrientSummary(dieta: Dieta) {
        logI("\n╔════════════════════════════════════════╗")
        logI("║   NUTRIENTES ALCANZADOS (DIETA)        ║")
        logI("╚════════════════════════════════════════╝")

        val dmi = dieta.animal.consumoDMI
        if (dieta.nutrientesTotales.isEmpty()) {
            logW("No hay nutrientes calculados para esta dieta.")
            return
        }

        // Nutrientes principales a destacar
        val nutrientesPrincipales = listOf(
            Animal.PROTEINA_CRUDA,
            Animal.FIBRA_DETERGENTE_NEUTRA,
            Animal.EXTRACTO_ETEREO,
            Animal.CALCIO,
            Animal.FOSFORO
        )

        logI(String.format("%-25s %12s %12s %8s", "Nutriente", "Total/día", "Por kg DMI", "Estado"))
        logI("─".repeat(65))

        nutrientesPrincipales.forEach { nutriente ->
            val totalDia = dieta.nutrientesTotales[nutriente]
            if (totalDia != null) {
                val porKgDmi = if (dmi > 0) totalDia / dmi else Double.NaN

                // Verificar vs requerimientos
                val minReq = dieta.animal.requerimientosMinimos[nutriente]
                val maxReq = dieta.animal.requerimientosMaximos[nutriente]

                val status = when {
                    minReq != null && porKgDmi < minReq -> "⚠ BAJO"
                    maxReq != null && porKgDmi > maxReq -> "⚠ ALTO"
                    else -> "✓ OK"
                }

                logI(
                    String.format(
                        "%-25s %12.3f %12.3f %8s",
                        nutriente,
                        totalDia,
                        porKgDmi,
                        status
                    )
                )
            }
        }

        // Mostrar otros nutrientes si hay
        val otrosNutrientes = dieta.nutrientesTotales.keys
            .filter { it !in nutrientesPrincipales }
            .sorted()

        if (otrosNutrientes.isNotEmpty()) {
            logI("\nOtros nutrientes:")
            otrosNutrientes.take(10).forEach { nutriente ->
                val totalDia = dieta.nutrientesTotales[nutriente] ?: 0.0
                val porKgDmi = if (dmi > 0) totalDia / dmi else Double.NaN
                logI(
                    String.format(
                        "%-25s %12.3f %12.3f",
                        nutriente,
                        totalDia,
                        porKgDmi
                    )
                )
            }
            if (otrosNutrientes.size > 10) {
                logI("  ... y ${otrosNutrientes.size - 10} nutrientes más")
            }
        }

        logI("╚════════════════════════════════════════╝\n")
    }

    /**
     * Modo de optimización disponible
     */
    enum class OptimizationMode {
        /** Minimiza el costo total de la dieta */
        COST,

        /** Minimiza la producción de metano entérico */
        METHANE
    }
}