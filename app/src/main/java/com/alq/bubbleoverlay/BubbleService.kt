package com.alq.bubbleoverlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat
import com.google.android.material.card.MaterialCardView
import android.view.ContextThemeWrapper
import java.io.File

data class Bubble(
    val view: View,
    val params: WindowManager.LayoutParams,
    var title: String,
    var isCircle: Boolean = true,
    var imageUri: Uri? = null
)

class BubbleService : Service() {

    private val bubbles = mutableListOf<Bubble>()
    private var activeBubble: Bubble? = null
    private var bubbleCounter = 1

    private var lastTouchTime = 0L
    private val doubleTapDelay = ViewConfiguration.getDoubleTapTimeout()
    private var isPotentialDoubleTap = false

    private lateinit var prefsHelper: PrefsHelper
    private lateinit var windowManager: WindowManager
    private lateinit var controlPanelView: View
    private lateinit var panelParams: WindowManager.LayoutParams
    private var isPanelVisible = false
    private lateinit var gestureDetector: GestureDetectorCompat

    companion object {
        var instance: BubbleService? = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefsHelper = PrefsHelper(this)
        setupNotification()
        initializeWindowManager()
        setupControlPanel()
        createNewBubble()
        setupGestureDetector()
    }

    private fun setupNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "bubble_channel",
                "Bubble Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Servicio de burbuja flotante" }

            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)

            startForeground(1, Notification.Builder(this, "bubble_channel")
                .setContentTitle("Burbuja Activa")
                .setSmallIcon(R.drawable.defaultimg)
                .build())
        }
    }

    private fun initializeWindowManager() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    private fun createNewBubble() {
        val bubble = setupBubble()
        bubbles.add(bubble)
        activeBubble = bubble
        updatePanelTitle()
    }

    private fun setupBubble(): Bubble {
        val themedContext = ContextThemeWrapper(this, R.style.Theme_BubbleOverlay)
        val bubbleView = LayoutInflater.from(themedContext).inflate(R.layout.bubble_layout, null)
        val params = configureBubbleParams()

        windowManager.addView(bubbleView, params)
        setupBubbleTouchListener(bubbleView, params)

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
        ).also { bubbleCounter++ }
    }

    private fun configureBubbleParams(): WindowManager.LayoutParams {
        val size = 60.dpToPx()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        return WindowManager.LayoutParams(
            size,
            size,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 100
        }
    }

    private fun setupControlPanel() {
        controlPanelView = LayoutInflater.from(this).inflate(R.layout.panel_control, null)
        configurePanelParams()
        setupPanelControls()
        windowManager.addView(controlPanelView, panelParams)
        controlPanelView.visibility = View.GONE
    }

    private fun configurePanelParams() {
        panelParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
            y = 0
        }
    }

    private fun setupGestureDetector() {
        gestureDetector = GestureDetectorCompat(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (!isPanelVisible) togglePanel()
                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                isPotentialDoubleTap = false
                return true
            }

            override fun onDown(e: MotionEvent): Boolean = true
        })
    }

    private fun getNavigationBarHeight(): Int {
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
    }

    private fun updatePanelTitle() {
        controlPanelView.findViewById<TextView>(R.id.tv_bubble_title).text = activeBubble?.title ?: ""
    }

    private fun setupBubbleTouchListener(bubbleView: View, params: WindowManager.LayoutParams) {
        bubbleView.setOnTouchListener { view, event ->
            gestureDetector.onTouchEvent(event)

            if (event.action == MotionEvent.ACTION_DOWN) {
                activeBubble = bubbles.find { it.view == view }
                updatePanelTitle()
            }

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastTouchTime = System.currentTimeMillis()
                    isPotentialDoubleTap = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (System.currentTimeMillis() - lastTouchTime > doubleTapDelay && !isPanelVisible) {
                        isPotentialDoubleTap = false

                        val displayMetrics = resources.displayMetrics
                        val screenWidth = displayMetrics.widthPixels
                        val screenHeight = displayMetrics.heightPixels
                        val margin = 50

                        val maxX = screenWidth - view.width - margin
                        val maxY = screenHeight - view.height - margin - getNavigationBarHeight()
                        val minX = margin
                        val minY = margin

                        val newX = (event.rawX - view.width / 2).coerceIn(minX.toFloat(), maxX.toFloat()).toInt()
                        val newY = (event.rawY - view.height / 2).coerceIn(minY.toFloat(), maxY.toFloat()).toInt()

                        params.x = newX
                        params.y = newY
                        windowManager.updateViewLayout(view, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    isPotentialDoubleTap = false
                    true
                }
                else -> false
            }
        }
    }

    private fun togglePanel() {
        if (isPanelVisible) hidePanel() else showPanel()
    }

    private fun showPanel() {
        controlPanelView.visibility = View.VISIBLE
        controlPanelView.animate().alpha(0.9f).duration = 200

        controlPanelView.post {
            activeBubble?.let { bubble ->
                val panelHeight = controlPanelView.findViewById<View>(R.id.panel_content).height
                bubble.params.y = panelHeight + 40.dpToPx()
                windowManager.updateViewLayout(bubble.view, bubble.params)
            }
        }

        isPanelVisible = true
        controlPanelView.findViewById<View>(R.id.overlay).setOnClickListener { hidePanel() }
    }

    private fun hidePanel() {
        controlPanelView.animate().alpha(0f).withEndAction {
            controlPanelView.visibility = View.GONE
        }.duration = 200

        activeBubble?.let { bubble ->
            windowManager.updateViewLayout(bubble.view, bubble.params)
        }

        isPanelVisible = false
    }

    private fun setupPanelControls() {
        controlPanelView.findViewById<Button>(R.id.btn_circle).setOnClickListener {
            changeBubbleShape(true)
        }

        controlPanelView.findViewById<Button>(R.id.btn_square).setOnClickListener {
            changeBubbleShape(false)
        }

        controlPanelView.findViewById<SeekBar>(R.id.size_slider).apply {
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    activeBubble?.let { bubble ->
                        val sizeInPx = progress.dpToPx()
                        bubble.params.width = sizeInPx
                        bubble.params.height = sizeInPx
                        windowManager.updateViewLayout(bubble.view, bubble.params)
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

        controlPanelView.findViewById<Button>(R.id.btn_change_image).setOnClickListener {
            hidePanel()
            openImagePicker()
        }

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

        controlPanelView.findViewById<Button>(R.id.btn_add_bubble).setOnClickListener {
            createNewBubble()
            hidePanel()
        }

        controlPanelView.findViewById<Button>(R.id.btn_close_app).setOnClickListener {
            closeApp()
        }
    }

    private fun openImagePicker() {
        val pickImageIntent = Intent(this, ImagePickerActivity::class.java)
        pickImageIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(pickImageIntent)
    }

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

    private fun changeBubbleShape(isCircle: Boolean) {
        activeBubble?.let { bubble ->
            val cardView = bubble.view.findViewById<MaterialCardView>(R.id.bubble_root)
            cardView.radius = if (isCircle) cardView.width / 2f else 0f
            bubble.isCircle = isCircle
            windowManager.updateViewLayout(bubble.view, bubble.params)
        }
    }

    private fun closeApp() {
        try {
            bubbles.forEach { windowManager.removeView(it.view) }
            bubbles.clear()
            windowManager.removeView(controlPanelView)
            stopSelf()
            val mainActivityIntent = Intent(this, MainActivity::class.java)
            mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(mainActivityIntent)
        } catch (e: Exception) {
            Log.e("BubbleService", "Error al cerrar: ${e.message}")
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        try {
            bubbles.forEach { windowManager.removeView(it.view) }
            windowManager.removeView(controlPanelView)
        } catch (e: Exception) {
            Log.e("BubbleService", "Error al limpiar recursos: ${e.message}")
        }
    }
}