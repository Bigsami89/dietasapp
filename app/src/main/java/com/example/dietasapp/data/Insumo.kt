package com.example.dietasapp.data

import com.example.dietasapp.domain.Ingredient

/**
 * Representa un insumo/ingrediente para formulación de raciones de rumiantes
 * Adaptado según especificaciones para nutrición de rumiantes en contexto mexicano
 */
data class Insumo(
    val id: String,
    val nombre: String,
    val costo: Double,  // Costo en Pesos Mexicanos (MXN) por kg
    val esForraje: Boolean,

    // Restricciones de inclusión (editables por el usuario)
    val inclusionMinima: Double = 0.0,  // Porcentaje mínimo en la ración (%)
    val inclusionMaxima: Double = 100.0,  // Porcentaje máximo en la ración (%)

    val nutrientes: Map<String, Double>  // Todos los nutrientes
) {
    companion object {
        // ============= ENERGÍA (MJ/kg MS) =============

        const val ENERGIA_BRUTA = "BE"
        const val ENERGIA_NETA_MANTENIMIENTO = "ENM"  // ** PRIORITARIO ** Energía Metabolizable
        const val ENERGIA_METABOLIZABLE = "EM"  // ** PRIORITARIO ** Energía Metabolizable

        // ============= COMPOSICIÓN NUTRICIONAL (%) =============
        const val PROTEINA_CRUDA = "PC"  // Proteína Cruda
        const val FIBRA_DETERGENTE_NEUTRA = "FDN"  // Fibra total
        const val FIBRA_DETERGENTE_ACIDA = "FDA"  // Porción menos digestible de fibra
        const val EXTRACTO_ETEREO = "EE"  // Grasa
        const val CENIZAS = "Cenizas"  // Contenido mineral total
        const val DEGRADABILIDAD_RUMINAL = "DegRum"  // % que se degrada en el rumen
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

        // ============= NUTRIENTES LEGACY (mantener compatibilidad) =============
        @Deprecated("Usar ENERGIA_BRUTA", ReplaceWith("ENERGIA_BRUTA"))
        const val GE = "GE"  // Mantenido para compatibilidad con código existente

        @Deprecated("Usar PROTEINA_CRUDA", ReplaceWith("PROTEINA_CRUDA"))
        const val CP = "CP"  // Mantenido para compatibilidad

        @Deprecated("Usar FIBRA_DETERGENTE_NEUTRA", ReplaceWith("FIBRA_DETERGENTE_NEUTRA"))
        const val NDF = "NDF"  // Mantenido para compatibilidad

        @Deprecated("Usar EXTRACTO_ETEREO", ReplaceWith("EXTRACTO_ETEREO"))
        const val FAT = "Fat"  // Mantenido para compatibilidad

        // Campos removidos según especificaciones
        // TDN - Ya no se utiliza
        // NEM (como Mcal/kg) - Reemplazado por ENERGIA_NETA_MANTENIMIENTO en MJ/kg
        // STARCH - No especificado para rumiantes en este contexto
    }

    /**
     * Obtiene un nutriente con valor por defecto si no existe
     */
    fun getNutriente(key: String, default: Double = 0.0): Double {
        return nutrientes[key] ?: default
    }


    fun Ingredient.getNutrientOrNull(key: String): Double? {
        return nutrients[key]
    }
    /**
     * Obtiene la Energía Metabolizable (campo prioritario)
     */
    fun getEnergiaMetabolizable(): Double = getNutriente(ENERGIA_METABOLIZABLE, 0.0)

    /**
     * Obtiene la Proteína Cruda
     */
    fun getProteinaCruda(): Double = getNutriente(PROTEINA_CRUDA, 0.0)

    /**
     * Obtiene la Fibra Detergente Neutra
     */
    fun getFibraDetengenteNeutra(): Double = getNutriente(FIBRA_DETERGENTE_NEUTRA, 0.0)

    /**
     * Verifica si el insumo tiene restricciones de inclusión personalizadas
     */
    fun tieneRestriccionesPersonalizadas(): Boolean {
        return inclusionMinima > 0.0 || inclusionMaxima < 100.0
    }

    /**
     * Valida que las restricciones de inclusión sean coherentes
     */
    fun validarRestricciones(): Boolean {
        return inclusionMinima >= 0.0 &&
                inclusionMaxima <= 100.0 &&
                inclusionMinima <= inclusionMaxima
    }
}