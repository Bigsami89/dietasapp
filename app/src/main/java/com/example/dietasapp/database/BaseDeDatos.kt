package com.example.dietasapp.database

import com.example.dietasapp.data.Animal
import com.example.dietasapp.data.Insumo
import com.example.dietasapp.data.InventarioItem
import com.example.dietasapp.domain.Dieta

/**
 * Interfaz para la base de datos
 * Define operaciones CRUD para todos los modelos
 */
interface BaseDeDatos {

    // ============= OPERACIONES CON ANIMALES =============

    /**
     * Obtiene todos los animales
     */
    suspend fun obtenerAnimales(): List<Animal>

    /**
     * Obtiene un animal por ID
     */
    suspend fun obtenerAnimalPorId(id: String): Animal?

    /**
     * Guarda o actualiza un animal
     */
    suspend fun guardarAnimal(animal: Animal): Boolean

    /**
     * Elimina un animal
     */
    suspend fun eliminarAnimal(id: String): Boolean

    // ============= OPERACIONES CON INSUMOS =============

    /**
     * Obtiene todos los insumos disponibles
     */
    suspend fun obtenerInsumos(): List<Insumo>

    /**
     * Obtiene un insumo por ID
     */
    suspend fun obtenerInsumoPorId(id: String): Insumo?

    /**
     * Guarda o actualiza un insumo
     */
    suspend fun guardarInsumo(insumo: Insumo): Boolean

    /**
     * Elimina un insumo
     */
    suspend fun eliminarInsumo(id: String): Boolean

    // ============= OPERACIONES CON INVENTARIO =============

    /**
     * Obtiene todos los items del inventario
     */
    suspend fun obtenerInventario(): List<InventarioItem>

    /**
     * Obtiene un item del inventario por ID de insumo
     */
    suspend fun obtenerInventarioItem(insumoId: String): InventarioItem?

    /**
     * Actualiza la cantidad de un item en inventario
     */
    suspend fun actualizarInventario(insumoId: String, cantidad: Double): Boolean

    /**
     * Agrega un item al inventario
     */
    suspend fun agregarInventario(item: InventarioItem): Boolean

    /**
     * Elimina un item del inventario
     */
    suspend fun eliminarInventario(insumoId: String): Boolean

    // ============= OPERACIONES CON DIETAS =============

    /**
     * Obtiene todas las dietas guardadas
     */
    suspend fun obtenerDietas(): List<Dieta>

    /**
     * Guarda una dieta
     */
    suspend fun guardarDieta(dieta: Dieta): Boolean

    /**
     * Elimina una dieta
     */
    suspend fun eliminarDieta(fechaCreacion: Long): Boolean

    /**
     * Obtiene dietas de un animal específico
     */
    suspend fun obtenerDietasPorAnimal(animalId: String): List<Dieta>
}