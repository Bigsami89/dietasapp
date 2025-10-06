package com.example.dietasapp.domain

/**
 * Perfil del animal para el optimizador
 * Corresponde a la clase "AnimalProfile" del diagrama
 */
data class AnimalProfile(
    val name: String,
    val bodyWeightKg: Double,
    val dmiKgDay: Double,
    val requirements: Map<String, Double>  // Combina min y max requirements
) {
    companion object {
        /**
         * Convierte un Animal (capa data) a AnimalProfile (capa domain)
         */
        fun fromAnimal(animal: com.example.dietasapp.data.Animal): AnimalProfile {
            // Combinar requerimientos mínimos y máximos con prefijos
            val allRequirements = mutableMapOf<String, Double>()

            animal.requerimientosMinimos.forEach { (key, value) ->
                allRequirements["min_$key"] = value
            }

            animal.requerimientosMaximos.forEach { (key, value) ->
                allRequirements["max_$key"] = value
            }

            // Agregar peso corporal y DMI como parte del perfil
            allRequirements["body_weight"] = animal.pesoKg
            allRequirements["dmi"] = animal.consumoDMI

            return AnimalProfile(
                name = animal.nombre,
                bodyWeightKg = animal.pesoKg,
                dmiKgDay = animal.consumoDMI,
                requirements = allRequirements
            )
        }
    }

    /**
     * Obtiene un requerimiento específico
     */
    fun getRequirement(key: String): Double {
        return requirements[key] ?: 0.0
    }
}