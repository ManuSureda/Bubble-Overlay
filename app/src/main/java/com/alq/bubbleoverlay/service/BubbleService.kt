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
import com.alq.bubbleoverlay.dao.BubbleShape
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

    // val = variable de solo lectura (inmutable)
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job()) // Alcance de corrutinas

    // debo inizializarlo despues, por que necesita el contexto que se crea tras el onCreate...
    private lateinit var bubbleDao: BubbleDao

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
    private var bubbleViewList = mutableListOf<BubbleView>()
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

        // Inicializar Room
        val database = BubbleDatabase.getInstance(this)
        bubbleDao = database.bubbleDao()

        // Configura la notificación permanente (requerido para servicios en primer plano)
        setupNotification()

        coroutineScope.launch {
            // 1) Cargar burbujas registradas en la BD
            // 2) asignar el resultado a bubblesList
            Log.e("BubbleService | onCreate", "bubblesList previo a loadBubbles $bubblesList")
            bubblesList = loadBubbles()
            Log.e("BubbleService | onCreate", "bubblesList pos loadBubbles $bubblesList")

            // 3) verificar si hay al menos una burbuja
            if (bubblesList.isEmpty()) { // si no habia ninguna burbuja, crea una
                createNewBubble()
            } else {
                // 5) crep la BubbleView de cada Bubble
                // infla el layout y genera el BubbleView
                bubblesList.forEach { bubble -> setupBubble(bubble) }
            }
            // 4) creo que es in-necesario
            activeBubble = bubblesList[0]

            // 6) creo la view del panel de control
            setupBubbleControlPanel()
        }
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

    private suspend fun loadBubbles(): MutableList<Bubble> {
        Log.e("BubbleService | loadBubbles", "loadBubbles: start -----------------------")
        return try {
            // Este bloque BLOCKEA solo la corrutina hasta que termine la lectura
            withContext(Dispatchers.IO) {
                bubbleDao.getAllBubbles().toMutableList()
            }
        } catch (e: Exception) {
            Log.e("BubbleService", "Error cargando burbujas", e)
            Toast.makeText(this@BubbleService, "Error cargando burbujas", Toast.LENGTH_SHORT).show()

            mutableListOf()
        }
    }

    private fun getDefaultImageUri(context: Context): Uri {
        val resourceId = R.drawable.defaultimg // ID generado automáticamente
        return Uri.parse("android.resource://${context.packageName}/$resourceId")
    }

    private suspend fun createNewBubble() {
        Log.e("BubbleService | createNewBubble", "-----------------------")
        val uri = getDefaultImageUri(this)
        val bubble = Bubble(
            title = "Burbuja Nueva",
            imageUri = uri
        )

        try {
            Log.e("BubbleService | createNewBubble", "bubble pre insert $bubble")

            // 1. Insertar en la BD y esperar confirmacion
            withContext(Dispatchers.IO) { // ejecuta el bloque en un hilo de fondo y espera a que termine
                bubbleDao.insert(bubble)  // Solo si la inserción es exitosa continúa con las operaciones en el hilo principal
            }

            Log.e("BubbleService | createNewBubble", "bubble pos insert $bubble")

            // 2. Si llega aca, la insercion fue exitosa
            bubblesList.add(bubble)
            setupBubble(bubble)
        } catch (e: Exception) {
            Log.e("BubbleService", "Error creando burbuja", e)
            Toast.makeText(
                this@BubbleService,
                "Error al crear la burbuja: ${e.localizedMessage}",
                Toast.LENGTH_LONG
            ).show()
        }

    }

    private fun setupBubble(bubble: Bubble) {
        Log.e("BubbleService | setupBubble", "------------------------------")
        Log.e("BubbleService | setupBubble", "setupBubble: -windowManager- $windowManager")

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
        bubbleViewList.add(bubbleView)
    }

    private fun onBubbleSelected(bubbleId: Long) {
        activeBubble = bubblesList.find { it.id == bubbleId }
    }

    // abre el panel de control y centra la burbuja
    private fun onBubbleDoubleTap(bubbleId: Long) {
        activeBubble = bubblesList.find { it.id == bubbleId }

        val currentBubble = activeBubble ?: run {
            // deberia ser imposible que esto pase, pero por las duadas
            Toast.makeText(this, "Error al seleccionar la burbuja", Toast.LENGTH_SHORT).show()
            return
        }

        // lo necesito para poder centrar la burbuja
        val panelHeight = bubbleControlPanelView.getPanelHeight()
        val bubbleView = bubbleViewList.find { it.getBubbleId() == currentBubble.id }?: run {
            // deberia ser imposible que esto pase, pero por las duadas
            Toast.makeText(this, "Error al seleccionar la burbuja", Toast.LENGTH_SHORT).show()
            return
        }
        bubbleView.centerUnderPanelView(panelHeight)

        bubbleControlPanelView.show(currentBubble)
    }

    private fun setupBubbleControlPanel() {
        Log.e("BubbleService | setupBubbleControlPanel", "------------------------------")
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
            onChangeImage = { bubbleId ->
                onChangeImage(bubbleId)
            },
            onRemoveBubble = {  },
            onAddBubble = {  },
            onCloseApp = {  }
        )
        bubbleControlPanelView.setupBubbleControlPanel()
    }

    private fun onRenameBubble(newTitle: String) {
        val currentBubble = activeBubble ?: run {
            // deberia ser imposible que esto pase, pero por las duadas
            Toast.makeText(this, "No hay burbuja seleccionada", Toast.LENGTH_SHORT).show()
            return
        }

        val oldTitle = currentBubble.title

        currentBubble.title = newTitle

        coroutineScope.launch { // usa el scope del servicio
            try {
                withContext(Dispatchers.IO) {
                    bubbleDao.update(currentBubble)
                }
            } catch (e: Exception) {
                currentBubble.title = oldTitle
                Log.e("BubbleService", "Error actualizando burbuja", e)
                Toast.makeText(
                    this@BubbleService,
                    "Error al guardar cambios",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun onChangeShape(newShape: BubbleShape) {
        val currentBubble = activeBubble ?: run {
            // deberia ser imposible que esto pase, pero por las duadas
            Toast.makeText(this, "No hay burbuja seleccionada", Toast.LENGTH_SHORT).show()
            return
        }
        val oldShape = currentBubble.shape

        currentBubble.shape = newShape

        coroutineScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    bubbleDao.update(currentBubble)
                }

                bubbleViewList.find { it.getBubbleId() == currentBubble.id }!!.changeShape()
            } catch (e: Exception) {
                currentBubble.shape = oldShape
                Log.e("BubbleService", "Error actualizando burbuja", e)
                Toast.makeText(
                    this@BubbleService,
                    "Error al guardar cambios",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun onResizeBubble(sizeDp: Int) {
        val currentBubble = activeBubble ?: run {
            // deberia ser imposible que esto pase, pero por las duadas
            Toast.makeText(this, "No hay burbuja seleccionada", Toast.LENGTH_SHORT).show()
            return
        }

        bubbleViewList.find { it.getBubbleId() == currentBubble.id }!!.resizeBubble(sizeDp)
    }

    private fun onChangeImage(bubbleId: Long) {
        activeBubble = bubblesList.find { it.id == bubbleId } ?: run {
            // deberia ser imposible que esto pase, pero por las duadas
            Toast.makeText(this, "No hay burbuja seleccionada", Toast.LENGTH_SHORT).show()
            return
        }

        // Problema: Los servicios no pueden usar registerForActivityResult directamente.
        // Solución: Crea una actividad transparente que actúe como intermediaria.
        // Iniciar actividad para selección de imagen
        //openImagePicker()

        val intent = Intent(this, ImageResultHandlerActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    // Abre la actividad para seleccionar imagen
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png"))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getParcelableExtra<Uri>("selected_image_uri")?.let { uri ->
            handleNewImage(uri)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun handleNewImage(uri: Uri) {
        activeBubble?.let { bubble ->

            // Actualizar y guardar
            bubble.imageUri = uri

            coroutineScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        bubbleDao.update(bubble)
                    }
                    // Actualizar la vista
                    updateBubbleViewImage(bubble.id)
                } catch (e: Exception) {
                    Log.e("BubbleService", "Error actualizando imagen", e)
                    Toast.makeText(
                        this@BubbleService,
                        "Error al guardar imagen",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun updateBubbleViewImage(bubbleId: Long) {
        bubbleViewList.find { it.getBubbleId() == bubbleId }?.updateImage()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 1. Limpiar todas las vistas de burbujas
        cleanUpBubbles()

        // 2. Limpiar panel de control
        cleanUpControlPanel()

        // 3. Cancelar todas las corrutinas
        coroutineScope.cancel()

        // 4. Liberar otras referencias
        bubblesList.clear()
        bubbleViewList.clear()
    }

    private fun cleanUpBubbles() {
        // Ejecutar en main thread (las operaciones de UI deben hacerse aquí)
        CoroutineScope(Dispatchers.Main).launch {
            bubbleViewList.forEach { bubbleView ->
                bubbleView.cleanUp()
            }
        }
    }

    private fun cleanUpControlPanel() {
        // Ejecutar en main thread
        CoroutineScope(Dispatchers.Main).launch {
            bubbleControlPanelView.cleanUp()
        }
    }

}