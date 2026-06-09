package com.pz64.rfa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.pz64.rfa.receivers.USBObserver
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var usbObserver: USBObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(usbObserver)
        enableEdgeToEdge()
        setContent {
        }
    }

    companion object {
        const val TAG = "MainActivity"
    }
}