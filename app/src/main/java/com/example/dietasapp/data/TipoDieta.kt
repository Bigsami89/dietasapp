package com.example.dietasapp.data

/**
 * Enumera los tipos de dieta según el contenido de forraje
 * Basado en las categorías de NASEM (2016) para cálculo de metano
 */
enum class TipoDieta(val descripcion: String, val porcentajeForraje: IntRange) {
    ALTO_FORRAJE(
        "Alto Forraje (≥40% MS)",
        40..100
    ),
    INTERMEDIO(
        "Forraje Intermedio (20-40% MS)",
        20..39
    ),
    BAJO_FORRAJE(
        "Bajo Forraje (≤20% MS)",
        0..20
    );

    companion object {
        /**
         * Determina el tipo de dieta basado en el porcentaje de forraje
         */
        fun fromForagePercentage(percentage: Double): TipoDieta {
            return when {
                percentage >= 40.0 -> ALTO_FORRAJE
                percentage >= 20.0 -> INTERMEDIO
                else -> BAJO_FORRAJE
            }
        }
    }
}