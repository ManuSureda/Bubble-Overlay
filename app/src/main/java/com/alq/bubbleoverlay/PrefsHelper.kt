/*package com.alq.bubbleoverlay

import android.content.Context
import android.net.Uri

/**
 * Sirve para guardar las preferencias del usuario.
 * burbujas / sus imagenes / cantidad...
 *
 * SharedPreferences:
 * Almacena datos en un archivo XML privado dentro de la carpeta de la app (/data/data/com.alq.bubbleoverlay/shared_prefs/BubblePrefs.xml).
 */
class PrefsHelper(context: Context) {
    private val sharedPref = context.getSharedPreferences("BubblePrefs", Context.MODE_PRIVATE)

    fun saveImagePath(path: String?) {
        with(sharedPref.edit()) {
            putString("bubble_image_path", path)
            apply()
        }
    }

    fun getImagePath(): String? {
        return sharedPref.getString("bubble_image_path", null)
    }

    fun saveImagePathForBubble(bubbleId: Int, path: String?) {
        sharedPref.edit().putString("bubble_image_$bubbleId", path).apply()
    }

    fun getImagePathForBubble(bubbleId: Int): String? {
        return sharedPref.getString("bubble_image_$bubbleId", null)
    }
}*/