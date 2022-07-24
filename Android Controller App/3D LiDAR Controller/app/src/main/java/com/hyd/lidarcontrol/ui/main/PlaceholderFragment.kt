package com.hyd.lidarcontrol.ui.main

import android.content.ClipData
import android.content.ClipDescription
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Point
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.SystemClock
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.hyd.lidarcontrol.R
import com.hyd.lidarcontrol.databinding.FragmentMainBinding
import com.hyd.lidarcontrol.utils.JoystickView
import com.hyd.lidarcontrol.utils.OnJoystickChange
import java.io.*
import java.net.Socket


/**
 * A placeholder fragment containing a simple view.
 */
class PlaceholderFragment : Fragment() {

    private lateinit var pageViewModel: PageViewModel
    private var _binding: FragmentMainBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var listerSocket: Socket? = null
    private var streamSocket: Socket? = null
    private var velArgSocket: Socket? = null
    private var mapSocket: Socket? = null
    var updateConversationHandler: Handler? = null

    private var timestamp: String = ""
    private var closeSocket: Boolean = false
    private var closeSocket2: Boolean = false
    private var closeSocket3: Boolean = false
    private var closeSocket4: Boolean = false

    // 0 for socketTest, 1 for Starting cores,
    // 2 for image preview, 3 for map stream
    private var streamType: Int = 0
    private var streamType2: Int = 0
    private var streamType3: Int = 0
    private var textBufferRead1: String = ""
    private var textBufferRead2: String = ""
    private var textBufferRead3: String = ""
    private var textBufferWrite1: String = ""
    private var textBufferWrite2: String = ""
    private var textBufferWrite3: String = ""

    private var currentTest: Int = 1

    // Tab 1
    private lateinit var ip: EditText
    private lateinit var delay1: TextView
    private lateinit var delay2: TextView
    private lateinit var delay3: TextView
    private lateinit var status: TextView
    private lateinit var imgPreview: ImageView
    private lateinit var commThread1: Runnable

    // Tab 2
    private lateinit var mapStream: ImageView
    private lateinit var joystick: JoystickView
    private var isStreaming: Boolean = false
    private lateinit var commThread2: Runnable
    private var socketRestore: Int = 3
    private var socketRestoreFlag1: Boolean = false
    private var velFlag = 0.0f
    private var angFlag = 0.0f
    private var waitInterval = 7
    private lateinit var velocityT: TextView
    private lateinit var angularT: TextView
    private var isControl: Boolean = false

    // Tab 3
    private lateinit var mappingStream: ImageView
    private var posX: Float = 0.0f
    private var posY: Float = 0.0f
    private var socketRestoreFlag2: Boolean = false
    private var isMapping: Boolean = false
    private lateinit var commThread3: Runnable

    // Video stream
    private var timer = object : CountDownTimer(60 * 60 * 1000, 100) {
        override fun onTick(millisUntilFinished: Long) {
            if (waitInterval != 7) {
                waitInterval--
                if (waitInterval == 0) {
                    waitInterval = 7
                }
                if (waitInterval % 3 == 0 && isControl) {
                    if (kotlin.math.abs(velFlag) > kotlin.math.abs(angFlag)) {
                        if (velFlag > 0)
                            socketVelAng('w')
                        else
                            socketVelAng('x')
                    } else if (kotlin.math.abs(velFlag) < kotlin.math.abs(angFlag)) {
                        if (angFlag > 0)
                            socketVelAng('d')
                        else
                            socketVelAng('a')
                    } else {
                        socketVelAng('s')
                    }
                }

            } else {
                waitInterval--
                if (!socketRestoreFlag1) {
                    textBufferWrite1 = "m000"
                    streamType = 3
                } else {
                    if (socketRestore == 0) {
                        println("Socket restore")
                        socketRestore = 3
                        socketRestoreFlag1 = false
                    } else {
                        socketRestore--
                        socketRestore()
                    }
                }
            }
        }

        override fun onFinish() {
            textBufferWrite1 = "m999"
            streamType = 3
        }
    }

    // Car frame move listener
    private val carFrameDragListener = View.OnDragListener { view, dragEvent ->
        val draggableItem = dragEvent.localState as View
        when (dragEvent.action) {
            DragEvent.ACTION_DRAG_STARTED -> {
                true
            }
            DragEvent.ACTION_DRAG_ENTERED -> {
                view.invalidate()
                mappingStream.alpha = 0.7f
                true
            }
            DragEvent.ACTION_DRAG_LOCATION -> {
                true
            }
            DragEvent.ACTION_DRAG_EXITED -> {
                mappingStream.alpha = 1.0f
                draggableItem.visibility = View.VISIBLE
                view.invalidate()
                true
            }
            DragEvent.ACTION_DROP -> {
                mappingStream.alpha = 1.0f
                if (dragEvent.clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                    val draggedData = dragEvent.clipData.getItemAt(0).text
                    //TODO : perform any action on the draggedData
                }
                draggableItem.x = dragEvent.x - (draggableItem.width / 2) - 32
                draggableItem.y = dragEvent.y - (draggableItem.height / 2) -16
                true
            }
            DragEvent.ACTION_DRAG_ENDED -> {
                draggableItem.visibility = View.VISIBLE
                view.invalidate()
                true
            }
            else -> {
                false
            }
        }
    }

    // PORT
    private var uHOST = "192.168.1.105"
    private val uPORT1 = 7230
    private val uPORT2 = 7231
    private val uPORT3 = 7232
    private val uPORT4 = 7233


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageViewModel = ViewModelProvider(this).get(PageViewModel::class.java).apply {
            setIndex(arguments?.getInt(ARG_SECTION_NUMBER) ?: 1)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentMainBinding.inflate(inflater, container, false)
        val root = binding.root

        pageViewModel.text.observe(viewLifecycleOwner, Observer {
            when (it) {
                "tab1" -> {
                    binding.constraintLayout1.visibility = View.VISIBLE
                    binding.constraintLayout2.visibility = View.GONE
                    binding.constraintLayout3.visibility = View.GONE

                    ip = binding.ip
                    delay1 = binding.delay1
                    delay2 = binding.delay2
                    delay3 = binding.delay3
                    status = binding.status
                    imgPreview = binding.imgPreview

                    updateConversationHandler = Handler()

                    isControl = false

                    binding.connect.setOnClickListener {
                        socketTestDelay()
                        socketConnectThread()
                        binding.ip.isEnabled = false
                        binding.connect.isEnabled = false
                        uHOST = ip.text.toString()
                        closeSocket = false
                    }

                    binding.retry.setOnClickListener {
                        currentTest = 1
                        socketTestDelay()
                    }

                    binding.start.setOnClickListener {
                        socketStartCore()
                    }

                    binding.imgPreview.setOnClickListener {
                        socketPreview()
                    }

                    binding.clear.setOnClickListener {
                        closeSocket = true
                    }


                }
                "tab2" -> {
                    binding.constraintLayout1.visibility = View.GONE
                    binding.constraintLayout2.visibility = View.VISIBLE
                    binding.constraintLayout3.visibility = View.GONE

                    mapStream = binding.mapStream
                    velocityT = binding.velocity
                    angularT = binding.angular

                    updateConversationHandler = Handler()

                    isControl = true

                    // assign joystick on change callback
                    joystick = binding.joystick
                    joystick.onChange = OnJoystickChange { horizontal, vertical ->
                        velFlag = vertical
                        angFlag = horizontal
                    }

                    binding.mapStream.setOnClickListener {
                        if (!isStreaming) {
                            socketConnectThread2()
                            socketConnectThread3()
                            closeSocket2 = false
                            closeSocket3 = false
                            timer.start()
                        } else {
                            closeSocket2 = true
                            timer.cancel()
                        }
                        isStreaming = !isStreaming
                    }
                }
                "tab3" -> {
                    binding.constraintLayout1.visibility = View.GONE
                    binding.constraintLayout2.visibility = View.GONE
                    binding.constraintLayout3.visibility = View.VISIBLE

                    mappingStream = binding.mapping
                    mappingStream.setOnDragListener(carFrameDragListener)
                    binding.carFrame.visibility = View.GONE

                    updateConversationHandler = Handler()

                    attachViewDragListener()
                    isControl = false
                    socketConnectThread4()

                    binding.create.setOnClickListener {
                        binding.carFrame.visibility = View.VISIBLE
                        socketCallMap()
                    }

                    binding.delete.setOnClickListener {
                        binding.carFrame.visibility = View.GONE
                    }

                    binding.go.setOnClickListener {
                        socketStartNavi()
                    }

                    mappingStream.setOnClickListener {
                        if (!isMapping) {
                            socketConnectThread2()
                            closeSocket2 = false
                            timer.start()
                        } else {
                            closeSocket2 = true
                            timer.cancel()
                        }
                        isMapping = !isMapping
                    }

                }
                else -> {

                }
            }
        })
        return root
    }

    companion object {
        private const val ARG_SECTION_NUMBER = "section_number"

        @JvmStatic
        fun newInstance(sectionNumber: Int): PlaceholderFragment {
            return PlaceholderFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SECTION_NUMBER, sectionNumber)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class CarFrameDragShadowBuilder(view: View) : View.DragShadowBuilder(view) {
        private val shadow = ResourcesCompat.getDrawable(
            view.context.resources,
            R.drawable.car_frame,
            view.context.theme
        )

        override fun onProvideShadowMetrics(size: Point, touch: Point) {
            val width: Int = view.width
            val height: Int = view.height
            shadow?.setBounds(0, 0, width, height)
            size.set(width, height)
            touch.set(width, height)
        }

        override fun onDrawShadow(canvas: Canvas) {
            shadow?.draw(canvas)
        }
    }

    private fun attachViewDragListener() {
        binding.carFrame.setOnLongClickListener { view: View ->
            val item = ClipData.Item("maskDragMessage")
            val dataToDrag = ClipData(
                "maskDragMessage",
                arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN),
                item
            )
            val maskShadow = CarFrameDragShadowBuilder(view)

            view.startDragAndDrop(dataToDrag, maskShadow, view, 0)
            view.visibility = View.INVISIBLE
            true
        }
    }


    private fun socketConnectThread() {
        object : Thread() {
            override fun run() {
                try {
                    listerSocket = Socket(ip.text.toString(), uPORT1)
                    val output =
                        PrintWriter(listerSocket!!.getOutputStream(), true)
                    val input =
                        BufferedReader(InputStreamReader(listerSocket!!.inputStream))
                    while (!closeSocket) {
                        if (textBufferWrite1 != "") {
                            when (streamType) {
                                // socket connection test
                                0 -> {
                                    try {
                                        if (currentTest == 1) {
                                            for (i in 1..3) {
                                                socketTestDelay()
                                                output.println(textBufferWrite1)
                                                textBufferRead1 = input.readLine()
                                                if (textBufferRead1 != "") {
                                                    decodeMessage(textBufferRead1)
                                                }
                                            }
                                        }
                                        textBufferRead1 = ""
                                        textBufferWrite1 = ""
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                // start cores
                                // start streaming preview images
                                1, 2 -> {
                                    try {
                                        output.println(textBufferWrite1)
                                        textBufferRead1 = input.readLine()
                                        if (textBufferRead1 != "") {
                                            decodeMessage(textBufferRead1)
                                        }
                                        textBufferRead1 = ""
                                        textBufferWrite1 = ""
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                else -> {
                                }
                            }
                        }
                        SystemClock.sleep(1)
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                } finally {
                    try {
                        listerSocket?.close()
                        interrupt()
                        activity?.runOnUiThread(Runnable {
                            binding.connect.isEnabled = true
                            binding.ip.isEnabled = true
                            delay1.text = "NA"
                            delay2.text = "NA"
                            delay3.text = "NA"
                            delay1.setTextColor(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.gray
                                )
                            )
                            delay2.setTextColor(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.gray
                                )
                            )
                            delay3.setTextColor(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.gray
                                )
                            )
                            status.text = "Not Connect!"
                            status.setTextColor(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.gray
                                )
                            )
                            val bitmap = BitmapFactory.decodeResource(
                                resources,
                                R.drawable.image_not_found_scaled
                            )
                            imgPreview.setImageBitmap(bitmap)
                            closeSocket = true
                            currentTest = 1
                        })
                    } catch (e: IOException) {
                        // TODO Auto-generated catch block
                        e.printStackTrace()
                    }
                }
            }
        }.start()
    }

    private fun socketConnectThread2() {
        object : Thread() {
            override fun run() {
                try {
                    streamSocket = Socket(uHOST, uPORT2)
                    while (!closeSocket2) {
                        val output =
                            PrintWriter(streamSocket!!.getOutputStream(), true)
                        val input =
                            BufferedReader(InputStreamReader(streamSocket!!.inputStream))
                        if (textBufferWrite1 != "") {
                            when (streamType) {
                                3, 4 -> {
                                    try {
                                        output.println(textBufferWrite1)
                                        println("Sent: $textBufferWrite1")
                                        if (socketRestoreFlag1) {
                                            val str = CharArray(9999999)
                                            textBufferRead1 = input.read(str, 0, 9999999).toString()
                                            println("Error Read: $textBufferRead1")
                                        } else {
                                            textBufferRead1 = input.readLine()
                                            println("Read: $textBufferRead1")
                                        }
                                        if (textBufferRead1 != "") {
                                            decodeMessage(textBufferRead1)
                                        }
                                        textBufferRead1 = ""
                                        textBufferWrite1 = ""
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                else -> {
                                }
                            }
                        }
                        SystemClock.sleep(1)
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                } finally {
                    try {
                        streamSocket?.close()
                        interrupt()
                        activity?.runOnUiThread(Runnable {
                            val bitmap = BitmapFactory.decodeResource(
                                resources,
                                R.drawable.stream_not_found_scaled
                            )
                            mapStream.setImageBitmap(bitmap)
                            closeSocket2 = true
                        })
                    } catch (e: IOException) {
                        // TODO Auto-generated catch block
                        e.printStackTrace()
                    }
                }
            }
        }.start()
    }

    private fun socketConnectThread3() {
        object : Thread() {
            override fun run() {
                try {
                    velArgSocket = Socket(uHOST, uPORT3)
                    while (!closeSocket3) {
                        val output =
                            PrintWriter(velArgSocket!!.getOutputStream(), true)
                        val input =
                            BufferedReader(InputStreamReader(velArgSocket!!.inputStream))
                        if (textBufferWrite2 != "") {
                            when (streamType2) {
                                5 -> {
                                    try {
                                        output.println(textBufferWrite2)
                                        println("Sent: $textBufferWrite2")
                                        textBufferRead2 = input.readLine()
                                        if (textBufferRead2 != "") {
                                            decodeMessage(textBufferRead2)
                                        }
                                        println("Read: $textBufferRead2")

                                        textBufferRead2 = ""
                                        textBufferWrite2 = ""
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                else -> {
                                }
                            }
                        }
                        SystemClock.sleep(1)
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                } finally {
                    try {
                        velArgSocket?.close()
                        interrupt()
                        activity?.runOnUiThread(Runnable {
                            velocityT.text = "NA"
                            angularT.text = "NA"
                            closeSocket3 = true
                        })
                    } catch (e: IOException) {
                        // TODO Auto-generated catch block
                        e.printStackTrace()
                    }
                }
            }
        }.start()
    }

    private fun socketConnectThread4() {
        object : Thread() {
            override fun run() {
                try {
                    mapSocket = Socket(uHOST, uPORT4)
                    while (!closeSocket4) {
                        val output =
                            PrintWriter(mapSocket!!.getOutputStream(), true)
                        val input =
                            BufferedReader(InputStreamReader(mapSocket!!.inputStream))
                        if (textBufferWrite3 != "") {
                            when (streamType3) {
                                6 -> {
                                    try {
                                        output.println(textBufferWrite3)
                                        println("Sent: $textBufferWrite3")
                                        textBufferRead3 = input.readLine()
                                        if (textBufferRead3 != "") {
                                            decodeMessage(textBufferRead3)
                                        }
                                        println("Read: $textBufferRead3")

                                        textBufferRead3 = ""
                                        textBufferWrite3 = ""
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                7 -> {
                                    try {
                                        output.println(textBufferWrite3)
                                        println("Sent: $textBufferWrite3")
                                        if (socketRestoreFlag2) {
                                            val str = CharArray(9999999)
                                            textBufferRead3 = input.read(str, 0, 9999999).toString()
                                            println("Error Read: $textBufferRead3")
                                        } else {
                                            textBufferRead3 = input.readLine()
                                            println("Read: $textBufferRead3")
                                        }
                                        if (textBufferRead3 != "") {
                                            decodeMessage(textBufferRead3)
                                        }
                                        textBufferRead3 = ""
                                        textBufferWrite3 = ""
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                else -> {
                                }
                            }
                        }
                        SystemClock.sleep(1)
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                } finally {
                    try {
                        mapSocket?.close()
                        interrupt()
                        activity?.runOnUiThread(Runnable {
                            closeSocket4 = true
                        })
                    } catch (e: IOException) {
                        // TODO Auto-generated catch block
                        e.printStackTrace()
                    }
                }
            }
        }.start()
    }

    private fun socketTestDelay() {
        val tsLong = System.currentTimeMillis()
        timestamp = "x${tsLong}"
        textBufferWrite1 = timestamp
        streamType = 0
    }

    private fun socketStartCore() {
        textBufferWrite1 = "c000"
        streamType = 1
    }

    private fun socketPreview() {
        textBufferWrite1 = "p000"
        streamType = 2
    }

    private fun socketRestore() {
        textBufferWrite1 = "z000"
        streamType = 4
    }

    private fun socketVelAng(arg: Char) {
        textBufferWrite2 = "t$arg"
        streamType2 = 5
    }

    private fun socketCallMap() {
        textBufferWrite3 = "n000"
        streamType3 = 6
    }

    private fun socketStartNavi() {
        textBufferWrite3 = "l"
        streamType3 = 7
    }

    private fun decodeMessage(msg: String) {
        var msgDecoded = ""

        when (msg[0]) {
            // start with x for testing the connection speed
            'x' -> {
                msgDecoded = msg.substring(1)
                val timestampDecoded = msgDecoded.toLong()
                val tsLong = System.currentTimeMillis()
                val tsDelay = tsLong - timestampDecoded
                val tsBusy = when {
                    tsDelay < 10 -> 0
                    tsDelay < 25 -> 1
                    else -> 2
                }
                activity?.runOnUiThread(Runnable {
                    when (currentTest) {
                        1 -> {
                            delay1.text = tsDelay.toString()
                            when (tsBusy) {
                                0 -> delay1.setTextColor(
                                    ContextCompat.getColor(
                                        requireContext(),
                                        R.color.green
                                    )
                                )
                                1 -> delay1.setTextColor(
                                    ContextCompat.getColor(
                                        requireContext(),
                                        R.color.yellow
                                    )
                                )
                                else -> delay1.setTextColor(
                                    ContextCompat.getColor(
                                        requireContext(),
                                        R.color.red
                                    )
                                )
                            }
                        }
                        2 -> {
                            delay2.text = tsDelay.toString()
                            when (tsBusy) {
                                0 -> delay2.setTextColor(
                                    ContextCompat.getColor(
                                        requireContext(),
                                        R.color.green
                                    )
                                )
                                1 -> delay2.setTextColor(
                                    ContextCompat.getColor(
                                        requireContext(),
                                        R.color.yellow
                                    )
                                )
                                else -> delay2.setTextColor(
                                    ContextCompat.getColor(
                                        requireContext(),
                                        R.color.red
                                    )
                                )
                            }
                        }
                        3 -> {
                            delay3.text = tsDelay.toString()
                            when (tsBusy) {
                                0 -> delay3.setTextColor(
                                    ContextCompat.getColor(
                                        requireContext(),
                                        R.color.green
                                    )
                                )
                                1 -> delay3.setTextColor(
                                    ContextCompat.getColor(
                                        requireContext(),
                                        R.color.yellow
                                    )
                                )
                                else -> delay3.setTextColor(
                                    ContextCompat.getColor(
                                        requireContext(),
                                        R.color.red
                                    )
                                )
                            }
                        }
                        else -> {
                            currentTest = 0
                        }
                    }
                    currentTest++
                })
            }
            // c for starting the cores
            'c' -> {
                msgDecoded = msg.substring(1)
                activity?.runOnUiThread(Runnable {
                    if (msgDecoded == "OK") {
                        status.text = "Success!"
                        status.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
                    } else {
                        status.text = "Failed!"
                        status.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                    }
                })
            }
            // p for the environment preview
            'p' -> {
                commThread1 = CommunicationThread(listerSocket!!)
                Thread(commThread1).start()
            }
            // m for the controlling video view
            'm' -> {
                commThread2 = CommunicationThread(streamSocket!!)
                Thread(commThread2).start()
            }
            // t for receiving the velocity data
            't' -> {
                val msgTemp = msg.substring(1).split("&")
                activity?.runOnUiThread(Runnable {
                    velocityT.text = (msgTemp[0].toFloat() / 10).toString()
                    angularT.text = (msgTemp[1].toFloat() / 10).toString()
                    // todo: Leave for completion rates
                })
            }
            // n for the map preview
            'n' -> {
                commThread3 = CommunicationThread(mapSocket!!)
                Thread(commThread3).start()
            }
            // l for the mapping video stream
            'l' -> {
                commThread3 = CommunicationThread(mapSocket!!)
                Thread(commThread3).start()
            }
            else -> {

            }
        }
    }

    inner class CommunicationThread(clientSocket: Socket) : Runnable {
        private var input: DataInputStream? = null

        override fun run() {
            while (!Thread.currentThread().isInterrupted) {
                try {
                    var data: ByteArray
                    val len = input!!.readInt()
                    println("Int: $len")
                    if (len < 0 || len > 9999999) {
                        println("Error: ${input!!.read()}")
                        socketRestoreFlag1 = true
                        Thread.currentThread().interrupt()
                    } else {
                        data = ByteArray(len)
                        if (len > 0) {
                            input!!.readFully(data, 0, data.size)
                            println("Read successfully.")
                        }
                        updateConversationHandler?.post(UpdateUIThread(data))
                        Thread.currentThread().interrupt()
                    }
                } catch (e: IOException) {
                    // TODO Auto-generated catch block
                    e.printStackTrace()
                }
            }
            println("Thread ended")
        }

        init {
            try {
                val `in` = clientSocket.getInputStream()
                input = DataInputStream(`in`)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    inner class CommunicationThread2(clientSocket: Socket) : Runnable {
        private var input: DataInputStream? = null

        override fun run() {
            while (!Thread.currentThread().isInterrupted) {
                try {
                    var data: ByteArray
                    val len = input!!.readInt()
                    println("Int: $len")
                    if (len < 0 || len > 9999999) {
                        println("Error: ${input!!.read()}")
                        socketRestoreFlag2 = true
                        Thread.currentThread().interrupt()
                    } else {
                        data = ByteArray(len)
                        if (len > 0) {
                            input!!.readFully(data, 0, data.size)
                            println("Read successfully.")
                        }
                        updateConversationHandler?.post(UpdateUIThread(data))
                        Thread.currentThread().interrupt()
                    }
                } catch (e: IOException) {
                    // TODO Auto-generated catch block
                    e.printStackTrace()
                }
            }
        }

        init {
            try {
                val `in` = clientSocket.getInputStream()
                input = DataInputStream(`in`)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    inner class UpdateUIThread(private val byteArray: ByteArray) : Runnable {
        override fun run() {
            val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            if (streamType == 2) {
                imgPreview.setImageBitmap(bitmap)
                Thread(commThread1).interrupt()
            } else if (streamType == 3) {
                if(isStreaming) {
                    mapStream.setImageBitmap(bitmap)
                }else if(isMapping) {
                    mappingStream.setImageBitmap(bitmap)
                }
                Thread(commThread2).interrupt()
            } else if (streamType3 == 6 || streamType3 == 7) {
                mappingStream.setImageBitmap(bitmap)
                Thread(commThread3).interrupt()
            }
        }
    }


}