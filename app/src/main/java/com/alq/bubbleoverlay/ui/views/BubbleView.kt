package com.alq.bubbleoverlay.ui.views

import android.content.Context
import android.graphics.PixelFormat
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Toast
import com.alq.bubbleoverlay.R
import com.alq.bubbleoverlay.dao.Bubble
import com.alq.bubbleoverlay.dao.BubbleShape
import com.alq.bubbleoverlay.ui.listeners.BubbleTouchListener
import com.alq.bubbleoverlay.utils.dpToPx
import com.google.android.material.card.MaterialCardView

class BubbleView (
    private val context: Context,
    private val windowManager: WindowManager,
    private val bubble: Bubble,
    private val onBubbleSelected: (bubbleId: Long) -> Unit,
    private val onBubbleDoubleTap: (bubbleId: Long) -> Unit
) {
    private var view: View? = null                         // Vista de la burbuja
    private var params: WindowManager.LayoutParams? = null // Parámetros de posición/tamaño

    fun getView(): View? { return view }
    fun getParams(): WindowManager.LayoutParams? { return params }
    fun getBubble(): Bubble { return bubble }

    fun getBubbleId(): Long {
        return bubble.id
    }

    fun setupBubble() {
        Log.e("BubbleView | setupBubble", "setupBubble: -windowManager- $windowManager")
        // Contexto con tema personalizado
        val themedContext = ContextThemeWrapper(context, R.style.Theme_BubbleOverlay)
        // Infla el layout de la burbuja
        view = LayoutInflater.from(themedContext).inflate(R.layout.bubble_layout, null, false)
        params = setupBubbleParams() // configura los parametros iniciales

        view!!.findViewById<ImageView>(R.id.bubble_icon).setImageURI(bubble.imageUri)

        val cardView = view!!.findViewById<MaterialCardView>(R.id.bubble_root)
// cardView.radius = if (bubble.shape == BubbleShape.CIRCLE) cardView.width / 2f else 0f

        // Calcular radio usando el tamaño definido en el XML (60dp)
        val radiusPx = if (bubble.shape == BubbleShape.CIRCLE) {
            30.dpToPx(context).toFloat() // 30dp = mitad de 60dp (ancho definido en XML)
        } else {
            0f
        }
        cardView.radius = radiusPx

        windowManager.addView(view, params)

        if (!bubble.isVisible) {
            hide()
        }

        windowManager.updateViewLayout(view, params)

        // Configura eventos táctiles
        val touchListener = BubbleTouchListener(
            context           = context,
            bubbleView        = this,
            windowManager     = windowManager,
            onBubbleSelected  = { b ->
                onBubbleSelected(b.id)
            },
            onBubbleDoubleTap = { b ->
                onBubbleDoubleTap(b.id)
            }
        )
        view!!.setOnTouchListener(touchListener)
    }

    private fun setupBubbleParams(): WindowManager.LayoutParams {
        val size = 60.dpToPx(context) // Convierte dp a píxeles

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

    fun hide() {
        view!!.visibility = View.GONE
    }

    fun show() {
        view!!.visibility = View.VISIBLE
    }

    fun centerUnderPanelView(panelHeight: Int) {
        Log.e("BubbleView", "BubbleView: centerView - panelHeight: $panelHeight")
        val displayMetrics = context.resources.displayMetrics

        // la primera vez que se llama panelHeight es 0
        var panelHeightAux = panelHeight
        if (panelHeightAux == 0) { panelHeightAux = 880 }

        // Centrado horizontal
        val screenCenterX = displayMetrics.widthPixels / 2
        val bubbleHalfWidth = view!!.width / 2
        params!!.x = screenCenterX - bubbleHalfWidth

        // Posición vertical debajo del panel
        params!!.y = panelHeightAux + 40.dpToPx(context)

        windowManager.updateViewLayout(view, params)
    }

    fun changeShape() {
        val cardView = view!!.findViewById<MaterialCardView>(R.id.bubble_root)
        cardView.radius = if (bubble.shape == BubbleShape.CIRCLE) cardView.width / 2f else 0f

        windowManager.updateViewLayout(view, params)
    }

    fun resizeBubble(sizeDp: Int) {
        val sizeInPx = sizeDp.dpToPx(context)
        params!!.width = sizeInPx
        params!!.height = sizeInPx

        windowManager.updateViewLayout(view, params)

        view!!.findViewById<ImageView>(R.id.bubble_icon).apply {
            // layoutParams.width  == bubble.view.layoutParams
            // (por el apply que usa bubble.view como parametro)
            layoutParams.width = sizeInPx
            layoutParams.height = sizeInPx
            requestLayout() // <- Fuerza el redibujado
        }
    }

    fun updateImage() {
        try {
            view!!.findViewById<ImageView>(R.id.bubble_icon)
                .setImageURI(bubble.imageUri)
        } catch (e: Exception) {
            Toast.makeText(context, "Error al cargar imagen", Toast.LENGTH_SHORT).show()
        }
    }

    fun cleanUp() {
        // 1. Remover vistas del WindowManager
        windowManager.removeView(view)  // Reemplaza yourRootView con la vista principal
    }
}