import java.net.ServerSocket
import java.net.Socket
import java.io.*
import java.io.BufferedWriter
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.PrintStream
import java.io.PrintWriter
import java.net.InetAddress
import java.net.UnknownHostException
import java.nio.ByteBuffer
import java.awt.image.BufferedImage


fun server1() {
    val server = ServerSocket(7230)
    val client = server.accept()
    while (true) {

        val output = PrintWriter(client.getOutputStream(), true)
        val input = BufferedReader(InputStreamReader(client.inputStream))
        val inputText = input.readLine()
        if (inputText == null || inputText == "") continue

        when (inputText[0]) {
            'x' -> {
                output.println("$inputText")
                println("timestamp received: $inputText")
                println("timestamp sent: $inputText\n")
            }
            'c' -> {
                val status = runtimeExec("./start_gazebo_rviz.sh", 1)
                output.println("c$status")
                println("exec starting cores...($inputText)")
                println("result: c$status\n")
            }
            'p' -> {
                val status = runtimeExec("./preview.sh", 0)
                if (status == "OK") {
                    output.println("p$status")

                    val fileName = "gazebo_rviz_preview.jpg"
                    val file = File(fileName)

                    val image: BufferedImage = javax.imageio.ImageIO.read(file)
                    val baos = ByteArrayOutputStream()
                    javax.imageio.ImageIO.write(image, "bmp", baos)
                    val array = baos.toByteArray()

                    val out: OutputStream = client.getOutputStream()
                    val dos = DataOutputStream(out)
                    dos.writeInt(array.size)
                    dos.write(array, 0, array.size)
                    dos.flush()

                    println("exec capturing preview...($inputText)")
                    println("result: $status\n")
                }
            }
            else -> {
                output.println("0")
            }
        }

    }
}


fun server2() {
    val server = ServerSocket(7231)
    val client = server.accept()
    var streamInit = false
    while (true) {

        val output = PrintWriter(client.getOutputStream(), true)
        val input = BufferedReader(InputStreamReader(client.inputStream))
        val inputText = input.readLine()
        if (inputText == null || inputText == "") continue

        when (inputText[0]) {
            'm' -> {
                if (inputText[1] == '0') {
                    if (!streamInit) {
                        val status = runtimeExec("./map_stream.sh", 0)
                        if (status == "OK") {
                            println("map stream init OK\n")
                            output.println("0")
                            streamInit = true
                        }
                    } else {
                        val status2 = runtimeExec("./map_stream_cont.sh", 0)
                        if (status2 == "OK") {
                            output.println("m$status2")

                            val fileName = "rviz_stream/map_stream.jpg"
                            val file = File(fileName)

                            val image: BufferedImage = javax.imageio.ImageIO.read(file)
                            val baos = ByteArrayOutputStream()
                            javax.imageio.ImageIO.write(image, "bmp", baos)
                            val array = baos.toByteArray()

                            val out: OutputStream = client.getOutputStream()
                            val dos = DataOutputStream(out)
                            dos.writeInt(array.size)
                            dos.write(array, 0, array.size)
                            dos.flush()

                            println("exec map streaming...($inputText)")
                            println("result: $status2\n")
                        }
                    }
                } else {
                    streamInit = false
                }
            }
            'z' -> {
                output.println("z000")
                output.flush()
                println("socket restore...($inputText)")
            }
            else -> {
                output.println("0")
            }
        }

    }
}

fun server3() {
    val server = ServerSocket(7232)
    val client = server.accept()
    var teleopInit = false
    var velocity = 0
    var angular = 0
    var completion = 0
    while (true) {

        val output = PrintWriter(client.getOutputStream(), true)
        val input = BufferedReader(InputStreamReader(client.inputStream))
        val inputText = input.readLine()
        if (inputText == null || inputText == "") continue
        when (inputText[0]) {
            't' -> {
                if (!teleopInit) {
                    val status = runtimeExec("./teleop.sh", 0)
                    if (status == "OK") {
                        println("teleop init OK\n")
                        output.println("0")
                        output.flush()
                        teleopInit = true
                    }
                } else {
                    println("move: $inputText[1]\n")
                    when (inputText[1]) {
                        'w' -> {
                            velocity++
                            runtimeExec("./teleop_control.sh w", 1)
                        }
                        'x' -> {
                            velocity--
                            runtimeExec("./teleop_control.sh x", 1)
                        }
                        'a' -> {
                            angular--
                            runtimeExec("./teleop_control.sh a", 1)
                        }
                        'd' -> {
                            angular++
                            runtimeExec("./teleop_control.sh d", 1)
                        }
                        's' -> {
                            velocity = 0
                            angular = 0
                            runtimeExec("./teleop_control.sh s", 1)
                        }
                        else -> {
                        }
                    }
                    if (velocity > 22) {
                        velocity = 22
                    }
                    if (velocity < -22) {
                        velocity = -22
                    }
                    if (angular > 28) {
                        velocity = 28
                    }
                    if (velocity < -28) {
                        velocity = -28
                    }

                    output.println("t$velocity&$angular&$completion")
                    output.flush()
                }

            }
            else -> {
                output.println("0")
            }
        }

    }
}

fun server4() {
    val server = ServerSocket(7233)
    val client = server.accept()
    while (true) {

        val output = PrintWriter(client.getOutputStream(), true)
        val input = BufferedReader(InputStreamReader(client.inputStream))
        val inputText = input.readLine()
        if (inputText == null || inputText == "") continue

        when (inputText[0]) {
            'n' -> {
                val status = runtimeExec("./mapping.sh", 0)
                if (status == "OK") {
                    output.println("n$status")

                    val fileName = "map.png"
                    val file = File(fileName)

                    val image: BufferedImage = javax.imageio.ImageIO.read(file)
                    val baos = ByteArrayOutputStream()
                    javax.imageio.ImageIO.write(image, "bmp", baos)
                    val array = baos.toByteArray()

                    val out: OutputStream = client.getOutputStream()
                    val dos = DataOutputStream(out)
                    dos.writeInt(array.size)
                    dos.write(array, 0, array.size)
                    dos.flush()

                    println("exec mapping result...($inputText)")
                    println("result: $status\n")
                }
            }
            'l' -> {
                val msgTemp = inputText.substring(1).split("&")
                val positionX = (msgTemp[0].toFloat()).toString()
                val positionY = (msgTemp[1].toFloat()).toString()
                
            }
            else -> {
                output.println("0")
            }
        }

    }
}

fun runtimeExec(command: String, ignoreOutput: Int): String {
    try {
        // Run a shell command
        val process = Runtime.getRuntime().exec(command)

        if (ignoreOutput == 0) {
            val reader = BufferedReader(
                InputStreamReader(process.inputStream)
            )

            val line = reader.readText()
            println(line)

            val exitVal = process.waitFor()
            if (exitVal == 0) {
                return "OK"
                //exitProcess(0)
            } else {
                return "Failed"
                //else...
            }
        } else {
            return "OK"
        }

    } catch (e: IOException) {
        e.printStackTrace()
    } catch (e: InterruptedException) {
        e.printStackTrace()
    }
    return "Failed"
}

fun main() {
    Thread { server1() }.start()
    Thread { server2() }.start()
    Thread { server3() }.start()
    Thread { server4() }.start()
}