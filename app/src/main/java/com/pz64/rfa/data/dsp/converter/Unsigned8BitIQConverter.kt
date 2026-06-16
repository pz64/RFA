package com.pz64.rfa.data.dsp.converter

import com.pz64.rfa.data.dsp.SamplePacket
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * <h1>RF Analyzer - unsigned 8-bit IQ Converter</h1>
 * 
 * Module:      Unsigned8BitIQConverter.java
 * Description: This class implements methods to convert the raw input data of IQ sources (8 bit unsigned)
 * to SamplePackets. It has also methods to do converting and down-mixing at the same
 * time.
 * 
 * @author Dennis Mantz
 * 
 * Copyright (C) 2025 Dennis Mantz
 * License: http://www.gnu.org/licenses/gpl.html GPL version 2 or higher
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
class Unsigned8BitIQConverter : IQConverter() {
    override fun generateLookupTable() {
        /**
         * The rtl_sdr delivers samples in the following format:
         * The bytes are interleaved, 8-bit, unsigned IQ samples (in-phase
         * component first, followed by the quadrature component):
         * 
         * [--------- first sample ----------]   [-------- second sample --------]
         * I                  Q                  I                Q ...
         * receivedBytes[0]   receivedBytes[1]   receivedBytes[2]       ...
         */

        lookupTable = FloatArray(256)
        for (i in 0..255) lookupTable!![i] = (i - 127.4f) / 128.0f
    }

    override fun generateMixerLookupTable(mixFrequency: Int) {
        // If mix frequency is too low, just add the sample rate (sampled spectrum is periodic):
        var mixFrequency = mixFrequency
        if (mixFrequency == 0 || (sampleRate / abs(mixFrequency) > MAX_COSINE_LENGTH)) mixFrequency += sampleRate

        // Only generate lookupTable if null or invalid:
        if (cosineRealLookupTable == null || mixFrequency != cosineFrequency) {
            cosineFrequency = mixFrequency
            val bestLength = calcOptimalCosineLength()
            cosineRealLookupTable = Array<FloatArray?>(bestLength) { FloatArray(256) }
            cosineImagLookupTable = Array<FloatArray?>(bestLength) { FloatArray(256) }
            var cosineAtT: Float
            var sineAtT: Float
            for (t in 0..<bestLength) {
                cosineAtT = cos(2 * Math.PI * cosineFrequency * t / sampleRate.toFloat()).toFloat()
                sineAtT = sin(2 * Math.PI * cosineFrequency * t / sampleRate.toFloat()).toFloat()
                for (i in 0..255) {
                    cosineRealLookupTable!![t]!![i] = (i - 127.4f) / 128.0f * cosineAtT
                    cosineImagLookupTable!![t]!![i] = (i - 127.4f) / 128.0f * sineAtT
                }
            }
            cosineIndex = 0
        }
    }

    override fun fillPacketIntoSamplePacket(
        packet: ByteArray,
        samplePacket: SamplePacket
    ): Int {
        val capacity: Int = samplePacket.capacity()
        var count = 0
        val startIndex: Int = samplePacket.size()
        if (startIndex >= capacity) return 0 // SamplePacket is already full

        val re: FloatArray = samplePacket.re()
        val im: FloatArray = samplePacket.im()
        var i = 0
        while (i < packet.size) {
            re[startIndex + count] = lookupTable!![packet[i].toInt() and 0xff]
            im[startIndex + count] = lookupTable!![packet[i + 1].toInt() and 0xff]
            count++
            if (startIndex + count >= capacity) break
            i += 2
        }
        samplePacket.setSize(samplePacket.size() + count) // update the size of the sample packet
        samplePacket.sampleRate = sampleRate // update the sample rate
        samplePacket.frequency = frequency // update the frequency
        return count
    }

    override fun fillPacketIntoInterleavedBuffer(
        packet: ByteArray,
        interleavedBuffer: FloatArray
    ): Boolean {
        if (interleavedBuffer.size < packet.size) return false
        for (i in packet.indices) {
            interleavedBuffer[i] = lookupTable!![packet[i].toInt() and 0xff]
        }
        return true
    }

    override fun mixPacketIntoSamplePacket(
        packet: ByteArray,
        samplePacket: SamplePacket,
        channelFrequency: Long
    ): Int {
        val mixFrequency = (frequency - channelFrequency).toInt()

        generateMixerLookupTable(mixFrequency) // will only generate table if really necessary

        // Mix the samples from packet and store the results in the samplePacket
        val capacity: Int = samplePacket.capacity()
        var count = 0
        val startIndex: Int = samplePacket.size()
        if (startIndex >= capacity) return 0 // SamplePacket is already full

        if (cosineRealLookupTable!!.size == 0) return 0 // Lookup Table is empty/invalid

        if (cosineIndex >= cosineRealLookupTable!!.size) cosineIndex =
            0 // Potentially reset cosine index if table was altered and now is smaller

        val re: FloatArray = samplePacket.re()
        val im: FloatArray = samplePacket.im()
        var i = 0
        while (i < packet.size) {
            re[startIndex + count] =
                cosineRealLookupTable!![cosineIndex]!![packet[i].toInt() and 0xff] - cosineImagLookupTable!![cosineIndex]!![packet[i + 1].toInt() and 0xff]
            im[startIndex + count] =
                cosineRealLookupTable!![cosineIndex]!![packet[i + 1].toInt() and 0xff] + cosineImagLookupTable!![cosineIndex]!![packet[i].toInt() and 0xff]
            cosineIndex = (cosineIndex + 1) % cosineRealLookupTable!!.size
            count++
            if (startIndex + count >= capacity) break
            i += 2
        }
        samplePacket.setSize(samplePacket.size() + count) // update the size of the sample packet
        samplePacket.sampleRate = sampleRate // update the sample rate
        samplePacket.frequency = frequency // update the frequency
        return count
    }
}
