package com.example.dietasapp.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.dietasapp.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        view.findViewById<View>(R.id.btnTemaClaro)?.setOnClickListener {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        view.findViewById<View>(R.id.btnTemaOscuro)?.setOnClickListener {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }

        view.findViewById<View>(R.id.btnTemaSistema)?.setOnClickListener {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }

        view.findViewById<View>(R.id.btnAcercaDe)?.setOnClickListener {
            mostrarAcercaDe()
        }

        return view
    }

    private fun mostrarAcercaDe() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.sobre_app))
            .setMessage(getString(R.string.acerca_descripcion))
            .setPositiveButton(getString(R.string.aceptar), null)
            .show()
    }
}