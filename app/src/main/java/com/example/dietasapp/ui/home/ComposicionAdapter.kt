package com.example.dietasapp.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.dietasapp.R
import com.example.dietasapp.databinding.ItemComposicionBinding

/**
 * Adapter para mostrar la composición detallada de una dieta
 */
class ComposicionAdapter : ListAdapter<ComposicionItem, ComposicionAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemComposicionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemComposicionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ComposicionItem) {
            binding.tvNombreIngrediente.text = item.nombre

            binding.tvCantidad.text = itemView.context.getString(
                R.string.formato_kg_dia,
                item.cantidad
            )

            binding.tvPorcentaje.text = itemView.context.getString(
                R.string.formato_porcentaje,
                item.porcentaje
            )

            binding.tvCostoItem.text = itemView.context.getString(
                R.string.formato_costo_total,
                item.cantidad * item.costo
            )

            // Color de la barra de progreso según porcentaje
            val colorResId = when {
                item.porcentaje >= 40.0 -> R.color.composicion_alto
                item.porcentaje >= 20.0 -> R.color.composicion_medio
                else -> R.color.composicion_bajo
            }

            binding.progressBar.setIndicatorColor(
                itemView.context.getColor(colorResId)
            )
            binding.progressBar.progress = item.porcentaje.toInt()
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<ComposicionItem>() {
        override fun areItemsTheSame(
            oldItem: ComposicionItem,
            newItem: ComposicionItem
        ): Boolean {
            return oldItem.nombre == newItem.nombre
        }

        override fun areContentsTheSame(
            oldItem: ComposicionItem,
            newItem: ComposicionItem
        ): Boolean {
            return oldItem == newItem
        }
    }
}

/**
 * Item de composición de dieta
 */
data class ComposicionItem(
    val nombre: String,
    val cantidad: Double,
    val porcentaje: Double,
    val costo: Double = 0.0
)