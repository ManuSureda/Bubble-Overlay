package com.alq.bubbleoverlay.ui.views

import android.app.AlertDialog
import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.core.view.ViewCompat
import com.alq.bubbleoverlay.R
import com.alq.bubbleoverlay.dao.Bubble
import com.alq.bubbleoverlay.dao.BubbleShape

class BubbleControlPanelView(
    private val context: Context,
    private val windowManager: WindowManager,

    private val onRenameBubble: (newName: String) -> Unit,
    private val onChangeShape: (newShape: BubbleShape) -> Unit,
    private val onResizeBubble: (sizeDp: Int) -> Unit,
    private val onChangeImage: (bubbleId: Long) -> Unit,
    private val onRemoveBubble: (bubble: Bubble) -> Unit,
    private val onAddBubble: () -> Unit,
    private val onCloseApp: () -> Unit
) {
    private lateinit var panelView: View
    private lateinit var activeBubble: Bubble

    fun setupBubbleControlPanel () {
        panelView = LayoutInflater.from(context).inflate(R.layout.panel_control, null, false)

        val panelParams = setupBubbleControlPanelParams()
        windowManager.addView(panelView, panelParams)

        setupBubbleControlPanelControls()

        panelView.visibility = View.GONE // Inicialmente oculto
    }

    private fun setupBubbleControlPanelParams() : WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
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

    private fun setupBubbleControlPanelControls() {

        // "cerrar" panel
        panelView.findViewById<View>(R.id.overlay)?.setOnClickListener {
            hide()
        }

        // cambiar nombre
        panelView.findViewById<TextView>(R.id.tv_bubble_title)?.setOnClickListener {
            renameBubble()
        }

        // Botón forma circular
        panelView.findViewById<Button>(R.id.btn_circle).setOnClickListener {
            onChangeShape(BubbleShape.CIRCLE)
        }

        // Botón forma cuadrada
        panelView.findViewById<Button>(R.id.btn_square).setOnClickListener {
            onChangeShape(BubbleShape.SQUARE)
        }

        // cambiar tamaño
        panelView.findViewById<SeekBar>(R.id.size_slider)?.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                onResizeBubble(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        panelView.findViewById<Button>(R.id.btn_change_image)?.setOnClickListener {
            hide()

            onChangeImage(activeBubble.id)
        }

        panelView?.findViewById<Button>(R.id.btn_remove_bubble)?.setOnClickListener {
            activeBubble?.let { onRemoveBubble(it) }
        }

        panelView?.findViewById<Button>(R.id.btn_add_bubble)?.setOnClickListener {
            onAddBubble()
        }

        panelView?.findViewById<Button>(R.id.btn_close_app)?.setOnClickListener {
            onCloseApp()
        }

    }

    private fun hide() {
        panelView.visibility = View.GONE
    }

    fun show(bubble: Bubble): Int {
        activeBubble = bubble

        panelView.visibility = View.VISIBLE
        panelView.animate().alpha(0.9f).duration = 200

        return panelView.findViewById<View>(R.id.panel_content).height
    }


    private fun renameBubble() {
        val editText = EditText(context).apply {
            setText(activeBubble.title)
            setSelectAllOnFocus(true)
        }

        AlertDialog.Builder(context)
            .setTitle("Renombrar burbuja")
            .setView(editText)
            .setPositiveButton("OK") { dialog, _ ->
                val newTitle = editText.text.toString().trim()
                if (newTitle .isNotEmpty()) {
                    panelView.findViewById<TextView>(R.id.tv_bubble_title).text = newTitle
                    onRenameBubble(newTitle)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }


    fun cleanUp() {
        // 1. Remover vista si está visible
        if (ViewCompat.isAttachedToWindow(panelView)) {
            windowManager.removeView(panelView)
        }
    }
}
