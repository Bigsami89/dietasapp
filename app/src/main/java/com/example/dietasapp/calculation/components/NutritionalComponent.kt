package com.example.dietasapp.calculation.components

import com.example.dietasapp.domain.AnimalProfile
import com.example.dietasapp.domain.Ingredient
import org.ojalgo.optimisation.ExpressionsBasedModel
import org.ojalgo.optimisation.Variable
import kotlin.math.abs

/**
 * Componente que agrega restricciones nutricionales al modelo.
 *
 * Aplica:
 *  - Igualdad de DMI (suma de kg de ingredientes == DMI requerido)
 *  - Requerimientos mínimos de nutrientes (min_*)
 *  - Límites máximos de nutrientes (max_*)
 *
 * CONVENCIÓN DE UNIDADES:
 *  - Nutrientes en % de la ración (CP, NDF, Ca, P, Starch, Fat, TDN):
 *      coeficiente = (%/100) [kg nutriente / kg ingrediente]
 *      RHS (min/max) = % * DMI  [kg nutriente / día]
 *  - Energía (NEm, NEg, GE) en Mcal/kg:
 *      coeficiente = valor tal cual [Mcal/kg]
 *      RHS (min/max) = valor * DMI [Mcal/día]
 */
class NutritionalComponent : IDietComponent {

    private val PERCENT_NUTRIENTS = setOf("CP", "NDF", "Ca", "P", "Starch", "Fat", "TDN")
    private val ENERGY_NUTRIENTS  = setOf("NEm", "NEg", "GE")

    override fun apply(
        model: ExpressionsBasedModel,
        variables: Map<Ingredient, Variable>,
        profile: AnimalProfile,
        ingredients: List<Ingredient>
    ) {
        // 1) Igualdad de DMI
        val dmi = profile.dmiKgDay
        val dmiExpr = model.addExpression("DMI_Total")
        variables.forEach { (_, v) -> dmiExpr.set(v, 1.0) }
        dmiExpr.lower(dmi)
        dmiExpr.upper(dmi)
        println("  → DMI requerido: ${"%.3f".format(dmi)} kg/día")

        // 2) Mínimos (min_*)
        profile.requirements
            .filterKeys { it.startsWith("min_") }
            .forEach { (key, raw) ->
                val nutrient = key.removePrefix("min_")
                val expr = model.addExpression("Min_$nutrient")

                variables.forEach { (ingredient, variable) ->
                    val coef = coeffOf(ingredient, nutrient)
                    if (abs(coef) > 0.0) expr.set(variable, coef)
                }

                val rhs = rhsMinFor(nutrient, raw, dmi)
                expr.lower(rhs)

                if (nutrient in ENERGY_NUTRIENTS) {
                    println("  → $nutrient mínimo: ${raw} Mcal/kg ⇒ ≥ ${"%.3f".format(rhs)} Mcal/d")
                } else {
                    println("  → $nutrient mínimo: ${raw}% ⇒ ≥ ${"%.3f".format(rhs)} kg")
                }
            }

        // 3) Máximos (max_*)
        profile.requirements
            .filterKeys { it.startsWith("max_") }
            .forEach { (key, raw) ->
                val nutrient = key.removePrefix("max_")
                val expr = model.addExpression("Max_$nutrient")

                variables.forEach { (ingredient, variable) ->
                    val coef = coeffOf(ingredient, nutrient)
                    if (abs(coef) > 0.0) expr.set(variable, coef)
                }

                val rhs = rhsMaxFor(nutrient, raw, dmi)
                expr.upper(rhs)

                if (nutrient in ENERGY_NUTRIENTS) {
                    println("  → $nutrient máximo: ${raw} Mcal/kg ⇒ ≤ ${"%.3f".format(rhs)} Mcal/d")
                } else {
                    println("  → $nutrient máximo: ${raw}% ⇒ ≤ ${"%.3f".format(rhs)} kg")
                }
            }

        logComponentApplication(profile)
    }

    /** Coeficiente lineal del nutriente en la fila: kg nutriente/kg ingrediente o Mcal/kg */
    private fun coeffOf(ingredient: Ingredient, nutrient: String): Double {
        val v = ingredient.nutrients[nutrient] ?: return 0.0
        return when {
            nutrient in PERCENT_NUTRIENTS -> v / 100.0   // % → fracción (kg/kg)
            nutrient in ENERGY_NUTRIENTS  -> v           // Mcal/kg (tal cual)
            else                          -> v
        }
    }

    /** RHS mínimo en unidades del LHS */
    private fun rhsMinFor(nutrient: String, raw: Double, dmi: Double): Double =
        when {
            nutrient in PERCENT_NUTRIENTS -> dmi * (raw / 100.0) // % → kg/día
            nutrient in ENERGY_NUTRIENTS  -> dmi * raw           // Mcal/kg → Mcal/d
            else                          -> raw
        }

    /** RHS máximo en unidades del LHS */
    private fun rhsMaxFor(nutrient: String, raw: Double, dmi: Double): Double =
        when {
            nutrient in PERCENT_NUTRIENTS -> dmi * (raw / 100.0)
            nutrient in ENERGY_NUTRIENTS  -> dmi * raw
            else                          -> raw
        }

    override fun getName(): String = "Nutritional Constraints"

    override fun getDescription(): String =
        "Aplica restricciones nutricionales basadas en los requerimientos del animal"

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
