package com.example.dietasapp

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.dietasapp.databinding.ActivityMainBinding
import com.example.dietasapp.SelectorEspecieActivity
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configurar tema según preferencias del sistema
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        // Ocultar FAB por defecto
        binding.appBarMain.fab.hide()

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        // Configurar los destinos de nivel superior
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home,
                R.id.nav_inventory,
                R.id.nav_history,
                R.id.nav_settings
            ),
            drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        val tipo = com.example.dietasapp.data.prefs.AppPrefs.getTipoAnimal(this)
        if (tipo == null) {
            startActivity(Intent(this, SelectorEspecieActivity::class.java))
            finish()
            return
        }

        // Cambiar el título del toolbar según el destino
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val base = destination.label?.toString() ?: getString(R.string.app_name)
            val especie = when (com.example.dietasapp.data.prefs.AppPrefs.getTipoAnimal(this)) {
                com.example.dietasapp.data.prefs.AppPrefs.TIPO_MONO -> "Monogástrico"
                else -> "Multigástrico"
            }
            supportActionBar?.title = "$base — $especie"
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}