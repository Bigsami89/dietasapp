package com.example.dietasapp.data

/**
 * Representa un ítem del inventario con cantidad disponible
 * Vincula un Insumo con su cantidad en stock
 */
data class InventarioItem(
    val insumo: Insumo,
    val cantidadDisponibleKg: Double,
    val fechaActualizacion: Long = System.currentTimeMillis()
) {
    /**
     * Verifica si hay suficiente cantidad disponible
     */
    fun haySuficiente(cantidadRequerida: Double): Boolean {
        return cantidadDisponibleKg >= cantidadRequerida
    }

    /**
     * Calcula el costo total de una cantidad específica
     */
    fun calcularCosto(cantidadKg: Double): Double {
        return insumo.costo * cantidadKg
    }
}