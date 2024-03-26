package com.suda.androidscrcpy

import android.app.Application
import android.content.pm.ActivityInfo
import android.hardware.SensorManager
import android.view.MotionEvent
import android.view.Surface
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.suda.androidscrcpy.control.ControlEventMessage
import com.suda.androidscrcpy.control.TouchEventMessage
import com.suda.androidscrcpy.decoder.VideoDecoder
import com.suda.androidscrcpy.model.ByteUtils
import com.suda.androidscrcpy.model.MediaPacket
import com.suda.androidscrcpy.model.VideoPacket
import com.suda.androidscrcpy.utils.ADBUtils
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

class ScrcpyVM(app: Application) : AndroidViewModel(app) {

    private val mUpdateAvailable = AtomicBoolean(false)
    private val mLetServerRunning = AtomicBoolean(true)
    private var mVideoDecoder: VideoDecoder? = null

    private val _rotationLiveData = MutableLiveData<Int>()
    val rotationLiveData: LiveData<Int> = _rotationLiveData

    var mScreenWidth = 0
        private set
    var mScreenHeight = 0
        private set
    private var mFirstTime = true
    private lateinit var mDevice: String
    private val mActionQueue = ConcurrentLinkedQueue<ControlEventMessage>()
    private var streamSettings: VideoPacket.StreamSettings? = null

    private val mPause = AtomicBoolean(false)

    var mSurface: Surface? = null
        set(value) {
            if (field != null) {
                field = value
                mUpdateAvailable.set(true)
            } else {
                field = value
            }
        }

    fun pause() {
        mPause.set(true)
    }

    fun resume(surface: Surface) {
        mSurface = surface
        mPause.set(false)
    }

    fun init(device: String) {
        if (!mFirstTime) {
            return
        }
        this.mDevice = device
        ADBUtils.exec(
            "adb.bin-arm",
            "-s",
            device.toString(),
            "push",
            "${ADBUtils.binPath}/scrcpy-server.jar",
            ADBUtils.SC_DEVICE_SERVER_PATH
        )

        ADBUtils.exec(
            "adb.bin-arm",
            "-s",
            device.toString(),
            "reverse",
            "localabstract:scrcpy",
            "tcp:5005"
        )

        val maxSize = 1080
        computeScreenInfo(maxSize)
        start()
        Thread {
            ADBUtils.exec2(
                "adb.bin-arm",
                "-s",
                device,
                "shell",
                "CLASSPATH=/data/local/tmp/scrcpy-server.jar app_process / com.genymobile.scrcpy.Server 1.0 max_size=$maxSize",
            )
        }.start()
    }

    fun offerTouchEvent(touchEvent: MotionEvent, surfaceViewW: Int, surfaceViewH: Int) {
        for (i in 0 until touchEvent.pointerCount) {
            mActionQueue.offer(
                TouchEventMessage(
                    touchEvent,
                    surfaceViewW,
                    surfaceViewH,
                    mScreenWidth,
                    mScreenHeight,
                    i
                )
            )
        }
    }


    override fun onCleared() {
        super.onCleared()
        ADBUtils.exec(
            "adb.bin-arm",
            "-s",
            mDevice.toString(),
            "reverse",
            "--remove-all"
        )
        mLetServerRunning.set(false)
        runCatching {
            mVideoDecoder?.stop()
        }
    }

    private fun computeScreenInfo(maxSize: Int) {
        val wh = ADBUtils.exec(
            "adb.bin-arm",
            "-s",
            mDevice.toString(),
            "shell",
            "wm size"
        ).replace("Physical size: ", "").split("x")

        var w: Int = wh[0].trimStart().toInt() and 7.inv() // in case it's not a multiple of 8
        var h: Int = wh[1].toInt() and 7.inv()
        if (maxSize > 0) {
            val portrait = h > w
            var major = if (portrait) h else w
            var minor = if (portrait) w else h
            if (major > maxSize) {
                val minorExact = minor * maxSize / major
                // +4 to round the value to the nearest multiple of 8
                minor = minorExact + 4 and 7.inv()
                major = maxSize
            }
            w = if (portrait) minor else major
            h = if (portrait) major else minor
        }
        mScreenWidth = w
        mScreenHeight = h
    }

    private fun start() {
        val thread = Thread { startConnection() }
        thread.start()
    }

    private fun startConnection() {
        mVideoDecoder = VideoDecoder()
        mVideoDecoder!!.start()
        var dataInputStream: DataInputStream
        var dataOutputStream: DataOutputStream
        var socket: Socket? = null
        var controlSocket: Socket? = null
        var serverSocket: ServerSocket? = null
        try {
            serverSocket = ServerSocket(5005)
            socket = serverSocket.accept()
            controlSocket = serverSocket.accept()
            dataInputStream = DataInputStream(socket.getInputStream())
            dataOutputStream = DataOutputStream(controlSocket.getOutputStream())


            while (mLetServerRunning.get()) {
                try {
                    handleSendEvent(dataOutputStream)
                    decodeVideo(dataInputStream)
                } catch (e: IOException) {
                } catch (e2: Exception) {
                    e2.printStackTrace()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            if (socket != null) {
                try {
                    socket.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun loadNewRotation(isLand: Boolean) {
        val temp = mScreenHeight + mScreenWidth
        mScreenWidth =
            if (isLand) Math.max(mScreenWidth, mScreenHeight) else Math.min(
                mScreenWidth,
                mScreenHeight
            )
        mScreenHeight = temp - mScreenWidth
        if (isLand) {
            _rotationLiveData.postValue(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        } else {
            _rotationLiveData.postValue(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        }
    }

    @Throws
    private fun handleSendEvent(dataOutputStream: DataOutputStream) {
        try {
            var event = mActionQueue.poll()
            while (event != null) {
                val bytes = event.makeEvent()
                dataOutputStream.write(bytes, 0, bytes.size)
                event = mActionQueue.poll()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Throws
    private fun decodeVideo(dataInputStream: DataInputStream) {
        if (dataInputStream.available() > 0) {
            val packetSize = ByteArray(4)
            dataInputStream.readFully(packetSize, 0, 4)
            val size = ByteUtils.bytesToInt(packetSize)
            val packet = ByteArray(size)
            dataInputStream.readFully(packet, 0, size)
            val videoPacket = VideoPacket.fromArray(packet)
            if (videoPacket.type == MediaPacket.Type.VIDEO) {
                val data = videoPacket.data
                if (videoPacket.flag == VideoPacket.Flag.CONFIG || mUpdateAvailable.get()) {
                    if (!mUpdateAvailable.get()) {
                        streamSettings = VideoPacket.getStreamSettings(data)
                        loadNewRotation(false)
                        if (!mFirstTime) {
                            while (!mUpdateAvailable.get()) {
                                // Waiting for new surface
                                try {
                                    Thread.sleep(100)
                                } catch (e: InterruptedException) {
                                    e.printStackTrace()
                                }
                            }
                        }

                    }
                    mUpdateAvailable.set(false)
                    mFirstTime = false
                    mVideoDecoder!!.configure(
                        mSurface,
                        mScreenWidth,
                        mScreenHeight,
                        streamSettings!!.sps,
                        streamSettings!!.pps
                    )
                } else if (videoPacket.flag == VideoPacket.Flag.END) {
                    // need close stream
                } else {
                    if (!mPause.get()) {
                        mVideoDecoder!!.decodeSample(
                            data,
                            0,
                            data.size,
                            0,
                            videoPacket.flag.flag.toInt()
                        )
                    }

                }
            }
        }
    }

}
