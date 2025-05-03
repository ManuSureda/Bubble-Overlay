package com.alq.bubbleoverlay

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream

class ImagePickerActivity : AppCompatActivity() {
    private val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    val file = File(filesDir, "bubble_image_${System.currentTimeMillis()}.jpg")
                    FileOutputStream(file).use { output ->
                        inputStream?.copyTo(output)
                    }
                    BubbleService.instance?.handleImageSelection(Uri.fromFile(file))
                } catch (e: Exception) {
                    Toast.makeText(this, "Error al guardar imagen", Toast.LENGTH_SHORT).show()
                }
            }
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png"))
        }
        startForResult.launch(intent)
    }
}