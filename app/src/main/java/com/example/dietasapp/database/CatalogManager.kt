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
    fun getIngredientsCatalog(): List<Insumo> {
        return listOf(
            // Granos y concentrados energéticos
            Insumo(
                id = "catalog_maiz_grano",
                nombre = "Maíz Grano",
                costo = 0.22,
                esForraje = false,
                nutrientes = mapOf(
                    "GE" to 4.44,
                    "CP" to 9.0,
                    "TDN" to 90.0,
                    "NEm" to 2.21,
                    "Ca" to 0.02,
                    "P" to 0.31,
                    "NDF" to 10.5,
                    "Starch" to 72.0,
                    "Fat" to 3.8
                )
            ),
            Insumo(
                id = "catalog_sorgo_grano",
                nombre = "Sorgo Grano",
                costo = 0.18,
                esForraje = false,
                nutrientes = mapOf(
                    "GE" to 4.30,
                    "CP" to 10.5,
                    "TDN" to 87.0,
                    "NEm" to 2.10,
                    "Ca" to 0.03,
                    "P" to 0.35,
                    "NDF" to 12.0,
                    "Starch" to 68.0,
                    "Fat" to 3.2
                )
            ),
            Insumo(
                id = "catalog_cebada_grano",
                nombre = "Cebada Grano",
                costo = 0.20,
                esForraje = false,
                nutrientes = mapOf(
                    "GE" to 4.35,
                    "CP" to 12.0,
                    "TDN" to 88.0,
                    "NEm" to 2.15,
                    "Ca" to 0.05,
                    "P" to 0.38,
                    "NDF" to 18.0,
                    "Starch" to 60.0,
                    "Fat" to 2.1
                )
            ),
            // Forrajes
            Insumo(
                id = "catalog_alfalfa_heno",
                nombre = "Alfalfa Heno",
                costo = 0.15,
                esForraje = true,
                nutrientes = mapOf(
                    "GE" to 4.20,
                    "CP" to 18.0,
                    "TDN" to 60.0,
                    "NEm" to 1.32,
                    "Ca" to 1.35,
                    "P" to 0.24,
                    "NDF" to 42.0,
                    "Starch" to 2.0,
                    "Fat" to 2.5
                )
            ),
            Insumo(
                id = "catalog_pasto_bermuda",
                nombre = "Pasto Bermuda Heno",
                costo = 0.10,
                esForraje = true,
                nutrientes = mapOf(
                    "GE" to 4.10,
                    "CP" to 10.5,
                    "TDN" to 55.0,
                    "NEm" to 1.20,
                    "Ca" to 0.45,
                    "P" to 0.20,
                    "NDF" to 65.0,
                    "Starch" to 1.5,
                    "Fat" to 2.0
                )
            ),
            Insumo(
                id = "catalog_ensilaje_maiz",
                nombre = "Ensilaje de Maíz",
                costo = 0.08,
                esForraje = true,
                nutrientes = mapOf(
                    "GE" to 3.95,
                    "CP" to 8.5,
                    "TDN" to 68.0,
                    "NEm" to 1.60,
                    "Ca" to 0.25,
                    "P" to 0.22,
                    "NDF" to 38.0,
                    "Starch" to 28.0,
                    "Fat" to 3.0
                )
            ),
            // Suplementos proteicos
            Insumo(
                id = "catalog_soya_pasta",
                nombre = "Pasta de Soya",
                costo = 0.45,
                esForraje = false,
                nutrientes = mapOf(
                    "GE" to 4.50,
                    "CP" to 48.0,
                    "TDN" to 84.0,
                    "NEm" to 2.25,
                    "Ca" to 0.30,
                    "P" to 0.65,
                    "NDF" to 9.0,
                    "Starch" to 5.0,
                    "Fat" to 1.5
                )
            ),
            Insumo(
                id = "catalog_canola_pasta",
                nombre = "Pasta de Canola",
                costo = 0.38,
                esForraje = false,
                nutrientes = mapOf(
                    "GE" to 4.40,
                    "CP" to 38.0,
                    "TDN" to 78.0,
                    "NEm" to 2.05,
                    "Ca" to 0.70,
                    "P" to 1.10,
                    "NDF" to 25.0,
                    "Starch" to 3.0,
                    "Fat" to 3.5
                )
            ),
            // Subproductos
            Insumo(
                id = "catalog_pulpa_citrica",
                nombre = "Pulpa Cítrica Seca",
                costo = 0.12,
                esForraje = false,
                nutrientes = mapOf(
                    "GE" to 3.90,
                    "CP" to 7.0,
                    "TDN" to 72.0,
                    "NEm" to 1.75,
                    "Ca" to 1.85,
                    "P" to 0.13,
                    "NDF" to 22.0,
                    "Starch" to 2.0,
                    "Fat" to 2.8
                )
            ),
            Insumo(
                id = "catalog_melaza",
                nombre = "Melaza de Caña",
                costo = 0.10,
                esForraje = false,
                nutrientes = mapOf(
                    "GE" to 3.20,
                    "CP" to 4.5,
                    "TDN" to 74.0,
                    "NEm" to 1.82,
                    "Ca" to 0.90,
                    "P" to 0.08,
                    "NDF" to 0.0,
                    "Starch" to 0.0,
                    "Fat" to 0.1
                )
            ),
            // Minerales y suplementos
            Insumo(
                id = "catalog_carbonato_calcio",
                nombre = "Carbonato de Calcio",
                costo = 0.08,
                esForraje = false,
                nutrientes = mapOf(
                    "GE" to 0.0,
                    "CP" to 0.0,
                    "TDN" to 0.0,
                    "NEm" to 0.0,
                    "Ca" to 38.0,
                    "P" to 0.0,
                    "NDF" to 0.0,
                    "Starch" to 0.0,
                    "Fat" to 0.0
                )
            ),
            Insumo(
                id = "catalog_fosfato_dicalcico",
                nombre = "Fosfato Dicálcico",
                costo = 0.35,
                esForraje = false,
                nutrientes = mapOf(
                    "GE" to 0.0,
                    "CP" to 0.0,
                    "TDN" to 0.0,
                    "NEm" to 0.0,
                    "Ca" to 24.0,
                    "P" to 18.5,
                    "NDF" to 0.0,
                    "Starch" to 0.0,
                    "Fat" to 0.0
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