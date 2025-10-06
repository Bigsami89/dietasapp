package com.example.dietasapp.calculation.components

import com.example.dietasapp.domain.AnimalProfile
import com.example.dietasapp.domain.Ingredient
import org.ojalgo.optimisation.ExpressionsBasedModel
import org.ojalgo.optimisation.Variable

/**
 * Componente que minimiza el costo total de la dieta
 *
 * Define la función objetivo como:
 * Minimizar: Σ (costo_ingrediente_i × cantidad_ingrediente_i)
 */
class CostComponent : IDietComponent {

    override fun apply(
        model: ExpressionsBasedModel,
        variables: Map<Ingredient, Variable>,
        profile: AnimalProfile,
        ingredients: List<Ingredient>
    ) {
        // Crear expresión para el costo total
        val costExpression = model.addExpression("Total_Cost")

        // Agregar cada ingrediente a la expresión con su costo como coeficiente
        variables.forEach { (ingredient, variable) ->
            costExpression.set(variable, ingredient.cost)
        }

        // Establecer como función objetivo a minimizar
        costExpression.weight(1.0)
        model.minimise()

        // Log para debugging (opcional)
        logComponentApplication(ingredients)
    }

    override fun getName(): String = "Cost Minimization"

    override fun getDescription(): String =
        "Minimiza el costo total de la dieta basado en el precio de cada ingrediente"

    /**
     * Registra información sobre la aplicación del componente
     */
    private fun logComponentApplication(ingredients: List<Ingredient>) {
        println("[${getName()}Name()] Aplicado con ${ingredients.size} ingredientes")
        println("  Función objetivo: MIN Σ (costo_i × kg_i)")
    }
}