package com.example.dietasapp.database

import android.content.Context
import com.example.dietasapp.data.Animal
import com.example.dietasapp.data.Insumo
import com.example.dietasapp.data.InventarioItem
import com.example.dietasapp.domain.Dieta
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Implementación de base de datos usando archivos JSON
 * Proporciona persistencia local en el dispositivo
 */
class BaseDeDatosJSON(context: Context) : BaseDeDatos {

    private val persistenceManager = PersistenceManager(context)

    // Mutex para evitar condiciones de carrera en escrituras concurrentes
    private val animalsMutex = Mutex()
    private val insumosMutex = Mutex()
    private val inventoryMutex = Mutex()
    private val dietasMutex = Mutex()

    init {
        // Inicializar archivos si no existen
        kotlinx.coroutines.runBlocking {
            persistenceManager.initializeDefaultData()
        }
    }

    // ============= IMPLEMENTACIÓN: ANIMALES =============

    override suspend fun obtenerAnimales(): List<Animal> {
        return persistenceManager.readAnimals() ?: emptyList()
    }

    override suspend fun obtenerAnimalPorId(id: String): Animal? {
        val animals = obtenerAnimales()
        return animals.find { it.id == id }
    }

    override suspend fun guardarAnimal(animal: Animal): Boolean = animalsMutex.withLock {
        val animals = obtenerAnimales().toMutableList()

        // Remover animal existente con el mismo ID
        animals.removeIf { it.id == animal.id }

        // Agregar nuevo animal
        animals.add(animal)

        return persistenceManager.writeAnimals(animals)
    }

    override suspend fun eliminarAnimal(id: String): Boolean = animalsMutex.withLock {
        val animals = obtenerAnimales().toMutableList()
        val removed = animals.removeIf { it.id == id }

        return if (removed) {
            persistenceManager.writeAnimals(animals)
        } else {
            false
        }
    }

    // ============= IMPLEMENTACIÓN: INSUMOS =============

    override suspend fun obtenerInsumos(): List<Insumo> {
        return persistenceManager.readInsumos() ?: emptyList()
    }

    override suspend fun obtenerInsumoPorId(id: String): Insumo? {
        val insumos = obtenerInsumos()
        return insumos.find { it.id == id }
    }

    override suspend fun guardarInsumo(insumo: Insumo): Boolean = insumosMutex.withLock {
        val insumos = obtenerInsumos().toMutableList()

        // Remover insumo existente con el mismo ID
        insumos.removeIf { it.id == insumo.id }

        // Agregar nuevo insumo
        insumos.add(insumo)

        return persistenceManager.writeInsumos(insumos)
    }

    override suspend fun eliminarInsumo(id: String): Boolean = insumosMutex.withLock {
        val insumos = obtenerInsumos().toMutableList()
        val removed = insumos.removeIf { it.id == id }

        return if (removed) {
            persistenceManager.writeInsumos(insumos)
        } else {
            false
        }
    }

    // ============= IMPLEMENTACIÓN: INVENTARIO =============

    override suspend fun obtenerInventario(): List<InventarioItem> {
        return persistenceManager.readInventory() ?: emptyList()
    }

    override suspend fun obtenerInventarioItem(insumoId: String): InventarioItem? {
        val inventory = obtenerInventario()
        return inventory.find { it.insumo.id == insumoId }
    }

    override suspend fun actualizarInventario(insumoId: String, cantidad: Double): Boolean =
        inventoryMutex.withLock {
            val inventory = obtenerInventario().toMutableList()
            val itemIndex = inventory.indexOfFirst { it.insumo.id == insumoId }

            if (itemIndex >= 0) {
                val item = inventory[itemIndex]
                inventory[itemIndex] = item.copy(
                    cantidadDisponibleKg = cantidad,
                    fechaActualizacion = System.currentTimeMillis()
                )
                persistenceManager.writeInventory(inventory)
            } else {
                false
            }
        }

    override suspend fun agregarInventario(item: InventarioItem): Boolean = inventoryMutex.withLock {
        val inventory = obtenerInventario().toMutableList()

        // Remover item existente con el mismo insumo
        inventory.removeIf { it.insumo.id == item.insumo.id }

        // Agregar nuevo item
        inventory.add(item)

        return persistenceManager.writeInventory(inventory)
    }

    override suspend fun eliminarInventario(insumoId: String): Boolean = inventoryMutex.withLock {
        val inventory = obtenerInventario().toMutableList()
        val removed = inventory.removeIf { it.insumo.id == insumoId }

        return if (removed) {
            persistenceManager.writeInventory(inventory)
        } else {
            false
        }
    }

    // ============= IMPLEMENTACIÓN: DIETAS =============

    override suspend fun obtenerDietas(): List<Dieta> {
        return persistenceManager.readDietas() ?: emptyList()
    }

    override suspend fun guardarDieta(dieta: Dieta): Boolean = dietasMutex.withLock {
        val dietas = obtenerDietas().toMutableList()

        // Agregar nueva dieta (permitir duplicados por fecha)
        dietas.add(dieta)

        // Ordenar por fecha (más reciente primero)
        dietas.sortByDescending { it.fechaCreacion }

        return persistenceManager.writeDietas(dietas)
    }

    override suspend fun eliminarDieta(fechaCreacion: Long): Boolean = dietasMutex.withLock {
        val dietas = obtenerDietas().toMutableList()
        val removed = dietas.removeIf { it.fechaCreacion == fechaCreacion }

        return if (removed) {
            persistenceManager.writeDietas(dietas)
        } else {
            false
        }
    }

    override suspend fun obtenerDietasPorAnimal(animalId: String): List<Dieta> {
        val dietas = obtenerDietas()
        return dietas.filter { it.animal.id == animalId }
    }
}