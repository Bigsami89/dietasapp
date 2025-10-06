package com.example.dietasapp.data

/**
 * Representa un animal en el sistema de inventario
 * Corresponde a la estructura JSON "animal_requirements"
 */
data class Animal(
    val id: String,
    val nombre: String,
    val tipo: TipoDieta,
    val pesoKg: Double,  // body_weight_kg
    val consumoDMI: Double,  // DMI_kg_day
    val requerimientosMinimos: Map<String, Double>,  // min_requirements
    val requerimientosMaximos: Map<String, Double>   // max_requirements
) {
    companion object {
        // Claves estándar para los requerimientos
        const val CP = "CP"           // Proteína Cruda
        const val NEM = "NEm"         // Energía Neta Mantenimiento
        const val CA = "Ca"           // Calcio
        const val P = "P"             // Fósforo
        const val NDF = "NDF"         // Fibra Detergente Neutro
    }
}