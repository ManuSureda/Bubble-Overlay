package com.alq.bubbleoverlay.ui

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.alq.bubbleoverlay.data.Bubble

class BubbleTouchListener(
    private val context: Context,
    private val bubble: Bubble,
    private val windowManager: WindowManager,
    private val onBubbleSelected: (Bubble) -> Unit,
    private val togglePanel: () -> Unit
) : View.OnTouchListener {
    /**
     * Unit = void en java
     *
     * Context es una clase clave que representa el “entorno” de tu aplicación o componente. Provee acceso a:
     *      Recursos (strings, layouts, dimensiones…): context.getResources(), context.getString(R.string.xxx), etc.
     *      Servicios del sistema: obtener el WindowManager, LayoutInflater, NotificationManager, ClipboardManager, … vía context.getSystemService(...).
     *      Inflar layouts, lanzar Activities/Services/Broadcasts, acceder a SharedPreferences, archivos internos, bases de datos, permisos, etc.
     *
     * WindowManager es el servicio de Android que gestiona “ventanas” a nivel de sistema. Te permite:
     *      Añadir vistas flotantes encima de otras apps (con permisos de overlay).
     *      Actualizar posición/tamaño de esas vistas.
     *      Eliminar vistas cuando ya no las necesitas.
     */
    /**
     * aca vamos a escuchar los eventos que ocurren en una determinada burbuja.
     * definimos en el constructor los metodos de "respuesta", pero solo definimos la firma: como lambdas onBubbleSelected: (Bubble) -> Unit
     * y posteriormente definimos ante que evento disparar dicho metodo de respuesta: onDoubleTap -> togglePanel
     * en el servicio (BubbleService) el cual tiene mayor alcance y contexto de la app, definimos el verdadero comportamiento
     * de dicha funcion:
     * (en BubbleService)
     * val touchListener = BubbleTouchListener(
     *   context          = this,
     *   bubble           = bubble,
     *   windowManager    = windowManager,
     *   onBubbleSelected = { b ->
     *     // ← aquí defines qué hace el servicio
     *     activeBubble = b
     *     controlPanelManager.updateTitle(b.title)
     *   },
     *   togglePanel      = {
     *     // ← y aquí defines qué ocurre al doble‑tap
     *     controlPanelManager.toggle()
     *   }
     * )
     *
     * gestureListener es donde se define la lógica de “qué hago cuando hay doble‑tap”.
     *
     * gestureDetector es el motor que traduce eventos crudos en gestos y despacha esas llamadas al listener.
     */

    private val dm = context.resources.displayMetrics
    private val margin = 20


    // GestureDetector es una clase que interpreta la secuencia de eventos táctiles (MotionEvent) que recibe de la View
    // pasas esa secuencia usando: gestureDetector.onTouchEvent(event)
    // Internamente agrupa movimientos en gestos (fling, scroll, doble‐tap, long‐press…) y, cuando reconoce uno, invoca el método correspondiente de tu gestureListener.
    // GestureDetector traduce raw events en llamadas a tu listener.
    // Tú solo sobrescribes los métodos que necesitas y devuelves true para indicar que consumiste el gesto.
    // Los callbacks (togglePanel, onBubbleSelected) viven en el Service y definen la acción concreta.
    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent) = true

        override fun onScroll(
            e1: MotionEvent?, // evento DOWN inicial
            e2: MotionEvent,  // evento MOVE actual
            distanceX: Float, // desplazamiento en X desde el último callback
            distanceY: Float  // desplazamiento en Y desde el último callback
        ): Boolean {
            // distanceX es cuánto se movió el dedo en X desde el último evento

            // Recalculo cada vez los límites según el tamaño actual de la vista
            val maxX = dm.widthPixels  - bubble.view.width  - margin
            val maxY = dm.heightPixels - bubble.view.height - margin

            val newX = (e2.rawX - bubble.view.width  / 2)
                .coerceIn(margin.toFloat(), maxX.toFloat()).toInt()
            val newY = (e2.rawY - bubble.view.height / 2)
                .coerceIn(margin.toFloat(), maxY.toFloat()).toInt()

            bubble.params.x = newX
            bubble.params.y = newY
            windowManager.updateViewLayout(bubble.view, bubble.params)

            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            togglePanel()
            return true
        }
    }

    // Usamos el detector nativo en vez de la versión obsoleta
    private val gestureDetector = GestureDetector(context, gestureListener).apply {
        setOnDoubleTapListener(gestureListener)
    }

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        // 1) Le pasas cada evento táctil al GestureDetector…
        // onTouchEvent envía cada MotionEvent al detector, que internamente decide cuándo llamar a tu gestureListener.onScroll u onDoubleTap.
        gestureDetector.onTouchEvent(event)

        onBubbleSelected(bubble)
        /*
        * podrias hacer if (event.action == MotionEvent.ACTION_DOWN) { onBubbleSelected(bubble) }
        * pero siempre (tanto double tap como on scroll empiezan con un ACTION_DOWN supongo
        * */

        // si lo borro sale un warning xD
        // BubbleTouchListener#onTouch should call View#performClick when a click is detected
        when (event.action) {
            MotionEvent.ACTION_UP -> view.performClick()
        }
        return true
    }
}

