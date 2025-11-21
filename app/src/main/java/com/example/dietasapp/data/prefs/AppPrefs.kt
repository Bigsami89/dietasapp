package com.example.dietasapp.data.prefs

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

object AppPrefs {
    const val APP_PREFS = "DietAppPreferences"
    const val KEY_TIPO_ANIMAL = "tipo_animal_seleccionado"
    const val TIPO_MULTI = "multigastrico"
    const val TIPO_MONO = "monogastrico"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)

    fun getTipoAnimal(context: Context): String? =
        prefs(context).getString(KEY_TIPO_ANIMAL, null)

    fun setTipoAnimal(context: Context, tipo: String) {
        prefs(context).edit().putString(KEY_TIPO_ANIMAL, tipo).apply()
    }

    fun clearTipoAnimal(context: Context) {
        prefs(context).edit().remove(KEY_TIPO_ANIMAL).apply()
    }

    /** Flow para observar cambios de especie (opcional). */
    fun observeTipoAnimal(context: Context): Flow<String?> = callbackFlow {
        val p = prefs(context)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_TIPO_ANIMAL) trySend(getTipoAnimal(context))
        }
        trySend(getTipoAnimal(context))
        p.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { p.unregisterOnSharedPreferenceChangeListener(listener) }
    }
}
