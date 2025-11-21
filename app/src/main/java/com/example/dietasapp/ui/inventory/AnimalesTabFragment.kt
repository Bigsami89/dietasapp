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
import com.example.dietasapp.database.CatalogManager
import com.example.dietasapp.databinding.DialogAnimalCompleteBinding
import com.example.dietasapp.databinding.TabAnimalesBinding
import com.example.dietasapp.inventory.Inventario
import kotlinx.coroutines.launch
import java.util.UUID

class AnimalesTabFragment : Fragment() {

    private var _binding: TabAnimalesBinding? = null
    private val binding get() = _binding!!

    private lateinit var inventario: Inventario
    private lateinit var catalogManager: CatalogManager
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
        catalogManager = CatalogManager(requireContext())

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
        // Mostrar opciones: Catálogo o Manual
        AlertDialog.Builder(requireContext())
            .setTitle("Agregar Animal")
            .setMessage("¿Cómo deseas agregar el animal?")
            .setPositiveButton("Desde Catálogo") { _, _ ->
                showCatalogDialog()
            }
            .setNegativeButton("Manual") { _, _ ->
                showAnimalDialog(null)
            }
            .setNeutralButton("Cancelar", null)
            .show()
    }

    private fun showCatalogDialog() {
        val catalog = catalogManager.getAnimalsCatalog()
        val nombres = catalog.map { it.nombre }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Seleccionar Animal del Catálogo")
            .setItems(nombres) { _, which ->
                val selectedAnimal = catalog[which]
                val newAnimal = catalogManager.createAnimalFromCatalog(selectedAnimal)
                guardarAnimal(newAnimal)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showEditDialog(animal: Animal) {
        showAnimalDialog(animal)
    }

    private fun showAnimalDialog(animal: Animal?) {
        val dialogBinding = DialogAnimalCompleteBinding.inflate(layoutInflater)

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
            dialogBinding.etNombre.setText(it.nombre)
            dialogBinding.etPeso.setText(it.pesoKg.toString())
            dialogBinding.etDMI.setText(it.consumoDMI.toString())
            dialogBinding.spinnerTipoDieta.setText(it.tipo.descripcion, false)

            // Requerimientos mínimos - Usando constantes de Animal
            it.requerimientosMinimos[Animal.PROTEINA_CRUDA]?.let { v ->
                dialogBinding.etCPMin.setText(v.toString())
            }
            it.requerimientosMinimos[Animal.ENERGIA_NETA_MANTENIMIENTO]?.let { v ->
                dialogBinding.etNEmMin.setText(v.toString())
            }
            it.requerimientosMinimos["TDN"]?.let { v ->
                dialogBinding.etTDNMin.setText(v.toString())
            }
            it.requerimientosMinimos[Animal.CALCIO]?.let { v ->
                dialogBinding.etCaMin.setText(v.toString())
            }
            it.requerimientosMinimos[Animal.FOSFORO]?.let { v ->
                dialogBinding.etPMin.setText(v.toString())
            }
            it.requerimientosMinimos[Animal.FIBRA_DETERGENTE_NEUTRA]?.let { v ->
                dialogBinding.etNDFMin.setText(v.toString())
            }
            it.requerimientosMinimos[Animal.EXTRACTO_ETEREO]?.let { v ->
                dialogBinding.etFatMin.setText(v.toString())
            }

            // Requerimientos máximos - Usando constantes de Animal
            it.requerimientosMaximos[Animal.PROTEINA_CRUDA]?.let { v ->
                dialogBinding.etCPMax.setText(v.toString())
            }
            it.requerimientosMaximos[Animal.ENERGIA_NETA_MANTENIMIENTO]?.let { v ->
                dialogBinding.etNEmMax.setText(v.toString())
            }
            it.requerimientosMaximos["TDN"]?.let { v ->
                dialogBinding.etTDNMax.setText(v.toString())
            }
            it.requerimientosMaximos[Animal.CALCIO]?.let { v ->
                dialogBinding.etCaMax.setText(v.toString())
            }
            it.requerimientosMaximos[Animal.FOSFORO]?.let { v ->
                dialogBinding.etPMax.setText(v.toString())
            }
            it.requerimientosMaximos[Animal.FIBRA_DETERGENTE_NEUTRA]?.let { v ->
                dialogBinding.etNDFMax.setText(v.toString())
            }
            it.requerimientosMaximos[Animal.EXTRACTO_ETEREO]?.let { v ->
                dialogBinding.etFatMax.setText(v.toString())
            }
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCancelar.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnGuardar.setOnClickListener {
            val resultado = validarYCrearAnimal(dialogBinding, animal)

            when {
                resultado.isSuccess -> {
                    guardarAnimal(resultado.getOrNull()!!)
                    dialog.dismiss()
                }
                else -> {
                    Toast.makeText(
                        requireContext(),
                        resultado.exceptionOrNull()?.message ?: "Error al crear animal",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        dialog.show()
    }

    /**
     * Valida los campos del diálogo y crea el objeto Animal
     */
    private fun validarYCrearAnimal(
        dialogBinding: DialogAnimalCompleteBinding,
        animalExistente: Animal?
    ): Result<Animal> {
        // Validar campos básicos
        val nombre = dialogBinding.etNombre.text.toString().trim()
        val pesoStr = dialogBinding.etPeso.text.toString().trim()
        val dmiStr = dialogBinding.etDMI.text.toString().trim()
        val tipoStr = dialogBinding.spinnerTipoDieta.text.toString()

        if (nombre.isEmpty() || pesoStr.isEmpty() || dmiStr.isEmpty() || tipoStr.isEmpty()) {
            return Result.failure(Exception(getString(R.string.error_campos_vacios)))
        }

        val peso = pesoStr.toDoubleOrNull()
        val dmi = dmiStr.toDoubleOrNull()

        if (peso == null || dmi == null || peso <= 0 || dmi <= 0) {
            return Result.failure(Exception("Valores numéricos inválidos"))
        }

        val tipo = TipoDieta.values().firstOrNull { it.descripcion == tipoStr }
            ?: return Result.failure(Exception("Tipo de dieta no válido"))

        // Construir requerimientos mínimos usando constantes de Animal
        val reqMin = buildMap {
            dialogBinding.etCPMin.text.toString().toDoubleOrNull()?.let {
                put(Animal.PROTEINA_CRUDA, it)
            }
            dialogBinding.etNEmMin.text.toString().toDoubleOrNull()?.let {
                put(Animal.ENERGIA_NETA_MANTENIMIENTO, it)
            }
            dialogBinding.etTDNMin.text.toString().toDoubleOrNull()?.let {
                put("TDN", it) // TDN no tiene constante en Animal aún
            }
            dialogBinding.etCaMin.text.toString().toDoubleOrNull()?.let {
                put(Animal.CALCIO, it)
            }
            dialogBinding.etPMin.text.toString().toDoubleOrNull()?.let {
                put(Animal.FOSFORO, it)
            }
            dialogBinding.etNDFMin.text.toString().toDoubleOrNull()?.let {
                put(Animal.FIBRA_DETERGENTE_NEUTRA, it)
            }
            dialogBinding.etFatMin.text.toString().toDoubleOrNull()?.let {
                put(Animal.EXTRACTO_ETEREO, it)
            }
        }

        // Construir requerimientos máximos usando constantes de Animal
        val reqMax = buildMap {
            dialogBinding.etCPMax.text.toString().toDoubleOrNull()?.let {
                put(Animal.PROTEINA_CRUDA, it)
            }
            dialogBinding.etNEmMax.text.toString().toDoubleOrNull()?.let {
                put(Animal.ENERGIA_NETA_MANTENIMIENTO, it)
            }
            dialogBinding.etTDNMax.text.toString().toDoubleOrNull()?.let {
                put("TDN", it)
            }
            dialogBinding.etCaMax.text.toString().toDoubleOrNull()?.let {
                put(Animal.CALCIO, it)
            }
            dialogBinding.etPMax.text.toString().toDoubleOrNull()?.let {
                put(Animal.FOSFORO, it)
            }
            dialogBinding.etNDFMax.text.toString().toDoubleOrNull()?.let {
                put(Animal.FIBRA_DETERGENTE_NEUTRA, it)
            }
            dialogBinding.etFatMax.text.toString().toDoubleOrNull()?.let {
                put(Animal.EXTRACTO_ETEREO, it)
            }
        }

        // Validar que los mínimos no sean mayores que los máximos
        reqMin.forEach { (key, minVal) ->
            reqMax[key]?.let { maxVal ->
                if (minVal > maxVal) {
                    return Result.failure(
                        Exception("El valor mínimo de $key no puede ser mayor que el máximo")
                    )
                }
            }
        }

        val nuevoAnimal = Animal(
            id = animalExistente?.id ?: UUID.randomUUID().toString(),
            nombre = nombre,
            tipo = tipo,
            pesoKg = peso,
            consumoDMI = dmi,
            requerimientosMinimos = reqMin,
            requerimientosMaximos = reqMax
        )

        return Result.success(nuevoAnimal)
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