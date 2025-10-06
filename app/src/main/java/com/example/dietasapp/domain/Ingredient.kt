package com.example.dietasapp.domain

/**
 * Modelo de dominio para ingredientes usado en el optimizador
 * Corresponde a la clase "Ingredient" del diagrama de ojAlgo
 */
data class Ingredient(
    val name: String,
    val cost: Double,
    val nutrients: Map<String, Double>
) {
    /**
     * Obtiene un nutriente específico
     */
    fun getNutrient(nutrientName: String): Double {
        return nutrients[nutrientName] ?: 0.0
    }

    companion object {
        /**
         * Convierte un Insumo (capa data) a Ingredient (capa domain)
         */
        fun fromInsumo(insumo: com.example.dietasapp.data.Insumo): Ingredient {
            return Ingredient(
                name = insumo.nombre,
                cost = insumo.costo,
                nutrients = insumo.nutrientes
            )
        }
    }
}