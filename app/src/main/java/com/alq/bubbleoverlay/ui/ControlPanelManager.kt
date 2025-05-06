package com.alq.bubbleoverlay.ui

import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import com.alq.bubbleoverlay.R
import com.alq.bubbleoverlay.data.Bubble

class ControlPanelManager(
    private val service: /* BubbleService o contexto */,
    private val windowManager: WindowManager,
    private val panelView: View,
    private val panelParams: WindowManager.LayoutParams
) {
    var activeBubble: Bubble? = null
    private var isVisible = false

    fun show() { /* animar, posicionar burbuja */ }
    fun hide() { /* ocultar panel */ }

    fun setupControls(
        onShapeChange: (Boolean) -> Unit,
        onSizeChange: (Int) -> Unit,
        onChangeImage: () -> Unit,
        onRemoveBubble: () -> Unit,
        onAddBubble: () -> Unit,
        onCloseApp: () -> Unit
    ) {
        panelView.findViewById<Button>(R.id.btn_circle).setOnClickListener { onShapeChange(true) }
        panelView.findViewById<Button>(R.id.btn_square).setOnClickListener { onShapeChange(false) }
        panelView.findViewById<SeekBar>(R.id.size_slider).setOnSeekBarChangeListener(
            /* llama onSizeChange(progress) */
        )
        panelView.findViewById<Button>(R.id.btn_change_image).setOnClickListener { onChangeImage() }
        panelView.findViewById<Button>(R.id.btn_remove_bubble).setOnClickListener { onRemoveBubble() }
        panelView.findViewById<Button>(R.id.btn_add_bubble).setOnClickListener { onAddBubble() }
        panelView.findViewById<Button>(R.id.btn_close_app).setOnClickListener { onCloseApp() }
    }

    fun updateTitle(title: String) {
        panelView.findViewById<TextView>(R.id.tv_bubble_title).text = title
    }
}
