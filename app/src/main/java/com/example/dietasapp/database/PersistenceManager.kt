package com.example.dietasapp.database


import com.example.dietasapp.data.*
import com.example.dietasapp.domain.*
import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Gestor de persistencia de datos en archivos JSON
 * Maneja lectura/escritura de archivos en almacenamiento interno
 */
class PersistenceManager(private val context: Context) {

    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    companion object {
        private const val ANIMALS_FILE = "animals.json"
        private const val INSUMOS_FILE = "insumos.json"
        private const val INVENTORY_FILE = "inventory.json"
        private const val DIETAS_FILE = "dietas.json"
    }

    // ============= MÉTODOS GENÉRICOS =============

    /**
     * Lee datos de un archivo JSON
     */
    suspend fun <T> readData(fileName: String, typeToken: TypeToken<T>): T? = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, fileName)

            if (!file.exists()) {
                println("Archivo $fileName no existe, devolviendo null")
                return@withContext null
            }

            val jsonString = file.readText()
            gson.fromJson<T>(jsonString, typeToken.type)

        } catch (e: IOException) {
            println("Error al leer archivo $fileName: ${e.message}")
            e.printStackTrace()
            null
        } catch (e: Exception) {
            println("Error al parsear JSON de $fileName: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    /**
     * Escribe datos a un archivo JSON
     */
    suspend fun <T> writeData(fileName: String, data: T): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, fileName)
            val jsonString = gson.toJson(data)
            file.writeText(jsonString)

            println("Datos guardados exitosamente en $fileName")
            true

        } catch (e: IOException) {
            println("Error al escribir archivo $fileName: ${e.message}")
            e.printStackTrace()
            false
        } catch (e: Exception) {
            println("Error al serializar datos para $fileName: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    /**
     * Verifica si existe un archivo
     */
    fun fileExists(fileName: String): Boolean {
        val file = File(context.filesDir, fileName)
        return file.exists()
    }

    /**
     * Elimina un archivo
     */
    suspend fun deleteFile(fileName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, fileName)
            if (file.exists()) {
                file.delete()
            } else {
                true
            }
        } catch (e: Exception) {
            println("Error al eliminar archivo $fileName: ${e.message}")
            false
        }
    }

    /**
     * Obtiene el tamaño de un archivo en bytes
     */
    fun getFileSize(fileName: String): Long {
        val file = File(context.filesDir, fileName)
        return if (file.exists()) file.length() else 0
    }

    // ============= MÉTODOS ESPECÍFICOS POR TIPO =============

    suspend fun readAnimals(): List<Animal>? {
        return readData(ANIMALS_FILE, object : TypeToken<List<Animal>>() {})
    }

    suspend fun writeAnimals(animals: List<Animal>): Boolean {
        return writeData(ANIMALS_FILE, animals)
    }

    suspend fun readInsumos(): List<Insumo>? {
        return readData(INSUMOS_FILE, object : TypeToken<List<Insumo>>() {})
    }

    suspend fun writeInsumos(insumos: List<Insumo>): Boolean {
        return writeData(INSUMOS_FILE, insumos)
    }

    suspend fun readInventory(): List<InventarioItem>? {
        return readData(INVENTORY_FILE, object : TypeToken<List<InventarioItem>>() {})
    }

    suspend fun writeInventory(inventory: List<InventarioItem>): Boolean {
        return writeData(INVENTORY_FILE, inventory)
    }

    suspend fun readDietas(): List<Dieta>? {
        return readData(DIETAS_FILE, object : TypeToken<List<Dieta>>() {})
    }

    suspend fun writeDietas(dietas: List<Dieta>): Boolean {
        return writeData(DIETAS_FILE, dietas)
    }

    // ============= UTILIDADES =============

    /**
     * Inicializa archivos con datos por defecto si no existen
     */
    suspend fun initializeDefaultData() = withContext(Dispatchers.IO) {
        if (!fileExists(ANIMALS_FILE)) {
            writeAnimals(emptyList())
        }
        if (!fileExists(INSUMOS_FILE)) {
            writeInsumos(emptyList())
        }
        if (!fileExists(INVENTORY_FILE)) {
            writeInventory(emptyList())
        }
        if (!fileExists(DIETAS_FILE)) {
            writeDietas(emptyList())
        }
    }

    /**
     * Respalda todos los datos
     */
    suspend fun backupAllData(): Boolean = withContext(Dispatchers.IO) {
        try {
            val backupDir = File(context.filesDir, "backup")
            if (!backupDir.exists()) {
                backupDir.mkdir()
            }

            val timestamp = System.currentTimeMillis()

            listOf(ANIMALS_FILE, INSUMOS_FILE, INVENTORY_FILE, DIETAS_FILE).forEach { fileName ->
                val sourceFile = File(context.filesDir, fileName)
                if (sourceFile.exists()) {
                    val backupFile = File(backupDir, "${timestamp}_$fileName")
                    sourceFile.copyTo(backupFile, overwrite = true)
                }
            }

            true
        } catch (e: Exception) {
            println("Error al hacer backup: ${e.message}")
            false
        }
    }

    /**
     * Limpia todos los datos
     */
    suspend fun clearAllData(): Boolean = withContext(Dispatchers.IO) {
        try {
            deleteFile(ANIMALS_FILE)
            deleteFile(INSUMOS_FILE)
            deleteFile(INVENTORY_FILE)
            deleteFile(DIETAS_FILE)
            initializeDefaultData()
            true
        } catch (e: Exception) {
            println("Error al limpiar datos: ${e.message}")
            false
        }
    }
}