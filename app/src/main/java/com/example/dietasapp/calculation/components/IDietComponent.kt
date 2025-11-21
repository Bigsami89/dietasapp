package com.example.dietasapp.calculation.components

import com.example.dietasapp.data.Animal
import com.example.dietasapp.domain.Ingredient
import org.ojalgo.optimisation.ExpressionsBasedModel
import org.ojalgo.optimisation.Variable

/**
 * Interfaz para componentes que aplican restricciones o funciones objetivo al modelo
 */
interface IDietComponent {

    /**
     * Aplica el componente al modelo de optimización
     *
     * @param model Modelo de optimización
     * @param variables Mapa de ingredientes a variables de decisión
     * @param animal Datos del animal con requerimientos nutricionales
     * @param ingredients Lista de ingredientes disponibles
     */
    fun apply(
        model: ExpressionsBasedModel,
        variables: Map<Ingredient, Variable>,
        animal: Animal,
        ingredients: List<Ingredient>
    )

    /**
     * Nombre del componente
     */
    fun getName(): String

    /**
     * Descripción del componente
     */
    fun getDescription(): String
}