package com.example.dietasapp.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.MarginLayoutParams
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dietasapp.R
import com.example.dietasapp.database.BaseDeDatosJSON
import com.example.dietasapp.databinding.FragmentHistoryBinding
import kotlinx.coroutines.launch

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerHistorial.layoutManager = LinearLayoutManager(requireContext())
        cargarHistorial()
    }

    private fun cargarHistorial() {
        lifecycleScope.launch {
            try {
                val baseDatos = BaseDeDatosJSON(requireContext())
                val dietas = baseDatos.obtenerDietas()

                if (dietas.isEmpty()) {
                    binding.emptyStateHistorial.visibility = View.VISIBLE
                    binding.recyclerHistorial.visibility = View.GONE
                } else {
                    binding.emptyStateHistorial.visibility = View.GONE
                    binding.recyclerHistorial.visibility = View.VISIBLE
                    Toast.makeText(
                        requireContext(),
                        "Dietas guardadas: ${dietas.size}",
                        Toast.LENGTH_SHORT
                    ).show()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}