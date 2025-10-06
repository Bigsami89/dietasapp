package com.example.dietasapp.export

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

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
     * Genera un nombre de archivo único basado en fecha y hora
     */
    fun generateFileName(prefix: String = "Dieta"): String {
        val timestamp = System.currentTimeMillis()
        val dateFormat = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
        return "${prefix}_${dateFormat.format(java.util.Date(timestamp))}"
    }

    /**
     * Genera un nombre de archivo descriptivo para una dieta
     */
    fun generateDietFileName(animalName: String): String {
        val sanitizedName = animalName.replace(Regex("[^a-zA-Z0-9]"), "_")
        val timestamp = System.currentTimeMillis()
        val dateFormat = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
        return "Dieta_${sanitizedName}_${dateFormat.format(java.util.Date(timestamp))}"
    }
}