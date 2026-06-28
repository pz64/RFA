package com.pz64.rfa.data.dsp

enum class RtlsdrDirectSamplingMode(val displayName: String, val intValue: Int) {
    OFF("Off", 0),
    I("I-Branch", 1),
    Q("Q-Branch", 2)
}