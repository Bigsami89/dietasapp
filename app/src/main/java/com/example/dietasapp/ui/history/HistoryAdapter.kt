package com.example.dietasapp.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.dietasapp.R
import com.example.dietasapp.databinding.ItemDietaBinding
import com.example.dietasapp.domain.Dieta
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter para mostrar el historial de dietas guardadas
 */
class HistoryAdapter(
    private val onDietaClick: (Dieta) -> Unit,
    private val onEliminarClick: (Dieta) -> Unit
) : ListAdapter<Dieta, HistoryAdapter.ViewHolder>(DiffCallback()) {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDietaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onDietaClick, onEliminarClick, dateFormat)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemDietaBinding,
        private val onDietaClick: (Dieta) -> Unit,
        private val onEliminarClick: (Dieta) -> Unit,
        private val dateFormat: SimpleDateFormat
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(dieta: Dieta) {
            val context = binding.root.context

            // Información básica
            binding.tvNombreAnimal.text = dieta.animal.nombre
            binding.tvFechaCreacion.text = dateFormat.format(Date(dieta.fechaCreacion))

            // No existe modoOptimizacion en Dieta actual
            binding.tvTipoOptimizacion.text = context.getString(R.string.no_aplica)

            // Costo total
            binding.tvCostoTotal.text = context.getString(
                R.string.formato_costo_total,
                dieta.costoTotal
            )

            // Metano (usar metanoProducidoGramos)
            binding.tvMetanoTotal.text = context.getString(
                R.string.formato_metano,
                dieta.metanoProducidoGramos
            )

            // Número de ingredientes
            binding.tvNumIngredientes.text = context.getString(
                R.string.ingredientes_count,
                dieta.composicion.size
            )

            // Click listeners
            binding.root.setOnClickListener { onDietaClick(dieta) }
            binding.btnEliminar.setOnClickListener { onEliminarClick(dieta) }
            binding.btnVerDetalles.setOnClickListener { onDietaClick(dieta) }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<Dieta>() {
        override fun areItemsTheSame(oldItem: Dieta, newItem: Dieta): Boolean {
            return oldItem.fechaCreacion == newItem.fechaCreacion
        }

        override fun areContentsTheSame(oldItem: Dieta, newItem: Dieta): Boolean {
            return oldItem == newItem
        }
    }
}
