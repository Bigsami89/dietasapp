package com.example.dietasapp.calculation.components

import com.example.dietasapp.calculation.methane.MethaneCalculator
import com.example.dietasapp.data.TipoDieta
import com.example.dietasapp.domain.AnimalProfile
import com.example.dietasapp.domain.Ingredient
import org.ojalgo.optimisation.ExpressionsBasedModel
import org.ojalgo.optimisation.Variable

/**
 * Componente que minimiza la producción de metano entérico
 *
 * Utiliza MethaneCalculator para estimar la producción de metano
 * de cada ingrediente y agrega restricciones al modelo
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
        profile: AnimalProfile,
        ingredients: List<Ingredient>
    ) {
        // Determinar tipo de dieta basado en perfil
        val tipoDieta = determineDietType(ingredients, variables)

        // Crear expresión para minimizar metano
        val methaneExpression = model.addExpression("Total_Methane")

        // Calcular factor de metano para cada ingrediente
        // Esto es una aproximación lineal simplificada
        variables.forEach { (ingredient, variable) ->
            val methaneFactor = calculateMethaneFactorPerKg(
                ingredient = ingredient,
                bodyWeightKg = profile.bodyWeightKg,
                tipoDieta = tipoDieta
            )

            // Agregar a la expresión de metano
            methaneExpression.set(variable, methaneFactor)
        }

        // Establecer como función objetivo a minimizar
        methaneExpression.weight(1.0)
        model.minimise()

        logComponentApplication(tipoDieta, ingredients.size)
    }

    override fun getName(): String = "Methane Minimization"

    override fun getDescription(): String =
        "Minimiza la producción de metano entérico según ecuaciones NASEM (2016)"

    /**
     * Determina el tipo de dieta basado en los ingredientes disponibles
     */
    private fun determineDietType(
        ingredients: List<Ingredient>,
        variables: Map<Ingredient, Variable>
    ): TipoDieta {
        // Contar ingredientes con alto contenido de NDF (forrajes)
        val forageCount = ingredients.count {
            it.getNutrient("NDF") > 40.0
        }

        val foragePercentage = (forageCount.toDouble() / ingredients.size) * 100.0

        return TipoDieta.fromForagePercentage(foragePercentage)
    }

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
        // Factores base según tipo de forraje/concentrado
        val ndf = ingredient.getNutrient("NDF")
        val starch = ingredient.getNutrient("Starch")
        val fat = ingredient.getNutrient("Fat")

        // Factor base según ecuaciones NASEM simplificadas
        var methaneFactor = when {
            ndf > 40.0 -> 20.0  // Forraje produce más metano
            starch > 60.0 -> 8.0  // Granos producen menos metano
            else -> 15.0  // Valor intermedio
        }

        // Ajustar por grasa (reduce metano)
        methaneFactor -= fat * 2.0

        // Ajustar por tipo de dieta
        methaneFactor *= when (tipoDieta) {
            TipoDieta.ALTO_FORRAJE -> 1.2
            TipoDieta.INTERMEDIO -> 1.0
            TipoDieta.BAJO_FORRAJE -> 0.8
        }

        // Asegurar que no sea negativo
        return methaneFactor.coerceAtLeast(0.0)
    }

    private fun logComponentApplication(tipoDieta: TipoDieta, numIngredients: Int) {
        println("[${getName()}()] Aplicado")
        println("  Tipo de dieta estimado: ${tipoDieta.descripcion}")
        println("  Ingredientes evaluados: $numIngredients")
        println("  Función objetivo: MIN Σ (factor_CH4_i × kg_i)")
    }

    /**
     * Método para calcular metano preciso después de la optimización
     * Este debe ser llamado externamente con la dieta final
     */
    fun calculatePreciseMethane(
        dietComposition: Map<Ingredient, Double>,
        bodyWeightKg: Double,
        dmiKgDay: Double,
        tipoDieta: TipoDieta
    ): MethaneCalculator.MethaneResult {
        return calculator.calculate(
            dietComposition = dietComposition,
            bodyWeightKg = bodyWeightKg,
            dmiKgDay = dmiKgDay,
            tipoDieta = tipoDieta
        )
    }
}