package com.example.dietasapp.ui.inventory

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.dietasapp.R
import com.example.dietasapp.data.Insumo
import com.example.dietasapp.databinding.ItemInsumoBinding

/**
 * Adapter para mostrar la lista de insumos disponibles
 */
class InsumoAdapter(
    private val onEdit: (Insumo) -> Unit,
    private val onDelete: (Insumo) -> Unit
) : ListAdapter<Insumo, InsumoAdapter.InsumoViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InsumoViewHolder {
        val binding = ItemInsumoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return InsumoViewHolder(binding, onEdit, onDelete)
    }

    override fun onBindViewHolder(holder: InsumoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class InsumoViewHolder(
        private val binding: ItemInsumoBinding,
        private val onEdit: (Insumo) -> Unit,
        private val onDelete: (Insumo) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(insumo: Insumo) {
            // Nombre del insumo
            binding.tvNombreInsumo.text = insumo.nombre

            // Costo en MXN
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

            // Nutrientes principales para rumiantes
            val pc = insumo.getProteinaCruda()
            val em = insumo.getEnergiaMetabolizable()
            val fdn = insumo.getFibraDetengenteNeutra()

            binding.tvNutrientes.text = buildString {
                append("PC: ${formatNutrient(pc)}% • ")
                append("EM: ${formatNutrient(em)} MJ/kg • ")
                append("FDN: ${formatNutrient(fdn)}%")
            }

            // Restricciones de inclusión
            if (insumo.tieneRestriccionesPersonalizadas()) {
                binding.tvRestricciones.text = buildString {
                    append("Inclusión: ${formatNutrient(insumo.inclusionMinima)}% - ")
                    append("${formatNutrient(insumo.inclusionMaxima)}%")
                }
                binding.tvRestricciones.visibility = android.view.View.VISIBLE
            } else {
                binding.tvRestricciones.visibility = android.view.View.GONE
            }

            // Icono según tipo
            binding.iconInsumo.setImageResource(
                if (insumo.esForraje) R.drawable.ic_grass else R.drawable.ic_grain
            )

            // Acciones
            binding.btnEdit.setOnClickListener { onEdit(insumo) }
            binding.btnDelete.setOnClickListener { onDelete(insumo) }

            // Click en el card también permite editar
            binding.root.setOnClickListener { onEdit(insumo) }
        }

        private fun formatNutrient(value: Double): String {
            return if (value == 0.0) {
                "0"
            } else {
                String.format("%.1f", value)
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