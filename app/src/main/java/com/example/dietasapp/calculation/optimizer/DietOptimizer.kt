package com.example.dietasapp.calculation.optimizer

import com.example.dietasapp.calculation.components.IDietComponent
import com.example.dietasapp.domain.AnimalProfile
import com.example.dietasapp.domain.DietResult
import com.example.dietasapp.domain.Ingredient
import org.ojalgo.optimisation.ExpressionsBasedModel
import org.ojalgo.optimisation.Optimisation
import org.ojalgo.optimisation.Variable
import java.text.Normalizer

/**
 * Motor de optimización de dietas usando ojAlgo.
 * Implementa programación lineal para encontrar la dieta óptima.
 */
class DietOptimizer(
    private val components: List<IDietComponent>
) {

    fun optimize(
        profile: AnimalProfile,
        ingredients: List<Ingredient>
    ): DietResult {

        println("")
        println("========================================")
        println("Iniciando optimización de dieta")
        println("========================================")
        println("Animal: ${profile.name}")
        println("Peso: ${profile.bodyWeightKg} kg")
        println("DMI requerido: ${"%.3f".format(profile.dmiKgDay)} kg/día")

        // 1) Deduplicar ingredientes por nombre normalizado
        val uniqueIngredients = dedupByName(ingredients)
        if (uniqueIngredients.size != ingredients.size) {
            val duplicated = ingredients.groupBy { normalize(it.name) }
                .filter { it.value.size > 1 }
                .map { it.value[0].name to it.value.size }
            if (duplicated.isNotEmpty()) {
                println("⚠ Ingredientes duplicados por equals/hashCode / nombre normalizado:")
                duplicated.forEach { (n, c) -> println("   - $n: $c apariciones") }
            }
        }
        println("Ingredientes disponibles: ${uniqueIngredients.size}")
        println("Componentes a aplicar: ${components.size}")
        println("----------------------------------------")

        // 2) Modelo y variables
        val model = ExpressionsBasedModel()
        val variables = LinkedHashMap<Ingredient, Variable>()

        uniqueIngredients.forEachIndexed { idx, ing ->
            val varName = sanitizeName(ing.name) + "_$idx"
            // ⬇️ ojAlgo 53.x: crea la variable DIRECTO en el modelo
            val v = model.addVariable(varName)
                .lower(0.0)
                .upper(profile.dmiKgDay) // no puede superar el DMI total
            // .weight(0.0) // opcional, el objetivo lo ponen los componentes
            variables[ing] = v
        }

        println("✓ Creadas ${variables.size} variables de decisión")

        // 3) Aplicar componentes (restricciones + objetivo)
        println("")
        println("Aplicando componentes:")
        components.forEachIndexed { i, c ->
            println("")
            println("${i + 1}. ${c.getName()}")
            println("   ${c.getDescription()}")
            c.apply(model, variables, profile, uniqueIngredients)
            println("   ✓ Aplicado exitosamente")
        }

        // 4) Resolver
        println("")
        println("========================================")
        println("Resolviendo modelo de optimización...")
        println("========================================")
        val result: Optimisation.Result = model.minimise()
        val state = result.state
        println("Estado de la solución: $state")

        // 5) Resultado
        val composition = LinkedHashMap<Ingredient, Double>()
        variables.forEach { (ing, v) ->
            val qty = v.value?.toDouble() ?: 0.0
            if (qty > 1e-8) composition[ing] = qty
        }

        val objectiveValue = result.value.toDouble()
        val totalCost = composition.entries.sumOf { (ing, qty) -> ing.cost * qty }

        println("")
        println("========================================")
        println("RESULTADOS DE LA OPTIMIZACIÓN")
        println("========================================")
        when (state) {
            Optimisation.State.OPTIMAL, Optimisation.State.FEASIBLE -> {
                println("Estado: ${if (state == Optimisation.State.OPTIMAL) "Óptimo" else "Factible"}")
                println("Costo total (si aplica): ${"%.4f".format(totalCost)}")
                println("Composición (kg/día):")
                composition.entries.sortedByDescending { it.value }.forEachIndexed { i, (ing, qty) ->
                    println("  ${i + 1}. ${ing.name}: ${"%.3f".format(qty)} kg/d")
                }
            }
            else -> {
                println("Estado: No existe solución factible")
                runCatching { println(model) } // volcado para diagnóstico
            }
        }
        println("========================================")

        return DietResult(
            status = state,
            composition = composition.toMap(),
            objectiveValue = objectiveValue,
            methaneGramsPerDay = 0.0,
            totalCost = totalCost
        )
    }

    // ------------------------ helpers ------------------------

    private fun dedupByName(list: List<Ingredient>): List<Ingredient> {
        val seen = HashSet<String>()
        val out = ArrayList<Ingredient>(list.size)
        list.forEach {
            val key = normalize(it.name)
            if (seen.add(key)) out.add(it)
        }
        return out
    }

    private fun normalize(s: String): String {
        val tmp = Normalizer.normalize(s.trim(), Normalizer.Form.NFD)
        return tmp.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .lowercase()
    }

    private fun sanitizeName(s: String): String {
        val base = normalize(s)
        return base.replace("[^a-z0-9_]+".toRegex(), "_")
            .replace("_+".toRegex(), "_")
            .trim('_')
            .ifEmpty { "x" }
    }
}
