package com.hyd.lidarcontrol.utils

import android.util.Log
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketAddress
import java.net.SocketTimeoutException


class SocketRequest(
    // 服务器地址
    private val mServerHost: String,
    // 服务端口号
    private var mServerPort: Int, sendMessage: String
) :
    Thread() {
    // 要发送的消息
    private val mSendMessage: String

    override fun run() {
        // TODO Auto-generated method stub
        super.run()
        val socket = Socket()
        try {
            Log.d(TAG, "请求连接到服务器...")
            val socketAddress: SocketAddress = InetSocketAddress(
                mServerHost,
                mServerPort
            )
            socket.connect(socketAddress, 5 * 1000) // 设置目标地址,请求超时限制

            // 判断是否连接成功
            if (socket.isConnected) {
                // 发送消息
                val dos = DataOutputStream(
                    socket.getOutputStream()
                )
                Log.d(TAG, "连接成功,开始发送消息,发送内容：$mSendMessage")
                dos.writeBytes(mSendMessage) // 服务区/客户端双方的写/读方式要一直,否则会报错
                //dos.flush();// 刷新输出流，使Server马上收到该字符串

                // 接收服务器消息
                val dis = DataInputStream(
                    socket.getInputStream()
                )
                Log.d(TAG, "接收到服务器反馈消息：" + dis.readByte())
                dos.close()
                dis.close()
            } else {
                Log.e(TAG, "未能成功连接至服务器！")
            }
        } catch (e: Exception) {
            // TODO Auto-generated catch block
            e.printStackTrace()
            if (e is SocketTimeoutException) {
                Log.e(TAG, "连接超时!")
            } else {
                Log.e(TAG, "通讯过程发生异常:$e")
            }
        } finally {
            try {
                socket.close()
                interrupt()
                Log.d(TAG, "关闭连接.")
            } catch (e: IOException) {
                // TODO Auto-generated catch block
                e.printStackTrace()
            }
        }
    }

    companion object {
        private val TAG = SocketRequest::class.java.simpleName
    }

    init {
        // TODO Auto-generated constructor stub
        mServerPort = mServerPort
        mSendMessage = sendMessage
    }
}