package com.pz64.rfa

import androidx.core.net.toUri
import com.pz64.rfa.data.DemodulationMode

object Constants {
    object RTLSDR {
        val ids = setOf( // from https://github.com/osmocom/rtl-sdr/blob/master/rtl-sdr.rules
            Pair(0x0bda, 0x2832), Pair(0x0bda, 0x2838), Pair(0x0413, 0x6680), Pair(0x0413, 0x6f0f), Pair(0x0458, 0x707f), Pair(0x0ccd, 0x00a9),
            Pair(0x0ccd, 0x00b3), Pair(0x0ccd, 0x00b4), Pair(0x0ccd, 0x00b5), Pair(0x0ccd, 0x00b7), Pair(0x0ccd, 0x00b8), Pair(0x0ccd, 0x00b9),
            Pair(0x0ccd, 0x00c0), Pair(0x0ccd, 0x00c6), Pair(0x0ccd, 0x00d3), Pair(0x0ccd, 0x00d7), Pair(0x0ccd, 0x00e0), Pair(0x1554, 0x5020),
            Pair(0x15f4, 0x0131), Pair(0x15f4, 0x0133), Pair(0x185b, 0x0620), Pair(0x185b, 0x0650), Pair(0x185b, 0x0680), Pair(0x1b80, 0xd393),
            Pair(0x1b80, 0xd394), Pair(0x1b80, 0xd395), Pair(0x1b80, 0xd397), Pair(0x1b80, 0xd398), Pair(0x1b80, 0xd39d), Pair(0x1b80, 0xd3a4),
            Pair(0x1b80, 0xd3a8), Pair(0x1b80, 0xd3af), Pair(0x1b80, 0xd3b0), Pair(0x1d19, 0x1101), Pair(0x1d19, 0x1102), Pair(0x1d19, 0x1103),
            Pair(0x1d19, 0x1104), Pair(0x1f4d, 0xa803), Pair(0x1f4d, 0xb803), Pair(0x1f4d, 0xc803), Pair(0x1f4d, 0xd286), Pair(0x1f4d, 0xd803)
        )

        val defaultFrequency = 97000000L
        val supportedSampleRates = listOf(1000000, 2000000, 2500000)

        val defaultDemodulationMode = DemodulationMode.WFM


        object DriverApp {
            const val packageName = "marto.rtl_tcp_andro"
            const val className = "com.sdrtouch.rtlsdr.DeviceOpenActivity"

            const val path = "127.0.0.1"
            const val port = 1234
            val url = "iqsrc://-a $path  -p $port -n 1 -T 0".toUri()
        }
    }
}