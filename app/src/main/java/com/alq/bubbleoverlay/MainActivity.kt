package com.alq.bubbleoverlay

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

// ":" es como decir 'extends' en java
// AppCompatActivity : Proporciona compatibilidad con características modernas de Android en versiones antiguas.
//                      Es la clase base recomendada para todas las actividades en Android moderno.
// El () al final de AppCompatActivity es crucial - indica que estás usando el constructor por defecto de la clase.
// En Kotlin, cuando heredas de una clase, debes inicializar la clase padre.
class MainActivity : AppCompatActivity() {
    // Objeto compañero para constantes
    /* companion object es un bloque especial que permite definir miembros (propiedades o funciones)
     que pertenecen a la clase en sí misma, no a instancias individuales. Es equivalente a los
     métodos estáticos en Java, pero con más flexibilidad.
     en java: private static final int OVERLAY_PERMISSION_REQUEST_CODE = 1001;
    companion object {
        // Código único para identificar la solicitud de permiso (mantenido para referencia)
        private const val OVERLAY_PERMISSION_REQUEST_CODE = 1001
    }
    ya no lo uso*/

    // Registramos el lanzador para el resultado del permiso usando la nueva API
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { handlePermissionResult() }

    //primer método que se ejecuta cuando Android crea tu Activity (pantalla)
    // savedInstanceState: Bundle?
    //  "paquete" que contiene datos guardados cuando la Activity fue destruida y recreada
    //  ? -> Indica que puede ser null (la primera vez que se abre la Activity).
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // intent = objeto de mensajería para comunicar componentes, solicitar acciones o pasar datos.
        // Verificar si se recibió una solicitud de cierre de la aplicación
        if (intent.getBooleanExtra("EXIT", false)) {
            // Finalizar completamente la actividad y la tarea
            finishAndRemoveTask()
            return
        }

        // Iniciar proceso de verificación de permisos
        checkOverlayPermission()
    }

    // Función para verificar permiso de dibujar sobre otras apps
    private fun checkOverlayPermission() {
        // Solo necesario para Android 6.0 (Marshmallow) y superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            // Intent para abrir configuración de permisos especiales
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")  // URI del paquete actual
            )
            // Lanzar actividad usando el nuevo sistema de resultados
            overlayPermissionLauncher.launch(intent)
        } else {
            // Si ya tiene permiso, iniciar servicio directamente
            startBubbleService()
        }
    }

    // Manejar el resultado del permiso
    private fun handlePermissionResult() {
        // Volver a verificar si ahora tiene el permiso
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
            startBubbleService()
        } else {
            // Mostrar advertencia si no se otorgó permiso
            Toast.makeText(
                this,
                "Falta permiso: Mostrar sobre otras apps",
                Toast.LENGTH_LONG
            ).show()
            checkOverlayPermission()
        }
    }

    // Función para iniciar el servicio de burbujas
    private fun startBubbleService() {
        // Evitar iniciar múltiples instancias del servicio
        if (!isMyServiceRunning(BubbleService::class.java)) {
            val intent = Intent(this, BubbleService::class.java)

            // Iniciar como Foreground Service en Android 8.0+ (Oreo)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                // Método legacy para versiones anteriores
                startService(intent)
            }
        }
        // Cerrar la actividad después de iniciar el servicio
        finish()
    }

    // Verificar si el servicio ya está en ejecución
    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        // Obtener administrador de actividades
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        // Obtener lista de servicios en ejecución y buscar coincidencia
        return manager.getRunningServices(Integer.MAX_VALUE)
            .any { it.service.className == serviceClass.name }
    }
}