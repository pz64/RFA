package com.pz64.rfa.ui.main

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.pz64.rfa.AnalyzerService
import com.pz64.rfa.Constants
import com.pz64.rfa.RLTSDRUsbManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RFActivity : ComponentActivity() {

    private val rfaViewModel: RFAViewModel by viewModels()

    @Inject
    lateinit var rtlsdrUsbManager: RLTSDRUsbManager

    private lateinit var rtlsdrDriverLauncher: ActivityResultLauncher<Intent>

    private var analyzerService: AnalyzerService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as AnalyzerService.LocalBinder
            analyzerService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            analyzerService = null
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupRtlsdrDriverLauncher()

        handleNotificationPermission(onGranted = ::bindAnalyzerService)

        lifecycle.addObserver(rtlsdrUsbManager)

        rtlsdrUsbManager.onUsbConnected = {
            Log.i(TAG, "RTLSDR usb connected")
            startConnectionToDriver()
        }


        enableEdgeToEdge()
        setContent {
        }
    }

    override fun onStart() {
        super.onStart()

    }


    override fun onStop() {
        unbindAnalyzerService()
        super.onStop()
    }


    private fun handleNotificationPermission(onGranted: () -> Unit) {
        val permission = Manifest.permission.POST_NOTIFICATIONS
        val needsRequest = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED

        if (needsRequest) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) onGranted()
            }.launch(permission)
        } else {
            onGranted()
        }
    }

    private fun bindAnalyzerService() {
        val intent = Intent(this, AnalyzerService::class.java)
        startForegroundService(intent)
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)
    }

    private fun unbindAnalyzerService() {
        unbindService(serviceConnection)
    }

    private fun setupRtlsdrDriverLauncher() {
        rtlsdrDriverLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    Log.i(TAG, "RTL-SDR Driver connection successful")
                } else {
                    Log.i(TAG, "Function… connection failed.")
                }
            }

    }

    private fun startConnectionToDriver() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setClassName(
                Constants.RTLSDR.DriverApp.packageName,
                Constants.RTLSDR.DriverApp.className
            )
            data = Constants.RTLSDR.DriverApp.serverPath
        }
        rtlsdrDriverLauncher.launch(intent)
    }


    companion object {
        const val TAG = "MainActivity"
    }
}