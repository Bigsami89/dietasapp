package com.example.dietasapp.calculation.components

import com.example.dietasapp.domain.AnimalProfile
import com.example.dietasapp.domain.Ingredient
import org.ojalgo.optimisation.ExpressionsBasedModel
import org.ojalgo.optimisation.Variable

/**
 * Componente que agrega restricciones nutricionales al modelo
 *
 * Aplica:
 * - Requerimientos mínimos de nutrientes (CP, NEm, Ca, P, etc.)
 * - Límites máximos de nutrientes (NDF, CP máximo, etc.)
 * - Restricción de consumo total de materia seca (DMI)
 */
class NutritionalComponent : IDietComponent {

    override fun apply(
        model: ExpressionsBasedModel,
        variables: Map<Ingredient, Variable>,
        profile: AnimalProfile,
        ingredients: List<Ingredient>
    ) {
        // 1. Restricción de DMI (Dry Matter Intake) - debe cumplirse exactamente
        applyDMIConstraint(model, variables, profile)

        // 2. Restricciones de requerimientos mínimos
        applyMinimumRequirements(model, variables, profile, ingredients)

        // 3. Restricciones de límites máximos
        applyMaximumLimits(model, variables, profile, ingredients)

        logComponentApplication(profile)
    }

    override fun getName(): String = "Nutritional Constraints"

    override fun getDescription(): String =
        "Aplica restricciones nutricionales basadas en los requerimientos del animal"

    /**
     * Aplica restricción de consumo total de materia seca
     * Σ kg_ingrediente_i = DMI_requerido
     */
    private fun applyDMIConstraint(
        model: ExpressionsBasedModel,
        variables: Map<Ingredient, Variable>,
        profile: AnimalProfile
    ) {
        val dmiExpression = model.addExpression("DMI_Total")

        // Cada ingrediente contribuye 1:1 a la DMI
        variables.forEach { (_, variable) ->
            dmiExpression.set(variable, 1.0)
        }

        // DMI debe ser exactamente el requerido por el animal
        dmiExpression.level(profile.dmiKgDay)

        println("  → DMI requerido: ${profile.dmiKgDay} kg/día")
    }

    /**
     * Aplica restricciones de requerimientos mínimos
     * Para cada nutriente: Σ (contenido_nutriente_i × kg_i) ≥ requerimiento_mínimo
     */
    private fun applyMinimumRequirements(
        model: ExpressionsBasedModel,
        variables: Map<Ingredient, Variable>,
        profile: AnimalProfile,
        ingredients: List<Ingredient>
    ) {
        val minRequirements = profile.requirements.filter { it.key.startsWith("min_") }

        minRequirements.forEach { (key, minValue) ->
            val nutrientName = key.removePrefix("min_")

            // Crear expresión para este nutriente
            val nutrientExpression = model.addExpression("Min_$nutrientName")

            // Agregar contribución de cada ingrediente
            variables.forEach { (ingredient, variable) ->
                val nutrientContent = getNutrientContent(
                    ingredient = ingredient,
                    nutrientName = nutrientName,
                    totalDMI = profile.dmiKgDay
                )

                if (nutrientContent > 0.0) {
                    nutrientExpression.set(variable, nutrientContent)
                }
            }

            // Establecer límite inferior
            nutrientExpression.lower(minValue)

            println("  → $nutrientName mínimo: $minValue")
        }
    }

    /**
     * Aplica restricciones de límites máximos
     * Para cada nutriente: Σ (contenido_nutriente_i × kg_i) ≤ límite_máximo
     */
    private fun applyMaximumLimits(
        model: ExpressionsBasedModel,
        variables: Map<Ingredient, Variable>,
        profile: AnimalProfile,
        ingredients: List<Ingredient>
    ) {
        val maxRequirements = profile.requirements.filter { it.key.startsWith("max_") }

        maxRequirements.forEach { (key, maxValue) ->
            val nutrientName = key.removePrefix("max_")

            // Crear expresión para este nutriente
            val nutrientExpression = model.addExpression("Max_$nutrientName")

            // Agregar contribución de cada ingrediente
            variables.forEach { (ingredient, variable) ->
                val nutrientContent = getNutrientContent(
                    ingredient = ingredient,
                    nutrientName = nutrientName,
                    totalDMI = profile.dmiKgDay
                )

                if (nutrientContent > 0.0) {
                    nutrientExpression.set(variable, nutrientContent)
                }
            }

            // Establecer límite superior
            nutrientExpression.upper(maxValue)

            println("  → $nutrientName máximo: $maxValue")
        }
    }

    /**
     * Obtiene el contenido de un nutriente de un ingrediente
     * Maneja conversión de porcentajes a valores absolutos según sea necesario
     */
    private fun getNutrientContent(
        ingredient: Ingredient,
        nutrientName: String,
        totalDMI: Double
    ): Double {
        val value = ingredient.getNutrient(nutrientName)

        return when (nutrientName) {
            // Estos nutrientes están en % y necesitan convertirse a kg
            "CP", "NDF", "Ca", "P" -> {
                value / 100.0  // Por cada kg de ingrediente
            }
            // Estos están en unidades absolutas (Mcal/kg)
            "NEm", "NEg", "GE" -> {
                value  // Ya están en la unidad correcta
            }
            else -> value
        }
    }

    private fun logComponentApplication(profile: AnimalProfile) {
        println("[${getName()}()] Aplicado")
        println("  Animal: ${profile.name}")
        println("  Peso: ${profile.bodyWeightKg} kg")

        val minCount = profile.requirements.count { it.key.startsWith("min_") }
        val maxCount = profile.requirements.count { it.key.startsWith("max_") }

        println("  Restricciones mínimas: $minCount")
        println("  Restricciones máximas: $maxCount")
    }
}