package com.example.dietasapp.calculation.optimizer

import com.example.dietasapp.calculation.components.CostComponent
import com.example.dietasapp.calculation.components.IDietComponent
import com.example.dietasapp.calculation.components.MethaneComponent
import com.example.dietasapp.calculation.components.NutritionalComponent
import com.example.dietasapp.calculation.methane.MethaneCalculator
import com.example.dietasapp.data.Animal
import com.example.dietasapp.data.Insumo
import com.example.dietasapp.data.TipoDieta
import com.example.dietasapp.domain.AnimalProfile
import com.example.dietasapp.domain.Dieta
import com.example.dietasapp.domain.DietResult
import com.example.dietasapp.domain.Ingredient

/**
 * API principal para el cálculo de dietas óptimas
 * Fachada que coordina el optimizador y el cálculo de metano
 *
 * Corresponde a la clase "DietAPI" del diagrama
 */
class DietAPI {

    private val methaneCalculator = MethaneCalculator()

    /**
     * Calcula la dieta óptima para un animal con los ingredientes disponibles
     *
     * @param animal Animal con requerimientos nutricionales
     * @param ingredientes Lista de ingredientes/insumos disponibles
     * @param optimizationMode Modo de optimización (COST o METHANE)
     * @return Dieta óptima calculada
     */
    fun calculateOptimalDiet(
        animal: Animal,
        ingredientes: List<Insumo>,
        optimizationMode: OptimizationMode = OptimizationMode.COST
    ): Dieta {

        println("\n╔════════════════════════════════════════╗")
        println("║   CÁLCULO DE DIETA ÓPTIMA              ║")
        println("╚════════════════════════════════════════╝")

        // 1. Convertir modelos de datos a modelos de dominio
        val profile = AnimalProfile.fromAnimal(animal)
        val ingredients = ingredientes.map { Ingredient.fromInsumo(it) }

        // 2. Crear componentes según el modo de optimización
        val components = createComponents(optimizationMode)

        // 3. Crear y ejecutar optimizador
        val optimizer = DietOptimizer(components)
        val dietResult = optimizer.optimize(profile, ingredients)

        // 4. Verificar si la optimización fue exitosa
        if (!dietResult.isFeasible()) {
            return createErrorDiet(animal, dietResult)
        }

        // 5. Calcular metano preciso con MethaneCalculator
        val tipoDieta = determineTipoDieta(dietResult.composition, ingredientes)
        val methaneResult = calculatePreciseMethane(
            dietResult = dietResult,
            animal = animal,
            tipoDieta = tipoDieta
        )

        // 6. Calcular nutrientes totales de la dieta
        val nutrientesTotales = calculateTotalNutrients(dietResult.composition)

        // 7. Crear objeto Dieta final
        val composicionMap = dietResult.composition.mapKeys { it.key.name }

        val dieta = Dieta(
            animal = animal,
            composicion = composicionMap,
            costoTotal = dietResult.totalCost,
            metanoProducidoGramos = methaneResult.mean,
            nutrientesTotales = nutrientesTotales,
            observaciones = buildObservations(methaneResult, dietResult)
        )

        // 8. Mostrar resumen final
        printFinalSummary(dieta, methaneResult)

        return dieta
    }

    /**
     * Calcula dieta óptima con componentes personalizados
     */
    fun calculateOptimalDietWithComponents(
        animal: Animal,
        ingredientes: List<Insumo>,
        components: List<IDietComponent>
    ): Dieta {
        val profile = AnimalProfile.fromAnimal(animal)
        val ingredients = ingredientes.map { Ingredient.fromInsumo(it) }

        val optimizer = DietOptimizer(components)
        val dietResult = optimizer.optimize(profile, ingredients)

        if (!dietResult.isFeasible()) {
            return createErrorDiet(animal, dietResult)
        }

        val tipoDieta = determineTipoDieta(dietResult.composition, ingredientes)
        val methaneResult = calculatePreciseMethane(dietResult, animal, tipoDieta)
        val nutrientesTotales = calculateTotalNutrients(dietResult.composition)
        val composicionMap = dietResult.composition.mapKeys { it.key.name }

        return Dieta(
            animal = animal,
            composicion = composicionMap,
            costoTotal = dietResult.totalCost,
            metanoProducidoGramos = methaneResult.mean,
            nutrientesTotales = nutrientesTotales,
            observaciones = buildObservations(methaneResult, dietResult)
        )
    }

    /**
     * Crea los componentes según el modo de optimización
     */
    private fun createComponents(mode: OptimizationMode): List<IDietComponent> {
        return when (mode) {
            OptimizationMode.COST -> listOf(
                NutritionalComponent(),
                CostComponent()
            )
            OptimizationMode.METHANE -> listOf(
                NutritionalComponent(),
                MethaneComponent()
            )
        }
    }

    /**
     * Determina el tipo de dieta basado en la composición
     */
    private fun determineTipoDieta(
        composition: Map<Ingredient, Double>,
        insumos: List<Insumo>
    ): TipoDieta {
        var totalForraje = 0.0
        var totalDM = 0.0

        composition.forEach { (ingredient, kg) ->
            totalDM += kg

            // Buscar el insumo correspondiente
            val insumo = insumos.find { it.nombre == ingredient.name }
            if (insumo?.esForraje == true) {
                totalForraje += kg
            }
        }

        val foragePercentage = if (totalDM > 0) (totalForraje / totalDM) * 100.0 else 0.0
        return TipoDieta.fromForagePercentage(foragePercentage)
    }

    /**
     * Calcula metano preciso usando MethaneCalculator
     */
    private fun calculatePreciseMethane(
        dietResult: DietResult,
        animal: Animal,
        tipoDieta: TipoDieta
    ): MethaneCalculator.MethaneResult {

        println("\n========================================")
        println("Calculando producción de metano...")
        println("========================================")

        val methaneResult = methaneCalculator.calculate(
            dietComposition = dietResult.composition,
            bodyWeightKg = animal.pesoKg,
            dmiKgDay = animal.consumoDMI,
            tipoDieta = tipoDieta
        )

        println(methaneResult.getSummary())

        return methaneResult
    }

    /**
     * Calcula los nutrientes totales de la dieta
     */
    private fun calculateTotalNutrients(
        composition: Map<Ingredient, Double>
    ): Map<String, Double> {
        // promedio del mix en UNIDADES NATIVAS (%, Mcal/kg, g/kg, etc.)
        val totals = mutableMapOf<String, Double>()
        val sumKg = composition.values.sum().coerceAtLeast(0.0)
        if (sumKg == 0.0) return emptyMap()

        val allKeys = composition.keys.flatMap { it.nutrients.keys }.distinct()
        allKeys.forEach { k ->
            var acc = 0.0
            composition.forEach { (ingredient, kgDia) ->
                val frac = kgDia / sumKg          // fracción de inclusión (sin cambiar unidades)
                val v = ingredient.getNutrient(k) // valor nativo del insumo
                if (v != null) acc += v * frac
            }
            totals[k] = acc // mismo k, mismas unidades nativas
        }
        return totals
    }


    /**
     * Construye observaciones sobre la dieta
     */
    private fun buildObservations(
        methaneResult: MethaneCalculator.MethaneResult,
        dietResult: DietResult
    ): String {
        return buildString {
            appendLine("Estado de optimización: ${dietResult.getStatusMessage()}")
            appendLine("Tipo de dieta: ${methaneResult.tipoDieta.descripcion}")
            appendLine("Metano (media): ${String.format("%.2f", methaneResult.mean)} g/día")
            appendLine("Rango de metano: ${String.format("%.2f", methaneResult.min)} - ${String.format("%.2f", methaneResult.max)} g/día")
            appendLine("Coeficiente de variación: ${String.format("%.1f", methaneResult.getCoefficientOfVariation())}%")
            appendLine("Ecuaciones utilizadas: ${methaneResult.predictions.size}")
        }
    }

    /**
     * Crea una dieta de error cuando la optimización falla
     */
    private fun createErrorDiet(animal: Animal, dietResult: DietResult): Dieta {
        return Dieta(
            animal = animal,
            composicion = emptyMap(),
            costoTotal = 0.0,
            metanoProducidoGramos = 0.0,
            nutrientesTotales = emptyMap(),
            observaciones = "ERROR: ${dietResult.getStatusMessage()}\n" +
                    "No se pudo encontrar una dieta factible con los ingredientes y restricciones proporcionados."
        )
    }

    /**
     * Imprime resumen final de la dieta
     */
    private fun printFinalSummary(dieta: Dieta, methaneResult: MethaneCalculator.MethaneResult) {
        println("\n╔════════════════════════════════════════╗")
        println("║   RESUMEN FINAL DE LA DIETA            ║")
        println("╚════════════════════════════════════════╝")
        println("Animal: ${dieta.animal.nombre}")
        println("Peso: ${dieta.animal.pesoKg} kg")
        println("DMI objetivo: ${dieta.animal.consumoDMI} kg/día")
        println("----------------------------------------")
        println("Costo total: $${String.format("%.2f", dieta.costoTotal)}/día")
        println("Costo por kg MS: $${String.format("%.4f", dieta.costoPorKgMS())}/kg")
        println("----------------------------------------")
        println("Metano producido (media): ${String.format("%.2f", methaneResult.mean)} g/día")
        println("Metano por kg DMI: ${String.format("%.2f", dieta.metanoPorKgDMI())} g/kg")
        println("Rango de incertidumbre: ±${String.format("%.2f", methaneResult.getUncertaintyRange() / 2)} g/día")
        println("----------------------------------------")
        println("Ingredientes en la dieta: ${dieta.composicion.size}")
        println("╚════════════════════════════════════════╝\n")

        printNutrientSummary(dieta)
    }

    /**
     * Modos de optimización disponibles
     */
    enum class OptimizationMode {
        COST,      // Minimizar costo
        METHANE    // Minimizar metano
    }

    private fun printNutrientSummary(dieta: Dieta) {
        println("\n╔════════════════════════════════════════╗")
        println("║   NUTRIENTES ALCANZADOS (DIETA)        ║")
        println("╚════════════════════════════════════════╝")

        val dmi = dieta.animal.consumoDMI
        if (dieta.nutrientesTotales.isEmpty()) {
            println("No hay nutrientes calculados para esta dieta.")
            return
        }

        println(String.format("%-22s %16s %18s", "Nutriente", "Total / día", "Por kg DMI"))
        println("─".repeat(60))

        dieta.nutrientesTotales.toSortedMap().forEach { (nutriente, totalDia) ->
            val porKgDmi = if (dmi > 0) totalDia / dmi else Double.NaN
            println(
                String.format(
                    "%-22s %16.3f %18.3f",
                    nutriente,
                    totalDia,
                    porKgDmi
                )
            )
        }
        println("╚════════════════════════════════════════╝\n")
    }
}