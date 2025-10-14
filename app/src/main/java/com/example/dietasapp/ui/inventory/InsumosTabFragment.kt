package com.example.dietasapp.ui.inventory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dietasapp.R
import com.example.dietasapp.data.Insumo
import com.example.dietasapp.database.BaseDeDatosJSON
import com.example.dietasapp.database.CatalogManager
import com.example.dietasapp.databinding.TabAnimalesBinding
import com.example.dietasapp.inventory.Inventario
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Fragmento del tab de Insumos
 * Permite agregar y editar insumos desde catálogo o manualmente
 */
class InsumosTabFragment : Fragment() {

    private var _binding: TabAnimalesBinding? = null
    private val binding get() = _binding!!

    private lateinit var inventario: Inventario
    private lateinit var catalogManager: CatalogManager
    private lateinit var adapter: InsumoAdapter

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
        cargarInsumos()
    }

    private fun setupRecyclerView() {
        adapter = InsumoAdapter(
            onEditClick = { insumo ->
                showEditDialog(insumo)
            },
            onDeleteClick = { insumo ->
                confirmarEliminar(insumo)
            }
        )

        binding.recyclerAnimales.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@InsumosTabFragment.adapter
        }
    }

    private fun cargarInsumos() {
        lifecycleScope.launch {
            try {
                val insumos = inventario.obtenerInsumos()

                if (insumos.isEmpty()) {
                    binding.emptyStateAnimales.visibility = View.VISIBLE
                    binding.recyclerAnimales.visibility = View.GONE
                } else {
                    binding.emptyStateAnimales.visibility = View.GONE
                    binding.recyclerAnimales.visibility = View.VISIBLE
                    adapter.submitList(insumos)
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

    /**
     * Muestra el diálogo para editar un insumo existente
     */
    private fun showEditDialog(insumo: Insumo) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_insumo, null)

        // Referencias a los campos
        val etNombre = dialogView.findViewById<TextInputEditText>(R.id.etNombre)
        val etCosto = dialogView.findViewById<TextInputEditText>(R.id.etCosto)
        val switchForraje = dialogView.findViewById<SwitchMaterial>(R.id.switchForraje)

        // Nutrientes
        val etGE = dialogView.findViewById<TextInputEditText>(R.id.etGE)
        val etCP = dialogView.findViewById<TextInputEditText>(R.id.etCP)
        val etTDN = dialogView.findViewById<TextInputEditText>(R.id.etTDN)
        val etNEm = dialogView.findViewById<TextInputEditText>(R.id.etNEm)
        val etCa = dialogView.findViewById<TextInputEditText>(R.id.etCa)
        val etP = dialogView.findViewById<TextInputEditText>(R.id.etP)
        val etNDF = dialogView.findViewById<TextInputEditText>(R.id.etNDF)
        val etStarch = dialogView.findViewById<TextInputEditText>(R.id.etStarch)
        val etFat = dialogView.findViewById<TextInputEditText>(R.id.etFat)

        // Establecer valores actuales
        etNombre.setText(insumo.nombre)
        etCosto.setText(insumo.costo.toString())
        switchForraje.isChecked = insumo.esForraje

        // Cargar nutrientes
        etGE.setText(insumo.getNutriente(Insumo.GE).toString())
        etCP.setText(insumo.getNutriente(Insumo.CP).toString())
        etTDN.setText(insumo.getNutriente(Insumo.TDN).toString())
        etNEm.setText(insumo.getNutriente(Insumo.NEM).toString())
        etCa.setText(insumo.getNutriente(Insumo.CA).toString())
        etP.setText(insumo.getNutriente(Insumo.P).toString())
        etNDF.setText(insumo.getNutriente(Insumo.NDF).toString())
        etStarch.setText(insumo.getNutriente(Insumo.STARCH).toString())
        etFat.setText(insumo.getNutriente(Insumo.FAT).toString())

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setPositiveButton(getString(R.string.guardar)) { _, _ ->
                // Validar campos obligatorios
                val nombre = etNombre.text.toString().trim()
                val costoStr = etCosto.text.toString().trim()

                if (nombre.isEmpty() || costoStr.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.error_campos_vacios),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                val costo = costoStr.toDoubleOrNull() ?: 0.0
                val esForraje = switchForraje.isChecked

                // Crear mapa de nutrientes
                val nutrientes = mutableMapOf<String, Double>()

                etGE.text.toString().toDoubleOrNull()?.let {
                    nutrientes[Insumo.GE] = it
                }
                etCP.text.toString().toDoubleOrNull()?.let {
                    nutrientes[Insumo.CP] = it
                }
                etTDN.text.toString().toDoubleOrNull()?.let {
                    nutrientes[Insumo.TDN] = it
                }
                etNEm.text.toString().toDoubleOrNull()?.let {
                    nutrientes[Insumo.NEM] = it
                }
                etCa.text.toString().toDoubleOrNull()?.let {
                    nutrientes[Insumo.CA] = it
                }
                etP.text.toString().toDoubleOrNull()?.let {
                    nutrientes[Insumo.P] = it
                }
                etNDF.text.toString().toDoubleOrNull()?.let {
                    nutrientes[Insumo.NDF] = it
                }
                etStarch.text.toString().toDoubleOrNull()?.let {
                    nutrientes[Insumo.STARCH] = it
                }
                etFat.text.toString().toDoubleOrNull()?.let {
                    nutrientes[Insumo.FAT] = it
                }

                // Crear insumo actualizado (mantener el mismo ID)
                val insumoActualizado = Insumo(
                    id = insumo.id,
                    nombre = nombre,
                    costo = costo,
                    esForraje = esForraje,
                    nutrientes = nutrientes
                )

                guardarInsumo(insumoActualizado)
            }
            .setNegativeButton(getString(R.string.cancelar), null)
            .show()
    }

    fun showAddDialog() {
        // Mostrar opciones: Catálogo o Manual
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Agregar Insumo")
            .setMessage("¿Cómo deseas agregar el insumo?")
            .setPositiveButton("Desde Catálogo") { _, _ ->
                showCatalogDialog()
            }
            .setNegativeButton("Manual") { _, _ ->
                showManualAddDialog()
            }
            .setNeutralButton("Cancelar", null)
            .show()
    }

    /**
     * Muestra el diálogo para agregar un insumo manualmente
     */
    private fun showManualAddDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_insumo, null)

        // Referencias a los campos
        val etNombre = dialogView.findViewById<TextInputEditText>(R.id.etNombre)
        val etCosto = dialogView.findViewById<TextInputEditText>(R.id.etCosto)
        val switchForraje = dialogView.findViewById<SwitchMaterial>(R.id.switchForraje)

        // Nutrientes
        val etGE = dialogView.findViewById<TextInputEditText>(R.id.etGE)
        val etCP = dialogView.findViewById<TextInputEditText>(R.id.etCP)
        val etTDN = dialogView.findViewById<TextInputEditText>(R.id.etTDN)
        val etNEm = dialogView.findViewById<TextInputEditText>(R.id.etNEm)
        val etCa = dialogView.findViewById<TextInputEditText>(R.id.etCa)
        val etP = dialogView.findViewById<TextInputEditText>(R.id.etP)
        val etNDF = dialogView.findViewById<TextInputEditText>(R.id.etNDF)
        val etStarch = dialogView.findViewById<TextInputEditText>(R.id.etStarch)
        val etFat = dialogView.findViewById<TextInputEditText>(R.id.etFat)


        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setPositiveButton(getString(R.string.guardar)) { _, _ ->
                // Validar campos obligatorios
                val nombre = etNombre.text.toString().trim()
                val costoStr = etCosto.text.toString().trim()

                if (nombre.isEmpty() || costoStr.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.error_campos_vacios),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                val costo = costoStr.toDoubleOrNull() ?: 0.0
                val esForraje = switchForraje.isChecked

                // Crear mapa de nutrientes
                val nutrientes = mutableMapOf<String, Double>()

                etGE.text.toString().toDoubleOrNull()?.let {
                    nutrientes[Insumo.GE] = it
                }
                etCP.text.toString().toDoubleOrNull()?.let {
                    nutrientes[Insumo.CP] = it
                }
                etTDN.text.toString().toDoubleOrNull()?.let {
                    nutrientes[Insumo.TDN] = it
                }
                etNEm.text.toString().toDoubleOrNull()?.let {
                    nutrientes[Insumo.NEM] = it
                }
                etCa.text.toString().toDoubleOrNull()?.let {
                    nutrientes[Insumo.CA] = it
                }
                etP.text.toString().toDoubleOrNull()?.let {
                    nutrientes[Insumo.P] = it
                }
                etNDF.text.toString().toDoubleOrNull()?.let {
                    nutrientes[Insumo.NDF] = it
                }
                etStarch.text.toString().toDoubleOrNull()?.let {
                    nutrientes[Insumo.STARCH] = it
                }
                etFat.text.toString().toDoubleOrNull()?.let {
                    nutrientes[Insumo.FAT] = it
                }

                // Crear nuevo insumo con ID único
                val nuevoInsumo = Insumo(
                    id = UUID.randomUUID().toString(),
                    nombre = nombre,
                    costo = costo,
                    esForraje = esForraje,
                    nutrientes = nutrientes
                )

                guardarInsumo(nuevoInsumo)
            }
            .setNegativeButton(getString(R.string.cancelar), null)
            .show()
    }

    private fun showCatalogDialog() {
        val catalog = catalogManager.getIngredientsCatalog()

        // Crear categorías para mejor presentación
        val categories = catalog.groupBy {
            when {
                it.esForraje -> "Forrajes"
                it.getNutriente("CP") > 30.0 -> "Suplementos Proteicos"
                it.getNutriente("Starch") > 50.0 -> "Granos Energéticos"
                else -> "Otros"
            }
        }

        // Crear lista de opciones formateada
        val opciones = mutableListOf<String>()
        val insumosPorIndice = mutableListOf<Insumo>()

        categories.forEach { (category, insumos) ->
            opciones.add("--- $category ---")
            insumosPorIndice.add(insumos[0]) // Placeholder

            insumos.forEach { insumo ->
                opciones.add("  ${insumo.nombre} - $${String.format("%.2f", insumo.costo)}/kg")
                insumosPorIndice.add(insumo)
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Seleccionar Insumo del Catálogo")
            .setItems(opciones.toTypedArray()) { _, which ->
                val selectedInsumo = insumosPorIndice[which]

                // Verificar que no sea un header de categoría
                if (!opciones[which].startsWith("---")) {
                    showAddToInventoryDialog(selectedInsumo)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showAddToInventoryDialog(catalogInsumo: Insumo) {
        val newInsumo = catalogManager.createIngredientFromCatalog(catalogInsumo)

        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_to_stock, null)

        val etCantidad = dialogView.findViewById<TextInputEditText>(R.id.etCantidad)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Agregar ${newInsumo.nombre}")
            .setMessage("¿Deseas agregar también al stock?")
            .setView(dialogView)
            .setPositiveButton("Agregar con Stock") { _, _ ->
                val cantidad = etCantidad.text.toString().toDoubleOrNull() ?: 0.0
                guardarInsumoConStock(newInsumo, cantidad)
            }
            .setNeutralButton("Solo Insumo") { _, _ ->
                guardarInsumo(newInsumo)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun guardarInsumo(insumo: Insumo) {
        lifecycleScope.launch {
            try {
                val exito = inventario.guardarInsumo(insumo)

                if (exito) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.insumo_guardado),
                        Toast.LENGTH_SHORT
                    ).show()
                    cargarInsumos()
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

    private fun guardarInsumoConStock(insumo: Insumo, cantidad: Double) {
        lifecycleScope.launch {
            try {
                // Guardar insumo
                val exitoInsumo = inventario.guardarInsumo(insumo)

                if (exitoInsumo && cantidad > 0) {
                    // Agregar al stock
                    val exitoStock = inventario.agregarAlInventario(insumo, cantidad)

                    if (exitoStock) {
                        Toast.makeText(
                            requireContext(),
                            "Insumo agregado con ${String.format("%.2f", cantidad)} kg en stock",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else if (exitoInsumo) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.insumo_guardado),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                cargarInsumos()
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "${getString(R.string.error_guardar)}: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun confirmarEliminar(insumo: Insumo) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.eliminar))
            .setMessage("¿Estás seguro de que deseas eliminar ${insumo.nombre}?")
            .setPositiveButton(getString(R.string.eliminar)) { _, _ ->
                eliminarInsumo(insumo)
            }
            .setNegativeButton(getString(R.string.cancelar), null)
            .show()
    }

    private fun eliminarInsumo(insumo: Insumo) {
        lifecycleScope.launch {
            try {
                val exito = inventario.eliminarInsumo(insumo.id)

                if (exito) {
                    Toast.makeText(
                        requireContext(),
                        "Insumo eliminado exitosamente",
                        Toast.LENGTH_SHORT
                    ).show()
                    cargarInsumos()
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