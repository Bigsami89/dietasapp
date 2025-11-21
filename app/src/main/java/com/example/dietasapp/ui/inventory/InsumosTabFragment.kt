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
import com.example.dietasapp.data.InventarioItem
import com.example.dietasapp.database.BaseDeDatosJSON
import com.example.dietasapp.database.CatalogManager
import com.example.dietasapp.databinding.TabInsumosBinding
import com.example.dietasapp.inventory.Inventario
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Fragmento del tab de Insumos para formulación de raciones de rumiantes
 * Permite agregar y editar insumos desde catálogo o manualmente
 */
class InsumosTabFragment : Fragment() {

    private var _binding: TabInsumosBinding? = null
    private val binding get() = _binding!!

    private lateinit var inventario: Inventario
    private lateinit var catalogManager: CatalogManager
    private lateinit var adapter: InsumoAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = TabInsumosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val baseDatos = BaseDeDatosJSON(requireContext())
        inventario = Inventario(baseDatos)
        catalogManager = CatalogManager(requireContext())

        setupRecyclerView()
        loadData()
    }

    private fun setupRecyclerView() {
        adapter = InsumoAdapter(
            onEdit = { insumo -> showEditDialog(insumo) },
            onDelete = { insumo -> confirmDelete(insumo) }
        )

        binding.recyclerInsumos.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@InsumosTabFragment.adapter
        }
    }

    private fun loadData() {
        lifecycleScope.launch {
            try {
                val insumos = inventario.obtenerInsumos()
                adapter.submitList(insumos)

                binding.emptyStateInsumos.visibility =
                    if (insumos.isEmpty()) View.VISIBLE else View.GONE
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
     * Muestra el diálogo para agregar un nuevo insumo
     */
    fun showAddDialog() {
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

        // Referencias a los campos básicos
        val etNombre = dialogView.findViewById<TextInputEditText>(R.id.etNombre)
        val etCosto = dialogView.findViewById<TextInputEditText>(R.id.etCosto)
        val switchForraje = dialogView.findViewById<SwitchMaterial>(R.id.switchForraje)
        val etInclusionMinima = dialogView.findViewById<TextInputEditText>(R.id.etInclusionMinima)
        val etInclusionMaxima = dialogView.findViewById<TextInputEditText>(R.id.etInclusionMaxima)

        // Referencias a campos de Energía
        val etEnergiaBruta = dialogView.findViewById<TextInputEditText>(R.id.etEnergiaBruta)
        val etEnergiaMetabolizable = dialogView.findViewById<TextInputEditText>(R.id.etEnergiaMetabolizable)
        val etEnergiaNetaMantenimiento = dialogView.findViewById<TextInputEditText>(R.id.etEnergiaNetaMantenimiento)

        // Referencias a campos de Composición Nutricional
        val etProteinaCruda = dialogView.findViewById<TextInputEditText>(R.id.etProteinaCruda)
        val etFDN = dialogView.findViewById<TextInputEditText>(R.id.etFDN)
        val etFDA = dialogView.findViewById<TextInputEditText>(R.id.etFDA)
        val etExtractoEtereo = dialogView.findViewById<TextInputEditText>(R.id.etExtractoEtereo)
        val etCenizas = dialogView.findViewById<TextInputEditText>(R.id.etCenizas)
        val etDegradabilidadRuminal = dialogView.findViewById<TextInputEditText>(R.id.etDegradabilidadRuminal)
        val etMetanoProducido = dialogView.findViewById<TextInputEditText>(R.id.etMetanoProducido)

        // Referencias a Macrominerales
        val etCalcio = dialogView.findViewById<TextInputEditText>(R.id.etCalcio)
        val etFosforo = dialogView.findViewById<TextInputEditText>(R.id.etFosforo)
        val etMagnesio = dialogView.findViewById<TextInputEditText>(R.id.etMagnesio)
        val etSodio = dialogView.findViewById<TextInputEditText>(R.id.etSodio)
        val etPotasio = dialogView.findViewById<TextInputEditText>(R.id.etPotasio)
        val etAzufre = dialogView.findViewById<TextInputEditText>(R.id.etAzufre)

        // Referencias a Microminerales
        val etCobre = dialogView.findViewById<TextInputEditText>(R.id.etCobre)
        val etZinc = dialogView.findViewById<TextInputEditText>(R.id.etZinc)
        val etSelenio = dialogView.findViewById<TextInputEditText>(R.id.etSelenio)
        val etCobalto = dialogView.findViewById<TextInputEditText>(R.id.etCobalto)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Nuevo Insumo")
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
                val inclusionMin = etInclusionMinima.text.toString().toDoubleOrNull() ?: 0.0
                val inclusionMax = etInclusionMaxima.text.toString().toDoubleOrNull() ?: 100.0

                // Validar restricciones de inclusión
                if (inclusionMin < 0.0 || inclusionMax > 100.0 || inclusionMin > inclusionMax) {
                    Toast.makeText(
                        requireContext(),
                        "Restricciones de inclusión inválidas",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                // Crear mapa de nutrientes
                val nutrientes = buildNutrientMap(
                    etEnergiaBruta, etEnergiaMetabolizable, etEnergiaNetaMantenimiento,
                    etProteinaCruda, etFDN, etFDA, etExtractoEtereo, etCenizas,
                    etDegradabilidadRuminal, etMetanoProducido,
                    etCalcio, etFosforo, etMagnesio, etSodio, etPotasio, etAzufre,
                    etCobre, etZinc, etSelenio, etCobalto
                )

                // Crear insumo
                val nuevoInsumo = Insumo(
                    id = UUID.randomUUID().toString(),
                    nombre = nombre,
                    costo = costo,
                    esForraje = esForraje,
                    inclusionMinima = inclusionMin,
                    inclusionMaxima = inclusionMax,
                    nutrientes = nutrientes
                )

                guardarInsumo(nuevoInsumo)
            }
            .setNegativeButton(getString(R.string.cancelar), null)
            .show()
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
        val etInclusionMinima = dialogView.findViewById<TextInputEditText>(R.id.etInclusionMinima)
        val etInclusionMaxima = dialogView.findViewById<TextInputEditText>(R.id.etInclusionMaxima)

        // Campos de Energía
        val etEnergiaBruta = dialogView.findViewById<TextInputEditText>(R.id.etEnergiaBruta)
        val etEnergiaMetabolizable = dialogView.findViewById<TextInputEditText>(R.id.etEnergiaMetabolizable)
        val etEnergiaNetaMantenimiento = dialogView.findViewById<TextInputEditText>(R.id.etEnergiaNetaMantenimiento)

        // Campos de Composición
        val etProteinaCruda = dialogView.findViewById<TextInputEditText>(R.id.etProteinaCruda)
        val etFDN = dialogView.findViewById<TextInputEditText>(R.id.etFDN)
        val etFDA = dialogView.findViewById<TextInputEditText>(R.id.etFDA)
        val etExtractoEtereo = dialogView.findViewById<TextInputEditText>(R.id.etExtractoEtereo)
        val etCenizas = dialogView.findViewById<TextInputEditText>(R.id.etCenizas)
        val etDegradabilidadRuminal = dialogView.findViewById<TextInputEditText>(R.id.etDegradabilidadRuminal)
        val etMetanoProducido = dialogView.findViewById<TextInputEditText>(R.id.etMetanoProducido)

        // Macrominerales
        val etCalcio = dialogView.findViewById<TextInputEditText>(R.id.etCalcio)
        val etFosforo = dialogView.findViewById<TextInputEditText>(R.id.etFosforo)
        val etMagnesio = dialogView.findViewById<TextInputEditText>(R.id.etMagnesio)
        val etSodio = dialogView.findViewById<TextInputEditText>(R.id.etSodio)
        val etPotasio = dialogView.findViewById<TextInputEditText>(R.id.etPotasio)
        val etAzufre = dialogView.findViewById<TextInputEditText>(R.id.etAzufre)

        // Microminerales
        val etCobre = dialogView.findViewById<TextInputEditText>(R.id.etCobre)
        val etZinc = dialogView.findViewById<TextInputEditText>(R.id.etZinc)
        val etSelenio = dialogView.findViewById<TextInputEditText>(R.id.etSelenio)
        val etCobalto = dialogView.findViewById<TextInputEditText>(R.id.etCobalto)

        // Establecer valores actuales
        etNombre.setText(insumo.nombre)
        etCosto.setText(insumo.costo.toString())
        switchForraje.isChecked = insumo.esForraje
        etInclusionMinima.setText(insumo.inclusionMinima.toString())
        etInclusionMaxima.setText(insumo.inclusionMaxima.toString())

        // Cargar nutrientes existentes
        loadNutrientValues(
            insumo,
            etEnergiaBruta, etEnergiaMetabolizable, etEnergiaNetaMantenimiento,
            etProteinaCruda, etFDN, etFDA, etExtractoEtereo, etCenizas,
            etDegradabilidadRuminal, etMetanoProducido,
            etCalcio, etFosforo, etMagnesio, etSodio, etPotasio, etAzufre,
            etCobre, etZinc, etSelenio, etCobalto
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Editar Insumo")
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
                val inclusionMin = etInclusionMinima.text.toString().toDoubleOrNull() ?: 0.0
                val inclusionMax = etInclusionMaxima.text.toString().toDoubleOrNull() ?: 100.0

                // Validar restricciones
                if (inclusionMin < 0.0 || inclusionMax > 100.0 || inclusionMin > inclusionMax) {
                    Toast.makeText(
                        requireContext(),
                        "Restricciones de inclusión inválidas",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                // Crear mapa de nutrientes actualizado
                val nutrientes = buildNutrientMap(
                    etEnergiaBruta, etEnergiaMetabolizable, etEnergiaNetaMantenimiento,
                    etProteinaCruda, etFDN, etFDA, etExtractoEtereo, etCenizas,
                    etDegradabilidadRuminal, etMetanoProducido,
                    etCalcio, etFosforo, etMagnesio, etSodio, etPotasio, etAzufre,
                    etCobre, etZinc, etSelenio, etCobalto
                )

                // Crear insumo actualizado (mantener el mismo ID)
                val insumoActualizado = Insumo(
                    id = insumo.id,
                    nombre = nombre,
                    costo = costo,
                    esForraje = esForraje,
                    inclusionMinima = inclusionMin,
                    inclusionMaxima = inclusionMax,
                    nutrientes = nutrientes
                )

                guardarInsumo(insumoActualizado)
            }
            .setNegativeButton(getString(R.string.cancelar), null)
            .show()
    }

    /**
     * Construye el mapa de nutrientes desde los campos del formulario
     */
    private fun buildNutrientMap(
        // Energía
        etEB: TextInputEditText?, etEM: TextInputEditText?, etENm: TextInputEditText?,
        // Composición
        etPC: TextInputEditText?, etFDN: TextInputEditText?, etFDA: TextInputEditText?,
        etEE: TextInputEditText?, etCenizas: TextInputEditText?,
        etDegRum: TextInputEditText?, etCH4: TextInputEditText?,
        // Macrominerales
        etCa: TextInputEditText?, etP: TextInputEditText?, etMg: TextInputEditText?,
        etNa: TextInputEditText?, etK: TextInputEditText?, etS: TextInputEditText?,
        // Microminerales
        etCu: TextInputEditText?, etZn: TextInputEditText?,
        etSe: TextInputEditText?, etCo: TextInputEditText?
    ): Map<String, Double> {
        val nutrientes = mutableMapOf<String, Double>()

        // Energía
        etEB?.text.toString().toDoubleOrNull()?.let { nutrientes[Insumo.ENERGIA_BRUTA] = it }
        etEM?.text.toString().toDoubleOrNull()?.let { nutrientes[Insumo.ENERGIA_METABOLIZABLE] = it }
        etENm?.text.toString().toDoubleOrNull()?.let { nutrientes[Insumo.ENERGIA_NETA_MANTENIMIENTO] = it }

        // Composición Nutricional
        etPC?.text.toString().toDoubleOrNull()?.let { nutrientes[Insumo.PROTEINA_CRUDA] = it }
        etFDN?.text.toString().toDoubleOrNull()?.let { nutrientes[Insumo.FIBRA_DETERGENTE_NEUTRA] = it }
        etFDA?.text.toString().toDoubleOrNull()?.let { nutrientes[Insumo.FIBRA_DETERGENTE_ACIDA] = it }
        etEE?.text.toString().toDoubleOrNull()?.let { nutrientes[Insumo.EXTRACTO_ETEREO] = it }
        etCenizas?.text.toString().toDoubleOrNull()?.let { nutrientes[Insumo.CENIZAS] = it }
        etDegRum?.text.toString().toDoubleOrNull()?.let { nutrientes[Insumo.DEGRADABILIDAD_RUMINAL] = it }
        etCH4?.text.toString().toDoubleOrNull()?.let { nutrientes[Insumo.METANO_PRODUCIDO] = it }

        // Macrominerales
        etCa?.text.toString().toDoubleOrNull()?.let { nutrientes[Insumo.CALCIO] = it }
        etP?.text.toString().toDoubleOrNull()?.let { nutrientes[Insumo.FOSFORO] = it }
        etMg?.text.toString().toDoubleOrNull()?.let { nutrientes[Insumo.MAGNESIO] = it }
        etNa?.text.toString().toDoubleOrNull()?.let { nutrientes[Insumo.SODIO] = it }
        etK?.text.toString().toDoubleOrNull()?.let { nutrientes[Insumo.POTASIO] = it }
        etS?.text.toString().toDoubleOrNull()?.let { nutrientes[Insumo.AZUFRE] = it }

        // Microminerales
        etCu?.text.toString().toDoubleOrNull()?.let { nutrientes[Insumo.COBRE] = it }
        etZn?.text.toString().toDoubleOrNull()?.let { nutrientes[Insumo.ZINC] = it }
        etSe?.text.toString().toDoubleOrNull()?.let { nutrientes[Insumo.SELENIO] = it }
        etCo?.text.toString().toDoubleOrNull()?.let { nutrientes[Insumo.COBALTO] = it }

        return nutrientes
    }

    /**
     * Carga los valores de nutrientes desde el insumo a los campos del formulario
     */
    private fun loadNutrientValues(
        insumo: Insumo,
        // Energía
        etEB: TextInputEditText?, etEM: TextInputEditText?, etENm: TextInputEditText?,
        // Composición
        etPC: TextInputEditText?, etFDN: TextInputEditText?, etFDA: TextInputEditText?,
        etEE: TextInputEditText?, etCenizas: TextInputEditText?,
        etDegRum: TextInputEditText?, etCH4: TextInputEditText?,
        // Macrominerales
        etCa: TextInputEditText?, etP: TextInputEditText?, etMg: TextInputEditText?,
        etNa: TextInputEditText?, etK: TextInputEditText?, etS: TextInputEditText?,
        // Microminerales
        etCu: TextInputEditText?, etZn: TextInputEditText?,
        etSe: TextInputEditText?, etCo: TextInputEditText?
    ) {
        // Energía
        etEB?.setText(insumo.getNutriente(Insumo.ENERGIA_BRUTA).toString())
        etEM?.setText(insumo.getNutriente(Insumo.ENERGIA_METABOLIZABLE).toString())
        etENm?.setText(insumo.getNutriente(Insumo.ENERGIA_NETA_MANTENIMIENTO).toString())

        // Composición
        etPC?.setText(insumo.getNutriente(Insumo.PROTEINA_CRUDA).toString())
        etFDN?.setText(insumo.getNutriente(Insumo.FIBRA_DETERGENTE_NEUTRA).toString())
        etFDA?.setText(insumo.getNutriente(Insumo.FIBRA_DETERGENTE_ACIDA).toString())
        etEE?.setText(insumo.getNutriente(Insumo.EXTRACTO_ETEREO).toString())
        etCenizas?.setText(insumo.getNutriente(Insumo.CENIZAS).toString())
        etDegRum?.setText(insumo.getNutriente(Insumo.DEGRADABILIDAD_RUMINAL).toString())
        etCH4?.setText(insumo.getNutriente(Insumo.METANO_PRODUCIDO).toString())

        // Macrominerales
        etCa?.setText(insumo.getNutriente(Insumo.CALCIO).toString())
        etP?.setText(insumo.getNutriente(Insumo.FOSFORO).toString())
        etMg?.setText(insumo.getNutriente(Insumo.MAGNESIO).toString())
        etNa?.setText(insumo.getNutriente(Insumo.SODIO).toString())
        etK?.setText(insumo.getNutriente(Insumo.POTASIO).toString())
        etS?.setText(insumo.getNutriente(Insumo.AZUFRE).toString())

        // Microminerales
        etCu?.setText(insumo.getNutriente(Insumo.COBRE).toString())
        etZn?.setText(insumo.getNutriente(Insumo.ZINC).toString())
        etSe?.setText(insumo.getNutriente(Insumo.SELENIO).toString())
        etCo?.setText(insumo.getNutriente(Insumo.COBALTO).toString())
    }

    /**
     * Muestra el catálogo de insumos predefinidos
     */
    private fun showCatalogDialog() {
        val insumos = catalogManager.obtenerCatalogoInsumos()
        val opciones = mutableListOf<String>()
        val insumosPorIndice = mutableListOf<Insumo>()

        // Agrupar por categorías
        val forrajes = insumos.filter { it.esForraje }
        val concentrados = insumos.filter { !it.esForraje }

        if (forrajes.isNotEmpty()) {
            opciones.add("--- FORRAJES ---")
            insumosPorIndice.add(forrajes[0])

            forrajes.forEach { insumo ->
                opciones.add("  ${insumo.nombre} - $${String.format("%.2f", insumo.costo)}/kg")
                insumosPorIndice.add(insumo)
            }
        }

        if (concentrados.isNotEmpty()) {
            opciones.add("--- CONCENTRADOS ---")
            insumosPorIndice.add(concentrados[0])

            concentrados.forEach { insumo ->
                opciones.add("  ${insumo.nombre} - $${String.format("%.2f", insumo.costo)}/kg")
                insumosPorIndice.add(insumo)
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Seleccionar Insumo del Catálogo")
            .setItems(opciones.toTypedArray()) { _, which ->
                val selectedInsumo = insumosPorIndice[which]

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
            .setNegativeButton("Solo Agregar") { _, _ ->
                guardarInsumo(newInsumo)
            }
            .setNeutralButton("Cancelar", null)
            .show()
    }

    private fun guardarInsumoConStock(insumo: Insumo, cantidad: Double) {
        lifecycleScope.launch {
            try {
                val exitoInsumo = inventario.guardarInsumo(insumo)

                if (exitoInsumo && cantidad > 0) {
                    val inventarioItem = InventarioItem(
                        insumo = insumo,
                        cantidadDisponibleKg = cantidad
                    )
                    inventario.agregarInventario(inventarioItem)
                }

                if (exitoInsumo) {
                    Toast.makeText(
                        requireContext(),
                        "Insumo agregado exitosamente",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadData()
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
                    getString(R.string.error_guardar),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun guardarInsumo(insumo: Insumo) {
        lifecycleScope.launch {
            try {
                val exito = inventario.guardarInsumo(insumo)

                if (exito) {
                    Toast.makeText(
                        requireContext(),
                        "Insumo guardado exitosamente",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadData()
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
                    getString(R.string.error_guardar),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun confirmDelete(insumo: Insumo) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar Insumo")
            .setMessage("¿Estás seguro de eliminar ${insumo.nombre}?")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteInsumo(insumo)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteInsumo(insumo: Insumo) {
        lifecycleScope.launch {
            try {
                val exito = inventario.eliminarInsumo(insumo.id)

                if (exito) {
                    Toast.makeText(
                        requireContext(),
                        "Insumo eliminado",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadData()
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
                    getString(R.string.error_eliminar),
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
