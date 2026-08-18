package com.heydar.filemanager.core.network

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.heydar.filemanager.R
import com.heydar.filemanager.domain.model.network.NetworkClientInfo
import com.heydar.filemanager.domain.model.network.NetworkServerState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.net.Inet4Address
import java.net.NetworkInterface

@AndroidEntryPoint
class ServerForegroundService : Service() {

    private var server: LocalHttpServer? = null
    private val _serverState = MutableStateFlow(NetworkServerState())
    val serverState: StateFlow<NetworkServerState> = _serverState.asStateFlow()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val port = intent.getIntExtra(EXTRA_PORT, 8080)
                val rootPath = intent.getStringExtra(EXTRA_ROOT_PATH)
                val password = intent.getStringExtra(EXTRA_PASSWORD)
                startServer(port, rootPath, password)
            }
            ACTION_STOP -> {
                stopServer()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopServer()
        super.onDestroy()
    }

    private fun startServer(port: Int, rootPath: String?, password: String?) {
        try {
            val actualRoot = rootPath ?: android.os.Environment.getExternalStorageDirectory().absolutePath
            server = LocalHttpServer(this, port, actualRoot, password).apply {
                setClientListener(object : LocalHttpServer.ClientListener {
                    override fun onClientConnected(client: NetworkClientInfo) {
                        _serverState.update { state ->
                            state.copy(
                                connectedClients = (state.connectedClients + client).distinctBy { it.ip }
                            )
                        }
                    }

                    override fun onClientDisconnected(ip: String) {
                        _serverState.update { state ->
                            state.copy(
                                connectedClients = state.connectedClients.filter { it.ip != ip }
                            )
                        }
                    }
                })
                start()
            }

            val ip = getLocalIpAddress() ?: "0.0.0.0"
            _serverState.update {
                NetworkServerState(
                    isRunning = true,
                    ipAddress = ip,
                    port = port,
                    rootPath = actualRoot,
                    passwordEnabled = !password.isNullOrEmpty(),
                    password = password ?: ""
                )
            }

            startForeground(NOTIFICATION_ID, buildNotification(ip, port))
            Log.d(TAG, "Server started on $ip:$port")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start server", e)
            _serverState.update {
                it.copy(error = "Failed to start server: ${e.message}")
            }
        }
    }

    private fun stopServer() {
        server?.stop()
        server = null
        _serverState.update { NetworkServerState() }
        Log.d(TAG, "Server stopped")
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "File Server",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "File Manager server running"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(ip: String, port: Int): Notification {
        val stopIntent = Intent(this, ServerForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("File Manager Server")
            .setContentText("Serving files at $ip:$port")
            .setSmallIcon(R.drawable.ic_folder)
            .setOngoing(true)
            .addAction(R.drawable.ic_nav_close, "Stop", stopPendingIntent)
            .build()
    }

    companion object {
        private const val TAG = "ServerForegroundService"
        private const val CHANNEL_ID = "file_server_channel"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.heydar.filemanager.START_SERVER"
        const val ACTION_STOP = "com.heydar.filemanager.STOP_SERVER"
        const val EXTRA_PORT = "extra_port"
        const val EXTRA_ROOT_PATH = "extra_root_path"
        const val EXTRA_PASSWORD = "extra_password"

        fun getLocalIpAddress(): String? {
            try {
                val interfaces = NetworkInterface.getNetworkInterfaces()
                while (interfaces.hasMoreElements()) {
                    val networkInterface = interfaces.nextElement()
                    val addresses = networkInterface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val address = addresses.nextElement()
                        if (!address.isLoopbackAddress && address is Inet4Address) {
                            return address.hostAddress
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get IP address", e)
            }
            return null
        }
    }
}
