package com.example.dietasapp.ui.inventory

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.dietasapp.data.Animal
import com.example.dietasapp.databinding.ItemAnimalBinding

class AnimalAdapter(
    private val onEditClick: (Animal) -> Unit,
    private val onDeleteClick: (Animal) -> Unit
) : ListAdapter<Animal, AnimalAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAnimalBinding.inflate(
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
        private val binding: ItemAnimalBinding,
        private val onEditClick: (Animal) -> Unit,
        private val onDeleteClick: (Animal) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(animal: Animal) {
            binding.apply {
                tvNombreAnimal.text = animal.nombre
                tvPeso.text = "${animal.pesoKg} kg"
                tvDMI.text = "${animal.consumoDMI} kg/día"
                chipTipoDieta.text = animal.tipo.descripcion

                btnEditarAnimal.setOnClickListener {
                    onEditClick(animal)
                }

                btnEliminarAnimal.setOnClickListener {
                    onDeleteClick(animal)
                }
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<Animal>() {
        override fun areItemsTheSame(oldItem: Animal, newItem: Animal): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Animal, newItem: Animal): Boolean {
            return oldItem == newItem
        }
    }
}