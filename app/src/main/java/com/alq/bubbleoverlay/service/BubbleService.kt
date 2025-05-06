package com.alq.bubbleoverlay.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.alq.bubbleoverlay.data.Bubble

/**
 * BubbleService es el orquestador: crea, almacena y destruye burbujas.
 */
class BubbleService : Service() {
    // val = variable de solo lectura (inmutable)
    // lista de burbujas activas
    private val bubblesList = mutableListOf<Bubble>()
    // burbuja seleccionada actualmente
    private val activeBubble: Bubble? = null
    // Contador para numerar burbujas
    private var bubbleCounter = 1

    /**
     * El método onBind() forma parte de la API de Android para componentes de tipo Service, y se invoca cuando un cliente (otra Activity, Service u otro componente) quiere “vincularse” (bind) a tu servicio para llamar a sus métodos directamente.
     *
     * ¿Qué hace onBind()?
     * Firma
     * override fun onBind(intent: Intent): IBinder?
     *
     * Propósito
     * Devuelve un objeto de tipo IBinder que actúa como “puente” (binder) entre el cliente y el servicio. A través de ese binder el cliente puede invocar métodos públicos del servicio y obtener resultados síncronos.
     *
     * Cuándo se llama
     * Cuando alguien hace:
     * bindService(
     *   Intent(context, MiServicio::class.java),
     *   connection,       // ServiceConnection
     *   Context.BIND_AUTO_CREATE
     * )
     * Qué devuelve
     *
     * Un objeto IBinder (normalmente tu propia implementación que expone la interfaz del servicio).
     * O null si no permites binding (servicio sólo “started”, no “bound”).
     */
    // Método obligatorio de Service (no usado aquí)
    override fun onBind(intent: Intent?): IBinder? = null

    // se ejecuta al crear el servicio
    override fun onCreate() {
        super.onCreate()


    }
}