package com.alq.bubbleoverlay.dao

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey

// "data" -> indica que es una data class o clase de datos. Está pensada para contener principalmente datos y no mucha lógica.
// Kotlin genera automáticamente varias funciones: equals(), hashCode(), toString(), copy(), componentN()

@Entity(tableName = "bubbles")
data class Bubble(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,

    var title: String,
    var shape: BubbleShape = BubbleShape.CIRCLE,
    var imageUri: Uri? = null,    // URI de la imagen asignada | Room no trabaja con Uris, necesita un String, por eso los TypeConverters
    var isVisible: Boolean = true
)