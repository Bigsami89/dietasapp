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
import com.example.dietasapp.calculation.optimizer.DietAPI.OptimizationMode
import com.example.dietasapp.data.Animal
import com.example.dietasapp.data.Insumo
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

    private lateinit var ingredientesAdapter: IngredienteSeleccionableAdapter
    private lateinit var composicionAdapter: ComposicionAdapter

    private var animales: List<Animal> = emptyList()
    private var insumos: List<Insumo> = emptyList()
    private var animalSeleccionado: Animal? = null
    private var dietaActual: Dieta? = null

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

        // Configurar RecyclerViews
        setupRecyclerViews()

        // Configurar listeners
        setupListeners()

        // Cargar datos iniciales
        cargarDatos()
    }

    private fun setupRecyclerViews() {
        // Adapter de ingredientes seleccionables
        ingredientesAdapter = IngredienteSeleccionableAdapter { ingredientesSeleccionados ->
            actualizarContadorSeleccion(ingredientesSeleccionados.size)
        }

        binding.rvIngredientes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ingredientesAdapter
        }

        // Adapter de composición de dieta
        composicionAdapter = ComposicionAdapter()
        binding.rvComposicion.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = composicionAdapter
        }
    }

    private fun setupListeners() {
        // Botón seleccionar todos
        binding.btnSeleccionarTodos.setOnClickListener {
            if (ingredientesAdapter.getSelectedIngredientes().size == insumos.size) {
                ingredientesAdapter.deselectAll()
                binding.btnSeleccionarTodos.text = getString(R.string.seleccionar_todos)
            } else {
                ingredientesAdapter.selectAll()
                binding.btnSeleccionarTodos.text = getString(R.string.deseleccionar_todos)
            }
        }

        // Botón calcular
        binding.btnCalcular.setOnClickListener {
            calcularDieta()
        }

        // Botón exportar
        binding.btnExportar.setOnClickListener {
            exportarDieta()
        }

        // Botón guardar
        binding.btnGuardar.setOnClickListener {
            guardarDieta()
        }
    }

    private fun cargarDatos() {
        lifecycleScope.launch {
            try {
                // Cargar animales
                animales = withContext(Dispatchers.IO) {
                    inventario.obtenerAnimales()
                }

                // Cargar insumos
                insumos = withContext(Dispatchers.IO) {
                    inventario.obtenerInsumos()
                }

                // Actualizar UI
                if (animales.isEmpty()) {
                    mostrarEmptyState()
                } else {
                    configurarSpinnerAnimales()
                    mostrarIngredientes()
                }

            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Error al cargar datos: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun configurarSpinnerAnimales() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            animales.map { it.nombre }
        )

        binding.spinnerAnimal.setAdapter(adapter)
        binding.spinnerAnimal.setOnItemClickListener { _, _, position, _ ->
            animalSeleccionado = animales[position]
        }

        // Seleccionar el primero por defecto
        if (animales.isNotEmpty()) {
            binding.spinnerAnimal.setText(animales[0].nombre, false)
            animalSeleccionado = animales[0]
        }
    }

    private fun mostrarIngredientes() {
        if (insumos.isEmpty()) {
            binding.rvIngredientes.visibility = View.GONE
            binding.tvSinIngredientes.visibility = View.VISIBLE
            binding.btnSeleccionarTodos.isEnabled = false
        } else {
            binding.rvIngredientes.visibility = View.VISIBLE
            binding.tvSinIngredientes.visibility = View.GONE
            binding.btnSeleccionarTodos.isEnabled = true

            val items = insumos.map { IngredienteSeleccionable(it) }
            ingredientesAdapter.submitList(items)
        }
    }

    private fun mostrarEmptyState() {
        binding.layoutEmptyState.visibility = View.VISIBLE
        binding.cardAnimal.visibility = View.GONE
        binding.cardIngredientes.visibility = View.GONE
        binding.cardOptimizacion.visibility = View.GONE
        binding.btnCalcular.visibility = View.GONE
    }

    private fun actualizarContadorSeleccion(cantidad: Int) {
        binding.tvIngredientesSeleccionados.text = if (cantidad == 0) {
            getString(R.string.ingredientes_seleccionados_none)
        } else {
            getString(R.string.ingredientes_seleccionados_count, cantidad)
        }

        // Actualizar botón de seleccionar todos
        binding.btnSeleccionarTodos.text = if (cantidad == insumos.size) {
            getString(R.string.deseleccionar_todos)
        } else {
            getString(R.string.seleccionar_todos)
        }
    }

    private fun calcularDieta() {



        // Validaciones
        val animal = animalSeleccionado
        if (animal == null) {
            Toast.makeText(
                requireContext(),
                getString(R.string.error_seleccionar_animal),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val ingredientesSeleccionados = ingredientesAdapter.getSelectedIngredientes()
        if (ingredientesSeleccionados.isEmpty()) {
            Toast.makeText(
                requireContext(),
                getString(R.string.error_seleccionar_ingredientes),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Determinar modo de optimización
        val modo = if (binding.rbMinimizarCosto.isChecked) {
            OptimizationMode.COST
        } else {
            OptimizationMode.METHANE
        }

        // Calcular dieta
        lifecycleScope.launch {
            try {
                // Mostrar loading
                binding.layoutCalculando.visibility = View.VISIBLE
                binding.btnCalcular.isEnabled = false
                binding.layoutResultado.visibility = View.GONE

                // Calcular en background
                val dieta = withContext(Dispatchers.IO) {
                    dietAPI.calculateOptimalDiet(
                        animal = animal,
                        ingredientes = ingredientesSeleccionados,
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
        binding.tvCostoTotal.text = getString(
            R.string.costo_total,
            String.format("%.2f", dieta.costoTotal)
        )
        binding.tvCostoPorKg.text = getString(
            R.string.costo_por_kg,
            String.format("%.4f", dieta.costoPorKgMS())
        )

        // Impacto ambiental
        binding.tvMetano.text = getString(
            R.string.metano_producido,
            String.format("%.2f", dieta.metanoProducidoGramos)
        )
        binding.tvMetanoPorKg.text = getString(
            R.string.metano_por_kg_dmi,
            String.format("%.2f", dieta.metanoPorKgDMI())
        )

        // Composición
        val totalKg = dieta.composicion.values.sum()
        val items = dieta.composicion.map { (nombre, kg) ->
            ComposicionItem(
                nombre = nombre,
                cantidad = kg,
                porcentaje = (kg / totalKg) * 100.0,
                costo = obtenerCostoIngrediente(nombre)
            )
        }.sortedByDescending { it.porcentaje }

        composicionAdapter.submitList(items)

        // Scroll al resultado
        binding.root.post {
            binding.root.smoothScrollTo(0, binding.layoutResultado.top)
        }
    }

    private fun obtenerCostoIngrediente(nombre: String): Double {
        return insumos.find { it.nombre == nombre }?.costo ?: 0.0
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
                            getString(R.string.exportacion_exitosa, result.filePath),
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
                val exito = withContext(Dispatchers.IO) {
                    inventario.guardarDieta(dieta)
                }

                if (exito) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.dieta_guardada_exitosamente),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.error_guardar_dieta),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "${getString(R.string.error_guardar_dieta)}: ${e.message}",
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