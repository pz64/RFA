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
import com.pz64.rfa.data.dsp.Demodulator
import com.pz64.rfa.data.dsp.FftProcessor
import com.pz64.rfa.data.dsp.FftProcessorData
import com.pz64.rfa.data.dsp.RtlsdrDirectSamplingMode
import com.pz64.rfa.data.dsp.RtlsdrSource
import com.pz64.rfa.data.dsp.Scheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AnalyzerService : Service() {

    private val binder = LocalBinder()
    private var isBound = false

    private val serviceScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    val fftProcessorData = FftProcessorData()

    private var source: RtlsdrSource? = null
    private var scheduler: Scheduler? = null
    private var fftProcessor: FftProcessor? = null
    private var demodulator: Demodulator? = null

    private val iqSourceActions: RtlsdrSource.Callback = object : RtlsdrSource.Callback {
        override fun onIQSourceReady(source: RtlsdrSource?) {
            Log.i(TAG, "onIQSourceReady: Source is ready, starting processing.")
            startProcessing()
        }

        override fun onIQSourceError(source: RtlsdrSource?, message: String?) {
            val errorMessage = "Error with Source (${source?.getName()}): $message"
            Log.e(TAG, "onIQSourceError: $errorMessage")
            serviceScope.launch {
                stopAnalyzer()
            }
        }
    }

    @Inject
    lateinit var rtlsdrUsbManager: RLTSDRUsbManager

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
        isBound = false
        return true // keep running in background
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundService()
        startAnalyzer()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopAnalyzer()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startForegroundService() {
        val notification = createNotification()
        Log.d(TAG, "startForegroundService: Moving service to foreground.")
        ServiceCompat.startForeground(this, 1, notification, FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
    }

    fun startAnalyzer() {
        if (source != null) {
            Log.w(TAG, "startAnalyzer: Analyzer is already running.")
            return
        }

        val currentSource = createRtlsdrSource()
        this.source = currentSource

        if (!currentSource.open(this, callback = iqSourceActions)) {
            Log.e(TAG, "startAnalyzer: Failed to open source.")
            source = null
        }
    }

    private fun startProcessing() {
        val currentSource = source ?: return

        val processor = FftProcessor(
            initialFftSize = 16384,
            initialSampleRate = Constants.RTLSDR.supportedSampleRates.first().toLong(),
            initialFrequency = Constants.RTLSDR.defaultFrequency,
            initialFftFramesPerSecond = 30f,
            initialChannelFrequencyRange = Pair(
                Constants.RTLSDR.defaultFrequency - Constants.RTLSDR.defaultDemodulationMode.defaultChannelWidth,
                Constants.RTLSDR.defaultFrequency + Constants.RTLSDR.defaultDemodulationMode.defaultChannelWidth
            ),
            fftProcessorData = fftProcessorData,
            waterfallBufferSize = 400,
            fftPeakHold = false,
            onAverageSignalStrengthChanged = {
                // Log.d(TAG, "startAnalyzer: Average signal strength changed: $it")
            },
            scope = serviceScope
        )
        this.fftProcessor = processor
        processor.start()

        val sched = Scheduler(currentSource) { fftSamples ->
            processor.putNewFftSamples(fftSamples)
        }
        this.scheduler = sched

        val demod = Demodulator(
            inputQueue = sched.demodOutputQueue,
            inputReturnQueue = sched.demodInputQueue,
            packetSize = currentSource.getPacketSize() / currentSource.getBytesPerSample()
        )
        this.demodulator = demod

        demod.audioVolumeLevel = 0.33f

        serviceScope.launch {
            demod.start()
        }

        serviceScope.launch {
            sched.start()
        }
    }

    fun stopAnalyzer() {
        Log.i(TAG, "stopAnalyzer: Stopping all components.")
        scheduler?.stopScheduler()
        fftProcessor?.stopLoop()
        source?.close()

        scheduler = null
        fftProcessor = null
        source = null
        demodulator = null
    }

    private fun createRtlsdrSource(): RtlsdrSource {
        return RtlsdrSource(
            Constants.RTLSDR.DriverApp.path,
            Constants.RTLSDR.DriverApp.port
        ).apply {
            isAllowOutOfBoundFrequency = true
            setFrequency(Constants.RTLSDR.defaultFrequency)
            setSampleRate(Constants.RTLSDR.supportedSampleRates[0])
            setFrequencyCorrection(0)
            setAutomaticGainControl(true)
            setDirectSamplingMode(RtlsdrDirectSamplingMode.OFF.intValue)
            setManualGain(false)
        }
    }

    private fun createNotification(): Notification {
        val activityIntent = Intent(this, RFActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val activityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
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