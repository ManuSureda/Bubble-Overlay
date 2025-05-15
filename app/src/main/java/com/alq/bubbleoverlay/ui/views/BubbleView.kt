package com.alq.bubbleoverlay.ui.views

import android.content.Context
import android.graphics.PixelFormat
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.SeekBar
import com.alq.bubbleoverlay.R
import com.alq.bubbleoverlay.dao.Bubble
import com.alq.bubbleoverlay.ui.listeners.BubbleTouchListener
import com.alq.bubbleoverlay.utils.dpToPx
import com.google.android.material.card.MaterialCardView

class BubbleView (
    private val context: Context,
    private val windowManager: WindowManager,
    private val bubble: Bubble,
    private val onBubbleSelected: (bubbleId: Long) -> Unit,
    private val onBubbleDoubleTap: (bubbleId: Long) -> Unit
){
    private var view: View? = null                         // Vista de la burbuja
    private var params: WindowManager.LayoutParams? = null // Parámetros de posición/tamaño

    fun getBubbleId(): Long {
        return bubble.id
    }

    fun setupBubble() {
        // Contexto con tema personalizado
        val themedContext = ContextThemeWrapper(context, R.style.Theme_BubbleOverlay)
        // Infla el layout de la burbuja
        view = LayoutInflater.from(themedContext).inflate(R.layout.bubble_layout, null, false)
        params = setupBubbleParams() // configura los parametros iniciales

        view!!.findViewById<ImageView>(R.id.bubble_icon).setImageURI(bubble.imageUri)

        val cardView = view!!.findViewById<MaterialCardView>(R.id.bubble_root)
        cardView.radius = if (bubble.isCircle) cardView.width / 2f else 0f

        windowManager.updateViewLayout(view, params)

        if (bubble.isVisible) {
            windowManager.addView(view, params)
        }
        // Configura eventos táctiles
        val touchListener = BubbleTouchListener(
            context           = context,
            bubble            = bubble,
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

    fun changeShape() {
        val cardView = view!!.findViewById<MaterialCardView>(R.id.bubble_root)
        cardView.radius = if (bubble.isCircle) cardView.width / 2f else 0f

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
        }
    }
}