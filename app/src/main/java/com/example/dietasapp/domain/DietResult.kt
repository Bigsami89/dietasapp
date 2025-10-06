package com.example.dietasapp.domain

import org.ojalgo.optimisation.Optimisation

/**
 * Resultado de la optimización con ojAlgo
 * Corresponde a la clase "DietResult" del diagrama
 */
data class DietResult(
    val status: Optimisation.State,
    val composition: Map<Ingredient, Double>,  // Ingrediente -> cantidad (kg)
    val objectiveValue: Double,  // Valor de la función objetivo (costo o metano)
    val methaneGramsPerDay: Double = 0.0,
    val totalCost: Double = 0.0
) {
    /**
     * Verifica si la optimización fue exitosa
     */
    fun isOptimal(): Boolean {
        return status == Optimisation.State.OPTIMAL
    }

    /**
     * Verifica si la optimización es factible
     */
    fun isFeasible(): Boolean {
        return status == Optimisation.State.OPTIMAL ||
                status == Optimisation.State.FEASIBLE
    }

    /**
     * Obtiene mensaje de estado legible
     */
    fun getStatusMessage(): String {
        return when (status) {
            Optimisation.State.OPTIMAL -> "Solución óptima encontrada"
            Optimisation.State.FEASIBLE -> "Solución factible encontrada"
            Optimisation.State.INFEASIBLE -> "No existe solución factible"
            Optimisation.State.UNBOUNDED -> "Problema sin límite"
            else -> "Estado desconocido: $status"
        }
    }

    companion object {
        /**
         * Crea un DietResult de error
         */
        fun error(status: Optimisation.State): DietResult {
            return DietResult(
                status = status,
                composition = emptyMap(),
                objectiveValue = Double.MAX_VALUE
            )
        }
    }
}