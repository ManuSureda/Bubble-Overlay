package com.alq.bubbleoverlay.data

import android.net.Uri
import android.view.View
import android.view.WindowManager

// "data" -> indica que es una data class o clase de datos. Está pensada para contener principalmente datos y no mucha lógica.
// Kotlin genera automáticamente varias funciones: equals(), hashCode(), toString(), copy(), componentN()
data class Bubble(
    val view: View,                         // Vista de la burbuja
    val params: WindowManager.LayoutParams, // Parámetros de posición/tamaño
    var title: String,                      // Título identificador
    var isCircle: Boolean = true,           // Si tiene forma circular
    var imageUri: Uri? = null               // URI de la imagen asignada
)