// DietOptimizer.kt
package com.example.dietasapp.calculation.optimizer

import android.util.Log
import com.example.dietasapp.calculation.components.IDietComponent
import com.example.dietasapp.data.Animal
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

    companion object {
        private const val TAG = "DietOptimizer"
    }

    private fun logI(msg: String) { Log.i(TAG, msg); println(msg) }
    private fun logW(msg: String) { Log.w(TAG, msg); println("W: $msg") }
    private fun logE(msg: String) { Log.e(TAG, msg); println("E: $msg") }

    /**
     * Optimiza la dieta para un animal específico
     *
     * @param animal Animal con sus requerimientos nutricionales
     * @param ingredients Lista de ingredientes disponibles
     * @return Resultado de la optimización con la composición de la dieta
     */
    fun optimize(
        animal: Animal,
        ingredients: List<Ingredient>
    ): DietResult {

        logI("")
        logI("========================================")
        logI("Iniciando optimización de dieta")
        logI("========================================")
        logI("Animal: ${animal.nombre}")
        logI("ID: ${animal.id}")
        logI("Tipo de dieta: ${animal.tipo.descripcion}")
        logI("Peso: ${animal.pesoKg} kg")
        logI("DMI requerido: ${"%.3f".format(animal.consumoDMI)} kg/día")

        // 1) Deduplicar ingredientes por nombre normalizado
        val uniqueIngredients = dedupByName(ingredients)
        if (uniqueIngredients.size != ingredients.size) {
            val duplicated = ingredients.groupBy { normalize(it.name) }
                .filter { it.value.size > 1 }
                .map { it.value[0].name to it.value.size }
            if (duplicated.isNotEmpty()) {
                logW("Ingredientes duplicados por equals/hashCode / nombre normalizado:")
                duplicated.forEach { (n, c) -> logW("   - $n: $c apariciones") }
            }
        }
        logI("Ingredientes disponibles: ${uniqueIngredients.size}")
        logI("Componentes a aplicar: ${components.size}")

        // Log de requerimientos
        logI("")
        logI("Requerimientos nutricionales:")
        logI("  Mínimos (${animal.requerimientosMinimos.size}):")
        animal.requerimientosMinimos.forEach { (k, v) ->
            logI("    - $k: ${"%.2f".format(v)}")
        }
        logI("  Máximos (${animal.requerimientosMaximos.size}):")
        animal.requerimientosMaximos.forEach { (k, v) ->
            logI("    - $k: ${"%.2f".format(v)}")
        }
        logI("----------------------------------------")

        // 2) Modelo y variables
        val model = ExpressionsBasedModel()
        val variables = LinkedHashMap<Ingredient, Variable>()

        uniqueIngredients.forEachIndexed { idx, ing ->
            val varName = sanitizeName(ing.name) + "_$idx"
            val v = model.addVariable(varName)
                .lower(0.0)
                .upper(animal.consumoDMI) // límite superior: no puede superar DMI total

            // Traza de cada variable creada
            logI("Var[$idx] ${v.name}  bounds=[0.0, ${"%.3f".format(animal.consumoDMI)}]  cost=${"%.4f".format(ing.cost)}")
            variables[ing] = v
        }
        logI("✓ Creadas ${variables.size} variables de decisión")

        // 3) Aplicar componentes (restricciones + objetivo)
        logI("")
        logI("Aplicando componentes:")
        components.forEachIndexed { i, c ->
            logI("")
            logI("${i + 1}. ${c.getName()}")
            logI("   ${c.getDescription()}")
            try {
                c.apply(model, variables, animal, uniqueIngredients)
                logI("   ✓ Aplicado exitosamente")
            } catch (t: Throwable) {
                logE("   ✗ Falló al aplicar componente '${c.getName()}': ${t.message}")
                t.stackTrace.forEach { st -> logE("     at $st") }
            }
        }

        // 4) Resolver
        logI("")
        logI("========================================")
        logI("Resolviendo modelo de optimización...")
        logI("========================================")
        val result: Optimisation.Result = try {
            model.minimise()
        } catch (t: Throwable) {
            logE("Excepción al resolver el modelo: ${t.message}")
            t.stackTrace.forEach { st -> logE("  at $st") }
            // Resultado sintético no factible
            return DietResult(
                status = Optimisation.State.FAILED,
                composition = emptyMap(),
                objectiveValue = Double.NaN,
                methaneGramsPerDay = 0.0,
                totalCost = 0.0
            )
        }
        val state = result.state
        logI("Estado de la solución: $state")

        // Validación y diagnóstico básico del modelo cuando NO hay solución
        if (state !in listOf(Optimisation.State.OPTIMAL, Optimisation.State.FEASIBLE)) {
            val valid = model.validate()
            logW("Validación del modelo: ${if (valid) "OK" else "NO VÁLIDO"}")
            logW("Dump del modelo (constraints/objetivo) ↓")
            logW(model.toString())
        }

        // 5) Resultado
        val composition = LinkedHashMap<Ingredient, Double>()
        variables.forEach { (ing, v) ->
            val qty = v.value?.toDouble() ?: 0.0
            if (qty > 1e-8) composition[ing] = qty
        }

        val objectiveValue = result.value.toDouble()
        val totalCost = composition.entries.sumOf { (ing, qty) -> ing.cost * qty }
        val totalDMI = composition.values.sum()

        logI("")
        logI("========================================")
        logI("RESULTADOS DE LA OPTIMIZACIÓN")
        logI("========================================")
        when (state) {
            Optimisation.State.OPTIMAL, Optimisation.State.FEASIBLE -> {
                logI("Estado: ${if (state == Optimisation.State.OPTIMAL) "Óptimo" else "Factible"}")
                logI("Valor objetivo: ${"%.4f".format(objectiveValue)}")
                logI("Costo total: $${"%.4f".format(totalCost)}/día")
                logI("DMI total: ${"%.3f".format(totalDMI)} kg/día (requerido: ${"%.3f".format(animal.consumoDMI)})")
                logI("")
                logI("Composición de la dieta (kg/día):")
                composition.entries.sortedByDescending { it.value }.forEachIndexed { i, (ing, qty) ->
                    val pct = (qty / totalDMI) * 100.0
                    logI("  ${i + 1}. ${ing.name.padEnd(30)} ${"%.3f".format(qty)} kg/d (${"%.1f".format(pct)}%)")
                }

                // Resumen nutricional
                logI("")
                logI("Resumen nutricional (promedio ponderado):")
                summarizeNutrition(composition, totalDMI, animal)
            }
            else -> {
                logE("Estado: No existe solución factible")
                logE("Posibles causas:")
                logE("  - Restricciones conflictivas")
                logE("  - Requerimientos imposibles de cumplir con los ingredientes disponibles")
                logE("  - Límites de inclusión muy restrictivos")
                runCatching { logW(model.toString()) }
            }
        }
        logI("========================================")

        return DietResult(
            status = state,
            composition = composition.toMap(),
            objectiveValue = objectiveValue,
            methaneGramsPerDay = 0.0,
            totalCost = totalCost
        )
    }

    // ------------------------ helpers ------------------------

    /**
     * Deduplica ingredientes por nombre normalizado
     */
    private fun dedupByName(list: List<Ingredient>): List<Ingredient> {
        val seen = HashSet<String>()
        val out = ArrayList<Ingredient>(list.size)
        list.forEach {
            val key = normalize(it.name)
            if (seen.add(key)) out.add(it)
        }
        return out
    }

    /**
     * Normaliza un string removiendo acentos y convirtiendo a minúsculas
     */
    private fun normalize(s: String): String {
        val tmp = Normalizer.normalize(s.trim(), Normalizer.Form.NFD)
        return tmp.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .lowercase()
    }

    /**
     * Sanitiza un nombre para usarlo como nombre de variable en ojAlgo
     */
    private fun sanitizeName(s: String): String {
        val base = normalize(s)
        return base.replace("[^a-z0-9_]+".toRegex(), "_")
            .replace("_+".toRegex(), "_")
            .trim('_')
            .ifEmpty { "x" }
    }

    /**
     * Imprime un resumen nutricional de la dieta
     */
    private fun summarizeNutrition(
        composition: Map<Ingredient, Double>,
        totalDMI: Double,
        animal: Animal
    ) {
        val nutrients = listOf(
            Animal.PROTEINA_CRUDA to "PC",
            Animal.FIBRA_DETERGENTE_NEUTRA to "FDN",
            Animal.EXTRACTO_ETEREO to "EE",
            Animal.CALCIO to "Ca",
            Animal.FOSFORO to "P"
        )

        nutrients.forEach { (key, label) ->
            val weightedSum = composition.entries.sumOf { (ing, kg) ->
                ing.getNutrient(key) * kg
            }
            val average = weightedSum / totalDMI

            // Verificar vs requerimientos
            val minReq = animal.requerimientosMinimos[key]
            val maxReq = animal.requerimientosMaximos[key]

            val status = when {
                minReq != null && average < minReq -> "⚠ BAJO (min: ${"%.2f".format(minReq)})"
                maxReq != null && average > maxReq -> "⚠ ALTO (max: ${"%.2f".format(maxReq)})"
                else -> "✓"
            }

            logI("  ${label.padEnd(5)}: ${"%.2f".format(average)}  $status")
        }
    }
}