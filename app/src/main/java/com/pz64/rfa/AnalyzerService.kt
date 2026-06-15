package com.pz64.rfa

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@AndroidEntryPoint
class AnalyzerService: Service() {

    private val binder = LocalBinder()
    private var isBound = false

    private val lifecycleScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    override fun onBind(p0: Intent): IBinder {
        Log.i(TAG, "onBind: Service bound")
        isBound = true
        return binder
    }

    override fun onRebind(intent: Intent?) {
        Log.i(TAG, "onRebind: Service rebound")
        isBound = true
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.i(TAG, "onUnbind: Service unbound")
        isBound = false
        return super.onUnbind(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundService()
        return START_NOT_STICKY
    }

    private fun startForegroundService() {
        val notification = createNotification()
        Log.d(TAG, "startForegroundService: Moving service to foreground.")
        ServiceCompat.startForeground(this, 1, notification, FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
    }

    private fun createNotification(): Notification {
        // Intent to launch the main activity
        val activityIntent = Intent(this, RFActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val activityPendingIntent = PendingIntent.getActivity(
            this, 0, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, "SERVICE_CHANNEL")
            .setContentTitle("RFA Running")
            .setContentText("RFA is running...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(activityPendingIntent)
            .build()
    }
    inner class LocalBinder : Binder() {
        fun getService(): AnalyzerService = this@AnalyzerService
    }

    companion object {
        private const val TAG = "AnalyzerService"
    }

}