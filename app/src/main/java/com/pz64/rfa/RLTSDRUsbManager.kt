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
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class RLTSDRUsbManager @Inject constructor(
    @ApplicationContext val context: Context
): DefaultLifecycleObserver, BroadcastReceiver()   {

    var onUsbConnected = {}

    override fun onReceive(context: Context, intent: Intent) {
        if(intent.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
            val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
            } else {
                // else branch necessary as long as minSdk is < 33 (tiramisu)
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
            }
            device?.let {
                Log.d(RFActivity.TAG, "Device attached: ${it.deviceName} (vendor: ${device.vendorId}, product: ${device.productId}, name: ${device.manufacturerName}|${device.productName})")
                if(Pair(it.vendorId, it.productId) in Constants.RTLSDR.ids) {
                    Toast.makeText( context, "RTL-SDR Device '${device.productName}' attached.", Toast.LENGTH_SHORT).show()
                    if (device.vendorId == 0x0bda && device.productId == 0x2838 && device.productName == "Blog V4") {
                            Log.i(RFActivity.TAG, "usbBroadcastReceiver:onReceive: RTL-SDR Blog V4 attached!")
                    }
                    onUsbConnected()
                }
            }
        } else if(intent.action == UsbManager.ACTION_USB_DEVICE_DETACHED) {
            val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
            } else {
                // else branch necessary as long as minSdk is < 33 (tiramisu)
                @Suppress("DEPRECATION")
                intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
            }
            device?.let {
                Log.d(RFActivity.TAG, "usbBroadcastReceiver:onReceive: Device detached (${device.vendorId}:${device.productId} - ${device.productName})")
                if (device.vendorId == 0x0bda && device.productId == 0x2838 && device.productName == "Blog V4") {
                    Log.i(RFActivity.TAG, "usbBroadcastReceiver:onReceive: RTL-SDR Blog V4 detached!")
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

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        if (isRtlSdrConnected()) {
            onUsbConnected()
        }
    }

    override fun onStart(owner: LifecycleOwner) {

        val filter = IntentFilter()
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        context.registerReceiver(this, filter)


    }

    override fun onStop(owner: LifecycleOwner) {
        context.unregisterReceiver(this)
    }
}