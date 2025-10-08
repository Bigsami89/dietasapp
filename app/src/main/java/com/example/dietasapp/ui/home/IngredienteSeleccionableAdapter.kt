package com.example.dietasapp.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.dietasapp.R
import com.example.dietasapp.data.Insumo
import com.example.dietasapp.databinding.ItemIngredienteSeleccionableBinding

/**
 * Adapter para mostrar ingredientes seleccionables con checkbox
 */
class IngredienteSeleccionableAdapter(
    private val onSelectionChanged: (List<Insumo>) -> Unit
) : ListAdapter<IngredienteSeleccionable, IngredienteSeleccionableAdapter.ViewHolder>(DiffCallback()) {

    private val selectedItems = mutableSetOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemIngredienteSeleccionableBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * Selecciona todos los ingredientes
     */
    fun selectAll() {
        selectedItems.clear()
        currentList.forEach { selectedItems.add(it.insumo.id) }
        notifyDataSetChanged()
        notifySelectionChanged()
    }

    /**
     * Deselecciona todos los ingredientes
     */
    fun deselectAll() {
        selectedItems.clear()
        notifyDataSetChanged()
        notifySelectionChanged()
    }

    /**
     * Obtiene la lista de ingredientes seleccionados
     */
    fun getSelectedIngredientes(): List<Insumo> {
        return currentList
            .filter { selectedItems.contains(it.insumo.id) }
            .map { it.insumo }
    }

    private fun notifySelectionChanged() {
        onSelectionChanged(getSelectedIngredientes())
    }

    inner class ViewHolder(
        private val binding: ItemIngredienteSeleccionableBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: IngredienteSeleccionable) {
            val insumo = item.insumo

            // Nombre
            binding.tvNombreIngrediente.text = insumo.nombre

            // Costo
            binding.tvCosto.text = itemView.context.getString(
                R.string.formato_costo_por_kg,
                insumo.costo
            )

            // Tipo (Forraje o Concentrado)
            binding.tvTipo.text = if (insumo.esForraje) {
                itemView.context.getString(R.string.forraje)
            } else {
                itemView.context.getString(R.string.concentrado)
            }

            // Nutrientes principales
            val cp = insumo.nutrientes["CP"] ?: 0.0
            val nem = insumo.nutrientes["NEm"] ?: 0.0
            val ndf = insumo.nutrientes["NDF"] ?: 0.0

            binding.tvNutrientes.text = itemView.context.getString(
                R.string.formato_nutrientes_principales,
                cp, nem, ndf
            )

            // Icono según tipo
            binding.iconIngrediente.setImageResource(
                if (insumo.esForraje) R.drawable.ic_grass else R.drawable.ic_grain
            )

            // Estado del checkbox
            val isSelected = selectedItems.contains(insumo.id)
            binding.checkboxIngrediente.isChecked = isSelected

            // Listener del checkbox
            binding.checkboxIngrediente.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedItems.add(insumo.id)
                } else {
                    selectedItems.remove(insumo.id)
                }
                notifySelectionChanged()
            }

            // Click en el card también selecciona
            binding.root.setOnClickListener {
                binding.checkboxIngrediente.isChecked = !binding.checkboxIngrediente.isChecked
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<IngredienteSeleccionable>() {
        override fun areItemsTheSame(
            oldItem: IngredienteSeleccionable,
            newItem: IngredienteSeleccionable
        ): Boolean {
            return oldItem.insumo.id == newItem.insumo.id
        }

        override fun areContentsTheSame(
            oldItem: IngredienteSeleccionable,
            newItem: IngredienteSeleccionable
        ): Boolean {
            return oldItem == newItem
        }
    }
}

/**
 * Data class para ingrediente seleccionable
 */
data class IngredienteSeleccionable(
    val insumo: Insumo,
    val disponible: Double = 0.0
)