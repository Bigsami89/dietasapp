package com.example.dietasapp.calculation.components

import com.example.dietasapp.domain.AnimalProfile
import com.example.dietasapp.domain.Ingredient
import org.ojalgo.optimisation.ExpressionsBasedModel
import org.ojalgo.optimisation.Variable

/**
 * Interfaz para componentes de dieta según el patrón Strategy
 * Cada componente aplica restricciones u objetivos específicos al modelo de optimización
 *
 * Corresponde a "IDietComponent" del diagrama de clases
 */
interface IDietComponent {

    /**
     * Aplica el componente al modelo de optimización
     *
     * @param model Modelo de ojAlgo donde se agregan expresiones
     * @param variables Mapa de variables (ingrediente -> variable de ojAlgo)
     * @param profile Perfil del animal con requerimientos
     * @param ingredients Lista de ingredientes disponibles
     */
    fun apply(
        model: ExpressionsBasedModel,
        variables: Map<Ingredient, Variable>,
        profile: AnimalProfile,
        ingredients: List<Ingredient>
    )

    /**
     * Nombre descriptivo del componente
     */
    fun getName(): String

    /**
     * Descripción de lo que hace el componente
     */
    fun getDescription(): String
}