package com.example.dietasapp.data

import java.io.Serializable

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
    val requerimientosMaximos: Map<String, Double>,   // max_requirements
) : Serializable {

    companion object {
        private const val serialVersionUID: Long = 1L
        // Claves estándar para los requerimientos


        const val ENERGIA_METABOLIZABLE = "NE"// Energía Metabolizable MGJ/KG
        const val ENERGIA_NETA_MANTENIMIENTO = "ENM"  // Grasa

        // ============= COMPOSICIÓN NUTRICIONAL (%) =============
        const val PROTEINA_CRUDA = "PC"  // Proteína Cruda
        const val FIBRA_DETERGENTE_NEUTRA = "FDN"  // Fibra total
        const val FIBRA_DETERGENTE_ACIDA = "FDA"  // Porción menos digestible de fibra
        const val EXTRACTO_ETEREO = "EE"  // Grasa

        const val CENIZAS = "Cenizas"  // Contenido mineral total
        const val METANO_PRODUCIDO = "CH4"  // g/kg MS - Gramos de metano por kg MS

        // ============= MINERALES =============
        // Macrominerales (g/kg o según especificación)
        const val CALCIO = "Ca"
        const val FOSFORO = "P"
        const val MAGNESIO = "Mg"
        const val SODIO = "Na"
        const val POTASIO = "K"
        const val AZUFRE = "S"

        // Microminerales (mg/kg o según especificación)
        const val COBRE = "Cu"
        const val ZINC = "Zn"
        const val SELENIO = "Se"
        const val COBALTO = "Co"

        // ============= AMINOÁCIDOS (%) =============
        const val LISINA = "Lisina"
        const val METIONINA = "Metionina"
        const val TREONINA = "Treonina"
        const val VALINA = "Valina"
        const val ISOLEUCINA = "Isoleucina"
        const val ARGININA = "Arginina"
        const val TRIPTOFANO = "Triptofano"
        const val LEUCINA = "Leucina"

    }
}