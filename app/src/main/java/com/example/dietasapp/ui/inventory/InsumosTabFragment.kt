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
import com.example.dietasapp.database.BaseDeDatosJSON
import com.example.dietasapp.databinding.TabAnimalesBinding
import com.example.dietasapp.inventory.Inventario
import kotlinx.coroutines.launch

/**
 * Fragmento del tab de Insumos
 */
class InsumosTabFragment : Fragment() {

    private var _binding: TabAnimalesBinding? = null
    private val binding get() = _binding!!

    private lateinit var inventario: Inventario
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

        setupRecyclerView()
        cargarInsumos()
    }

    private fun setupRecyclerView() {
        adapter = InsumoAdapter(
            onEditClick = { insumo ->
                Toast.makeText(requireContext(), "Editar: ${insumo.nombre}", Toast.LENGTH_SHORT).show()
            },
            onDeleteClick = { insumo ->
                lifecycleScope.launch {
                    inventario.eliminarInsumo(insumo.id)
                    cargarInsumos()
                }
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

    fun showAddDialog() {
        Toast.makeText(requireContext(), "Agregar insumo - Funcionalidad en desarrollo", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}