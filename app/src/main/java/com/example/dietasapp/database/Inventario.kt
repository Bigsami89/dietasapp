package com.example.dietasapp.inventory

import com.example.dietasapp.data.Animal
import com.example.dietasapp.data.Insumo
import com.example.dietasapp.data.InventarioItem
import com.example.dietasapp.database.BaseDeDatos

import com.example.dietasapp.domain.Dieta

/**
 * Gestor de inventario
 * Proporciona operaciones de alto nivel sobre animales, insumos e inventario
 */
class Inventario(private val baseDeDatos: BaseDeDatos) {

    // ============= OPERACIONES CON ANIMALES =============

    /**
     * Obtiene todos los animales
     */
    suspend fun obtenerAnimales(): List<Animal> {
        return baseDeDatos.obtenerAnimales()
    }

    /**
     * Obtiene un animal por ID
     */
    suspend fun obtenerAnimal(id: String): Animal? {
        return baseDeDatos.obtenerAnimalPorId(id)
    }

    /**
     * Agrega o actualiza un animal
     */
    suspend fun guardarAnimal(animal: Animal): Boolean {
        return baseDeDatos.guardarAnimal(animal)
    }

    /**
     * Elimina un animal
     */
    suspend fun eliminarAnimal(id: String): Boolean {
        return baseDeDatos.eliminarAnimal(id)
    }

    // ============= OPERACIONES CON INSUMOS =============

    /**
     * Obtiene todos los insumos
     */
    suspend fun obtenerInsumos(): List<Insumo> {
        return baseDeDatos.obtenerInsumos()
    }

    /**
     * Obtiene un insumo por ID
     */
    suspend fun obtenerInsumo(id: String): Insumo? {
        return baseDeDatos.obtenerInsumoPorId(id)
    }

    /**
     * Agrega o actualiza un insumo
     */
    suspend fun guardarInsumo(insumo: Insumo): Boolean {
        return baseDeDatos.guardarInsumo(insumo)
    }

    /**
     * Elimina un insumo
     */
    suspend fun eliminarInsumo(id: String): Boolean {
        // Verificar si el insumo está en inventario
        val inventarioItem = baseDeDatos.obtenerInventarioItem(id)
        if (inventarioItem != null) {
            // Eliminar del inventario primero
            baseDeDatos.eliminarInventario(id)
        }

        return baseDeDatos.eliminarInsumo(id)
    }

    // ============= OPERACIONES CON INVENTARIO =============

    /**
     * Obtiene todos los items del inventario
     */
    suspend fun obtenerInventarioInsumos(): List<InventarioItem> {
        return baseDeDatos.obtenerInventario()
    }

    /**
     * Obtiene items del inventario con stock disponible
     */
    suspend fun obtenerInventarioDisponible(): List<InventarioItem> {
        val inventario = obtenerInventarioInsumos()
        return inventario.filter { it.cantidadDisponibleKg > 0 }
    }

    /**
     * Obtiene un item del inventario por ID de insumo
     */
    suspend fun obtenerInventarioItem(insumoId: String): InventarioItem? {
        return baseDeDatos.obtenerInventarioItem(insumoId)
    }

    /**
     * Agrega un insumo al inventario
     */
    suspend fun agregarAlInventario(insumo: Insumo, cantidad: Double): Boolean {
        val item = InventarioItem(
            insumo = insumo,
            cantidadDisponibleKg = cantidad,
            fechaActualizacion = System.currentTimeMillis()
        )
        return baseDeDatos.agregarInventario(item)
    }

    /**
     * Actualiza la cantidad de un item en inventario
     */
    suspend fun actualizarCantidad(insumoId: String, cantidad: Double): Boolean {
        if (cantidad < 0) {
            println("Error: La cantidad no puede ser negativa")
            return false
        }
        return baseDeDatos.actualizarInventario(insumoId, cantidad)
    }

    /**
     * Incrementa la cantidad de un item en inventario
     */
    suspend fun incrementarCantidad(insumoId: String, incremento: Double): Boolean {
        val item = obtenerInventarioItem(insumoId) ?: return false
        val nuevaCantidad = item.cantidadDisponibleKg + incremento
        return actualizarCantidad(insumoId, nuevaCantidad)
    }

    /**
     * Decrementa la cantidad de un item en inventario (uso de dieta)
     */
    suspend fun decrementarCantidad(insumoId: String, decremento: Double): Boolean {
        val item = obtenerInventarioItem(insumoId) ?: return false
        val nuevaCantidad = (item.cantidadDisponibleKg - decremento).coerceAtLeast(0.0)
        return actualizarCantidad(insumoId, nuevaCantidad)
    }

    /**
     * Elimina un item del inventario
     */
    suspend fun eliminarDelInventario(insumoId: String): Boolean {
        return baseDeDatos.eliminarInventario(insumoId)
    }

    /**
     * Verifica si hay suficiente stock para una dieta
     */
    suspend fun verificarStockParaDieta(
        composicion: Map<String, Double>,
        diasProyectados: Int = 1
    ): StockVerification {
        val faltantes = mutableListOf<StockFaltante>()
        val inventario = obtenerInventarioInsumos()

        composicion.forEach { (nombreInsumo, kgPorDia) ->
            val cantidadRequerida = kgPorDia * diasProyectados
            val item = inventario.find { it.insumo.nombre == nombreInsumo }

            if (item == null) {
                faltantes.add(StockFaltante(
                    nombreInsumo = nombreInsumo,
                    requerido = cantidadRequerida,
                    disponible = 0.0,
                    faltante = cantidadRequerida
                ))
            } else if (item.cantidadDisponibleKg < cantidadRequerida) {
                faltantes.add(StockFaltante(
                    nombreInsumo = nombreInsumo,
                    requerido = cantidadRequerida,
                    disponible = item.cantidadDisponibleKg,
                    faltante = cantidadRequerida - item.cantidadDisponibleKg
                ))
            }
        }

        return StockVerification(
            suficiente = faltantes.isEmpty(),
            faltantes = faltantes
        )
    }

    /**
     * Consume stock según una dieta
     */
    suspend fun consumirStockDieta(composicion: Map<String, Double>): Boolean {
        val inventario = obtenerInventarioInsumos()

        composicion.forEach { (nombreInsumo, kgPorDia) ->
            val item = inventario.find { it.insumo.nombre == nombreInsumo }
            if (item != null) {
                decrementarCantidad(item.insumo.id, kgPorDia)
            }
        }

        return true
    }
    suspend fun guardarDieta(dieta: Dieta): Boolean {
        return baseDeDatos.guardarDieta(dieta)
    }

    /**
     * Obtiene todas las dietas guardadas
     */
    suspend fun obtenerDietas(): List<Dieta> {
        return baseDeDatos.obtenerDietas()
    }

    /**
     * Obtiene dietas de un animal específico
     */
    suspend fun obtenerDietasPorAnimal(animalId: String): List<Dieta> {
        return baseDeDatos.obtenerDietasPorAnimal(animalId)
    }

    /**
     * Elimina una dieta
     */
    suspend fun eliminarDieta(fechaCreacion: Long): Boolean {
        return baseDeDatos.eliminarDieta(fechaCreacion)
    }

    // ============= REPORTES Y ESTADÍSTICAS =============

    /**
     * Genera reporte de inventario
     */
    suspend fun generarReporteInventario(): InventoryReport {
        val inventario = obtenerInventarioInsumos()
        val insumos = obtenerInsumos()

        val valorTotal = inventario.sumOf {
            it.insumo.costo * it.cantidadDisponibleKg
        }

        val itemsBajos = inventario.filter {
            it.cantidadDisponibleKg < 10.0 // Menos de 10 kg
        }

        val itemsAgotados = inventario.filter {
            it.cantidadDisponibleKg == 0.0
        }

        return InventoryReport(
            totalItems = inventario.size,
            totalInsumos = insumos.size,
            valorTotal = valorTotal,
            itemsBajoStock = itemsBajos,
            itemsAgotados = itemsAgotados
        )
    }

    // ============= CLASES DE DATOS AUXILIARES =============

    data class StockFaltante(
        val nombreInsumo: String,
        val requerido: Double,
        val disponible: Double,
        val faltante: Double
    )

    data class StockVerification(
        val suficiente: Boolean,
        val faltantes: List<StockFaltante>
    ) {
        fun obtenerMensaje(): String {
            return if (suficiente) {
                "Stock suficiente para la dieta"
            } else {
                buildString {
                    appendLine("Stock insuficiente:")
                    faltantes.forEach {
                        appendLine("  - ${it.nombreInsumo}: " +
                                "Necesita ${String.format("%.2f", it.requerido)} kg, " +
                                "disponible ${String.format("%.2f", it.disponible)} kg, " +
                                "falta ${String.format("%.2f", it.faltante)} kg")
                    }
                }
            }
        }
    }

    data class InventoryReport(
        val totalItems: Int,
        val totalInsumos: Int,
        val valorTotal: Double,
        val itemsBajoStock: List<InventarioItem>,
        val itemsAgotados: List<InventarioItem>
    ) {
        fun obtenerResumen(): String {
            return buildString {
                appendLine("═══════════════════════════════════════")
                appendLine("REPORTE DE INVENTARIO")
                appendLine("═══════════════════════════════════════")
                appendLine("Total de insumos: $totalInsumos")
                appendLine("Items en inventario: $totalItems")
                appendLine("Valor total: $${String.format("%.2f", valorTotal)}")
                appendLine("Items con bajo stock: ${itemsBajoStock.size}")
                appendLine("Items agotados: ${itemsAgotados.size}")
                appendLine("═══════════════════════════════════════")
            }
        }
    }
}