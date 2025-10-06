package com.example.dietasapp.data

/**
 * Representa un insumo/ingrediente disponible
 * Corresponde a "available_ingredients" en el JSON
 */
data class Insumo(
    val id: String,
    val nombre: String,
    val costo: Double,  // Costo por kg
    val esForraje: Boolean,  // is_forage
    val nutrientes: Map<String, Double>  // Todos los nutrientes
) {
    companion object {
        // Claves de nutrientes estándar
        const val GE = "GE"           // Energía Bruta (Mcal/kg)
        const val CP = "CP"           // Proteína Cruda (%)
        const val TDN = "TDN"         // Total Digestible Nutrients (%)
        const val NEM = "NEm"         // Energía Neta Mantenimiento (Mcal/kg)
        const val CA = "Ca"           // Calcio (%)
        const val P = "P"             // Fósforo (%)
        const val NDF = "NDF"         // Fibra Detergente Neutro (%)
        const val STARCH = "Starch"   // Almidón (%)
        const val FAT = "Fat"         // Grasa/Extracto Etéreo (%)
    }

    /**
     * Obtiene un nutriente con valor por defecto si no existe
     */
    fun getNutriente(key: String, default: Double = 0.0): Double {
        return nutrientes[key] ?: default
    }
}