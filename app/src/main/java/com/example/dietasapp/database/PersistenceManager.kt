package com.example.dietasapp.database

import android.content.Context
import com.example.dietasapp.data.*
import com.example.dietasapp.domain.*
import com.example.dietasapp.data.prefs.AppPrefs
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException

/**
 * Gestor de persistencia en UN SOLO archivo JSON (db.json)
 *
 * Estructura:
 * {
 *   "animals":   { "monogastrico": [ ... ], "multigastrico": [ ... ] },
 *   "insumos":   { "monogastrico": [ ... ], "multigastrico": [ ... ] },
 *   "inventory": { "monogastrico": [ ... ], "multigastrico": [ ... ] },
 *   "dietas":    { "monogastrico": [ ... ], "multigastrico": [ ... ] }
 * }
 *
 * Se respeta la API pública previa (readAnimals/writeAnimals/...) para no
 * modificar BaseDeDatosJSON ni Inventario.
 */
class PersistenceManager(private val context: Context) {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    private companion object {
        private const val DB_FILE = "db.json"

        // Claves de las secciones
        private const val ANIMALS = "animals"
        private const val INSUMOS = "insumos"
        private const val INVENTORY = "inventory"
        private const val DIETAS = "dietas"

        // Archivos legacy (vía ZIP) — solo para migración si existieran
        private const val ANIMALS_FILE = "animals.json"
        private const val INSUMOS_FILE = "insumos.json"
        private const val INVENTORY_FILE = "inventory.json"
        private const val DIETAS_FILE = "dietas.json"
    }

    // ========= UTILIDAD BÁSICA DE ARCHIVO ÚNICO =========

    private fun dbFile(): File = File(context.filesDir, DB_FILE)

    private fun readRoot(): JSONObject {
        val f = dbFile()
        if (!f.exists()) {
            return JSONObject().apply {
                put(ANIMALS, JSONObject())
                put(INSUMOS, JSONObject())
                put(INVENTORY, JSONObject())
                put(DIETAS, JSONObject())
            }
        }
        val txt = f.readText()
        return try { JSONObject(txt) } catch (_: JSONException) {
            JSONObject().apply {
                put(ANIMALS, JSONObject())
                put(INSUMOS, JSONObject())
                put(INVENTORY, JSONObject())
                put(DIETAS, JSONObject())
            }
        }
    }

    private fun writeRoot(root: JSONObject) {
        dbFile().writeText(root.toString())
    }

    private fun ensureSection(root: JSONObject, section: String): JSONObject {
        if (!root.has(section) || root.opt(section) !is JSONObject) {
            root.put(section, JSONObject())
        }
        return root.getJSONObject(section)
    }

    private fun ensureSpeciesArray(sectionObj: JSONObject, species: String): JSONArray {
        if (!sectionObj.has(species) || sectionObj.opt(species) !is JSONArray) {
            sectionObj.put(species, JSONArray())
        }
        return sectionObj.getJSONArray(species)
    }

    private fun currentSpecies(): String {
        return AppPrefs.getTipoAnimal(context) ?: AppPrefs.TIPO_MULTI
    }

    // ========= GENÉRICOS: LEER/ESCRIBIR SECCIÓN+ESPECIE =========

    private suspend fun <T> readSection(
        section: String,
        typeToken: TypeToken<T>
    ): T? = withContext(Dispatchers.IO) {
        try {
            val root = readRoot()
            val sec = ensureSection(root, section)
            val arr = ensureSpeciesArray(sec, currentSpecies())
            val jsonString = arr.toString()
            gson.fromJson<T>(jsonString, typeToken.type)
        } catch (e: IOException) {
            println("Error IO al leer sección $section: ${e.message}")
            null
        } catch (e: Exception) {
            println("Error parseando sección $section: ${e.message}")
            null
        }
    }

    private suspend fun <T> writeSection(
        section: String,
        data: T
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = readRoot()
            val sec = ensureSection(root, section)
            val jsonString = gson.toJson(data)
            val arr = JSONArray(jsonString) // data es lista
            sec.put(currentSpecies(), arr)
            writeRoot(root)
            true
        } catch (e: IOException) {
            println("Error IO al escribir sección $section: ${e.message}")
            false
        } catch (e: Exception) {
            println("Error serializando sección $section: ${e.message}")
            false
        }
    }

    // ========= API PÚBLICA COMPATIBLE (usada por BaseDeDatosJSON) =========

    suspend fun readAnimals(): List<Animal>? =
        readSection(ANIMALS, object : TypeToken<List<Animal>>() {})

    suspend fun writeAnimals(animals: List<Animal>): Boolean =
        writeSection(ANIMALS, animals)

    suspend fun readInsumos(): List<Insumo>? =
        readSection(INSUMOS, object : TypeToken<List<Insumo>>() {})

    suspend fun writeInsumos(insumos: List<Insumo>): Boolean =
        writeSection(INSUMOS, insumos)

    suspend fun readInventory(): List<InventarioItem>? =
        readSection(INVENTORY, object : TypeToken<List<InventarioItem>>() {})

    suspend fun writeInventory(inventory: List<InventarioItem>): Boolean =
        writeSection(INVENTORY, inventory)

    suspend fun readDietas(): List<Dieta>? =
        readSection(DIETAS, object : TypeToken<List<Dieta>>() {})

    suspend fun writeDietas(dietas: List<Dieta>): Boolean =
        writeSection(DIETAS, dietas)

    // ========= UTILIDADES / COMPAT =========

    fun fileExists(fileName: String): Boolean {
        // Mantener compat: cualquier chequeo apunta al archivo único
        return dbFile().exists()
    }

    fun getFileSize(fileName: String): Long {
        val f = dbFile()
        return if (f.exists()) f.length() else 0L
    }

    private fun deleteFile(fileName: String) {
        // Compat: borrar el archivo único
        val f = dbFile()
        if (f.exists()) f.delete()
    }

    /**
     * Inicializa `db.json` si no existe. Si existen archivos legacy
     * (animals.json, insumos.json, etc.), los migra a especie por defecto
     * (multigástrico) sin sobreescribir si ya hay datos.
     */
    suspend fun initializeDefaultData() = withContext(Dispatchers.IO) {
        val root = readRoot()

        // Asegurar estructura base
        ensureSection(root, ANIMALS)
        ensureSection(root, INSUMOS)
        ensureSection(root, INVENTORY)
        ensureSection(root, DIETAS)

        // Crear arrays vacíos por especie si faltan
        listOf(ANIMALS, INSUMOS, INVENTORY, DIETAS).forEach { sec ->
            val sObj = ensureSection(root, sec)
            ensureSpeciesArray(sObj, AppPrefs.TIPO_MONO)
            ensureSpeciesArray(sObj, AppPrefs.TIPO_MULTI)
        }

        // Migración simple desde archivos legacy → especie MULTI (si hay y si destino vacío)
        fun <T> readLegacy(file: String, token: TypeToken<T>): T? {
            val f = File(context.filesDir, file)
            if (!f.exists()) return null
            return try {
                gson.fromJson<T>(f.readText(), token.type)
            } catch (_: Exception) { null }
        }

        // ANIMALES
        val animalsObj = root.getJSONObject(ANIMALS)
        if (animalsObj.getJSONArray(AppPrefs.TIPO_MULTI).length() == 0) {
            readLegacy(ANIMALS_FILE, object : TypeToken<List<Animal>>() {} )?.let {
                animalsObj.put(AppPrefs.TIPO_MULTI, JSONArray(gson.toJson(it)))
            }
        }

        // INSUMOS
        val insumosObj = root.getJSONObject(INSUMOS)
        if (insumosObj.getJSONArray(AppPrefs.TIPO_MULTI).length() == 0) {
            readLegacy(INSUMOS_FILE, object : TypeToken<List<Insumo>>() {} )?.let {
                insumosObj.put(AppPrefs.TIPO_MULTI, JSONArray(gson.toJson(it)))
            }
        }

        // INVENTARIO
        val invObj = root.getJSONObject(INVENTORY)
        if (invObj.getJSONArray(AppPrefs.TIPO_MULTI).length() == 0) {
            readLegacy(INVENTORY_FILE, object : TypeToken<List<InventarioItem>>() {} )?.let {
                invObj.put(AppPrefs.TIPO_MULTI, JSONArray(gson.toJson(it)))
            }
        }

        // DIETAS
        val dietasObj = root.getJSONObject(DIETAS)
        if (dietasObj.getJSONArray(AppPrefs.TIPO_MULTI).length() == 0) {
            readLegacy(DIETAS_FILE, object : TypeToken<List<Dieta>>() {} )?.let {
                dietasObj.put(AppPrefs.TIPO_MULTI, JSONArray(gson.toJson(it)))
            }
        }

        writeRoot(root)
    }

    /**
     * Limpia la base y vuelve a crear estructura vacía.
     */
    suspend fun clearAllData(): Boolean = withContext(Dispatchers.IO) {
        try {
            deleteFile(DB_FILE)
            initializeDefaultData()
            true
        } catch (e: Exception) {
            println("Error al limpiar datos: ${e.message}")
            false
        }
    }
}
