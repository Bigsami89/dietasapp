package com.example.dietasapp.ui.inventory

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dietasapp.R
import com.example.dietasapp.data.Animal
import com.example.dietasapp.data.TipoDieta
import com.example.dietasapp.database.BaseDeDatosJSON
import com.example.dietasapp.databinding.DialogAnimalBinding
import com.example.dietasapp.databinding.TabAnimalesBinding
import com.example.dietasapp.inventory.Inventario
import kotlinx.coroutines.launch
import java.util.UUID

class AnimalesTabFragment : Fragment() {

    private var _binding: TabAnimalesBinding? = null
    private val binding get() = _binding!!

    private lateinit var inventario: Inventario
    private lateinit var adapter: AnimalAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = TabAnimalesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val baseDatos = BaseDeDatosJSON(requireContext())
        inventario = Inventario(baseDatos)

        setupRecyclerView()
        cargarAnimales()
    }

    private fun setupRecyclerView() {
        adapter = AnimalAdapter(
            onEditClick = { animal -> showEditDialog(animal) },
            onDeleteClick = { animal -> confirmarEliminar(animal) }
        )

        binding.recyclerAnimales.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@AnimalesTabFragment.adapter
        }
    }

    private fun cargarAnimales() {
        lifecycleScope.launch {
            try {
                val animales = inventario.obtenerAnimales()

                if (animales.isEmpty()) {
                    binding.emptyStateAnimales.visibility = View.VISIBLE
                    binding.recyclerAnimales.visibility = View.GONE
                } else {
                    binding.emptyStateAnimales.visibility = View.GONE
                    binding.recyclerAnimales.visibility = View.VISIBLE
                    adapter.submitList(animales)
                }
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_cargar_datos),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun showAddDialog() {
        showAnimalDialog(null)
    }

    private fun showEditDialog(animal: Animal) {
        showAnimalDialog(animal)
    }

    private fun showAnimalDialog(animal: Animal?) {
        val dialogBinding = DialogAnimalBinding.inflate(layoutInflater)

        // Configurar spinner de tipo de dieta
        val tiposDieta = TipoDieta.values().map { it.descripcion }
        val tipoAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            tiposDieta
        )
        dialogBinding.spinnerTipoDieta.setAdapter(tipoAdapter)

        // Si es edición, llenar campos
        animal?.let {
            dialogBinding.tvTitulo.text = getString(R.string.editar_animal)
            dialogBinding.etNombre.setText(it.nombre)
            dialogBinding.etPeso.setText(it.pesoKg.toString())
            dialogBinding.etDMI.setText(it.consumoDMI.toString())
            dialogBinding.spinnerTipoDieta.setText(it.tipo.descripcion, false)

            // Requerimientos mínimos
            dialogBinding.etCPMin.setText(it.requerimientosMinimos["CP"]?.toString() ?: "")
            dialogBinding.etNEmMin.setText(it.requerimientosMinimos["NEm"]?.toString() ?: "")
            dialogBinding.etCaMin.setText(it.requerimientosMinimos["Ca"]?.toString() ?: "")
            dialogBinding.etPMin.setText(it.requerimientosMinimos["P"]?.toString() ?: "")

            // Requerimientos máximos
            dialogBinding.etNDFMax.setText(it.requerimientosMaximos["NDF"]?.toString() ?: "")
            dialogBinding.etCPMax.setText(it.requerimientosMaximos["CP"]?.toString() ?: "")
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCancelar.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnGuardar.setOnClickListener {
            val nombre = dialogBinding.etNombre.text.toString().trim()
            val pesoStr = dialogBinding.etPeso.text.toString().trim()
            val dmiStr = dialogBinding.etDMI.text.toString().trim()
            val tipoStr = dialogBinding.spinnerTipoDieta.text.toString()

            if (nombre.isEmpty() || pesoStr.isEmpty() || dmiStr.isEmpty() || tipoStr.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_campos_vacios),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val peso = pesoStr.toDoubleOrNull()
            val dmi = dmiStr.toDoubleOrNull()

            if (peso == null || dmi == null) {
                Toast.makeText(
                    requireContext(),
                    "Valores numéricos inválidos",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val tipo = TipoDieta.values().first { it.descripcion == tipoStr }

            // Requerimientos mínimos
            val reqMin = mutableMapOf<String, Double>()
            dialogBinding.etCPMin.text.toString().toDoubleOrNull()?.let { reqMin["CP"] = it }
            dialogBinding.etNEmMin.text.toString().toDoubleOrNull()?.let { reqMin["NEm"] = it }
            dialogBinding.etCaMin.text.toString().toDoubleOrNull()?.let { reqMin["Ca"] = it }
            dialogBinding.etPMin.text.toString().toDoubleOrNull()?.let { reqMin["P"] = it }

            // Requerimientos máximos
            val reqMax = mutableMapOf<String, Double>()
            dialogBinding.etNDFMax.text.toString().toDoubleOrNull()?.let { reqMax["NDF"] = it }
            dialogBinding.etCPMax.text.toString().toDoubleOrNull()?.let { reqMax["CP"] = it }

            val nuevoAnimal = Animal(
                id = animal?.id ?: UUID.randomUUID().toString(),
                nombre = nombre,
                tipo = tipo,
                pesoKg = peso,
                consumoDMI = dmi,
                requerimientosMinimos = reqMin,
                requerimientosMaximos = reqMax
            )

            guardarAnimal(nuevoAnimal)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun guardarAnimal(animal: Animal) {
        lifecycleScope.launch {
            try {
                val exito = inventario.guardarAnimal(animal)

                if (exito) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.animal_guardado),
                        Toast.LENGTH_SHORT
                    ).show()
                    cargarAnimales()
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.error_guardar),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "${getString(R.string.error_guardar)}: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun confirmarEliminar(animal: Animal) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.eliminar))
            .setMessage(getString(R.string.confirmar_eliminar_animal))
            .setPositiveButton(getString(R.string.si)) { _, _ ->
                eliminarAnimal(animal)
            }
            .setNegativeButton(getString(R.string.no), null)
            .show()
    }

    private fun eliminarAnimal(animal: Animal) {
        lifecycleScope.launch {
            try {
                val exito = inventario.eliminarAnimal(animal.id)

                if (exito) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.animal_eliminado),
                        Toast.LENGTH_SHORT
                    ).show()
                    cargarAnimales()
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.error_eliminar),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "${getString(R.string.error_eliminar)}: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}