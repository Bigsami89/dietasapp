package com.example.dietasapp.calculation.methane

import com.example.dietasapp.data.Animal
import com.example.dietasapp.data.TipoDieta
import com.example.dietasapp.domain.Ingredient
import kotlin.math.exp
import kotlin.math.pow

/**
 * Calculadora de metano entérico basada en NASEM (2016)
 * Implementa ecuaciones empíricas (ELS) para diferentes tipos de dietas
 *
 * Basado en: Nutrient Requirements of Beef Cattle, 8th Edition (NASEM, 2016)
 */
class MethaneCalculator {

    /**
     * Calcula la producción de metano usando múltiples ecuaciones empíricas
     * y devuelve estadísticas del conjunto (ensemble approach)
     *
     * @param dietComposition Composición de la dieta (ingrediente -> kg/día)
     * @param bodyWeightKg Peso corporal del animal (kg)
     * @param dmiKgDay Consumo de materia seca (kg/día)
     * @param tipoDieta Tipo de dieta según contenido de forraje
     * @return Resultado del cálculo con estadísticas
     */
    fun calculate(
        dietComposition: Map<Ingredient, Double>,
        bodyWeightKg: Double,
        dmiKgDay: Double,
        tipoDieta: TipoDieta
    ): MethaneResult {

        // Calcular nutrientes totales de la dieta
        val nutrients = calculateTotalNutrients(dietComposition, dmiKgDay)

        // Seleccionar ecuaciones según tipo de dieta
        val predictions = when (tipoDieta) {
            TipoDieta.ALTO_FORRAJE -> calculateHighForage(
                bodyWeightKg, dmiKgDay, nutrients
            )
            TipoDieta.INTERMEDIO -> calculateIntermediateForage(
                bodyWeightKg, dmiKgDay, nutrients
            )
            TipoDieta.BAJO_FORRAJE -> calculateLowForage(
                bodyWeightKg, dmiKgDay, nutrients
            )
        }

        // Calcular estadísticas del conjunto
        return calculateStatistics(predictions, tipoDieta)
    }

    /**
     * Versión sobrecargada que acepta un Animal directamente
     */
    fun calculate(
        dietComposition: Map<Ingredient, Double>,
        animal: Animal
    ): MethaneResult {
        return calculate(
            dietComposition = dietComposition,
            bodyWeightKg = animal.pesoKg,
            dmiKgDay = animal.consumoDMI,
            tipoDieta = animal.tipo
        )
    }

    /**
     * Calcula nutrientes totales de la dieta usando constantes de Animal
     */
    private fun calculateTotalNutrients(
        dietComposition: Map<Ingredient, Double>,
        dmiKgDay: Double
    ): DietNutrients {
        var totalGE = 0.0
        var totalCP = 0.0
        var totalFDN = 0.0
        var totalStarch = 0.0
        var totalEE = 0.0
        var totalME = 0.0
        var totalDM = 0.0

        dietComposition.forEach { (ingredient, kgPerDay) ->
            // Usar las constantes de Animal para acceder a nutrientes
            val ge = ingredient.getNutrientOrNull("GE") ?: 0.0
            val pc = ingredient.getNutrient(Animal.PROTEINA_CRUDA) / 100.0
            val fdn = ingredient.getNutrient(Animal.FIBRA_DETERGENTE_NEUTRA) / 100.0
            val almidon = ingredient.getNutrientOrNull("Almidon") ?: 0.0
            val ee = ingredient.getNutrient(Animal.EXTRACTO_ETEREO) / 100.0

            totalGE += ge * kgPerDay
            totalCP += pc * kgPerDay
            totalFDN += fdn * kgPerDay
            totalStarch += (almidon / 100.0) * kgPerDay
            totalEE += ee * kgPerDay
            totalDM += kgPerDay

            // Calcular ME aproximado (GE * 0.82 como estimación simplificada)
            totalME += ge * 0.82 * kgPerDay
        }

        val foragePercentage = calculateForagePercentage(dietComposition)

        return DietNutrients(
            gei = totalGE,
            mei = totalME,
            dmi = dmiKgDay,
            cpKgDay = totalCP,
            fdnKgDay = totalFDN,
            starchKgDay = totalStarch,
            eeKgDay = totalEE,
            eePercent = (totalEE / totalDM) * 100.0,
            foragePercent = foragePercentage,
            gePerKg = if (dmiKgDay > 0) totalGE / dmiKgDay else 0.0
        )
    }

    /**
     * Calcula el porcentaje de forraje en la dieta
     * Asume que ingredientes con FDN > 40% son forrajes
     */
    private fun calculateForagePercentage(dietComposition: Map<Ingredient, Double>): Double {
        var totalForage = 0.0
        var totalDM = 0.0

        dietComposition.forEach { (ingredient, kgPerDay) ->
            totalDM += kgPerDay
            val fdn = ingredient.getNutrient(Animal.FIBRA_DETERGENTE_NEUTRA)
            if (fdn > 40.0) {
                totalForage += kgPerDay
            }
        }

        return if (totalDM > 0) (totalForage / totalDM) * 100.0 else 0.0
    }

    // ========================================================================
    // ECUACIONES PARA DIETAS CON ALTO CONTENIDO DE FORRAJE (≥40% MS)
    // ========================================================================

    private fun calculateHighForage(
        bw: Double,
        dmi: Double,
        nutrients: DietNutrients
    ): List<MethanePrediction> {
        val predictions = mutableListOf<MethanePrediction>()

        // Eq. 16-8: Escobar-Bahamondes y Beauchemin
        try {
            val ch4 = 71.5 + 0.12 * bw + 0.10 * dmi.pow(3) - 244.8 * nutrients.eeKgDay.pow(3)
            predictions.add(MethanePrediction("Escobar-Bahamondes (16-8)", ch4, "g/d"))
        } catch (e: Exception) {
            // Si falla, continuar con otras ecuaciones
        }

        // IPCC (2006, Nivel II)
        try {
            val ch4MJ = dmi * nutrients.gePerKg * 0.065
            val ch4Grams = ch4MJ * 1000 / 55.65
            predictions.add(MethanePrediction("IPCC (2006)", ch4Grams, "g/d"))
        } catch (e: Exception) {
            // Continuar
        }

        // Ellis et al. (2009, Eq. G)
        if (nutrients.foragePercent <= 75.0 && bw in 180.0..630.0) {
            try {
                val ch4MJ = -1.01 + 2.76 * nutrients.fdnKgDay + 0.722 * nutrients.starchKgDay
                val ch4Grams = ch4MJ * 1000 / 55.65
                predictions.add(MethanePrediction("Ellis (2009, Eq. G)", ch4Grams, "g/d"))
            } catch (e: Exception) {
                // Continuar
            }
        }

        // Mills et al. (2003, Eq. NL2)
        if (nutrients.foragePercent <= 75.0) {
            try {
                val ch4MJ = 45.98 - (45.98 * exp(-0.003 * nutrients.mei))
                val ch4Grams = ch4MJ * 1000 / 55.65
                predictions.add(MethanePrediction("Mills (2003, NL2)", ch4Grams, "g/d"))
            } catch (e: Exception) {
                // Continuar
            }
        }

        // Ellis et al. (2009, Eq. N)
        if (nutrients.foragePercent <= 75.0 && bw in 180.0..630.0) {
            try {
                val starchFDNRatio = if (nutrients.fdnKgDay > 0) {
                    nutrients.starchKgDay / nutrients.fdnKgDay
                } else 0.0

                val ch4MJ = 2.68 - 1.14 * starchFDNRatio + 0.786 * dmi
                val ch4Grams = ch4MJ * 1000 / 55.65
                predictions.add(MethanePrediction("Ellis (2009, Eq. N)", ch4Grams, "g/d"))
            } catch (e: Exception) {
                // Continuar
            }
        }

        // Moraes et al. (2014) - Para novillos
        if (bw in 170.0..630.0) {
            try {
                val geiMJ = nutrients.gei * 4.184
                val ch4Grams = (-0.221 + 0.048 * geiMJ + 0.005 * bw) * 1000 / 55.65
                predictions.add(MethanePrediction("Moraes (2014, Novillos)", ch4Grams, "g/d"))
            } catch (e: Exception) {
                // Continuar
            }
        }

        return predictions
    }

    // ========================================================================
    // ECUACIONES PARA DIETAS CON CONTENIDO INTERMEDIO DE FORRAJE (20-40% MS)
    // ========================================================================

    private fun calculateIntermediateForage(
        bw: Double,
        dmi: Double,
        nutrients: DietNutrients
    ): List<MethanePrediction> {
        val predictions = mutableListOf<MethanePrediction>()

        // Ellis et al. (2007, Eq. 12b)
        if (bw in 200.0..660.0) {
            try {
                val ch4MJ = 2.7 + 1.16 * dmi - 15.8 * (nutrients.eeKgDay / dmi)
                val ch4Grams = ch4MJ * 1000 / 55.65
                predictions.add(MethanePrediction("Ellis (2007, Eq. 12b)", ch4Grams, "g/d"))
            } catch (e: Exception) {
                // Continuar
            }
        }

        // Promedio de ecuaciones alto y bajo forraje
        try {
            val highForagePreds = calculateHighForage(bw, dmi, nutrients)
            val lowForagePreds = calculateLowForage(bw, dmi, nutrients)

            if (highForagePreds.isNotEmpty() && lowForagePreds.isNotEmpty()) {
                val avgHigh = highForagePreds.map { it.value }.average()
                val avgLow = lowForagePreds.map { it.value }.average()
                val avgTotal = (avgHigh + avgLow) / 2.0

                predictions.add(MethanePrediction("Promedio Alto-Bajo Forraje", avgTotal, "g/d"))
            }
        } catch (e: Exception) {
            // Continuar
        }

        return predictions
    }

    // ========================================================================
    // ECUACIONES PARA DIETAS CON BAJO CONTENIDO DE FORRAJE (≤20% MS)
    // ========================================================================

    private fun calculateLowForage(
        bw: Double,
        dmi: Double,
        nutrients: DietNutrients
    ): List<MethanePrediction> {
        val predictions = mutableListOf<MethanePrediction>()

        // Eq. 16-9: Escobar-Bahamondes y Beauchemin
        try {
            val cpFDNRatio = if (nutrients.fdnKgDay > 0) {
                nutrients.cpKgDay / nutrients.fdnKgDay
            } else 0.0

            val starchFDNRatio = if (nutrients.fdnKgDay > 0) {
                nutrients.starchKgDay / nutrients.fdnKgDay
            } else 0.0

            val ch4 = -10.1 + 0.21 * bw + 0.36 * dmi.pow(2) - 69.2 * nutrients.eeKgDay +
                    13.0 * cpFDNRatio - 4.9 * starchFDNRatio

            predictions.add(MethanePrediction("Escobar-Bahamondes (16-9)", ch4, "g/d"))
        } catch (e: Exception) {
            // Continuar
        }

        // Ellis et al. (2007, Eq. 9b)
        try {
            val meiMJ = nutrients.mei * 4.184
            val ch4Grams = (0.357 + 0.0591 * meiMJ + 0.05 * nutrients.foragePercent) * 1000 / 55.65
            predictions.add(MethanePrediction("Ellis (2007, Eq. 9b)", ch4Grams, "g/d"))
        } catch (e: Exception) {
            // Continuar
        }

        // Ellis et al. (2007, Eq. 10b)
        try {
            val ch4Grams = (-1.02 + 0.681 * dmi + 0.0481 * nutrients.foragePercent) * 1000 / 55.65
            predictions.add(MethanePrediction("Ellis (2007, Eq. 10b)", ch4Grams, "g/d"))
        } catch (e: Exception) {
            // Continuar
        }

        // Ellis et al. (2009, Eq. G)
        try {
            val ch4MJ = -1.01 + 2.76 * nutrients.fdnKgDay + 0.722 * nutrients.starchKgDay
            val ch4Grams = ch4MJ * 1000 / 55.65
            predictions.add(MethanePrediction("Ellis (2009, Eq. G)", ch4Grams, "g/d"))
        } catch (e: Exception) {
            // Continuar
        }

        return predictions
    }

    // ========================================================================
    // CÁLCULO DE ESTADÍSTICAS
    // ========================================================================

    private fun calculateStatistics(
        predictions: List<MethanePrediction>,
        tipoDieta: TipoDieta
    ): MethaneResult {
        if (predictions.isEmpty()) {
            return MethaneResult(
                mean = 0.0,
                median = 0.0,
                standardDeviation = 0.0,
                min = 0.0,
                max = 0.0,
                predictions = emptyList(),
                tipoDieta = tipoDieta,
                isValid = false
            )
        }

        val values = predictions.map { it.value }.sorted()
        val mean = values.average()
        val median = if (values.size % 2 == 0) {
            (values[values.size / 2 - 1] + values[values.size / 2]) / 2.0
        } else {
            values[values.size / 2]
        }

        val variance = values.map { (it - mean).pow(2) }.average()
        val stdDev = kotlin.math.sqrt(variance)

        return MethaneResult(
            mean = mean,
            median = median,
            standardDeviation = stdDev,
            min = values.minOrNull() ?: 0.0,
            max = values.maxOrNull() ?: 0.0,
            predictions = predictions,
            tipoDieta = tipoDieta,
            isValid = true
        )
    }

    // ========================================================================
    // CLASES DE DATOS
    // ========================================================================

    /**
     * Nutrientes calculados de la dieta
     * Actualizado para usar nomenclatura de Animal
     */
    private data class DietNutrients(
        val gei: Double,           // Gross Energy Intake (Mcal/d)
        val mei: Double,           // Metabolizable Energy Intake (Mcal/d)
        val dmi: Double,           // Dry Matter Intake (kg/d)
        val cpKgDay: Double,       // Crude Protein (kg/d) - PC
        val fdnKgDay: Double,      // Neutral Detergent Fiber (kg/d) - FDN
        val starchKgDay: Double,   // Starch (kg/d) - Almidón
        val eeKgDay: Double,       // Ether Extract (kg/d) - EE
        val eePercent: Double,     // Ether Extract (%)
        val foragePercent: Double, // Forage content (%)
        val gePerKg: Double        // GE per kg DM
    )

    /**
     * Predicción individual de metano
     */
    data class MethanePrediction(
        val equation: String,
        val value: Double,  // g/d
        val unit: String = "g/d"
    )

    /**
     * Resultado del cálculo de metano con estadísticas
     */
    data class MethaneResult(
        val mean: Double,              // Media de predicciones (g/d)
        val median: Double,            // Mediana de predicciones (g/d)
        val standardDeviation: Double, // Desviación estándar (g/d)
        val min: Double,               // Mínimo (g/d)
        val max: Double,               // Máximo (g/d)
        val predictions: List<MethanePrediction>,
        val tipoDieta: TipoDieta,
        val isValid: Boolean = true
    ) {
        /**
         * Obtiene el rango de incertidumbre
         */
        fun getUncertaintyRange(): Double = max - min

        /**
         * Obtiene el coeficiente de variación (%)
         */
        fun getCoefficientOfVariation(): Double {
            return if (mean > 0) (standardDeviation / mean) * 100.0 else 0.0
        }

        /**
         * Genera un resumen legible
         */
        fun getSummary(): String {
            return buildString {
                appendLine("=== Producción de Metano Entérico ===")
                appendLine("Tipo de dieta: ${tipoDieta.descripcion}")
                appendLine("Media: ${String.format("%.2f", mean)} g/d")
                appendLine("Mediana: ${String.format("%.2f", median)} g/d")
                appendLine("Rango: ${String.format("%.2f", min)} - ${String.format("%.2f", max)} g/d")
                appendLine("Desv. Estándar: ${String.format("%.2f", standardDeviation)} g/d")
                appendLine("Coef. Variación: ${String.format("%.2f", getCoefficientOfVariation())}%")
                appendLine("\nEcuaciones utilizadas (${predictions.size}):")
                predictions.forEach {
                    appendLine("  - ${it.equation}: ${String.format("%.2f", it.value)} ${it.unit}")
                }
            }
        }
    }
}