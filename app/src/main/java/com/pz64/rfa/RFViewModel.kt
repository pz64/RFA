package com.pz64.rfa

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel

import javax.inject.Inject

@HiltViewModel
class RFAViewModel @Inject constructor(private val rtlsdrUsbManager: RLTSDRUsbManager) :
    ViewModel() {
    val connectionState = rtlsdrUsbManager.isConnected
}