package com.example.dietasapp.calculation.components

import com.example.dietasapp.domain.AnimalProfile
import com.example.dietasapp.domain.Ingredient
import org.ojalgo.optimisation.ExpressionsBasedModel
import org.ojalgo.optimisation.Variable

/**
 * Componente que aplica restricciones de inclusión mínima y máxima para cada ingrediente
 *
 * Las restricciones se definen como:
 * - Inclusión Mínima: ingrediente_i >= (inclusionMin_i / 100) × DMI
 * - Inclusión Máxima: ingrediente_i <= (inclusionMax_i / 100) × DMI
 *
 * Donde:
 * - inclusionMin_i: Porcentaje mínimo del ingrediente i en la ración total
 * - inclusionMax_i: Porcentaje máximo del ingrediente i en la ración total
 * - DMI: Dry Matter Intake (Consumo de Materia Seca del animal)
 */
class InclusionConstraintsComponent : IDietComponent {

    override fun apply(
        model: ExpressionsBasedModel,
        variables: Map<Ingredient, Variable>,
        profile: AnimalProfile,
        ingredients: List<Ingredient>
    ) {
        val dmi = profile.dmiKgDay

        variables.forEach { (ingredient, variable) ->
            // Aplicar restricción de inclusión mínima
            if (ingredient.inclusionMin > 0.0) {
                val minKg = (ingredient.inclusionMin / 100.0) * dmi

                val minConstraint = model.addExpression("${ingredient.name}_Min_Inclusion")
                minConstraint.set(variable, 1.0)
                minConstraint.lower(minKg)

                logConstraint(ingredient.name, "Mínima", ingredient.inclusionMin, minKg)
            }

            // Aplicar restricción de inclusión máxima
            if (ingredient.inclusionMax < 100.0) {
                val maxKg = (ingredient.inclusionMax / 100.0) * dmi

                val maxConstraint = model.addExpression("${ingredient.name}_Max_Inclusion")
                maxConstraint.set(variable, 1.0)
                maxConstraint.upper(maxKg)

                logConstraint(ingredient.name, "Máxima", ingredient.inclusionMax, maxKg)
            }
        }

        // Log resumen
        val withConstraints = ingredients.count { it.hasCustomConstraints() }
        println("[${getName()}] Aplicado a $withConstraints ingredientes con restricciones personalizadas")
    }

    override fun getName(): String = "Inclusion Constraints"

    override fun getDescription(): String =
        "Aplica restricciones de inclusión mínima y máxima personalizadas para cada ingrediente"

    /**
     * Registra información sobre la restricción aplicada
     */
    private fun logConstraint(
        ingredientName: String,
        type: String,
        percentage: Double,
        kg: Double
    ) {
        println("  - $ingredientName: Inclusión $type = ${String.format("%.1f", percentage)}% (${String.format("%.2f", kg)} kg)")
    }
}