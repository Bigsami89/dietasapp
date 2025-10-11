package com.example.dietasapp.domain

import java.io.Serializable

/**
 * Representa una dieta calculada/formulada
 */
data class Dieta(
    val animal: com.example.dietasapp.data.Animal,
    val composicion: Map<String, Double>,  // nombre ingrediente -> kg/día
    val costoTotal: Double,
    val metanoProducidoGramos: Double,
    val nutrientesTotales: Map<String, Double>,
    val fechaCreacion: Long = System.currentTimeMillis(),
    val observaciones: String = "",
    val ingredientesUsados: List<Ingredient> = emptyList() // Lista de ingredientes completos
) : Serializable {

    /**
     * Calcula el costo por kg de materia seca
     */
    fun costoPorKgMS(): Double {
        val totalKg = composicion.values.sum()
        return if (totalKg > 0) costoTotal / totalKg else 0.0
    }

    /**
     * Calcula el metano por kg de DMI
     */
    fun metanoPorKgDMI(): Double {
        return metanoProducidoGramos / animal.consumoDMI
    }

    /**
     * Calcula el porcentaje de forraje en la dieta
     */
    fun porcentajeForraje(insumos: List<com.example.dietasapp.data.Insumo>): Double {
        var totalForraje = 0.0
        var totalDieta = 0.0

        composicion.forEach { (nombreInsumo, cantidad) ->
            val insumo = insumos.find { it.nombre == nombreInsumo }
            totalDieta += cantidad
            if (insumo?.esForraje == true) {
                totalForraje += cantidad
            }
        }

        return if (totalDieta > 0) (totalForraje / totalDieta) * 100 else 0.0
    }
}