package com.alq.bubbleoverlay.service

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts

class ImageResultHandlerActivity : AppCompatActivity() {
    private val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.data
            uri?.let {
                // Enviar URI de vuelta al servicio
                val serviceIntent = Intent(this, BubbleService::class.java).apply {
                    putExtra("selected_image_uri", uri)
                }
                startService(serviceIntent)
            }
        }
        finish() // Cerrar la actividad inmediatamente
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        startForResult.launch(intent)
    }
}