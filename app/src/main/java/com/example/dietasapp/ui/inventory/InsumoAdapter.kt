package com.example.dietasapp.ui.inventory

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.dietasapp.data.Insumo
import com.example.dietasapp.databinding.ItemInsumoBinding

class InsumoAdapter(
    private val onEditClick: (Insumo) -> Unit,
    private val onDeleteClick: (Insumo) -> Unit
) : ListAdapter<Insumo, InsumoAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemInsumoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onEditClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemInsumoBinding,
        private val onEditClick: (Insumo) -> Unit,
        private val onDeleteClick: (Insumo) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(insumo: Insumo) {
            binding.apply {
                tvNombreInsumo.text = insumo.nombre
                tvCosto.text = "$${String.format("%.2f", insumo.costo)}/kg"

                // Chip de forraje
                if (insumo.esForraje) {
                    chipForraje.visibility = View.VISIBLE
                } else {
                    chipForraje.visibility = View.GONE
                }

                // Mostrar algunos nutrientes clave
                val nutrientesTexto = buildString {
                    val cp = insumo.getNutriente("CP")
                    val ndf = insumo.getNutriente("NDF")

                    if (cp > 0) append("CP: ${String.format("%.1f", cp)}%")
                    if (ndf > 0) {
                        if (isNotEmpty()) append(" • ")
                        append("NDF: ${String.format("%.1f", ndf)}%")
                    }
                }
                tvNutrientes.text = nutrientesTexto

                btnEditarInsumo.setOnClickListener {
                    onEditClick(insumo)
                }

                btnEliminarInsumo.setOnClickListener {
                    onDeleteClick(insumo)
                }
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<Insumo>() {
        override fun areItemsTheSame(oldItem: Insumo, newItem: Insumo): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Insumo, newItem: Insumo): Boolean {
            return oldItem == newItem
        }
    }
}