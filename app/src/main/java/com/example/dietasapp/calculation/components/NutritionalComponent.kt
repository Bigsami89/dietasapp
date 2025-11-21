package com.example.dietasapp.calculation.components

import com.example.dietasapp.data.Animal
import com.example.dietasapp.domain.Ingredient
import org.ojalgo.optimisation.ExpressionsBasedModel
import org.ojalgo.optimisation.Variable
import kotlin.math.abs

/**
 * Componente que agrega restricciones nutricionales al modelo.
 *
 * Aplica:
 *  - Igualdad de DMI (suma de kg de ingredientes == DMI requerido)
 *  - Requerimientos mínimos de nutrientes (requerimientosMinimos)
 *  - Límites máximos de nutrientes (requerimientosMaximos)
 *
 * CONVENCIÓN DE UNIDADES:
 *  - Nutrientes en % de la ración (PC, FDN, EE, Ca, P, etc):
 *      coeficiente = valor nativo del ingrediente
 *      RHS = valor requerido (mismo formato)
 *  - Energía (ENm, ENg, etc) en Mcal/kg:
 *      coeficiente = valor tal cual [Mcal/kg]
 *      RHS = valor requerido
 */
class NutritionalComponent : IDietComponent {

    private val PERCENT_NUTRIENTS = setOf(
        Animal.PROTEINA_CRUDA,              // PC
        Animal.FIBRA_DETERGENTE_NEUTRA,     // FDN
        Animal.FIBRA_DETERGENTE_ACIDA,      // FDA
        Animal.EXTRACTO_ETEREO,             // EE
        Animal.CENIZAS,                     // Cenizas
        Animal.CALCIO,                      // Ca
        Animal.FOSFORO                      // P
    )



    override fun apply(
        model: ExpressionsBasedModel,
        variables: Map<Ingredient, Variable>,
        animal: Animal,
        ingredients: List<Ingredient>
    ) {
        // 1) Restricción DMI: suma de ingredientes = consumo requerido
        val dmi = animal.consumoDMI
        val dmiExpr = model.addExpression("DMI_Total")
        variables.forEach { (_, v) -> dmiExpr.set(v, 1.0) }
        dmiExpr.level(dmi)
        println("  → DMI requerido: ${"%.3f".format(dmi)} kg/día")

        // 2) Restricciones mínimas
        animal.requerimientosMinimos.forEach { (nutrient, minValue) ->
            if (nutrient in PERCENT_NUTRIENTS ) {
                val expr = model.addExpression("Min_$nutrient")

                variables.forEach { (ingredient, v) ->
                    val nutrientValue = ingredient.getNutrient(nutrient)
                    val coef = nutrientValue - minValue

                    if (abs(coef) > 0.0) {
                        expr.set(v, coef)
                    }
                }

                expr.lower(0.0)

                val unidad = if (nutrient in PERCENT_NUTRIENTS) "%" else "Mcal/kg"
                println("  → $nutrient mínimo: $minValue $unidad (promedio del mix)")
            }
        }

        // 3) Restricciones máximas
        animal.requerimientosMaximos.forEach { (nutrient, maxValue) ->
            if (nutrient in PERCENT_NUTRIENTS ) {
                val expr = model.addExpression("Max_$nutrient")

                variables.forEach { (ingredient, v) ->
                    val nutrientValue = ingredient.getNutrient(nutrient)
                    val coef = nutrientValue - maxValue

                    if (abs(coef) > 0.0) {
                        expr.set(v, coef)
                    }
                }

                expr.upper(0.0)

                val unidad = if (nutrient in PERCENT_NUTRIENTS) "%" else "Mcal/kg"
                println("  → $nutrient máximo: $maxValue $unidad (promedio del mix)")
            }
        }

        logComponentApplication(animal)
    }

    override fun getName(): String = "Nutritional Constraints"

    override fun getDescription(): String =
        "Aplica restricciones nutricionales basadas en los requerimientos del animal"

    private fun logComponentApplication(animal: Animal) {
        println("[${getName()}] Aplicado")
        println("  Animal: ${animal.nombre}")
        println("  Tipo: ${animal.tipo.descripcion}")
        println("  Peso: ${animal.pesoKg} kg")
        println("  Restricciones mínimas: ${animal.requerimientosMinimos.size}")
        println("  Restricciones máximas: ${animal.requerimientosMaximos.size}")
    }
}