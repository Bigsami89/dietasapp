package com.example.dietasapp.ui.history

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.core.view.isVisible
import com.example.dietasapp.data.prefs.AppPrefs
import com.example.dietasapp.R
import com.example.dietasapp.databinding.DialogDietaDetalleBinding
import com.example.dietasapp.domain.Dieta
import com.example.dietasapp.ui.home.ComposicionAdapter
import com.example.dietasapp.ui.home.ComposicionItem
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dialog para mostrar los detalles completos de una dieta guardada
 */
class DietaDetalleDialog : DialogFragment() {

    private var _binding: DialogDietaDetalleBinding? = null
    private val binding get() = _binding!!

    private lateinit var dieta: Dieta
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    companion object {
        private const val ARG_DIETA = "dieta"

        fun newInstance(dieta: Dieta): DietaDetalleDialog {
            return DietaDetalleDialog().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_DIETA, dieta)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dieta = arguments?.getSerializable(ARG_DIETA) as Dieta
        setStyle(STYLE_NORMAL, R.style.Theme_Dietasapp_Dialog_FullScreen)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogDietaDetalleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        mostrarDetalles()
    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            setNavigationIcon(R.drawable.ic_close)
            setNavigationOnClickListener { dismiss() }
            title = getString(R.string.detalles_dieta)
        }
    }

    private fun mostrarDetalles() {
        // Información básica
        binding.tvNombreAnimal.text = dieta.animal.nombre
        binding.tvFechaCreacion.text = dateFormat.format(Date(dieta.fechaCreacion))

        // Este proyecto no tiene modoOptimizacion en Dieta, mostramos "No aplica"
        binding.tvModoOptimizacion.text = getString(R.string.no_aplica)

        // Resumen de costos y emisiones
        binding.tvCostoTotal.text = getString(R.string.formato_costo_total, dieta.costoTotal)

        // Usar metanoProducidoGramos en lugar de emisionesMetano
        val isRuminant = AppPrefs.getTipoAnimal(requireContext()) == AppPrefs.TIPO_MULTI
        binding.tvMetanoTotal.isVisible = isRuminant
        if (isRuminant) {
            binding.tvMetanoTotal.text = getString(R.string.formato_metano, dieta.metanoProducidoGramos)
        }

        // Composición de la dieta (kg/día, %, costo proporcional)
        val totalKg = dieta.composicion.values.sum().coerceAtLeast(0.0)
        val composicionItems = dieta.composicion.map { (nombreInsumo, kgPorDia) ->
            val porcentaje = if (totalKg > 0) (kgPorDia / totalKg) * 100.0 else 0.0
            val costoItem = if (totalKg > 0) dieta.costoTotal * (kgPorDia / totalKg) else 0.0

            ComposicionItem(
                nombre = nombreInsumo,
                cantidad = kgPorDia,
                porcentaje = porcentaje,
                costo = costoItem
            )
        }.sortedByDescending { it.porcentaje }

        val adapter = ComposicionAdapter()
        binding.rvComposicion.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = adapter
        }
        adapter.submitList(composicionItems)

        // Información nutricional (con sinónimos)
        mostrarInformacionNutricional(totalKg)

        // Requerimientos del animal (los campos que sí existen)
        mostrarRequerimientosAnimal()
    }

    private fun obtenerNutrientePorClaves(
        mapa: Map<String, Double>,
        claves: List<String>,
        default: Double = 0.0
    ): Double {
        for (k in claves) {
            mapa[k]?.let { return it }
        }
        return default
    }

    private fun mostrarInformacionNutricional(totalKg: Double) {
        // Materia seca: si no viene en el mapa, usamos la suma de kg/día de la composición
        val ms = obtenerNutrientePorClaves(
            dieta.nutrientesTotales,
            listOf("Materia Seca", "MS", "DM"),
            default = totalKg
        )
        binding.tvMateriaSecaTotal.text = getString(R.string.formato_kg, ms)

        val proteina = obtenerNutrientePorClaves(
            dieta.nutrientesTotales,
            listOf("Proteína Cruda", "PC", "CP")
        )
        binding.tvProteinaCruda.text = getString(R.string.formato_porcentaje, proteina)

        // Energía: ENL o NEm
        val energia = obtenerNutrientePorClaves(
            dieta.nutrientesTotales,
            listOf("ENL", "NEm", "Energía Neta Mantenimiento", "Energia Neta Mantenimiento")
        )
        binding.tvEnergia.text = getString(R.string.formato_energia, energia)

        // Fibra: FDN o NDF
        val fibra = obtenerNutrientePorClaves(
            dieta.nutrientesTotales,
            listOf("FDN", "NDF", "Fibra Detergente Neutro")
        )
        binding.tvFibra.text = getString(R.string.formato_porcentaje, fibra)
    }

    private fun mostrarRequerimientosAnimal() {
        // Peso
        binding.tvPesoAnimal.text = getString(R.string.formato_peso, dieta.animal.pesoKg)

        // Estos campos no existen en Animal: mostramos "No aplica"
        binding.tvProduccionLeche.text = getString(R.string.no_aplica)
        binding.tvGananciaPeso.text = getString(R.string.no_aplica)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
