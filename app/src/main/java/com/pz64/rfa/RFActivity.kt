package com.pz64.rfa

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.pz64.rfa.ui.main.MainScreenRoute
import com.pz64.rfa.ui.navigation.NavDestination
import com.pz64.rfa.ui.theme.RFATheme
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
        window.colorMode = ActivityInfo.COLOR_MODE_HDR

        setupRtlsdrDriverLauncher()

        handleNotificationPermission(onGranted = ::bindAnalyzerService)

        lifecycle.addObserver(rtlsdrUsbManager)

        rtlsdrUsbManager.onUsbConnected = {
            Log.i(TAG, "RTLSDR usb connected")
            startConnectionToDriver()
        }


        enableEdgeToEdge()
        setContent {
            RFAApp()
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
                ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED

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
        stopService(Intent(this, AnalyzerService::class.java))
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


@Composable
fun RFAApp() {
    RFATheme {
        val backStack = remember { mutableStateListOf<NavDestination>(NavDestination.Main) }

        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryProvider = { key ->
                when (key) {
                    NavDestination.Main -> NavEntry(key) {
                        MainScreenRoute()
                    }

                    NavDestination.Settings -> NavEntry(key) {
                        // SettingsScreen()
                    }
                }
            }
        )
    }
}
