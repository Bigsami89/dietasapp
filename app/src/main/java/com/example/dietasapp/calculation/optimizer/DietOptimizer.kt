package com.example.dietasapp.calculation.optimizer

import com.example.dietasapp.calculation.components.IDietComponent
import com.example.dietasapp.domain.AnimalProfile
import com.example.dietasapp.domain.DietResult
import com.example.dietasapp.domain.Ingredient
import org.ojalgo.optimisation.ExpressionsBasedModel
import org.ojalgo.optimisation.Optimisation
import org.ojalgo.optimisation.Variable

/**
 * Motor de optimización de dietas usando ojAlgo
 * Implementa programación lineal para encontrar la dieta óptima
 *
 * Corresponde a la clase "DietOptimizer" del diagrama
 */
class DietOptimizer(
    private val components: List<IDietComponent>
) {

    /**
     * Optimiza la dieta para un animal con los ingredientes disponibles
     *
     * @param profile Perfil del animal con requerimientos nutricionales
     * @param ingredients Lista de ingredientes disponibles
     * @return Resultado de la optimización con composición de la dieta
     */
    fun optimize(
        profile: AnimalProfile,
        ingredients: List<Ingredient>
    ): DietResult {

        println("\n========================================")
        println("Iniciando optimización de dieta")
        println("========================================")
        println("Animal: ${profile.name}")
        println("Peso: ${profile.bodyWeightKg} kg")
        println("DMI requerido: ${profile.dmiKgDay} kg/día")
        println("Ingredientes disponibles: ${ingredients.size}")
        println("Componentes a aplicar: ${components.size}")
        println("----------------------------------------")

        try {
            // 1. Crear modelo de ojAlgo
            val model = ExpressionsBasedModel()

            // 2. Crear variables de decisión (una por ingrediente)
            val variables = createDecisionVariables(model, ingredients)

            // 3. Aplicar todos los componentes (objetivos y restricciones)
            applyComponents(model, variables, profile, ingredients)

            // 4. Resolver el modelo
            val result = solveModel(model, variables, ingredients)

            // 5. Mostrar resultados
            logOptimizationResult(result)

            return result

        } catch (e: Exception) {
            println("ERROR durante la optimización: ${e.message}")
            e.printStackTrace()
            return DietResult.error(Optimisation.State.FAILED)
        }
    }

    /**
     * Crea variables de decisión para cada ingrediente
     * Cada variable representa los kg/día de ese ingrediente en la dieta
     */
    private fun createDecisionVariables(
        model: ExpressionsBasedModel,
        ingredients: List<Ingredient>
    ): Map<Ingredient, Variable> {

        val variables = mutableMapOf<Ingredient, Variable>()

        ingredients.forEach { ingredient ->
            // Crear variable con límites
            // Límite inferior: 0 kg (no puede ser negativo)
            // Límite superior: sin restricción explícita (será limitado por DMI)
            val variable = Variable.make(ingredient.name)
                .lower(0.0)  // No puede haber cantidades negativas

            variables[ingredient] = variable
            model.addVariable(variable)
        }

        println("✓ Creadas ${variables.size} variables de decisión")
        return variables
    }

    /**
     * Aplica todos los componentes (Strategy Pattern)
     * Cada componente agrega sus restricciones u objetivos al modelo
     */
    private fun applyComponents(
        model: ExpressionsBasedModel,
        variables: Map<Ingredient, Variable>,
        profile: AnimalProfile,
        ingredients: List<Ingredient>
    ) {
        println("\nAplicando componentes:")

        components.forEachIndexed { index, component ->
            println("\n${index + 1}. ${component.getName()}")
            println("   ${component.getDescription()}")

            try {
                component.apply(model, variables, profile, ingredients)
                println("   ✓ Aplicado exitosamente")
            } catch (e: Exception) {
                println("   ✗ Error al aplicar: ${e.message}")
                throw e
            }
        }
    }

    /**
     * Resuelve el modelo de optimización
     */
    private fun solveModel(
        model: ExpressionsBasedModel,
        variables: Map<Ingredient, Variable>,
        ingredients: List<Ingredient>
    ): DietResult {

        println("\n========================================")
        println("Resolviendo modelo de optimización...")
        println("========================================")

        // Resolver el modelo
        val result = model.minimise()

        // Verificar estado de la solución
        val state = result.state
        println("Estado de la solución: $state")

        if (!result.state.isOptimal && !result.state.isFeasible) {
            println("⚠ No se encontró solución factible")
            return DietResult.error(state)
        }

        // Extraer composición de la dieta
        val composition = extractComposition(result, variables)

        // Calcular valor objetivo y otros valores
        val objectiveValue = result.value
        val totalCost = calculateTotalCost(composition)

        return DietResult(
            status = state,
            composition = composition,
            objectiveValue = objectiveValue,
            totalCost = totalCost,
            methaneGramsPerDay = 0.0  // Se calculará después con MethaneCalculator
        )
    }

    /**
     * Extrae la composición de la dieta del resultado de ojAlgo
     */
    private fun extractComposition(
        result: Optimisation.Result,
        variables: Map<Ingredient, Variable>
    ): Map<Ingredient, Double> {

        val composition = mutableMapOf<Ingredient, Double>()

        variables.forEach { (ingredient, variable) ->
            val amount = variable.value.toDouble()

            // Solo incluir ingredientes con cantidad significativa (>0.001 kg)
            if (amount > 0.001) {
                composition[ingredient] = amount
            }
        }

        return composition
    }

    /**
     * Calcula el costo total de la dieta
     */
    private fun calculateTotalCost(composition: Map<Ingredient, Double>): Double {
        return composition.entries.sumOf { (ingredient, kg) ->
            ingredient.cost * kg
        }
    }

    /**
     * Registra los resultados de la optimización
     */
    private fun logOptimizationResult(result: DietResult) {
        println("\n========================================")
        println("RESULTADOS DE LA OPTIMIZACIÓN")
        println("========================================")
        println("Estado: ${result.getStatusMessage()}")

        if (result.isFeasible()) {
            println("Valor objetivo: ${String.format("%.4f", result.objectiveValue)}")
            println("Costo total: $${String.format("%.2f", result.totalCost)}/día")
            println("\nComposición de la dieta:")
            println("----------------------------------------")

            val totalKg = result.composition.values.sum()

            result.composition.entries
                .sortedByDescending { it.value }
                .forEach { (ingredient, kg) ->
                    val percentage = (kg / totalKg) * 100.0
                    println("  ${ingredient.name}:")
                    println("    Cantidad: ${String.format("%.3f", kg)} kg/día")
                    println("    Porcentaje: ${String.format("%.1f", percentage)}%")
                    println("    Costo: $${String.format("%.2f", ingredient.cost * kg)}/día")
                }

            println("----------------------------------------")
            println("Total MS: ${String.format("%.3f", totalKg)} kg/día")

        } else {
            println("⚠ No se pudo encontrar una dieta factible")
            println("Posibles razones:")
            println("  - Restricciones nutricionales demasiado estrictas")
            println("  - Ingredientes insuficientes")
            println("  - Requerimientos incompatibles")
        }
        println("========================================\n")
    }
}