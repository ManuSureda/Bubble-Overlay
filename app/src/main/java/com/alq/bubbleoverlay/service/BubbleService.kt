package com.alq.bubbleoverlay.service

import BubbleDao
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.alq.bubbleoverlay.R
import com.alq.bubbleoverlay.dao.Bubble
import com.alq.bubbleoverlay.dao.BubbleDatabase
import com.alq.bubbleoverlay.ui.views.BubbleControlPanelView
import com.alq.bubbleoverlay.ui.views.BubbleView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * BubbleService es el orquestador: crea, almacena y destruye burbujas.
 */
class BubbleService : Service() {
    private val NOTIFICATION_ID = 1001

    private lateinit var bubbleDao: BubbleDao
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job()) // Alcance de corrutinas

    // val = variable de solo lectura (inmutable)
    // lista de burbujas activas
    private var bubblesList = mutableListOf<Bubble>()
    // burbuja seleccionada actualmente
    private var activeBubble: Bubble? = null

    // Componentes de la UI y gestion
    private lateinit var windowManager: WindowManager // Manager de ventanas

    // Para comunicación interna entre componentes
    private lateinit var localBroadcastManager: LocalBroadcastManager

    // todo: al parecer, no es correcto tener views dentro de un service por posibles memoryleak
    // todo: corregir!!!
    //private lateinit var bubbleView: BubbleView
    private lateinit var bubbleViewList: MutableList<BubbleView>
    private lateinit var bubbleControlPanelView: BubbleControlPanelView


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

        Log.e("BubbleService", "--onCreate()--")

        super.onCreate()

        initializeWindowManager()
        initializeLocalBroadcastManager()

        // Configura la notificación permanente (requerido para servicios en primer plano)
        setupNotification()

        // Inicializar Room
        val database = BubbleDatabase.getInstance(this)
        bubbleDao = database.bubbleDao()

        // Cargar burbujas al iniciar el servicio
        // tambien hace el setup de burbuja
        loadBubbles() // si no habia ninguna burbuja, crea una

        if (activeBubble == null) {  activeBubble = bubblesList[0] }

        setupBubbleControlPanel()
    }

    private fun initializeWindowManager() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    private fun initializeLocalBroadcastManager() {
        localBroadcastManager = LocalBroadcastManager.getInstance(this)
    }

    // Configura la notificación permanente (requerido para servicios en primer plano)
    private fun setupNotification() {
        NotificationHelper.createChannel(this)
        val notification = NotificationHelper.buildNotification(this)

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun loadBubbles() {

//        coroutineScope.launch(Dispatchers.IO) { // Ejecutar en hilo de fondo
//            ... bubbles.forEach { bubble -> setupBubble(bubble) } // aca hay UI
//            ... Toast.makeText(this@BubbleService, "Error car.... // aca tambien
//        } esto genera un error, ya que Android demanda que lo  lo UI se haga en el main thread y no en uno I/O

        coroutineScope.launch { // por defecto Dispatchers.Main
            try {
                // 1) Lee la BD en IO
                val bubbles = withContext(Dispatchers.IO) {
                    bubbleDao.getAllBubbles().toMutableList()
                }

                // 2) Ya en Main, configura vistas y posibles Toasts
                bubbles.forEach { bubble -> setupBubble(bubble) }
                if (bubbles.isEmpty()) createNewBubble()

            } catch (e: Exception) {
                Log.e("BubbleService", "Error cargando burbujas", e)
                Toast.makeText(this@BubbleService, "Error cargando burbujas", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getDefaultImageUri(context: Context): Uri {
        val resourceId = R.drawable.defaultimg // ID generado automáticamente
        return Uri.parse("android.resource://${context.packageName}/$resourceId")
    }

    private fun createNewBubble() {
        val uri = getDefaultImageUri(this)
        val bubble = Bubble (
            title = "Burbuja Nueva",
            imageUri = uri
        )
        setupBubble(bubble)
        bubblesList.add(bubble)
        activeBubble = bubble

        CoroutineScope(Dispatchers.IO).launch {
            bubbleDao.insert(bubble)
        }
    }

    private fun setupBubble(bubble: Bubble) {

        val bubbleView = BubbleView(
            this,
            windowManager,
            bubble,
            onBubbleSelected = { bubbleId ->
                onBubbleSelected(bubbleId)
            },
            onBubbleDoubleTap = { bubbleId ->
                onBubbleDoubleTap(bubbleId)
            }
        )
        bubbleView.setupBubble()
        bubblesList.add(bubble)
        bubbleViewList.add(bubbleView)
    }

    private fun onBubbleSelected(bubbleId: Long) {
        activeBubble = bubblesList.find { it.id == bubbleId }
    }

    private fun onBubbleDoubleTap(bubbleId: Long) {
        activeBubble = bubblesList.find { it.id == bubbleId }

        activeBubble?.let { bubbleControlPanelView.show(it) }
    }

    private fun setupBubbleControlPanel() {
        bubbleControlPanelView = BubbleControlPanelView(
            this,
            windowManager,

            onRenameBubble = { newTitle ->
                onRenameBubble(newTitle)
            },
            onChangeShape = { isCircle ->
                onChangeShape(isCircle)
            },
            onResizeBubble = { sizeDp ->
                onResizeBubble(sizeDp)
            },
            onChangeImage = {  },
            onRemoveBubble = {  },
            onAddBubble = {  },
            onCloseApp = {  }
        )
        bubbleControlPanelView.setupBubbleControlPanel()
    }

    private fun onRenameBubble(newTitle: String) {
        activeBubble!!.title = newTitle

        CoroutineScope(Dispatchers.IO).launch {
            bubbleDao.update(activeBubble!!)
        }
    }

    private fun onChangeShape(isCircle: Boolean) {
        activeBubble!!.isCircle = isCircle

        bubbleViewList.find { it.getBubbleId() == activeBubble!!.id }!!.changeShape()

        CoroutineScope(Dispatchers.IO).launch {
            bubbleDao.update(activeBubble!!)
        }
    }

    private fun onResizeBubble(sizeDp: Int) {
        // como la view no se guarda en la BD, solo lo paso a BubbleView
        bubbleView
    }

    // todo: revisar
    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel() // Cancelar todas las corrutinas
        // Limpiar todas las vistas
        cleanUpBubbles()
    }
    // todo: revisar
    private fun cleanUpBubbles() {
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        coroutineScope.launch(Dispatchers.IO) {
            bubbleDao.getAllBubbles().forEach { bubble ->
                bubble.view?.let { windowManager.removeView(it) }
            }
        }
    }

}