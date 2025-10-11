package com.example.dietasapp.ui.history

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
import com.example.dietasapp.databinding.FragmentHistoryBinding
import com.example.dietasapp.domain.Dieta
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var baseDatos: BaseDeDatosJSON
    private lateinit var adapter: HistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        baseDatos = BaseDeDatosJSON(requireContext())
        setupRecyclerView()
        cargarHistorial()
    }

    private fun setupRecyclerView() {
        adapter = HistoryAdapter(
            onDietaClick = { dieta ->
                mostrarDetallesDieta(dieta)
            },
            onEliminarClick = { dieta ->
                confirmarEliminarDieta(dieta)
            }
        )

        binding.recyclerHistorial.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@HistoryFragment.adapter
        }
    }

    private fun cargarHistorial() {
        lifecycleScope.launch {
            try {
                val dietas = baseDatos.obtenerDietas()

                if (dietas.isEmpty()) {
                    binding.emptyStateHistorial.visibility = View.VISIBLE
                    binding.recyclerHistorial.visibility = View.GONE
                } else {
                    binding.emptyStateHistorial.visibility = View.GONE
                    binding.recyclerHistorial.visibility = View.VISIBLE
                    adapter.submitList(dietas)
                }
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_cargar_historial),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun mostrarDetallesDieta(dieta: Dieta) {
        val dialog = DietaDetalleDialog.newInstance(dieta)
        dialog.show(parentFragmentManager, "DietaDetalleDialog")
    }

    private fun confirmarEliminarDieta(dieta: Dieta) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.eliminar_dieta))
            .setMessage(getString(R.string.confirmar_eliminar_dieta))
            .setPositiveButton(getString(R.string.eliminar)) { _, _ ->
                eliminarDieta(dieta)
            }
            .setNegativeButton(getString(R.string.cancelar), null)
            .show()
    }

    private fun eliminarDieta(dieta: Dieta) {
        lifecycleScope.launch {
            try {
                val eliminado = baseDatos.eliminarDieta(dieta.fechaCreacion)

                if (eliminado) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.dieta_eliminada),
                        Toast.LENGTH_SHORT
                    ).show()
                    cargarHistorial() // Recargar lista
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.error_eliminar_dieta),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_eliminar_dieta),
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