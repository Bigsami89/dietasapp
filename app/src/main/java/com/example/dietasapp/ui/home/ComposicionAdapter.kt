package com.example.dietasapp.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.dietasapp.databinding.ItemIngredienteBinding

class ComposicionAdapter : ListAdapter<ComposicionItem, ComposicionAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemIngredienteBinding.inflate(
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
        private val binding: ItemIngredienteBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ComposicionItem) {
            binding.tvNombreIngrediente.text = item.nombre
            binding.tvCantidad.text = String.format("%.2f kg/día", item.cantidad)
            binding.tvPorcentaje.text = String.format("%.1f%%", item.porcentaje)

            // Configurar barra de progreso
            binding.progressIngrediente.progress = item.porcentaje.toInt()
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<ComposicionItem>() {
        override fun areItemsTheSame(oldItem: ComposicionItem, newItem: ComposicionItem): Boolean {
            return oldItem.nombre == newItem.nombre
        }

        override fun areContentsTheSame(oldItem: ComposicionItem, newItem: ComposicionItem): Boolean {
            return oldItem == newItem
        }
    }
}