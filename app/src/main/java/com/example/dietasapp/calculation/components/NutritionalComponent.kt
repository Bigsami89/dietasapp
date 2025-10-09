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
        // 1) Suma de mezcla = DMI (esto NO convierte unidades, solo fija masa total)
        val dmi = profile.dmiKgDay
        val dmiExpr = model.addExpression("DMI_Total")
        variables.forEach { (_, v) -> dmiExpr.set(v, 1.0) }
        dmiExpr.level(dmi)
        println("  → DMI requerido (sin conversiones de nutrientes): ${"%.3f".format(dmi)} kg/día")

        val PERCENT_NUTRIENTS = setOf("CP", "NDF", "Ca", "P", "Starch", "Fat", "TDN")
        val ENERGY_NUTRIENTS  = setOf("NEm", "NEg", "GE")

        fun valueOf(ing: Ingredient, k: String) = ing.nutrients[k] ?: 0.0

        // 2) Mínimos (como promedio ponderado del mix, sin conversiones)
        profile.requirements
            .filterKeys { it.startsWith("min_") }
            .forEach { (key, raw) ->
                val nutrient = key.removePrefix("min_")
                if (nutrient in PERCENT_NUTRIENTS || nutrient in ENERGY_NUTRIENTS) {
                    val expr = model.addExpression("Min_$nutrient")
                    variables.forEach { (ingredient, v) ->
                        val vi = valueOf(ingredient, nutrient) // % o Mcal/kg (nativo)
                        val coef = vi - raw                     // lineal, sin /100 ni ×DMI
                        if (abs(coef) > 0.0) expr.set(v, coef)
                    }
                    expr.lower(0.0)
                    val unidad = if (nutrient in PERCENT_NUTRIENTS) "%" else "Mcal/kg"
                    println("  → $nutrient mínimo (sin conversiones): ${raw} $unidad (promedio del mix)")
                }
            }

        // 3) Máximos (promedio ponderado del mix, sin conversiones)
        profile.requirements
            .filterKeys { it.startsWith("max_") }
            .forEach { (key, raw) ->
                val nutrient = key.removePrefix("max_")
                if (nutrient in PERCENT_NUTRIENTS || nutrient in ENERGY_NUTRIENTS) {
                    val expr = model.addExpression("Max_$nutrient")
                    variables.forEach { (ingredient, v) ->
                        val vi = valueOf(ingredient, nutrient) // % o Mcal/kg (nativo)
                        val coef = vi - raw
                        if (abs(coef) > 0.0) expr.set(v, coef)
                    }
                    expr.upper(0.0)
                    val unidad = if (nutrient in PERCENT_NUTRIENTS) "%" else "Mcal/kg"
                    println("  → $nutrient máximo (sin conversiones): ${raw} $unidad (promedio del mix)")
                }
            }

        println("[${getName()}] Aplicado SIN conversiones de unidades (todo por kg y en unidades nativas).")
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
