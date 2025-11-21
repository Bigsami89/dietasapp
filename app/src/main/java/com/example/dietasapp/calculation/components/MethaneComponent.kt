package com.example.dietasapp.calculation.components

import com.example.dietasapp.calculation.methane.MethaneCalculator
import com.example.dietasapp.data.Animal
import com.example.dietasapp.data.TipoDieta
import com.example.dietasapp.domain.Ingredient
import org.ojalgo.optimisation.ExpressionsBasedModel
import org.ojalgo.optimisation.Variable

/**
 * Componente que minimiza la producción de metano entérico
 *
 * Utiliza MethaneCalculator para estimar la producción de metano
 * y agrega restricciones al modelo
 *
 * NOTA: Este componente usa una aproximación lineal simplificada.
 * Para cálculos precisos, se debe ejecutar MethaneCalculator después
 * de obtener la dieta óptima.
 */
class MethaneComponent(
    private val calculator: MethaneCalculator = MethaneCalculator()
) : IDietComponent {

    override fun apply(
        model: ExpressionsBasedModel,
        variables: Map<Ingredient, Variable>,
        animal: Animal,
        ingredients: List<Ingredient>
    ) {
        // Usar el tipo de dieta definido en el animal
        val tipoDieta = animal.tipo

        // Crear expresión para minimizar metano
        val methaneExpression = model.addExpression("Total_Methane")

        // Calcular factor de metano para cada ingrediente
        // Esto es una aproximación lineal simplificada
        variables.forEach { (ingredient, variable) ->
            val methaneFactor = calculateMethaneFactorPerKg(
                ingredient = ingredient,
                bodyWeightKg = animal.pesoKg,
                tipoDieta = tipoDieta
            )

            // Agregar a la expresión de metano
            methaneExpression.set(variable, methaneFactor)
        }

        // Establecer como función objetivo a minimizar
        methaneExpression.weight(1.0)
        model.minimise()

        logComponentApplication(animal, ingredients.size)
    }

    override fun getName(): String = "Methane Minimization"

    override fun getDescription(): String =
        "Minimiza la producción de metano entérico según ecuaciones NASEM (2016)"

    /**
     * Calcula un factor de metano aproximado por kg de ingrediente
     *
     * IMPORTANTE: Esta es una aproximación lineal para el optimizador.
     * El cálculo final debe hacerse con MethaneCalculator sobre la dieta completa.
     */
    private fun calculateMethaneFactorPerKg(
        ingredient: Ingredient,
        bodyWeightKg: Double,
        tipoDieta: TipoDieta
    ): Double {
        // Si el ingrediente tiene CH4 calculado, usar ese valor
        val ch4Directo = ingredient.getNutrientOrNull(Animal.METANO_PRODUCIDO)
        if (ch4Directo != null && ch4Directo > 0.0) {
            return ch4Directo
        }

        // Factores base según tipo de forraje/concentrado
        val fdn = ingredient.getNutrient(Animal.FIBRA_DETERGENTE_NEUTRA)
        val almidon = ingredient.getNutrientOrNull("Almidon") ?: 0.0
        val ee = ingredient.getNutrient(Animal.EXTRACTO_ETEREO)

        // Factor base según ecuaciones NASEM simplificadas
        var methaneFactor = when {
            fdn > 40.0 -> 20.0      // Forraje produce más metano
            almidon > 60.0 -> 8.0   // Granos producen menos metano
            else -> 15.0            // Valor intermedio
        }

        // Ajustar por grasa (reduce metano)
        methaneFactor -= ee * 2.0

        // Ajustar por tipo de dieta
        methaneFactor *= when (tipoDieta) {
            TipoDieta.ALTO_FORRAJE -> 1.2
            TipoDieta.INTERMEDIO -> 1.0
            TipoDieta.BAJO_FORRAJE -> 0.8
        }

        // Asegurar que no sea negativo
        return methaneFactor.coerceAtLeast(0.0)
    }

    private fun logComponentApplication(animal: Animal, numIngredients: Int) {
        println("[${getName()}] Aplicado")
        println("  Animal: ${animal.nombre}")
        println("  Tipo de dieta: ${animal.tipo.descripcion}")
        println("  Peso: ${animal.pesoKg} kg")
        println("  Ingredientes evaluados: $numIngredients")
        println("  Función objetivo: MIN Σ (factor_CH4_i × kg_i)")
    }

    /**
     * Método para calcular metano preciso después de la optimización
     * Este debe ser llamado externamente con la dieta final
     */
    fun calculatePreciseMethane(
        dietComposition: Map<Ingredient, Double>,
        animal: Animal
    ): MethaneCalculator.MethaneResult {
        return calculator.calculate(
            dietComposition = dietComposition,
            bodyWeightKg = animal.pesoKg,
            dmiKgDay = animal.consumoDMI,
            tipoDieta = animal.tipo
        )
    }
}