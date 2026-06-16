package com.pz64.rfa

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RLTSDRUsbManager @Inject constructor(
    @ApplicationContext val context: Context
) : BroadcastReceiver() {


    private val _isConnected = MutableStateFlow(isRtlSdrConnected())
    val isConnected = _isConnected.asStateFlow()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
            val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
            } else {
                // else branch necessary as long as minSdk is < 33 (tiramisu)
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
            }
            device?.let { device ->
                Log.d(
                    RFActivity.TAG,
                    "Device attached: ${device.deviceName} (vendor: ${device.vendorId}, product: ${device.productId}, name: ${device.manufacturerName}|${device.productName})"
                )
                if (Pair(device.vendorId, device.productId) in Constants.RTLSDR.ids) {
                    Toast.makeText(
                        context,
                        "RTL-SDR Device '${device.productName}' attached.",
                        Toast.LENGTH_SHORT
                    ).show()
                    if (device.vendorId == 0x0bda && device.productId == 0x2838 && device.productName == "Blog V4") {
                        Log.i(
                            RFActivity.TAG,
                            "usbBroadcastReceiver:onReceive: RTL-SDR Blog V4 attached!"
                        )
                    }
                    _isConnected.value = true
                }
            }
        } else if (intent.action == UsbManager.ACTION_USB_DEVICE_DETACHED) {
            val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
            } else {
                // else branch necessary as long as minSdk is < 33 (tiramisu)
                @Suppress("DEPRECATION")
                intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
            }
            device?.let {
                _isConnected.value = false
                Log.d(
                    RFActivity.TAG,
                    "usbBroadcastReceiver:onReceive: Device detached (${device.vendorId}:${device.productId} - ${device.productName})"
                )
                if (device.vendorId == 0x0bda && device.productId == 0x2838 && device.productName == "Blog V4") {
                    Log.i(
                        RFActivity.TAG,
                        "usbBroadcastReceiver:onReceive: RTL-SDR Blog V4 detached!"
                    )
                }
            }
        }
    }

    fun isRtlSdrConnected(): Boolean {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        // deviceList is a Snapshot of currently connected devices
        return usbManager.deviceList.values.any { device ->
            Pair(device.vendorId, device.productId) in Constants.RTLSDR.ids
        }
    }


    init {
        val filter = IntentFilter()
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        context.registerReceiver(this, filter)
    }
}