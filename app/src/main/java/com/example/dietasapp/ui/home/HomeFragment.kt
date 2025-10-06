package com.example.dietasapp.ui.home

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
import com.example.dietasapp.calculation.optimizer.DietAPI
import com.example.dietasapp.data.Animal
import com.example.dietasapp.database.BaseDeDatosJSON
import com.example.dietasapp.databinding.FragmentHomeBinding
import com.example.dietasapp.domain.Dieta
import com.example.dietasapp.export.ExportHelper
import com.example.dietasapp.export.ExportadorExcel
import com.example.dietasapp.inventory.Inventario
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var inventario: Inventario
    private lateinit var dietAPI: DietAPI
    private lateinit var exportador: ExportadorExcel

    private var animales: List<Animal> = emptyList()
    private var animalSeleccionado: Animal? = null
    private var dietaActual: Dieta? = null

    private lateinit var composicionAdapter: ComposicionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar componentes
        val baseDatos = BaseDeDatosJSON(requireContext())
        inventario = Inventario(baseDatos)
        dietAPI = DietAPI()
        exportador = ExportadorExcel(requireContext())

        // Configurar RecyclerView
        composicionAdapter = ComposicionAdapter()
        binding.recyclerComposicion.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = composicionAdapter
        }

        // Configurar listeners
        setupListeners()

        // Cargar datos
        cargarAnimales()
    }

    private fun setupListeners() {
        // Listener del spinner de animales
        binding.spinnerAnimal.setOnItemClickListener { _, _, position, _ ->
            animalSeleccionado = animales[position]
            mostrarInfoAnimal(animalSeleccionado!!)
        }

        // Listener del botón calcular
        binding.btnCalcular.setOnClickListener {
            calcularDieta()
        }

        // Listener del botón exportar
        binding.btnExportar.setOnClickListener {
            exportarDieta()
        }

        // Listener del botón guardar
        binding.btnGuardar.setOnClickListener {
            guardarDieta()
        }
    }

    private fun cargarAnimales() {
        lifecycleScope.launch {
            try {
                animales = inventario.obtenerAnimales()

                if (animales.isEmpty()) {
                    mostrarEmptyState()
                } else {
                    ocultarEmptyState()
                    configurarSpinnerAnimales()
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

    private fun configurarSpinnerAnimales() {
        val nombresAnimales = animales.map { it.nombre }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            nombresAnimales
        )
        binding.spinnerAnimal.setAdapter(adapter)
    }

    private fun mostrarInfoAnimal(animal: Animal) {
        binding.layoutInfoAnimal.visibility = View.VISIBLE
        binding.tvPesoAnimal.text = "Peso: ${animal.pesoKg} kg"
        binding.tvDMIAnimal.text = "DMI: ${animal.consumoDMI} kg/día"
    }

    private fun calcularDieta() {
        if (animalSeleccionado == null) {
            Toast.makeText(
                requireContext(),
                getString(R.string.seleccionar_animal),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        lifecycleScope.launch {
            try {
                // Mostrar progreso
                binding.layoutCalculando.visibility = View.VISIBLE
                binding.layoutResultado.visibility = View.GONE
                binding.btnCalcular.isEnabled = false

                // Obtener insumos disponibles
                val insumos = inventario.obtenerInventarioDisponible()
                    .map { it.insumo }

                if (insumos.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.sin_insumos),
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                // Determinar modo de optimización
                val modo = if (binding.radioMinimizarCosto.isChecked) {
                    DietAPI.OptimizationMode.COST
                } else {
                    DietAPI.OptimizationMode.METHANE
                }

                // Calcular dieta en background
                val dieta = withContext(Dispatchers.Default) {
                    dietAPI.calculateOptimalDiet(
                        animal = animalSeleccionado!!,
                        ingredientes = insumos,
                        optimizationMode = modo
                    )
                }

                // Verificar si la dieta es válida
                if (dieta.composicion.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.error_calcular_dieta),
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                // Guardar y mostrar resultado
                dietaActual = dieta
                mostrarResultado(dieta)

            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "${getString(R.string.error_calcular_dieta)}: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                e.printStackTrace()
            } finally {
                binding.layoutCalculando.visibility = View.GONE
                binding.btnCalcular.isEnabled = true
            }
        }
    }

    private fun mostrarResultado(dieta: Dieta) {
        binding.layoutResultado.visibility = View.VISIBLE

        // Resumen económico
        binding.tvCostoTotal.text = String.format(
            getString(R.string.costo_total),
            "%.2f".format(dieta.costoTotal)
        )
        binding.tvCostoPorKg.text = String.format(
            getString(R.string.costo_por_kg),
            "%.4f".format(dieta.costoPorKgMS())
        )

        // Impacto ambiental
        binding.tvMetano.text = String.format(
            getString(R.string.metano_producido),
            "%.2f".format(dieta.metanoProducidoGramos)
        )
        binding.tvMetanoPorKg.text = String.format(
            getString(R.string.metano_por_kg_dmi),
            "%.2f".format(dieta.metanoPorKgDMI())
        )

        // Composición
        val totalKg = dieta.composicion.values.sum()
        val items = dieta.composicion.map { (nombre, kg) ->
            ComposicionItem(
                nombre = nombre,
                cantidad = kg,
                porcentaje = (kg / totalKg) * 100.0
            )
        }.sortedByDescending { it.porcentaje }

        composicionAdapter.submitList(items)

        // Scroll al resultado
        binding.root.post {
            binding.root.smoothScrollTo(0, binding.layoutResultado.top)
        }
    }

    private fun exportarDieta() {
        val dieta = dietaActual ?: return

        lifecycleScope.launch {
            try {
                val nombreArchivo = ExportHelper.generateDietFileName(dieta.animal.nombre)

                val result = exportador.exportar(
                    dieta = dieta,
                    nombreArchivo = nombreArchivo
                )

                when (result) {
                    is ExportadorExcel.ExportResult.Success -> {
                        Toast.makeText(
                            requireContext(),
                            String.format(getString(R.string.exportacion_exitosa), result.filePath),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    is ExportadorExcel.ExportResult.Error -> {
                        Toast.makeText(
                            requireContext(),
                            "${getString(R.string.error_exportar)}: ${result.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "${getString(R.string.error_exportar)}: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun guardarDieta() {
        val dieta = dietaActual ?: return

        lifecycleScope.launch {
            try {
                val baseDatos = BaseDeDatosJSON(requireContext())
                val exito = baseDatos.guardarDieta(dieta)

                if (exito) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.dieta_guardada),
                        Toast.LENGTH_SHORT
                    ).show()
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

    private fun mostrarEmptyState() {
        binding.layoutEmptyState.visibility = View.VISIBLE
        binding.cardAnimal.visibility = View.GONE
        binding.cardModo.visibility = View.GONE
        binding.btnCalcular.visibility = View.GONE
    }

    private fun ocultarEmptyState() {
        binding.layoutEmptyState.visibility = View.GONE
        binding.cardAnimal.visibility = View.VISIBLE
        binding.cardModo.visibility = View.VISIBLE
        binding.btnCalcular.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// Data class para items de composición
data class ComposicionItem(
    val nombre: String,
    val cantidad: Double,
    val porcentaje: Double
)