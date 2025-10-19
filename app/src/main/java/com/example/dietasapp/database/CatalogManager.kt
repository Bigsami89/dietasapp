package com.example.dietasapp.database

import android.content.Context
import com.example.dietasapp.data.Animal
import com.example.dietasapp.data.Insumo
import com.example.dietasapp.data.TipoDieta
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

/**
 * Gestor de catálogos base de animales e insumos
 * Proporciona datos predefinidos que pueden agregarse al inventario
 */
class CatalogManager(private val context: Context) {

    private val gson = Gson()

    /**
     * Obtiene el catálogo de animales predefinidos
     */
    fun getAnimalsCatalog(): List<Animal> {
        return listOf(
            // Ganado lechero
            Animal(
                id = "catalog_vaca_lechera_alta",
                nombre = "Vaca Lechera - Alta Producción",
                tipo = TipoDieta.ALTO_FORRAJE,
                pesoKg = 650.0,
                consumoDMI = 24.0,
                requerimientosMinimos = mapOf(
                    "CP" to 16.5,
                    "NEm" to 1.65,
                    "Ca" to 0.70,
                    "P" to 0.40
                ),
                requerimientosMaximos = mapOf(
                    "NDF" to 35.0,
                    "CP" to 19.0
                )
            ),
            Animal(
                id = "catalog_vaca_lechera_media",
                nombre = "Vaca Lechera - Producción Media",
                tipo = TipoDieta.ALTO_FORRAJE,
                pesoKg = 600.0,
                consumoDMI = 20.0,
                requerimientosMinimos = mapOf(
                    "CP" to 14.0,
                    "NEm" to 1.50,
                    "Ca" to 0.50,
                    "P" to 0.35
                ),
                requerimientosMaximos = mapOf(
                    "NDF" to 40.0,
                    "CP" to 17.0
                )
            ),
            // Ganado de engorde
            Animal(
                id = "catalog_novillo_engorde_inicial",
                nombre = "Novillo Engorde - Fase Inicial",
                tipo = TipoDieta.INTERMEDIO,
                pesoKg = 250.0,
                consumoDMI = 8.0,
                requerimientosMinimos = mapOf(
                    "CP" to 13.0,
                    "NEm" to 1.40,
                    "Ca" to 0.40,
                    "P" to 0.30
                ),
                requerimientosMaximos = mapOf(
                    "NDF" to 45.0,
                    "CP" to 16.0
                )
            ),
            Animal(
                id = "catalog_novillo_engorde_final",
                nombre = "Novillo Engorde - Finalización",
                tipo = TipoDieta.BAJO_FORRAJE,
                pesoKg = 450.0,
                consumoDMI = 12.0,
                requerimientosMinimos = mapOf(
                    "CP" to 12.0,
                    "NEm" to 1.80,
                    "Ca" to 0.35,
                    "P" to 0.28
                ),
                requerimientosMaximos = mapOf(
                    "NDF" to 25.0,
                    "CP" to 14.5
                )
            ),
            // Ganado de cría
            Animal(
                id = "catalog_vaca_cria",
                nombre = "Vaca de Cría - Gestante",
                tipo = TipoDieta.ALTO_FORRAJE,
                pesoKg = 550.0,
                consumoDMI = 13.0,
                requerimientosMinimos = mapOf(
                    "CP" to 10.0,
                    "NEm" to 1.30,
                    "Ca" to 0.40,
                    "P" to 0.25
                ),
                requerimientosMaximos = mapOf(
                    "NDF" to 50.0,
                    "CP" to 13.0
                )
            ),
            Animal(
                id = "catalog_becerro_destete",
                nombre = "Becerro Post-Destete",
                tipo = TipoDieta.INTERMEDIO,
                pesoKg = 180.0,
                consumoDMI = 5.5,
                requerimientosMinimos = mapOf(
                    "CP" to 14.0,
                    "NEm" to 1.50,
                    "Ca" to 0.45,
                    "P" to 0.32
                ),
                requerimientosMaximos = mapOf(
                    "NDF" to 35.0,
                    "CP" to 17.0
                )
            )
        )
    }

    /**
     * Obtiene el catálogo de insumos predefinidos
     */
    fun obtenerCatalogoInsumos(): List<Insumo> {
        return listOf(
            // ========================================
            // FORRAJES
            // ========================================
            Insumo(
                id = "catalog_pasto_taiwan",
                nombre = "Pasto Taiwán (Pennisetum purpureum)",
                costo = 0.08,
                esForraje = true,
                inclusionMinima = 0.0,
                inclusionMaxima = 100.0,
                nutrientes = mapOf(
                    Insumo.ENERGIA_BRUTA to 18.5,  // MJ/kg MS
                    Insumo.ENERGIA_METABOLIZABLE to 8.5,  // MJ/kg MS - PRIORITARIO
                    Insumo.ENERGIA_NETA_MANTENIMIENTO to 4.8,
                    Insumo.PROTEINA_CRUDA to 8.5,  // %
                    Insumo.FIBRA_DETERGENTE_NEUTRA to 68.0,
                    Insumo.FIBRA_DETERGENTE_ACIDA to 42.0,
                    Insumo.EXTRACTO_ETEREO to 2.1,
                    Insumo.CENIZAS to 9.5,
                    Insumo.DEGRADABILIDAD_RUMINAL to 65.0,
                    Insumo.METANO_PRODUCIDO to 18.07,  // g/kg MS
                    // Minerales
                    Insumo.CALCIO to 4.5,  // g/kg
                    Insumo.FOSFORO to 2.8,
                    Insumo.MAGNESIO to 2.2,
                    Insumo.SODIO to 0.5,
                    Insumo.POTASIO to 28.0,
                    Insumo.AZUFRE to 1.8,
                    Insumo.COBRE to 8.5,  // mg/kg
                    Insumo.ZINC to 25.0,
                    Insumo.SELENIO to 0.15,
                    Insumo.COBALTO to 0.12
                )
            ),
            Insumo(
                id = "catalog_pasto_guinea",
                nombre = "Pasto Guinea (Panicum maximum)",
                costo = 0.07,
                esForraje = true,
                inclusionMinima = 0.0,
                inclusionMaxima = 100.0,
                nutrientes = mapOf(
                    Insumo.ENERGIA_BRUTA to 18.2,
                    Insumo.ENERGIA_METABOLIZABLE to 8.2,
                    Insumo.ENERGIA_NETA_MANTENIMIENTO to 4.6,
                    Insumo.PROTEINA_CRUDA to 9.2,
                    Insumo.FIBRA_DETERGENTE_NEUTRA to 70.5,
                    Insumo.FIBRA_DETERGENTE_ACIDA to 44.0,
                    Insumo.EXTRACTO_ETEREO to 2.3,
                    Insumo.CENIZAS to 8.8,
                    Insumo.DEGRADABILIDAD_RUMINAL to 62.0,
                    Insumo.METANO_PRODUCIDO to 17.85,
                    Insumo.CALCIO to 3.8,
                    Insumo.FOSFORO to 2.5,
                    Insumo.MAGNESIO to 2.0,
                    Insumo.SODIO to 0.4,
                    Insumo.POTASIO to 26.0,
                    Insumo.AZUFRE to 1.6,
                    Insumo.COBRE to 7.8,
                    Insumo.ZINC to 23.0,
                    Insumo.SELENIO to 0.12,
                    Insumo.COBALTO to 0.10
                )
            ),
            Insumo(
                id = "catalog_alfalfa_heno",
                nombre = "Alfalfa Heno",
                costo = 0.25,
                esForraje = true,
                inclusionMinima = 0.0,
                inclusionMaxima = 100.0,
                nutrientes = mapOf(
                    Insumo.ENERGIA_BRUTA to 18.8,
                    Insumo.ENERGIA_METABOLIZABLE to 9.2,
                    Insumo.ENERGIA_NETA_MANTENIMIENTO to 5.5,
                    Insumo.PROTEINA_CRUDA to 18.0,
                    Insumo.FIBRA_DETERGENTE_NEUTRA to 42.0,
                    Insumo.FIBRA_DETERGENTE_ACIDA to 32.0,
                    Insumo.EXTRACTO_ETEREO to 2.5,
                    Insumo.CENIZAS to 10.2,
                    Insumo.DEGRADABILIDAD_RUMINAL to 70.0,
                    Insumo.METANO_PRODUCIDO to 16.5,
                    Insumo.CALCIO to 13.5,
                    Insumo.FOSFORO to 2.4,
                    Insumo.MAGNESIO to 2.8,
                    Insumo.SODIO to 1.2,
                    Insumo.POTASIO to 24.0,
                    Insumo.AZUFRE to 2.5,
                    Insumo.COBRE to 10.0,
                    Insumo.ZINC to 28.0,
                    Insumo.SELENIO to 0.18,
                    Insumo.COBALTO to 0.15
                )
            ),
            Insumo(
                id = "catalog_ensilaje_maiz",
                nombre = "Ensilaje de Maíz",
                costo = 0.12,
                esForraje = true,
                inclusionMinima = 0.0,
                inclusionMaxima = 100.0,
                nutrientes = mapOf(
                    Insumo.ENERGIA_BRUTA to 18.0,
                    Insumo.ENERGIA_METABOLIZABLE to 10.5,
                    Insumo.ENERGIA_NETA_MANTENIMIENTO to 6.2,
                    Insumo.PROTEINA_CRUDA to 8.5,
                    Insumo.FIBRA_DETERGENTE_NEUTRA to 38.0,
                    Insumo.FIBRA_DETERGENTE_ACIDA to 24.0,
                    Insumo.EXTRACTO_ETEREO to 3.0,
                    Insumo.CENIZAS to 4.8,
                    Insumo.DEGRADABILIDAD_RUMINAL to 75.0,
                    Insumo.METANO_PRODUCIDO to 14.2,
                    Insumo.CALCIO to 2.5,
                    Insumo.FOSFORO to 2.2,
                    Insumo.MAGNESIO to 1.8,
                    Insumo.SODIO to 0.3,
                    Insumo.POTASIO to 12.0,
                    Insumo.AZUFRE to 1.2,
                    Insumo.COBRE to 6.5,
                    Insumo.ZINC to 20.0,
                    Insumo.SELENIO to 0.10,
                    Insumo.COBALTO to 0.08
                )
            ),

            // ========================================
            // CONCENTRADOS ENERGÉTICOS
            // ========================================
            Insumo(
                id = "catalog_maiz_molido",
                nombre = "Maíz Molido",
                costo = 0.35,
                esForraje = false,
                inclusionMinima = 0.0,
                inclusionMaxima = 60.0,  // Restricción típica para rumiantes
                nutrientes = mapOf(
                    Insumo.ENERGIA_BRUTA to 18.6,
                    Insumo.ENERGIA_METABOLIZABLE to 13.5,  // PRIORITARIO
                    Insumo.ENERGIA_NETA_MANTENIMIENTO to 8.8,
                    Insumo.PROTEINA_CRUDA to 9.0,
                    Insumo.FIBRA_DETERGENTE_NEUTRA to 10.5,
                    Insumo.FIBRA_DETERGENTE_ACIDA to 3.5,
                    Insumo.EXTRACTO_ETEREO to 3.8,
                    Insumo.CENIZAS to 1.5,
                    Insumo.DEGRADABILIDAD_RUMINAL to 85.0,
                    Insumo.METANO_PRODUCIDO to 11.5,
                    Insumo.CALCIO to 0.2,
                    Insumo.FOSFORO to 3.1,
                    Insumo.MAGNESIO to 1.2,
                    Insumo.SODIO to 0.1,
                    Insumo.POTASIO to 4.0,
                    Insumo.AZUFRE to 1.0,
                    Insumo.COBRE to 3.5,
                    Insumo.ZINC to 18.0,
                    Insumo.SELENIO to 0.05,
                    Insumo.COBALTO to 0.04
                )
            ),
            Insumo(
                id = "catalog_sorgo_grano",
                nombre = "Sorgo Grano",
                costo = 0.28,
                esForraje = false,
                inclusionMinima = 0.0,
                inclusionMaxima = 60.0,
                nutrientes = mapOf(
                    Insumo.ENERGIA_BRUTA to 18.0,
                    Insumo.ENERGIA_METABOLIZABLE to 12.8,
                    Insumo.ENERGIA_NETA_MANTENIMIENTO to 8.2,
                    Insumo.PROTEINA_CRUDA to 10.5,
                    Insumo.FIBRA_DETERGENTE_NEUTRA to 12.0,
                    Insumo.FIBRA_DETERGENTE_ACIDA to 4.2,
                    Insumo.EXTRACTO_ETEREO to 3.2,
                    Insumo.CENIZAS to 1.8,
                    Insumo.DEGRADABILIDAD_RUMINAL to 82.0,
                    Insumo.METANO_PRODUCIDO to 11.8,
                    Insumo.CALCIO to 0.3,
                    Insumo.FOSFORO to 3.5,
                    Insumo.MAGNESIO to 1.5,
                    Insumo.SODIO to 0.15,
                    Insumo.POTASIO to 4.2,
                    Insumo.AZUFRE to 1.1,
                    Insumo.COBRE to 4.0,
                    Insumo.ZINC to 16.0,
                    Insumo.SELENIO to 0.06,
                    Insumo.COBALTO to 0.05
                )
            ),
            Insumo(
                id = "catalog_melaza_cana",
                nombre = "Melaza de Caña",
                costo = 0.15,
                esForraje = false,
                inclusionMinima = 0.0,
                inclusionMaxima = 10.0,  // Restricción típica: máximo 10% por palatabilidad
                nutrientes = mapOf(
                    Insumo.ENERGIA_BRUTA to 16.2,
                    Insumo.ENERGIA_METABOLIZABLE to 12.0,
                    Insumo.ENERGIA_NETA_MANTENIMIENTO to 7.5,
                    Insumo.PROTEINA_CRUDA to 4.5,
                    Insumo.FIBRA_DETERGENTE_NEUTRA to 0.0,
                    Insumo.FIBRA_DETERGENTE_ACIDA to 0.0,
                    Insumo.EXTRACTO_ETEREO to 0.2,
                    Insumo.CENIZAS to 9.8,
                    Insumo.DEGRADABILIDAD_RUMINAL to 98.0,
                    Insumo.METANO_PRODUCIDO to 13.2,
                    Insumo.CALCIO to 8.5,
                    Insumo.FOSFORO to 0.8,
                    Insumo.MAGNESIO to 3.5,
                    Insumo.SODIO to 2.8,
                    Insumo.POTASIO to 42.0,
                    Insumo.AZUFRE to 4.2,
                    Insumo.COBRE to 15.0,
                    Insumo.ZINC to 8.0,
                    Insumo.SELENIO to 0.08,
                    Insumo.COBALTO to 0.12
                )
            ),

            // ========================================
            // SUPLEMENTOS PROTEICOS
            // ========================================
            Insumo(
                id = "catalog_soya_pasta",
                nombre = "Pasta de Soya (44% PC)",
                costo = 0.65,
                esForraje = false,
                inclusionMinima = 0.0,
                inclusionMaxima = 35.0,
                nutrientes = mapOf(
                    Insumo.ENERGIA_BRUTA to 19.5,
                    Insumo.ENERGIA_METABOLIZABLE to 13.2,
                    Insumo.ENERGIA_NETA_MANTENIMIENTO to 8.5,
                    Insumo.PROTEINA_CRUDA to 48.0,
                    Insumo.FIBRA_DETERGENTE_NEUTRA to 9.0,
                    Insumo.FIBRA_DETERGENTE_ACIDA to 5.5,
                    Insumo.EXTRACTO_ETEREO to 1.5,
                    Insumo.CENIZAS to 6.5,
                    Insumo.DEGRADABILIDAD_RUMINAL to 65.0,
                    Insumo.METANO_PRODUCIDO to 10.2,
                    Insumo.CALCIO to 3.0,
                    Insumo.FOSFORO to 6.5,
                    Insumo.MAGNESIO to 2.8,
                    Insumo.SODIO to 0.3,
                    Insumo.POTASIO to 20.0,
                    Insumo.AZUFRE to 4.0,
                    Insumo.COBRE to 18.0,
                    Insumo.ZINC to 45.0,
                    Insumo.SELENIO to 0.25,
                    Insumo.COBALTO to 0.18
                )
            ),
            Insumo(
                id = "catalog_canola_pasta",
                nombre = "Pasta de Canola",
                costo = 0.55,
                esForraje = false,
                inclusionMinima = 0.0,
                inclusionMaxima = 30.0,
                nutrientes = mapOf(
                    Insumo.ENERGIA_BRUTA to 19.0,
                    Insumo.ENERGIA_METABOLIZABLE to 12.5,
                    Insumo.ENERGIA_NETA_MANTENIMIENTO to 7.8,
                    Insumo.PROTEINA_CRUDA to 38.0,
                    Insumo.FIBRA_DETERGENTE_NEUTRA to 25.0,
                    Insumo.FIBRA_DETERGENTE_ACIDA to 18.0,
                    Insumo.EXTRACTO_ETEREO to 3.5,
                    Insumo.CENIZAS to 7.2,
                    Insumo.DEGRADABILIDAD_RUMINAL to 60.0,
                    Insumo.METANO_PRODUCIDO to 11.0,
                    Insumo.CALCIO to 7.0,
                    Insumo.FOSFORO to 11.0,
                    Insumo.MAGNESIO to 5.0,
                    Insumo.SODIO to 0.8,
                    Insumo.POTASIO to 12.0,
                    Insumo.AZUFRE to 8.5,
                    Insumo.COBRE to 6.0,
                    Insumo.ZINC to 55.0,
                    Insumo.SELENIO to 1.2,
                    Insumo.COBALTO to 0.20
                )
            ),
            Insumo(
                id = "catalog_urea",
                nombre = "Urea (46% N)",
                costo = 0.45,
                esForraje = false,
                inclusionMinima = 0.0,
                inclusionMaxima = 3.0,  // Máximo 3% por toxicidad
                nutrientes = mapOf(
                    Insumo.ENERGIA_BRUTA to 0.0,
                    Insumo.ENERGIA_METABOLIZABLE to 0.0,
                    Insumo.ENERGIA_NETA_MANTENIMIENTO to 0.0,
                    Insumo.PROTEINA_CRUDA to 288.0,  // Equivalente de proteína (46% N × 6.25)
                    Insumo.FIBRA_DETERGENTE_NEUTRA to 0.0,
                    Insumo.FIBRA_DETERGENTE_ACIDA to 0.0,
                    Insumo.EXTRACTO_ETEREO to 0.0,
                    Insumo.CENIZAS to 0.0,
                    Insumo.DEGRADABILIDAD_RUMINAL to 100.0,
                    Insumo.METANO_PRODUCIDO to 0.0,
                    Insumo.CALCIO to 0.0,
                    Insumo.FOSFORO to 0.0,
                    Insumo.MAGNESIO to 0.0,
                    Insumo.SODIO to 0.0,
                    Insumo.POTASIO to 0.0,
                    Insumo.AZUFRE to 0.0,
                    Insumo.COBRE to 0.0,
                    Insumo.ZINC to 0.0,
                    Insumo.SELENIO to 0.0,
                    Insumo.COBALTO to 0.0
                )
            ),

            // ========================================
            // SUBPRODUCTOS
            // ========================================
            Insumo(
                id = "catalog_pulpa_citrica",
                nombre = "Pulpa Cítrica Seca",
                costo = 0.18,
                esForraje = false,
                inclusionMinima = 0.0,
                inclusionMaxima = 40.0,
                nutrientes = mapOf(
                    Insumo.ENERGIA_BRUTA to 17.8,
                    Insumo.ENERGIA_METABOLIZABLE to 12.5,
                    Insumo.ENERGIA_NETA_MANTENIMIENTO to 7.8,
                    Insumo.PROTEINA_CRUDA to 7.0,
                    Insumo.FIBRA_DETERGENTE_NEUTRA to 24.0,
                    Insumo.FIBRA_DETERGENTE_ACIDA to 18.0,
                    Insumo.EXTRACTO_ETEREO to 2.8,
                    Insumo.CENIZAS to 6.5,
                    Insumo.DEGRADABILIDAD_RUMINAL to 80.0,
                    Insumo.METANO_PRODUCIDO to 12.8,
                    Insumo.CALCIO to 18.0,
                    Insumo.FOSFORO to 1.2,
                    Insumo.MAGNESIO to 2.2,
                    Insumo.SODIO to 1.8,
                    Insumo.POTASIO to 10.0,
                    Insumo.AZUFRE to 1.5,
                    Insumo.COBRE to 8.0,
                    Insumo.ZINC to 12.0,
                    Insumo.SELENIO to 0.12,
                    Insumo.COBALTO to 0.08
                )
            ),
            Insumo(
                id = "catalog_salvado_trigo",
                nombre = "Salvado de Trigo",
                costo = 0.22,
                esForraje = false,
                inclusionMinima = 0.0,
                inclusionMaxima = 35.0,
                nutrientes = mapOf(
                    Insumo.ENERGIA_BRUTA to 18.5,
                    Insumo.ENERGIA_METABOLIZABLE to 11.8,
                    Insumo.ENERGIA_NETA_MANTENIMIENTO to 7.2,
                    Insumo.PROTEINA_CRUDA to 16.0,
                    Insumo.FIBRA_DETERGENTE_NEUTRA to 42.0,
                    Insumo.FIBRA_DETERGENTE_ACIDA to 12.0,
                    Insumo.EXTRACTO_ETEREO to 4.0,
                    Insumo.CENIZAS to 5.8,
                    Insumo.DEGRADABILIDAD_RUMINAL to 70.0,
                    Insumo.METANO_PRODUCIDO to 13.5,
                    Insumo.CALCIO to 1.2,
                    Insumo.FOSFORO to 12.0,
                    Insumo.MAGNESIO to 5.0,
                    Insumo.SODIO to 0.2,
                    Insumo.POTASIO to 12.0,
                    Insumo.AZUFRE to 1.8,
                    Insumo.COBRE to 10.0,
                    Insumo.ZINC to 80.0,
                    Insumo.SELENIO to 0.15,
                    Insumo.COBALTO to 0.10
                )
            )
        )
    }

    /**
     * Crea un nuevo animal basado en uno del catálogo
     * Genera un nuevo ID único
     */
    fun createAnimalFromCatalog(catalogAnimal: Animal, customName: String? = null): Animal {
        return catalogAnimal.copy(
            id = UUID.randomUUID().toString(),
            nombre = customName ?: catalogAnimal.nombre
        )
    }

    /**
     * Crea un nuevo insumo basado en uno del catálogo
     * Genera un nuevo ID único
     */
    fun createIngredientFromCatalog(catalogIngredient: Insumo, customName: String? = null): Insumo {
        return catalogIngredient.copy(
            id = UUID.randomUUID().toString(),
            nombre = customName ?: catalogIngredient.nombre
        )
    }
}