package com.example.dietasapp.domain

import java.io.Serializable

/**
 * Modelo de dominio para ingredientes usado en el optimizador
 * Actualizado para incluir restricciones de inclusión para rumiantes
 */
data class Ingredient(
    val name: String,
    val cost: Double,
    val nutrients: Map<String, Double>,
    val inclusionMin: Double = 0.0,  // Porcentaje mínimo en la ración
    val inclusionMax: Double = 100.0  // Porcentaje máximo en la ración
) : Serializable {

    /**
     * Obtiene un nutriente específico
     */
    fun getNutrient(nutrientName: String): Double {
        return nutrients[nutrientName] ?: 0.0
    }

    /**
     * Verifica si el ingrediente tiene restricciones personalizadas
     */
    fun hasCustomConstraints(): Boolean {
        return inclusionMin > 0.0 || inclusionMax < 100.0
    }

    /**
     * Valida que un porcentaje de inclusión esté dentro de los límites
     */
    fun isValidInclusion(percentage: Double): Boolean {
        return percentage >= inclusionMin && percentage <= inclusionMax
    }

    companion object {
        private const val serialVersionUID: Long = 1L

        /**
         * Convierte un Insumo (capa data) a Ingredient (capa domain)
         * Incluye las restricciones de inclusión
         */
        fun fromInsumo(insumo: com.example.dietasapp.data.Insumo): Ingredient {
            return Ingredient(
                name = insumo.nombre,
                cost = insumo.costo,
                nutrients = insumo.nutrientes,
                inclusionMin = insumo.inclusionMinima,
                inclusionMax = insumo.inclusionMaxima
            )
        }

        // Constantes de nutrientes para fácil acceso
        // Energía (MJ/kg MS)
        const val ENERGIA_METABOLIZABLE = "EM"
        const val ENERGIA_BRUTA = "EB"
        const val ENERGIA_NETA_MANTENIMIENTO = "ENm"

        // Composición (%)
        const val PROTEINA_CRUDA = "PC"
        const val FIBRA_DETERGENTE_NEUTRA = "FDN"
        const val FIBRA_DETERGENTE_ACIDA = "FDA"
        const val EXTRACTO_ETEREO = "EE"

        // Minerales
        const val CALCIO = "Ca"
        const val FOSFORO = "P"
        const val MAGNESIO = "Mg"
        const val SODIO = "Na"
        const val POTASIO = "K"
        const val AZUFRE = "S"
        const val COBRE = "Cu"
        const val ZINC = "Zn"
        const val SELENIO = "Se"
        const val COBALTO = "Co"

        // Otros
        const val DEGRADABILIDAD_RUMINAL = "DegRum"
        const val METANO_PRODUCIDO = "CH4"
    }
}