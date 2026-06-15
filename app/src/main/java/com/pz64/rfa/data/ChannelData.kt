package com.pz64.rfa.data

import java.util.UUID

data class ChannelData(
    val channelName: String,
    val modulation: String,
    val frequency: Float,
    val id: String = UUID.randomUUID().toString()
)
