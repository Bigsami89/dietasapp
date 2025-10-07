package com.example.dietasapp.ui.inventory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.dietasapp.R
import com.example.dietasapp.data.InventarioItem
import com.example.dietasapp.database.BaseDeDatosJSON
import com.example.dietasapp.databinding.ItemlStockBinding
import com.example.dietasapp.databinding.TabAnimalesBinding
import com.example.dietasapp.inventory.Inventario
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fragmento del tab de Stock
 * Muestra el inventario actual de insumos con cantidades disponibles
 */
class StockTabFragment : Fragment() {

    private var _binding: TabAnimalesBinding? = null
    private val binding get() = _binding!!

    private lateinit var inventario: Inventario
    private lateinit var adapter: StockAdapter

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
        cargarStock()
    }

    private fun setupRecyclerView() {
        adapter = StockAdapter { item ->
            showEditStockDialog(item)
        }

        binding.recyclerAnimales.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@StockTabFragment.adapter
        }
    }

    private fun cargarStock() {
        lifecycleScope.launch {
            try {
                val stock = inventario.obtenerInventarioInsumos()

                if (stock.isEmpty()) {
                    binding.emptyStateAnimales.visibility = View.VISIBLE
                    binding.recyclerAnimales.visibility = View.GONE
                } else {
                    binding.emptyStateAnimales.visibility = View.GONE
                    binding.recyclerAnimales.visibility = View.VISIBLE
                    adapter.submitList(stock)
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
        lifecycleScope.launch {
            // Obtener insumos que no están en stock
            val insumos = inventario.obtenerInsumos()
            val stockActual = inventario.obtenerInventarioInsumos()
            val insumosEnStock = stockActual.map { it.insumo.id }.toSet()

            val insumosDisponibles = insumos.filterNot { insumosEnStock.contains(it.id) }

            if (insumosDisponibles.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Todos los insumos ya están en stock. Agrega más insumos primero.",
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }

            val nombres = insumosDisponibles.map { it.nombre }.toTypedArray()

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Agregar Insumo al Stock")
                .setItems(nombres) { _, which ->
                    val selectedInsumo = insumosDisponibles[which]
                    showQuantityDialog(selectedInsumo.id, selectedInsumo.nombre)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun showQuantityDialog(insumoId: String, insumoNombre: String) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_to_stock, null)

        val etCantidad = dialogView.findViewById<TextInputEditText>(R.id.etCantidad)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Agregar $insumoNombre al Stock")
            .setView(dialogView)
            .setPositiveButton("Agregar") { _, _ ->
                val cantidad = etCantidad.text.toString().toDoubleOrNull() ?: 0.0
                if (cantidad > 0) {
                    agregarAlStock(insumoId, cantidad)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Cantidad inválida",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showEditStockDialog(item: InventarioItem) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_to_stock, null)

        val tilCantidad = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilCantidad)
        val etCantidad = dialogView.findViewById<TextInputEditText>(R.id.etCantidad)

        tilCantidad.hint = "Nueva cantidad (kg)"
        etCantidad.setText(item.cantidadDisponibleKg.toString())

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Editar Stock de ${item.insumo.nombre}")
            .setMessage("Cantidad actual: ${String.format("%.2f", item.cantidadDisponibleKg)} kg")
            .setView(dialogView)
            .setPositiveButton("Actualizar") { _, _ ->
                val nuevaCantidad = etCantidad.text.toString().toDoubleOrNull() ?: 0.0
                if (nuevaCantidad >= 0) {
                    actualizarStock(item.insumo.id, nuevaCantidad)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Cantidad inválida",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNeutralButton("Eliminar del Stock") { _, _ ->
                confirmarEliminarStock(item)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun agregarAlStock(insumoId: String, cantidad: Double) {
        lifecycleScope.launch {
            try {
                val insumo = inventario.obtenerInsumo(insumoId)
                if (insumo != null) {
                    val exito = inventario.agregarAlInventario(insumo, cantidad)

                    if (exito) {
                        Toast.makeText(
                            requireContext(),
                            "Stock agregado exitosamente",
                            Toast.LENGTH_SHORT
                        ).show()
                        cargarStock()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.error_guardar),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
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

    private fun actualizarStock(insumoId: String, nuevaCantidad: Double) {
        lifecycleScope.launch {
            try {
                val exito = inventario.actualizarCantidad(insumoId, nuevaCantidad)

                if (exito) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.stock_actualizado),
                        Toast.LENGTH_SHORT
                    ).show()
                    cargarStock()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Error al actualizar stock",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Error al actualizar stock: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun confirmarEliminarStock(item: InventarioItem) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar del Stock")
            .setMessage("¿Seguro que deseas eliminar ${item.insumo.nombre} del stock?\n\nEsto no eliminará el insumo, solo su entrada en el inventario.")
            .setPositiveButton(getString(R.string.si)) { _, _ ->
                eliminarStock(item.insumo.id)
            }
            .setNegativeButton(getString(R.string.no), null)
            .show()
    }

    private fun eliminarStock(insumoId: String) {
        lifecycleScope.launch {
            try {
                val exito = inventario.eliminarDelInventario(insumoId)

                if (exito) {
                    Toast.makeText(
                        requireContext(),
                        "Item eliminado del stock",
                        Toast.LENGTH_SHORT
                    ).show()
                    cargarStock()
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

    // Adapter para Stock
    private class StockAdapter(
        private val onEditClick: (InventarioItem) -> Unit
    ) : ListAdapter<InventarioItem, StockAdapter.ViewHolder>(DiffCallback()) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemlStockBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ViewHolder(binding, onEditClick)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        class ViewHolder(
            private val binding: ItemlStockBinding,
            private val onEditClick: (InventarioItem) -> Unit
        ) : RecyclerView.ViewHolder(binding.root) {

            fun bind(item: InventarioItem) {
                binding.apply {
                    tvNombreStock.text = item.insumo.nombre
                    tvCantidadStock.text = "${String.format("%.2f", item.cantidadDisponibleKg)} kg"

                    val valorTotal = item.insumo.costo * item.cantidadDisponibleKg
                    tvValorStock.text = "$${String.format("%.2f", valorTotal)}"

                    // Formatear fecha
                    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    tvUltimaActualizacion.text = "Actualizado: ${sdf.format(Date(item.fechaActualizacion))}"

                    // Barra de progreso visual (ejemplo: capacidad máxima de 1000 kg)
                    val maxCapacity = 1000.0
                    val percentage = ((item.cantidadDisponibleKg / maxCapacity) * 100).toInt().coerceIn(0, 100)
                    progressStock.progress = percentage

                    btnEditarStock.setOnClickListener {
                        onEditClick(item)
                    }
                }
            }
        }

        private class DiffCallback : DiffUtil.ItemCallback<InventarioItem>() {
            override fun areItemsTheSame(oldItem: InventarioItem, newItem: InventarioItem): Boolean {
                return oldItem.insumo.id == newItem.insumo.id
            }

            override fun areContentsTheSame(oldItem: InventarioItem, newItem: InventarioItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}