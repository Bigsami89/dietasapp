package com.example.dietasapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.dietasapp.MainActivity
import com.example.dietasapp.data.prefs.AppPrefs
import com.example.dietasapp.databinding.ActivitySelectorEspecieBinding

class SelectorEspecieActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySelectorEspecieBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySelectorEspecieBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Si ya hay selección, salta directo a Main
        AppPrefs.getTipoAnimal(this)?.let {
            goHome()
            return
        }

        binding.cardMulti.setOnClickListener {
            AppPrefs.setTipoAnimal(this, AppPrefs.TIPO_MULTI)
            goHome()
        }

        binding.cardMono.setOnClickListener {
            AppPrefs.setTipoAnimal(this, AppPrefs.TIPO_MONO)
            goHome()
        }

        binding.cardAves.setOnClickListener {
            AppPrefs.setTipoAnimal(this, AppPrefs.TIPO_AVES)
            goHome()
        }
    }

    private fun goHome() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
