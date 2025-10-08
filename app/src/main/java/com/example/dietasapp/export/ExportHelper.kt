package com.example.dietasapp.export

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*


/**
 * Helper para gestionar permisos y operaciones de exportación
 */
object ExportHelper {

    /**
     * Verifica si se tienen los permisos necesarios para exportar
     */
    fun hasExportPermissions(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ no necesita permisos para escribir en Downloads
            true
        } else {
            // Android 9 y anteriores necesitan permiso de escritura
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Obtiene los permisos necesarios según la versión de Android
     */
    fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            emptyArray()
        } else {
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }


    /**
     * Genera un nombre de archivo descriptivo para una dieta
     */
    fun generateDietFileName(animalNombre: String): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            .format(Date())

        val nombreLimpio = animalNombre
            .replace(" ", "_")
            .replace(Regex("[^a-zA-Z0-9_]"), "")
            .take(20)

        return "Dieta_${nombreLimpio}_$timestamp"
    }

    /**
     * Formatea un número con separador de miles
     */
    fun formatNumber(number: Double, decimals: Int = 2): String {
        return String.format(Locale.getDefault(), "%.${decimals}f", number)
    }

    /**
     * Formatea moneda
     */
    fun formatCurrency(amount: Double): String {
        return String.format(Locale.getDefault(), "$%.2f", amount)
    }
}