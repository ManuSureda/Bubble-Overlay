package com.alq.bubbleoverlay.dao

import android.net.Uri
import android.view.View
import android.view.WindowManager
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

// "data" -> indica que es una data class o clase de datos. Está pensada para contener principalmente datos y no mucha lógica.
// Kotlin genera automáticamente varias funciones: equals(), hashCode(), toString(), copy(), componentN()

@Entity(tableName = "bubbles")
data class Bubble(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    var title: String,            // Título identificador
    var isCircle: Boolean = true, // Si tiene forma circular
    var imageUri: Uri? = null,    // URI de la imagen asignada | Room no trabaja con Uris, necesita un String, por eso los TypeConverters
    var isVisible: Boolean = true
) /*{
    @Ignore
    var view: View? = null                         // Vista de la burbuja
    @Ignore
    var params: WindowManager.LayoutParams? = null // Parámetros de posición/tamaño
}*/
