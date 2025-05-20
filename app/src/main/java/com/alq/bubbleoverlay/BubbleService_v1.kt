
package com.alq.bubbleoverlay
/*
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.net.Uri
import android.os.IBinder
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.GestureDetectorCompat
import com.google.android.material.card.MaterialCardView
import android.view.ContextThemeWrapper
import androidx.localbroadcastmanager.content.LocalBroadcastManager

// Data class para representar una burbuja flotante
data class Bubble(
    val view: View,          // Vista de la burbuja
    val params: WindowManager.LayoutParams, // Parámetros de posición/tamaño
    var title: String,       // Título identificador
    var isCircle: Boolean = true, // Si tiene forma circular
    var imageUri: Uri? = null // URI de la imagen asignada
)

// Servicio principal que gestiona las burbujas
class BubbleService_v1 : Service() {
    // Lista de burbujas activas
    private val bubbles = mutableListOf<Bubble>()
    // Burbuja seleccionada actualmente
    private var activeBubble: Bubble? = null
    // Contador para numerar burbujas
    private var bubbleCounter = 1

    // Variables para control de toques
    private var lastTouchTime = 0L
    private val doubleTapDelay = ViewConfiguration.getDoubleTapTimeout()
    private var isPotentialDoubleTap = false

    // Componentes de la UI y gestión
    private lateinit var prefsHelper: PrefsHelper // Almacenamiento local
    private lateinit var windowManager: WindowManager // Manager de ventanas
    private lateinit var controlPanelView: View // Panel de control flotante
    private lateinit var panelParams: WindowManager.LayoutParams // Parámetros del panel
    private var isPanelVisible = false // Estado de visibilidad del panel
    private lateinit var gestureDetector: GestureDetectorCompat // Detección de gestos

    // Para comunicación interna entre componentes
    private lateinit var localBroadcastManager: LocalBroadcastManager

    // Método obligatorio de Service (no usado aquí)
    override fun onBind(intent: Intent?): IBinder? = null

    // Se ejecuta al crear el servicio
    override fun onCreate() {
        super.onCreate()

        // Inicializa el sistema de broadcast local
        localBroadcastManager = LocalBroadcastManager.getInstance(this)
        setupImageReceiver()  // Configura receptor de imágenes

        prefsHelper = PrefsHelper(this) // Inicializa preferencias
        setupNotification()   // Crea notificación permanente
        initializeWindowManager() // Obtiene WindowManager
        setupControlPanel()   // Prepara panel de control
        createNewBubble()     // Crea primera burbuja
        setupGestureDetector() // Configura gestos
    }

    // Configura el receptor de imágenes seleccionadas
    private fun setupImageReceiver() {
        val filter = IntentFilter("IMAGE_SELECTED_ACTION")
        localBroadcastManager.registerReceiver(imageReceiver, filter)
    }

    // Receptor para manejar imágenes seleccionadas
    private val imageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.getParcelableExtra<Uri>("image_uri")?.let { uri ->
                handleImageSelection(uri) // Actualiza imagen en burbuja
            }
        }
    }

    // Configura la notificación permanente (requerido para servicios en primer plano)
    private fun setupNotification() {
        // Canal de notificación (necesario desde Android 8+)
        val channel = NotificationChannel(
            "bubble_channel",
            "Burbujas Activas",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Controla la visualización de burbujas flotantes"
        }

        // Registra el canal en el sistema
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).apply {
            createNotificationChannel(channel)
        }

        // Construye la notificación
        val notification = Notification.Builder(this, "bubble_channel")
            .setContentTitle("Burbujas Activas")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()

        // Inicia el servicio en primer plano
        startForeground(1, notification)
    }

    // Obtiene el servicio de ventanas del sistema
    private fun initializeWindowManager() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    // Crea una nueva burbuja y la añade a la lista
    private fun createNewBubble() {
        val bubble = setupBubble()
        bubbles.add(bubble)
        activeBubble = bubble
        updatePanelTitle() // Actualiza título en panel
    }

    // Configura una burbuja individual
    private fun setupBubble(): Bubble {
        // Contexto con tema personalizado
        val themedContext = ContextThemeWrapper(this, R.style.Theme_BubbleOverlay)
        // Infla el layout de la burbuja
        val bubbleView = LayoutInflater.from(themedContext).inflate(R.layout.bubble_layout, null, false)
        val params = configureBubbleParams() // Configura parámetros iniciales

        // Añade la vista al WindowManager
        windowManager.addView(bubbleView, params)
        setupBubbleTouchListener(bubbleView, params) // Configura eventos táctiles

        // Carga imagen guardada (si existe)
        prefsHelper.getImagePath()?.let { uriString ->
            try {
                val uri = Uri.parse(uriString)
                bubbleView.findViewById<ImageView>(R.id.bubble_icon).setImageURI(uri)
            } catch (e: Exception) {
                Log.e("BubbleService", "Error al cargar imagen", e)
            }
        }

        return Bubble(
            view = bubbleView,
            params = params,
            title = "Burbuja $bubbleCounter"
        ).also { bubbleCounter++ } // Incrementa contador después de crear
    }

    // Configura los parámetros iniciales de la ventana de la burbuja
    private fun configureBubbleParams(): WindowManager.LayoutParams {
        val size = 60.dpToPx() // Convierte dp a píxeles

        return WindowManager.LayoutParams(
            size, // Ancho
            size, // Alto
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, // Tipo de ventana (flotante)
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or // No captura eventos táctiles
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, // Permite posición fuera de límites
            PixelFormat.TRANSLUCENT // Formato de píxel transparente
        ).apply {
            gravity = Gravity.TOP or Gravity.START // Posición inicial
            x = 0
            y = 100
        }
    }

    // Configura el panel de control flotante
    private fun setupControlPanel() {
        controlPanelView = LayoutInflater.from(this).inflate(R.layout.panel_control, null, false)
        configurePanelParams() // Establece parámetros de ventana
        setupPanelControls()  // Configura botones y controles
        windowManager.addView(controlPanelView, panelParams) // Añade al WindowManager
        controlPanelView.visibility = View.GONE // Inicialmente oculto
    }

    // Parámetros para el panel de control
    private fun configurePanelParams() {
        panelParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, // Ancho completo
            WindowManager.LayoutParams.WRAP_CONTENT, // Alto según contenido
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, // Tipo flotante
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, // No captura eventos
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP // Posición en parte superior
            y = 0
        }
    }

    // Configura el detector de gestos (doble toque)
    private fun setupGestureDetector() {
        gestureDetector = GestureDetectorCompat(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (!isPanelVisible) togglePanel() // Muestra/oculta panel
                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                isPotentialDoubleTap = false
                return true
            }

            override fun onDown(e: MotionEvent): Boolean = true
        })
    }

    // Actualiza el título en el panel con la burbuja activa
    private fun updatePanelTitle() {
        controlPanelView.findViewById<TextView>(R.id.tv_bubble_title).text = activeBubble?.title ?: ""
    }

    // Configura los eventos táctiles de una burbuja
    private fun setupBubbleTouchListener(bubbleView: View, params: WindowManager.LayoutParams) {
        bubbleView.setOnTouchListener { view, event ->
            gestureDetector.onTouchEvent(event) // Delega al detector de gestos

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastTouchTime = System.currentTimeMillis()
                    isPotentialDoubleTap = true
                    activeBubble = bubbles.find { it.view == view } // Establece burbuja activa
                    updatePanelTitle()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (System.currentTimeMillis() - lastTouchTime > doubleTapDelay && !isPanelVisible) {
                        isPotentialDoubleTap = false
                        // Calcula nueva posición
                        val displayMetrics = resources.displayMetrics
                        val screenWidth = displayMetrics.widthPixels
                        val screenHeight = displayMetrics.heightPixels
                        val margin = 20

                        val maxX = screenWidth - view.width - margin
//                        val maxY = screenHeight - view.height - margin - getNavigationBarHeight()
                        val maxY = screenHeight - view.height - margin

                        val newX = (event.rawX - view.width / 2).coerceIn(margin.toFloat(), maxX.toFloat()).toInt()
                        val newY = (event.rawY - view.height / 2).coerceIn(margin.toFloat(), maxY.toFloat()).toInt()

                        // Actualiza posición
                        params.x = newX
                        params.y = newY
                        windowManager.updateViewLayout(view, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    view.performClick()
                    isPotentialDoubleTap = false
                    true
                }
                else -> false
            }
        }
    }

    // Muestra u oculta el panel de control
    private fun togglePanel() {
        if (isPanelVisible) hidePanel() else showPanel()
    }

    // Muestra el panel con animación
    private fun showPanel() {
        controlPanelView.visibility = View.VISIBLE
        controlPanelView.animate().alpha(0.9f).duration = 200

        controlPanelView.post {
            activeBubble?.let { bubble ->
                val panelHeight = controlPanelView.findViewById<View>(R.id.panel_content).height
                val displayMetrics = resources.displayMetrics

                // Centrado horizontal
                val screenCenterX = displayMetrics.widthPixels / 2
                val bubbleHalfWidth = bubble.view.width / 2
                bubble.params.x = screenCenterX - bubbleHalfWidth

                // Posición vertical debajo del panel
                bubble.params.y = panelHeight + 40.dpToPx()

                windowManager.updateViewLayout(bubble.view, bubble.params)
            }
        }

        isPanelVisible = true
        controlPanelView.findViewById<View>(R.id.overlay).setOnClickListener { hidePanel() }
    }

    // Oculta el panel con animación
    private fun hidePanel() {
        controlPanelView.animate().alpha(0f).withEndAction {
            controlPanelView.visibility = View.GONE
        }.duration = 200

        activeBubble?.let { bubble ->
            windowManager.updateViewLayout(bubble.view, bubble.params)
        }

        isPanelVisible = false
    }

    // Configura los controles del panel
    private fun setupPanelControls() {
        // Botón forma circular
        controlPanelView.findViewById<Button>(R.id.btn_circle).setOnClickListener {
            changeBubbleShape(true)
        }

        // Botón forma cuadrada
        controlPanelView.findViewById<Button>(R.id.btn_square).setOnClickListener {
            changeBubbleShape(false)
        }

        // SeekBar para tamaño
        controlPanelView.findViewById<SeekBar>(R.id.size_slider).apply {
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    activeBubble?.let { bubble ->
                        val sizeInPx = progress.dpToPx()
                        bubble.params.width = sizeInPx
                        bubble.params.height = sizeInPx
                        windowManager.updateViewLayout(bubble.view, bubble.params)
                        // Actualiza tamaño de la imagen
                        bubble.view.findViewById<ImageView>(R.id.bubble_icon).apply {
                            layoutParams.width = sizeInPx
                            layoutParams.height = sizeInPx
                            requestLayout()
                        }
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }

        // Botón cambiar imagen
        controlPanelView.findViewById<Button>(R.id.btn_change_image).setOnClickListener {
            hidePanel()
            openImagePicker()
        }

        // Botón eliminar burbuja
        controlPanelView.findViewById<Button>(R.id.btn_remove_bubble).setOnClickListener {
            activeBubble?.let { bubble ->
                windowManager.removeView(bubble.view)
                bubbles.remove(bubble)
                if (bubbles.isEmpty()) closeApp() else {
                    activeBubble = bubbles.first()
                    updatePanelTitle()
                    hidePanel()
                }
            }
        }

        // Botón nueva burbuja
        controlPanelView.findViewById<Button>(R.id.btn_add_bubble).setOnClickListener {
            createNewBubble()
            hidePanel()
        }

        // Botón cerrar app
        controlPanelView.findViewById<Button>(R.id.btn_close_app).setOnClickListener {
            closeApp()
        }
    }

    // Abre la actividad para seleccionar imagen
    private fun openImagePicker() {
        val pickImageIntent = Intent(this, ImagePickerActivity::class.java)
        pickImageIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(pickImageIntent)
    }

    // Maneja la imagen seleccionada
    fun handleImageSelection(uri: Uri) {
        activeBubble?.let { bubble ->
            try {
                val imageView = bubble.view.findViewById<ImageView>(R.id.bubble_icon)
                imageView.setImageURI(uri)
                if (uri.scheme == "file") {
                    prefsHelper.saveImagePath(uri.path)
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error al cargar imagen", Toast.LENGTH_SHORT).show()
                prefsHelper.saveImagePath(null)
            }
        }
    }

    // Cambia la forma de la burbuja (círculo/cuadrado)
    private fun changeBubbleShape(isCircle: Boolean) {
        activeBubble?.let { bubble ->
            val cardView = bubble.view.findViewById<MaterialCardView>(R.id.bubble_root)
            cardView.radius = if (isCircle) cardView.width / 2f else 0f
            bubble.isCircle = isCircle
            windowManager.updateViewLayout(bubble.view, bubble.params)
        }
    }

    // Cierra la aplicación completamente
    private fun closeApp() {
        try {
            stopSelf() // Detiene el servicio
            bubbles.forEach { windowManager.removeView(it.view) } // Elimina todas las vistas
            bubbles.clear()
            windowManager.removeView(controlPanelView)
            android.os.Process.killProcess(android.os.Process.myPid()) // Finaliza proceso
        } catch (e: Exception) {
            Log.e("BubbleService", "Error al cerrar: ${e.message}")
        }
    }

    // Extensión para convertir dp a píxeles
    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    // Limpieza al destruir el servicio
    override fun onDestroy() {
        super.onDestroy()
        try {
            localBroadcastManager.unregisterReceiver(imageReceiver)
            bubbles.forEach { windowManager.removeView(it.view) }
            windowManager.removeView(controlPanelView)
        } catch (e: Exception) {
            Log.e("BubbleService", "Error al limpiar recursos: ${e.message}")
        }
    }
}
*/