package com.example.dietasapp.database

import android.content.Context
import com.example.dietasapp.data.Animal
import com.example.dietasapp.data.Insumo
import com.example.dietasapp.data.TipoDieta
import com.example.dietasapp.data.prefs.AppPrefs
import java.util.UUID



/**
 * Gestor de catálogos base validado científicamente.
 * Fuentes: NRC 2001/2012, NASEM 2016/2021, Feedipedia, FEDNA 2019, INRA-CIRAD-AFZ
 * Precios: México 2024-2025 (MXN/kg)
 * Última validación: Octubre 2024
 */
class CatalogManager(private val context: Context) {

    private fun currentSpecies(): String =
        AppPrefs.getTipoAnimal(context) ?: AppPrefs.TIPO_MULTI

    fun getAnimalsCatalog(): List<Animal> = when (currentSpecies()) {
        AppPrefs.TIPO_MONO -> getMonoAnimals()
        AppPrefs.TIPO_AVES -> getPoultryAnimals()
        else -> getMultiAnimals()
    }

    fun obtenerCatalogoInsumos(): List<Insumo> = when (currentSpecies()) {
        AppPrefs.TIPO_MONO -> getMonoInsumos()
        AppPrefs.TIPO_AVES -> getPoultryInsumos()
        else -> getMultiInsumos()
    }

    fun createAnimalFromCatalog(catalogAnimal: Animal, customName: String? = null): Animal {
        return catalogAnimal.copy(
            id = UUID.randomUUID().toString(),
            nombre = customName ?: catalogAnimal.nombre
        )
    }

    fun createIngredientFromCatalog(catalogIngredient: Insumo, customName: String? = null): Insumo {
        return catalogIngredient.copy(
            id = UUID.randomUUID().toString(),
            nombre = customName ?: catalogIngredient.nombre
        )
    }

    // ============================
    //       MULTIGÁSTRICOS (RUMIANTES)
    // ============================
    private fun getMultiAnimals(): List<Animal> {
        return listOf(
            // Ganado lechero
            Animal(
                id = "catalog_vaca_lechera_alta",
                nombre = "Vaca Lechera - Alta Producción",
                tipo = TipoDieta.ALTO_FORRAJE,
                pesoKg = 550.0,
                consumoDMI = 24.0,
                requerimientosMinimos = mapOf(
                    Animal.PROTEINA_CRUDA to 16.5,  // Validado NRC 2001

                    Animal.CALCIO to 0.70,  // Validado NRC 2001
                    Animal.FOSFORO to 0.40  // Validado NRC 2001
                ),
                requerimientosMaximos = mapOf(
                    // NOTA: FDN 28-33% es ÓPTIMO (no máximo rígido)
                    Animal.FIBRA_DETERGENTE_NEUTRA to 33.0,  // óptimo superior del rango
                    Animal.PROTEINA_CRUDA to 19.0            // límite económico/ambiental
                )
            ),
            Animal(
                id = "catalog_vaca_lechera_media",
                nombre = "Vaca Lechera - Producción Media",
                tipo = TipoDieta.ALTO_FORRAJE,
                pesoKg = 600.0,
                consumoDMI = 20.0,
                requerimientosMinimos = mapOf(
                    Animal.PROTEINA_CRUDA to 14.5,  // óptimo vs mínimo absoluto 14%

                    Animal.CALCIO to 0.50,
                    Animal.FOSFORO to 0.35
                ),
                requerimientosMaximos = mapOf(
                    Animal.FIBRA_DETERGENTE_NEUTRA to 40.0, // permite más forraje
                    Animal.PROTEINA_CRUDA to 17.0
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
                    Animal.PROTEINA_CRUDA to 13.0,  // NASEM 2016
                    Animal.CALCIO to 0.40,
                    Animal.FOSFORO to 0.30
                ),
                requerimientosMaximos = mapOf(
                    Animal.FIBRA_DETERGENTE_NEUTRA to 45.0, // sistema alto forraje
                    Animal.PROTEINA_CRUDA to 16.0
                )
            ),
            Animal(
                id = "catalog_novillo_engorde_final",
                nombre = "Novillo Engorde - Finalización",
                tipo = TipoDieta.BAJO_FORRAJE,
                pesoKg = 450.0,
                consumoDMI = 12.0,
                requerimientosMinimos = mapOf(
                    Animal.PROTEINA_CRUDA to 12.0,
                    Animal.CALCIO to 0.35,
                    Animal.FOSFORO to 0.28,
                    // CRÍTICO: FDN 20-25% es MÍNIMO para salud ruminal
                    Animal.FIBRA_DETERGENTE_NEUTRA to 20.0  // mínimo funcional
                ),
                requerimientosMaximos = mapOf(
                    Animal.FIBRA_DETERGENTE_NEUTRA to 25.0, // rango óptimo 20-25%
                    Animal.PROTEINA_CRUDA to 14.5
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
                    Animal.PROTEINA_CRUDA to 10.5,  // último tercio gestación
                    Animal.CALCIO to 0.40,
                    Animal.FOSFORO to 0.25
                ),
                requerimientosMaximos = mapOf(
                    Animal.FIBRA_DETERGENTE_NEUTRA to 50.0, // forraje calidad media
                    Animal.PROTEINA_CRUDA to 13.0
                )
            ),
            Animal(
                id = "catalog_becerro_destete",
                nombre = "Becerro Post-Destete",
                tipo = TipoDieta.INTERMEDIO,
                pesoKg = 180.0,
                consumoDMI = 5.5,
                requerimientosMinimos = mapOf(
                    Animal.PROTEINA_CRUDA to 14.0,  // crecimiento
                    Animal.CALCIO to 0.45,  // crecimiento esquelético
                    Animal.FOSFORO to 0.32
                ),
                requerimientosMaximos = mapOf(
                    Animal.FIBRA_DETERGENTE_NEUTRA to 35.0,
                    Animal.PROTEINA_CRUDA to 17.0
                )
            )
        )
    }

    private fun getMultiInsumos(): List<Insumo> {
        return listOf(
            // ===== FORRAJES =====
            Insumo(
                id = "catalog_pasto_taiwan",
                nombre = "Pasto Taiwán (Pennisetum purpureum)",
                costo = 1.50, // MXN/kg MS (~4.50 MXN/kg forraje fresco)
                esForraje = true,
                inclusionMinima = 0.0,
                inclusionMaxima = 100.0,
                nutrientes = mapOf(
                    Insumo.ENERGIA_METABOLIZABLE to 8.7,  // Ajustado (rango 5.9-10.8)
                    Insumo.PROTEINA_CRUDA to 10.0,  // Ajustado: típico a 45 días
                    Insumo.FIBRA_DETERGENTE_NEUTRA to 65.0, // Validado (55-75%)
                    Insumo.FIBRA_DETERGENTE_ACIDA to 40.0,  // Ajustado
                    Insumo.EXTRACTO_ETEREO to 2.1,
                    Insumo.CENIZAS to 9.5,
                    Insumo.DEGRADABILIDAD_RUMINAL to 65.0,
                    Insumo.METANO_PRODUCIDO to 18.07,
                    Insumo.CALCIO to 4.0,   // Ajustado (g/kg MS)
                    Insumo.FOSFORO to 2.5,  // Ajustado
                    Insumo.MAGNESIO to 2.2,
                    Insumo.SODIO to 0.5,
                    Insumo.POTASIO to 28.0,
                    Insumo.AZUFRE to 1.8,
                    Insumo.COBRE to 8.5,
                    Insumo.ZINC to 25.0,
                    Insumo.SELENIO to 0.15,
                    Insumo.COBALTO to 0.12
                )
            ),
            Insumo(
                id = "catalog_pasto_guinea",
                nombre = "Pasto Guinea (Panicum maximum)",
                costo = 1.40, // MXN/kg MS
                esForraje = true,
                inclusionMinima = 0.0,
                inclusionMaxima = 100.0,
                nutrientes = mapOf(
                    Insumo.ENERGIA_METABOLIZABLE to 9.0,  // Ajustado
                    Insumo.PROTEINA_CRUDA to 8.5,  // Ajustado: típico
                    Insumo.FIBRA_DETERGENTE_NEUTRA to 68.0, // Validado (62-76%)
                    Insumo.FIBRA_DETERGENTE_ACIDA to 42.0,  // Ajustado
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
                costo = 5.80, // MXN/kg MS (5.00-6.67 rango)
                esForraje = true,
                inclusionMinima = 0.0,
                inclusionMaxima = 100.0,
                nutrientes = mapOf(
                    Insumo.ENERGIA_METABOLIZABLE to 9.1,  // Validado
                    Insumo.PROTEINA_CRUDA to 17.0, // Ajustado: promedio validado
                    Insumo.FIBRA_DETERGENTE_NEUTRA to 42.0, // Validado (37-46%)
                    Insumo.FIBRA_DETERGENTE_ACIDA to 32.0, // Validado (28-36.6%)
                    Insumo.EXTRACTO_ETEREO to 2.5,
                    Insumo.CENIZAS to 10.2,
                    Insumo.DEGRADABILIDAD_RUMINAL to 75.0, // Ajustado (70-80%)
                    Insumo.METANO_PRODUCIDO to 16.5,
                    Insumo.CALCIO to 15.0,  // Validado: excepcional
                    Insumo.FOSFORO to 2.5,  // Validado
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
                costo = 2.40, // MXN/kg MS (2.00-2.80 rango)
                esForraje = true,
                inclusionMinima = 0.0,
                inclusionMaxima = 100.0,
                nutrientes = mapOf(
                    Insumo.ENERGIA_METABOLIZABLE to 10.5, // Validado (9.5-11.5)
                    Insumo.PROTEINA_CRUDA to 8.0,  // Ajustado: típico (7-9%)
                    Insumo.FIBRA_DETERGENTE_NEUTRA to 42.0, // Validado (38-50%)
                    Insumo.FIBRA_DETERGENTE_ACIDA to 24.0, // Validado
                    Insumo.EXTRACTO_ETEREO to 3.0,
                    Insumo.CENIZAS to 4.8,
                    Insumo.DEGRADABILIDAD_RUMINAL to 75.0,
                    Insumo.METANO_PRODUCIDO to 14.2,
                    Insumo.CALCIO to 2.5,
                    Insumo.FOSFORO to 2.0,  // Ajustado
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
            // ===== CONCENTRADOS ENERGÉTICOS =====
            Insumo(
                id = "catalog_maiz_molido",
                nombre = "Maíz Molido",
                costo = 4.85, // MXN/kg (4.65-5.03 rango)
                esForraje = false,
                inclusionMinima = 0.0,
                inclusionMaxima = 60.0,
                nutrientes = mapOf(
                    Insumo.ENERGIA_METABOLIZABLE to 13.5, // Validado
                    Insumo.PROTEINA_CRUDA to 9.0,  // Validado (8-12%)
                    Insumo.FIBRA_DETERGENTE_NEUTRA to 10.5,
                    Insumo.FIBRA_DETERGENTE_ACIDA to 3.5,
                    Insumo.EXTRACTO_ETEREO to 4.0,  // Ajustado
                    Insumo.CENIZAS to 1.5,
                    Insumo.DEGRADABILIDAD_RUMINAL to 85.0,
                    Insumo.METANO_PRODUCIDO to 11.5,
                    Insumo.CALCIO to 0.3,  // Ajustado
                    Insumo.FOSFORO to 2.8, // Validado
                    Insumo.MAGNESIO to 1.2,
                    Insumo.SODIO to 0.1,
                    Insumo.POTASIO to 4.0,
                    Insumo.AZUFRE to 1.0,
                    Insumo.COBRE to 3.5,
                    Insumo.ZINC to 20.0,  // Ajustado (20-30)
                    Insumo.SELENIO to 0.05,
                    Insumo.COBALTO to 0.04
                )
            ),
            Insumo(
                id = "catalog_sorgo_grano",
                nombre = "Sorgo Grano",
                costo = 4.55, // MXN/kg (4.10-5.03 rango)
                esForraje = false,
                inclusionMinima = 0.0,
                inclusionMaxima = 60.0,
                nutrientes = mapOf(
                    Insumo.ENERGIA_METABOLIZABLE to 12.8, // Validado (12.8-13.5)
                    Insumo.PROTEINA_CRUDA to 10.5, // Validado (9-13%)
                    Insumo.FIBRA_DETERGENTE_NEUTRA to 12.0,
                    Insumo.FIBRA_DETERGENTE_ACIDA to 4.2,
                    Insumo.EXTRACTO_ETEREO to 3.5,  // Ajustado
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
                costo = 3.00, // MXN/kg (2.00-3.70 mayoreo)
                esForraje = false,
                inclusionMinima = 0.0,
                inclusionMaxima = 10.0, // CRÍTICO: nunca exceder por toxicidad
                nutrientes = mapOf(

                    Insumo.ENERGIA_METABOLIZABLE to 11.5, // Ajustado (11.0-12.5)

                    Insumo.PROTEINA_CRUDA to 6.0,  // Ajustado: típico
                    Insumo.FIBRA_DETERGENTE_NEUTRA to 0.0, // Validado
                    Insumo.FIBRA_DETERGENTE_ACIDA to 0.0,  // Validado
                    Insumo.EXTRACTO_ETEREO to 0.2,
                    Insumo.CENIZAS to 9.8,
                    Insumo.DEGRADABILIDAD_RUMINAL to 98.0,
                    Insumo.METANO_PRODUCIDO to 13.2,
                    Insumo.CALCIO to 9.0,   // Ajustado (8-10)
                    Insumo.FOSFORO to 0.8,  // Validado: muy bajo
                    Insumo.MAGNESIO to 3.5,
                    Insumo.SODIO to 2.8,
                    Insumo.POTASIO to 40.0, // Ajustado: muy alto
                    Insumo.AZUFRE to 8.2,   // Ajustado: muy alto
                    Insumo.COBRE to 15.0,
                    Insumo.ZINC to 8.0,
                    Insumo.SELENIO to 0.08,
                    Insumo.COBALTO to 0.12
                )
            ),
            // ===== SUPLEMENTOS PROTEICOS =====
            Insumo(
                id = "catalog_soya_pasta",
                nombre = "Pasta de Soya (44% PC)",
                costo = 8.50, // MXN/kg (6.00-11.90 rango, común 6.50-10.00)
                esForraje = false,
                inclusionMinima = 0.0,
                inclusionMaxima = 35.0,
                nutrientes = mapOf(
                    Insumo.ENERGIA_METABOLIZABLE to 13.0,  // Ajustado (12.5-13.5)
                    Insumo.PROTEINA_CRUDA to 49.0, // Ajustado: base MS (44% húmedo)
                    Insumo.FIBRA_DETERGENTE_NEUTRA to 12.0, // Ajustado (10-15%)
                    Insumo.FIBRA_DETERGENTE_ACIDA to 6.0,   // Ajustado
                    Insumo.EXTRACTO_ETEREO to 1.5,
                    Insumo.CENIZAS to 6.5,
                    Insumo.DEGRADABILIDAD_RUMINAL to 70.0, // Ajustado (65-80%)
                    Insumo.METANO_PRODUCIDO to 10.2,
                    Insumo.CALCIO to 3.5,   // Ajustado (3-4)
                    Insumo.FOSFORO to 6.5,  // Validado (6-7)
                    Insumo.MAGNESIO to 2.8,
                    Insumo.SODIO to 0.3,
                    Insumo.POTASIO to 20.0, // Ajustado (18-22)
                    Insumo.AZUFRE to 4.5,   // Ajustado (4-5)
                    Insumo.COBRE to 18.0,
                    Insumo.ZINC to 45.0,
                    Insumo.SELENIO to 0.25,
                    Insumo.COBALTO to 0.18
                )
            ),
            Insumo(
                id = "catalog_canola_pasta",
                nombre = "Pasta de Canola",
                costo = 3.90, // MXN/kg (3.00-4.90 rango, muy económica)
                esForraje = false,
                inclusionMinima = 0.0,
                inclusionMaxima = 30.0,
                nutrientes = mapOf(
                    Insumo.ENERGIA_METABOLIZABLE to 11.5, // Ajustado (11-12)
                    Insumo.PROTEINA_CRUDA to 38.0, // Validado (38-40%)
                    Insumo.FIBRA_DETERGENTE_NEUTRA to 27.0, // Ajustado (25-30%)
                    Insumo.FIBRA_DETERGENTE_ACIDA to 18.0,
                    Insumo.EXTRACTO_ETEREO to 3.5,
                    Insumo.CENIZAS to 7.2,
                    Insumo.DEGRADABILIDAD_RUMINAL to 50.0, // Ajustado (40-56%)
                    Insumo.METANO_PRODUCIDO to 11.0,
                    Insumo.CALCIO to 7.0,
                    Insumo.FOSFORO to 11.0,
                    Insumo.MAGNESIO to 5.0,
                    Insumo.SODIO to 0.8,
                    Insumo.POTASIO to 12.0,
                    Insumo.AZUFRE to 11.0,  // Ajustado (10-12): excepcional
                    Insumo.COBRE to 6.0,
                    Insumo.ZINC to 55.0,
                    Insumo.SELENIO to 1.2,
                    Insumo.COBALTO to 0.20
                )
            ),
            Insumo(
                id = "catalog_urea",
                nombre = "Urea (46% N)",
                costo = 5.25, // MXN/kg (5.00-5.50 rango)
                esForraje = false,
                inclusionMinima = 0.0,
                inclusionMaxima = 3.0, // CRÍTICO: límite absoluto por toxicidad
                nutrientes = mapOf(
                    Insumo.ENERGIA_METABOLIZABLE to 0.0,
                    Insumo.PROTEINA_CRUDA to 287.5, // Ajustado: 46% N × 6.25
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
            // ===== SUBPRODUCTOS =====
            Insumo(
                id = "catalog_pulpa_citrica",
                nombre = "Pulpa Cítrica Seca",
                costo = 4.25, // MXN/kg (3.50-5.00 estimado)
                esForraje = false,
                inclusionMinima = 0.0,
                inclusionMaxima = 40.0,
                nutrientes = mapOf(
                    Insumo.ENERGIA_METABOLIZABLE to 12.1, // Ajustado: validado
                    Insumo.PROTEINA_CRUDA to 6.5,  // Ajustado (6.0-7.0%)
                    Insumo.FIBRA_DETERGENTE_NEUTRA to 23.0, // Ajustado (22-23.4%)
                    Insumo.FIBRA_DETERGENTE_ACIDA to 20.0,  // Ajustado
                    Insumo.EXTRACTO_ETEREO to 2.8,
                    Insumo.CENIZAS to 6.5,
                    Insumo.DEGRADABILIDAD_RUMINAL to 85.0, // Ajustado: alta por pectina
                    Insumo.METANO_PRODUCIDO to 12.8,
                    Insumo.CALCIO to 17.0,  // Ajustado (15-20): muy alto
                    Insumo.FOSFORO to 1.2,  // Validado: muy bajo
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
                costo = 4.70, // MXN/kg (2.40-8.00, común ~5.00)
                esForraje = false,
                inclusionMinima = 0.0,
                inclusionMaxima = 35.0,
                nutrientes = mapOf(
                    Insumo.ENERGIA_METABOLIZABLE to 11.2, // Ajustado (10.5-12.0)
                    Insumo.PROTEINA_CRUDA to 16.0, // Validado (15.5-17%)
                    Insumo.FIBRA_DETERGENTE_NEUTRA to 42.0, // Validado (40-45%)
                    Insumo.FIBRA_DETERGENTE_ACIDA to 11.0,  // Ajustado (10-12%)
                    Insumo.EXTRACTO_ETEREO to 4.0,
                    Insumo.CENIZAS to 5.8,
                    Insumo.DEGRADABILIDAD_RUMINAL to 70.0,
                    Insumo.METANO_PRODUCIDO to 13.5,
                    Insumo.CALCIO to 1.2,   // Validado
                    Insumo.FOSFORO to 11.0, // Ajustado (10-12): excepcional
                    Insumo.MAGNESIO to 5.0,
                    Insumo.SODIO to 0.2,
                    Insumo.POTASIO to 12.0,
                    Insumo.AZUFRE to 1.8,
                    Insumo.COBRE to 10.0,
                    Insumo.ZINC to 90.0,    // Ajustado (80-100): muy alto
                    Insumo.SELENIO to 0.15,
                    Insumo.COBALTO to 0.10
                )
            )
        )
    }

    // ============================
    //        MONOGÁSTRICOS (CERDOS)
    // ============================
    private fun getMonoAnimals(): List<Animal> {
        return listOf(
            Animal(
                id = "catalog_cerda_gestante",
                nombre = "Cerda Gestante",
                tipo = TipoDieta.BAJO_FORRAJE,
                pesoKg = 180.0,
                consumoDMI = 3.2,
                requerimientosMinimos = mapOf(
                    Animal.PROTEINA_CRUDA to 13.0,   // NRC 2012 (12-14%)
                    Animal.CALCIO to 0.75,           // ↑ de 0.60%
                    Animal.FOSFORO to 0.60
                ),
                requerimientosMaximos = mapOf(
                    Animal.PROTEINA_CRUDA to 16.0
                )
            ),
            Animal(
                id = "catalog_cerda_lactante",
                nombre = "Cerda Lactante",
                tipo = TipoDieta.BAJO_FORRAJE,
                pesoKg = 190.0,
                consumoDMI = 6.0,
                requerimientosMinimos = mapOf(
                    Animal.PROTEINA_CRUDA to 16.0,
                    Animal.CALCIO to 0.75,
                    Animal.FOSFORO to 0.60
                ),
                requerimientosMaximos = mapOf(
                    Animal.PROTEINA_CRUDA to 19.0
                )
            ),
            Animal(
                id = "catalog_cerdos_crecimiento",
                nombre = "Cerdos Crecimiento (25–50 kg)",
                tipo = TipoDieta.BAJO_FORRAJE,
                pesoKg = 35.0,
                consumoDMI = 2.0,
                requerimientosMinimos = mapOf(
                    Animal.PROTEINA_CRUDA to 16.5,  // NRC 2012
                    Animal.CALCIO to 0.66,          // de 0.70%
                    Animal.FOSFORO to 0.56          // de 0.60%
                ),
                requerimientosMaximos = mapOf(
                    Animal.PROTEINA_CRUDA to 20.0
                )
            ),
            Animal(
                id = "catalog_cerdos_finalizacion",
                nombre = "Cerdos Finalización (90–120 kg)",
                tipo = TipoDieta.BAJO_FORRAJE,
                pesoKg = 100.0,
                consumoDMI = 3.0,
                requerimientosMinimos = mapOf(
                    Animal.PROTEINA_CRUDA to 14.0,
                    Animal.CALCIO to 0.55,
                    Animal.FOSFORO to 0.45
                ),
                requerimientosMaximos = mapOf(
                    Animal.PROTEINA_CRUDA to 16.0
                )
            )
        )
    }

    private fun getMonoInsumos(): List<Insumo> {
        return listOf(
            Insumo(
                id = "mono_maiz_molido",
                nombre = "Maíz Molido",
                costo = 4.85, // MXN/kg (4.65-5.03)
                esForraje = false,
                inclusionMinima = 0.0,
                inclusionMaxima = 80.0,
                nutrientes = mapOf(
                    Insumo.ENERGIA_METABOLIZABLE to 15.3, // Ajustado: cerdos (15.1-15.5)
                    Insumo.PROTEINA_CRUDA to 8.7,  // Ajustado
                    Insumo.FIBRA_DETERGENTE_NEUTRA to 10.0,
                    Insumo.FIBRA_DETERGENTE_ACIDA to 3.0,
                    Insumo.EXTRACTO_ETEREO to 3.9,  // Ajustado
                    Insumo.CENIZAS to 1.5,
                    Insumo.DEGRADABILIDAD_RUMINAL to 0.0, // N/A en cerdos
                    Insumo.METANO_PRODUCIDO to 0.0,       // N/A en cerdos
                    Insumo.CALCIO to 0.2,   // Ajustado
                    Insumo.FOSFORO to 2.7,  // Ajustado (0.25-0.30%)
                    Insumo.MAGNESIO to 1.2,
                    Insumo.SODIO to 0.1,
                    Insumo.POTASIO to 4.0,
                    Insumo.AZUFRE to 0.9,
                    Insumo.COBRE to 3.0,
                    Insumo.ZINC to 18.0,
                    Insumo.SELENIO to 0.05,
                    Insumo.COBALTO to 0.04
                )
            ),
            Insumo(
                id = "mono_pasta_soya",
                nombre = "Pasta de Soya",
                costo = 8.50, // MXN/kg
                esForraje = false,
                inclusionMinima = 0.0,
                inclusionMaxima = 35.0,
                nutrientes = mapOf(
                    Insumo.ENERGIA_METABOLIZABLE to 15.2, // Ajustado para cerdos
                    Insumo.PROTEINA_CRUDA to 48.0, // Ajustado (47.5-48.5% dehulled)
                    Insumo.FIBRA_DETERGENTE_NEUTRA to 9.0,  // Ajustado (dehulled)
                    Insumo.FIBRA_DETERGENTE_ACIDA to 4.0,   // Ajustado (dehulled)
                    Insumo.EXTRACTO_ETEREO to 1.5,
                    Insumo.CENIZAS to 6.5,
                    Insumo.DEGRADABILIDAD_RUMINAL to 0.0, // N/A
                    Insumo.METANO_PRODUCIDO to 0.0,       // N/A
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
                id = "mono_ddgs_maiz",
                nombre = "DDGS de Maíz",
                costo = 5.50, // MXN/kg (4.50-6.50 estimado)
                esForraje = false,
                inclusionMinima = 0.0,
                inclusionMaxima = 30.0,
                nutrientes = mapOf(
                    Insumo.ENERGIA_METABOLIZABLE to 15.5, // Ajustado para cerdos
                    Insumo.PROTEINA_CRUDA to 27.5, // Ajustado (27-28%)
                    Insumo.FIBRA_DETERGENTE_NEUTRA to 35.0, // Ajustado (30-40%)
                    Insumo.FIBRA_DETERGENTE_ACIDA to 17.0,
                    Insumo.EXTRACTO_ETEREO to 9.5,
                    Insumo.CENIZAS to 4.5,
                    Insumo.DEGRADABILIDAD_RUMINAL to 0.0, // N/A
                    Insumo.METANO_PRODUCIDO to 0.0,       // N/A
                    Insumo.CALCIO to 0.3,
                    Insumo.FOSFORO to 7.0,  // Ajustado (0.60-0.80%, 90% disponible)
                    Insumo.MAGNESIO to 3.0,
                    Insumo.SODIO to 0.3,
                    Insumo.POTASIO to 9.0,
                    Insumo.AZUFRE to 3.5,
                    Insumo.COBRE to 6.0,
                    Insumo.ZINC to 25.0,
                    Insumo.SELENIO to 0.15,
                    Insumo.COBALTO to 0.10
                )
            ),
            Insumo(
                id = "mono_aceite_vegetal",
                nombre = "Aceite Vegetal",
                costo = 40.0, // MXN/kg (35-45 mayoreo industrial)
                esForraje = false,
                inclusionMinima = 0.0,
                inclusionMaxima = 6.0,  // Ajustado: típico 2-6%
                nutrientes = mapOf(
                    Insumo.ENERGIA_METABOLIZABLE to 36.0, // Ajustado para cerdos
                    Insumo.PROTEINA_CRUDA to 0.0,
                    Insumo.FIBRA_DETERGENTE_NEUTRA to 0.0,
                    Insumo.FIBRA_DETERGENTE_ACIDA to 0.0,
                    Insumo.EXTRACTO_ETEREO to 99.5, // Ajustado
                    Insumo.CENIZAS to 0.0,
                    Insumo.DEGRADABILIDAD_RUMINAL to 0.0,
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
            Insumo(
                id = "mono_premix_vit_mineral",
                nombre = "Premix Vitamínico-Mineral",
                costo = 105.0, // MXN/kg (75-135 industrial común)
                esForraje = false,
                inclusionMinima = 0.1,
                inclusionMaxima = 1.0,
                nutrientes = mapOf(
                    Insumo.ENERGIA_METABOLIZABLE to 0.0,
                    Insumo.PROTEINA_CRUDA to 0.0,
                    Insumo.FIBRA_DETERGENTE_NEUTRA to 0.0,
                    Insumo.FIBRA_DETERGENTE_ACIDA to 0.0,
                    Insumo.EXTRACTO_ETEREO to 0.0,
                    Insumo.CENIZAS to 90.0,
                    Insumo.DEGRADABILIDAD_RUMINAL to 0.0,
                    Insumo.METANO_PRODUCIDO to 0.0,
                    Insumo.CALCIO to 25.0,
                    Insumo.FOSFORO to 20.0,
                    Insumo.MAGNESIO to 10.0,
                    Insumo.SODIO to 10.0,
                    Insumo.POTASIO to 0.0,
                    Insumo.AZUFRE to 5.0,
                    Insumo.COBRE to 200.0,
                    Insumo.ZINC to 500.0,
                    Insumo.SELENIO to 2.0,
                    Insumo.COBALTO to 1.0
                )
            ),
            Insumo(
                id = "mono_harina_pescado",
                nombre = "Harina de Pescado",
                costo = 26.0, // MXN/kg (18-35, común 20-25)
                esForraje = false,
                inclusionMinima = 0.0,
                inclusionMaxima = 10.0,
                nutrientes = mapOf(
                    Insumo.ENERGIA_METABOLIZABLE to 14.5, // Ajustado para cerdos
                    Insumo.PROTEINA_CRUDA to 65.0, // Ajustado (63-67%)
                    Insumo.FIBRA_DETERGENTE_NEUTRA to 0.0,
                    Insumo.FIBRA_DETERGENTE_ACIDA to 0.0,
                    Insumo.EXTRACTO_ETEREO to 9.5,  // Ajustado (9-10%)
                    Insumo.CENIZAS to 18.0, // Ajustado (17-19%)
                    Insumo.DEGRADABILIDAD_RUMINAL to 0.0,
                    Insumo.METANO_PRODUCIDO to 0.0,
                    Insumo.CALCIO to 50.0,  // Ajustado (4.0-6.0%)
                    Insumo.FOSFORO to 30.0, // Ajustado (2.5-3.5%)
                    Insumo.MAGNESIO to 3.0,
                    Insumo.SODIO to 8.0,    // Ajustado (0.60-1.20%)
                    Insumo.POTASIO to 1.0,
                    Insumo.AZUFRE to 2.0,
                    Insumo.COBRE to 15.0,
                    Insumo.ZINC to 80.0,
                    Insumo.SELENIO to 1.0,
                    Insumo.COBALTO to 0.5
                )
            ),
            Insumo(
                id = "mono_salvado_trigo",
                nombre = "Salvado de Trigo",
                costo = 4.70, // MXN/kg
                esForraje = false,
                inclusionMinima = 0.0,
                inclusionMaxima = 30.0,
                nutrientes = mapOf(
                    Insumo.ENERGIA_METABOLIZABLE to 11.8, // Ajustado para cerdos
                    Insumo.PROTEINA_CRUDA to 16.0,
                    Insumo.FIBRA_DETERGENTE_NEUTRA to 43.0, // Ajustado
                    Insumo.FIBRA_DETERGENTE_ACIDA to 11.0,  // Ajustado
                    Insumo.EXTRACTO_ETEREO to 4.0,
                    Insumo.CENIZAS to 5.8,
                    Insumo.DEGRADABILIDAD_RUMINAL to 0.0,
                    Insumo.METANO_PRODUCIDO to 0.0,
                    Insumo.CALCIO to 1.2,
                    Insumo.FOSFORO to 11.5, // Ajustado (1.0-1.3%)
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

    // ============================
    //           AVES (POULTRY)
    // ============================
    private fun getPoultryAnimals(): List<Animal> {
        return listOf(
            // ================= ROSS (BROILERS) =================
            Animal(
                id = "ross_iniciador",
                nombre = "Ross - Iniciador (0-10 días)",
                tipo = TipoDieta.BAJO_FORRAJE,
                pesoKg = 0.200, // Promedio estimado
                consumoDMI = 0.030, // Estimado
                requerimientosMinimos = mapOf(
                    Animal.ENERGIA_METABOLIZABLE to 12.4,
                    Animal.PROTEINA_CRUDA to 23.0,
                    Animal.CALCIO to 0.95,
                    Animal.FOSFORO to 0.50,
                    Animal.LISINA to 1.32,
                    Animal.METIONINA to 0.55,
                    Animal.TREONINA to 0.88,
                    Animal.VALINA to 1.00,
                    Animal.ISOLEUCINA to 0.88,
                    Animal.ARGININA to 1.40,
                    Animal.TRIPTOFANO to 0.21,
                    Animal.LEUCINA to 1.45
                ),
                requerimientosMaximos = mapOf(
                    Animal.FIBRA_DETERGENTE_NEUTRA to 2.0 // Fc % MAX interpretado como FDN/Fibra Cruda aprox
                )
            ),
            Animal(
                id = "ross_crecimiento",
                nombre = "Ross - Crecimiento (11-24 días)",
                tipo = TipoDieta.BAJO_FORRAJE,
                pesoKg = 0.900,
                consumoDMI = 0.090,
                requerimientosMinimos = mapOf(
                    Animal.ENERGIA_METABOLIZABLE to 12.8,
                    Animal.PROTEINA_CRUDA to 21.5,
                    Animal.CALCIO to 0.75,
                    Animal.FOSFORO to 0.42,
                    Animal.LISINA to 1.18,
                    Animal.METIONINA to 0.51,
                    Animal.TREONINA to 0.79,
                    Animal.VALINA to 0.91,
                    Animal.ISOLEUCINA to 0.80,
                    Animal.ARGININA to 1.27,
                    Animal.TRIPTOFANO to 0.19,
                    Animal.LEUCINA to 1.30
                ),
                requerimientosMaximos = mapOf(
                    Animal.FIBRA_DETERGENTE_NEUTRA to 3.0
                )
            ),
            Animal(
                id = "ross_finalizador1",
                nombre = "Ross - Finalizador 1 (25-39 días)",
                tipo = TipoDieta.BAJO_FORRAJE,
                pesoKg = 2.0,
                consumoDMI = 0.160,
                requerimientosMinimos = mapOf(
                    Animal.ENERGIA_METABOLIZABLE to 13.0,
                    Animal.PROTEINA_CRUDA to 19.5,
                    Animal.CALCIO to 0.65,
                    Animal.FOSFORO to 0.36,
                    Animal.LISINA to 1.08,
                    Animal.METIONINA to 0.48,
                    Animal.TREONINA to 0.72,
                    Animal.VALINA to 0.84,
                    Animal.ISOLEUCINA to 0.75,
                    Animal.ARGININA to 1.17,
                    Animal.TRIPTOFANO to 0.17,
                    Animal.LEUCINA to 1.19
                ),
                requerimientosMaximos = mapOf(
                    Animal.FIBRA_DETERGENTE_NEUTRA to 4.0
                )
            ),
            Animal(
                id = "ross_finalizador2",
                nombre = "Ross - Finalizador 2 (40-Sacrificio)",
                tipo = TipoDieta.BAJO_FORRAJE,
                pesoKg = 3.0,
                consumoDMI = 0.220,
                requerimientosMinimos = mapOf(
                    Animal.ENERGIA_METABOLIZABLE to 13.1,
                    Animal.PROTEINA_CRUDA to 18.0,
                    Animal.CALCIO to 0.60,
                    Animal.FOSFORO to 0.34,
                    Animal.LISINA to 1.02,
                    Animal.METIONINA to 0.45,
                    Animal.TREONINA to 0.68,
                    Animal.VALINA to 0.80,
                    Animal.ISOLEUCINA to 0.70,
                    Animal.ARGININA to 1.12,
                    Animal.TRIPTOFANO to 0.16,
                    Animal.LEUCINA to 1.12
                ),
                requerimientosMaximos = mapOf(
                    Animal.FIBRA_DETERGENTE_NEUTRA to 4.0
                )
            ),

            // ================= COBB 500 (BROILERS) =================
            Animal(
                id = "cobb_iniciador",
                nombre = "Cobb 500 - Iniciador (0-12 días)",
                tipo = TipoDieta.BAJO_FORRAJE,
                pesoKg = 0.250,
                consumoDMI = 0.035,
                requerimientosMinimos = mapOf(
                    Animal.ENERGIA_METABOLIZABLE to 12.13,
                    Animal.PROTEINA_CRUDA to 22.0,
                    Animal.CALCIO to 0.96,
                    Animal.FOSFORO to 0.58,
                    Animal.LISINA to 1.26,
                    Animal.METIONINA to 0.48,
                    Animal.TREONINA to 0.86,
                    Animal.VALINA to 0.96,
                    Animal.ISOLEUCINA to 0.81,
                    Animal.ARGININA to 1.36,
                    Animal.TRIPTOFANO to 0.21,
                    Animal.LEUCINA to 1.39
                ),
                requerimientosMaximos = mapOf(
                    Animal.FIBRA_DETERGENTE_NEUTRA to 2.0
                )
            ),
            Animal(
                id = "cobb_crecimiento1",
                nombre = "Cobb 500 - Crecimiento 1 (13-28 días)",
                tipo = TipoDieta.BAJO_FORRAJE,
                pesoKg = 1.0,
                consumoDMI = 0.100,
                requerimientosMinimos = mapOf(
                    Animal.ENERGIA_METABOLIZABLE to 12.34,
                    Animal.PROTEINA_CRUDA to 20.0,
                    Animal.CALCIO to 0.80,
                    Animal.FOSFORO to 0.40,
                    Animal.LISINA to 1.16,
                    Animal.METIONINA to 0.47,
                    Animal.TREONINA to 0.78,
                    Animal.VALINA to 0.88,
                    Animal.ISOLEUCINA to 0.75,
                    Animal.ARGININA to 1.25,
                    Animal.TRIPTOFANO to 0.18,
                    Animal.LEUCINA to 1.28
                ),
                requerimientosMaximos = mapOf(
                    Animal.FIBRA_DETERGENTE_NEUTRA to 3.0
                )
            ),
            Animal(
                id = "cobb_crecimiento2",
                nombre = "Cobb 500 - Crecimiento 2 (29-39 días)",
                tipo = TipoDieta.BAJO_FORRAJE,
                pesoKg = 2.0,
                consumoDMI = 0.180,
                requerimientosMinimos = mapOf(
                    Animal.ENERGIA_METABOLIZABLE to 12.76,
                    Animal.PROTEINA_CRUDA to 19.0,
                    Animal.CALCIO to 0.74,
                    Animal.FOSFORO to 0.37,
                    Animal.LISINA to 1.06,
                    Animal.METIONINA to 0.44,
                    Animal.TREONINA to 0.70,
                    Animal.VALINA to 0.81,
                    Animal.ISOLEUCINA to 0.69,
                    Animal.ARGININA to 1.16,
                    Animal.TRIPTOFANO to 0.19,
                    Animal.LEUCINA to 1.17
                ),
                requerimientosMaximos = mapOf(
                    Animal.FIBRA_DETERGENTE_NEUTRA to 3.0
                )
            ),
            Animal(
                id = "cobb_finalizador1",
                nombre = "Cobb 500 - Finalizador 1 (40-49 días)",
                tipo = TipoDieta.BAJO_FORRAJE,
                pesoKg = 3.0,
                consumoDMI = 0.230,
                requerimientosMinimos = mapOf(
                    Animal.ENERGIA_METABOLIZABLE to 12.97,
                    Animal.PROTEINA_CRUDA to 18.0,
                    Animal.CALCIO to 0.72,
                    Animal.FOSFORO to 0.36,
                    Animal.LISINA to 0.96,
                    Animal.METIONINA to 0.40,
                    Animal.TREONINA to 0.62,
                    Animal.VALINA to 0.74,
                    Animal.ISOLEUCINA to 0.63,
                    Animal.ARGININA to 1.05,
                    Animal.TRIPTOFANO to 0.17,
                    Animal.LEUCINA to 1.06
                ),
                requerimientosMaximos = mapOf(
                    Animal.FIBRA_DETERGENTE_NEUTRA to 4.0
                )
            ),
            Animal(
                id = "cobb_finalizador2",
                nombre = "Cobb 500 - Finalizador 2 (50-Sacrificio)",
                tipo = TipoDieta.BAJO_FORRAJE,
                pesoKg = 3.8,
                consumoDMI = 0.250,
                requerimientosMinimos = mapOf(
                    Animal.ENERGIA_METABOLIZABLE to 13.18,
                    Animal.PROTEINA_CRUDA to 18.0,
                    Animal.CALCIO to 0.68,
                    Animal.FOSFORO to 0.34,
                    Animal.LISINA to 0.86,
                    Animal.METIONINA to 0.35,
                    Animal.TREONINA to 0.56,
                    Animal.VALINA to 0.67,
                    Animal.ISOLEUCINA to 0.57,
                    Animal.ARGININA to 0.95,
                    Animal.TRIPTOFANO to 0.15,
                    Animal.LEUCINA to 0.95
                ),
                requerimientosMaximos = mapOf(
                    Animal.FIBRA_DETERGENTE_NEUTRA to 4.0
                )
            ),

            // ================= HUBBARD (BROILERS) =================
            Animal(
                id = "hubbard_iniciador",
                nombre = "Hubbard - Iniciador (0-10 días)",
                tipo = TipoDieta.BAJO_FORRAJE,
                pesoKg = 0.200,
                consumoDMI = 0.030,
                requerimientosMinimos = mapOf(
                    Animal.ENERGIA_METABOLIZABLE to 12.6,
                    Animal.PROTEINA_CRUDA to 23.1,
                    Animal.CALCIO to 0.93,
                    Animal.FOSFORO to 0.35,
                    Animal.LISINA to 1.34,
                    Animal.METIONINA to 0.43,
                    Animal.TREONINA to 0.96,
                    Animal.VALINA to 0.94,
                    Animal.ISOLEUCINA to 0.79,
                    Animal.ARGININA to 1.43,
                    Animal.TRIPTOFANO to 0.22,
                    Animal.LEUCINA to 1.39
                ),
                requerimientosMaximos = mapOf(
                    Animal.FIBRA_DETERGENTE_NEUTRA to 2.0
                )
            ),
            Animal(
                id = "hubbard_crecimiento",
                nombre = "Hubbard - Crecimiento (10-22 días)",
                tipo = TipoDieta.BAJO_FORRAJE,
                pesoKg = 0.800,
                consumoDMI = 0.080,
                requerimientosMinimos = mapOf(
                    Animal.ENERGIA_METABOLIZABLE to 12.6,
                    Animal.PROTEINA_CRUDA to 21.4,
                    Animal.CALCIO to 0.72,
                    Animal.FOSFORO to 0.35,
                    Animal.LISINA to 1.08,
                    Animal.METIONINA to 0.40,
                    Animal.TREONINA to 0.83,
                    Animal.VALINA to 0.85,
                    Animal.ISOLEUCINA to 0.72,
                    Animal.ARGININA to 1.23,
                    Animal.TRIPTOFANO to 0.20,
                    Animal.LEUCINA to 1.28
                ),
                requerimientosMaximos = mapOf(
                    Animal.FIBRA_DETERGENTE_NEUTRA to 3.0
                )
            ),
            Animal(
                id = "hubbard_finalizador1",
                nombre = "Hubbard - Finalizador 1 (22-38 días)",
                tipo = TipoDieta.BAJO_FORRAJE,
                pesoKg = 1.8,
                consumoDMI = 0.150,
                requerimientosMinimos = mapOf(
                    Animal.ENERGIA_METABOLIZABLE to 13.4,
                    Animal.PROTEINA_CRUDA to 20.0,
                    Animal.CALCIO to 0.70,
                    Animal.FOSFORO to 0.37,
                    Animal.LISINA to 0.95,
                    Animal.METIONINA to 0.38,
                    Animal.TREONINA to 0.74,
                    Animal.VALINA to 0.78,
                    Animal.ISOLEUCINA to 0.71,
                    Animal.ARGININA to 1.13,
                    Animal.TRIPTOFANO to 0.17,
                    Animal.LEUCINA to 1.17
                ),
                requerimientosMaximos = mapOf(
                    Animal.FIBRA_DETERGENTE_NEUTRA to 3.0
                )
            ),
            Animal(
                id = "hubbard_finalizador2",
                nombre = "Hubbard - Finalizador 2 (39-Sacrificio)",
                tipo = TipoDieta.BAJO_FORRAJE,
                pesoKg = 2.8,
                consumoDMI = 0.200,
                requerimientosMinimos = mapOf(
                    Animal.ENERGIA_METABOLIZABLE to 12.6,
                    Animal.PROTEINA_CRUDA to 19.2,
                    Animal.CALCIO to 0.68,
                    Animal.FOSFORO to 0.35,
                    Animal.LISINA to 0.90,
                    Animal.METIONINA to 0.37,
                    Animal.TREONINA to 0.72,
                    Animal.VALINA to 0.76,
                    Animal.ISOLEUCINA to 0.69,
                    Animal.ARGININA to 1.10,
                    Animal.TRIPTOFANO to 0.17,
                    Animal.LEUCINA to 1.06
                ),
                requerimientosMaximos = mapOf(
                    Animal.FIBRA_DETERGENTE_NEUTRA to 4.0
                )
            ),

            // ================= NICK BROWN (PONEDORAS) =================
            Animal(
                id = "nick_iniciador",
                nombre = "Nick Brown - Iniciador (0-5 semanas)",
                tipo = TipoDieta.BAJO_FORRAJE,
                pesoKg = 0.300,
                consumoDMI = 0.030,
                requerimientosMinimos = mapOf(
                    Animal.ENERGIA_METABOLIZABLE to 12.35,
                    Animal.PROTEINA_CRUDA to 20.0,
                    Animal.CALCIO to 1.05,
                    Animal.FOSFORO to 0.45,
                    Animal.LISINA to 1.00,
                    Animal.METIONINA to 0.44,
                    Animal.TREONINA to 0.78,
                    Animal.VALINA to 0.78,
                    Animal.ISOLEUCINA to 0.69,
                    Animal.ARGININA to 1.05,
                    Animal.TRIPTOFANO to 0.19,
                    Animal.LEUCINA to 1.39
                ),
                requerimientosMaximos = mapOf(
                    Animal.FIBRA_DETERGENTE_NEUTRA to 2.0
                )
            ),
            Animal(
                id = "nick_crecimiento",
                nombre = "Nick Brown - Crecimiento (6-10 semanas)",
                tipo = TipoDieta.BAJO_FORRAJE,
                pesoKg = 0.800,
                consumoDMI = 0.060,
                requerimientosMinimos = mapOf(
                    Animal.ENERGIA_METABOLIZABLE to 11.93,
                    Animal.PROTEINA_CRUDA to 18.0,
                    Animal.CALCIO to 1.00,
                    Animal.FOSFORO to 0.41,
                    Animal.LISINA to 0.86,
                    Animal.METIONINA to 0.39,
                    Animal.TREONINA to 0.70,
                    Animal.VALINA to 0.67,
                    Animal.ISOLEUCINA to 0.65,
                    Animal.ARGININA to 0.90,
                    Animal.TRIPTOFANO to 0.18,
                    Animal.LEUCINA to 1.28
                ),
                requerimientosMaximos = mapOf(
                    Animal.FIBRA_DETERGENTE_NEUTRA to 3.0
                )
            ),
            Animal(
                id = "nick_desarrollo",
                nombre = "Nick Brown - Desarrollo (11-17 semanas)",
                tipo = TipoDieta.BAJO_FORRAJE,
                pesoKg = 1.3,
                consumoDMI = 0.080,
                requerimientosMinimos = mapOf(
                    Animal.ENERGIA_METABOLIZABLE to 11.51,
                    Animal.PROTEINA_CRUDA to 15.5,
                    Animal.CALCIO to 0.90,
                    Animal.FOSFORO to 0.37,
                    Animal.LISINA to 0.56,
                    Animal.METIONINA to 0.26,
                    Animal.TREONINA to 0.46,
                    Animal.VALINA to 0.45,
                    Animal.ISOLEUCINA to 0.43,
                    Animal.ARGININA to 0.59,
                    Animal.TRIPTOFANO to 0.13,
                    Animal.LEUCINA to 1.17
                ),
                requerimientosMaximos = mapOf(
                    Animal.FIBRA_DETERGENTE_NEUTRA to 3.0
                )
            ),
            Animal(
                id = "nick_prepuesta",
                nombre = "Nick Brown - Prepuesta (18 sem - 1er huevo)",
                tipo = TipoDieta.BAJO_FORRAJE,
                pesoKg = 1.6,
                consumoDMI = 0.090,
                requerimientosMinimos = mapOf(
                    Animal.ENERGIA_METABOLIZABLE to 11.40,
                    Animal.PROTEINA_CRUDA to 11.5,
                    Animal.CALCIO to 2.00,
                    Animal.FOSFORO to 0.40,
                    Animal.LISINA to 0.70,
                    Animal.METIONINA to 0.35,
                    Animal.TREONINA to 0.49,
                    Animal.VALINA to 0.62,
                    Animal.ISOLEUCINA to 0.56,
                    Animal.ARGININA to 0.73,
                    Animal.TRIPTOFANO to 0.15,
                    Animal.LEUCINA to 1.06
                ),
                requerimientosMaximos = mapOf(
                    Animal.FIBRA_DETERGENTE_NEUTRA to 4.0
                )
            )
        )
    }

    private fun getPoultryInsumos(): List<Insumo> {
        // Reutilizamos los insumos de monogástricos (cerdos) como base,
        // ya que comparten ingredientes como maíz y soya.
        // Idealmente, se deberían ajustar los valores nutricionales específicos para aves.
        return getMonoInsumos()
    }
}