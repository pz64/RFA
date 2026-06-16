package com.pz64.rfa.data.dsp.converter

import com.pz64.rfa.data.dsp.SamplePacket
import kotlin.math.abs

/**
 * <h1>RF Analyzer - IQ Converter</h1>
 *
 * Module:      IQConverter.java
 * Description: This class implements methods to convert the raw input data of IQ sources (bytes)
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
abstract class IQConverter {
    var frequency: Long =
        0 // Baseband frequency of the converted samples (is put into the SamplePacket)
    var sampleRate: Int = 0 // Sample rate of the converted samples (is put into the SamplePacket)
        set(value) {
            if (field != value) {
                field = value
                this.cosineFrequency =
                    -1 // invalidate cosineFrequency. This causes generateMixerLookupTable() to recalculate the lut on next iteration.
            }
        }
    protected var lookupTable: FloatArray? = null // Lookup table to transform IQ bytes into doubles
    protected var cosineRealLookupTable: Array<FloatArray?>? =
        null // Lookup table to transform IQ bytes into frequency shifted doubles
    protected var cosineImagLookupTable: Array<FloatArray?>? =
        null // Lookup table to transform IQ bytes into frequency shifted doubles
    protected var cosineFrequency: Int = 0 // Frequency of the cosine that is mixed to the signal
    protected var cosineIndex: Int = 0 // current index within the cosine

    init {
        generateLookupTable()
    }

    protected fun calcOptimalCosineLength(): Int {
        if (sampleRate <= 0 || cosineFrequency == 0) {
            return 1 // sampleRate or cosineFrequency == 0 is invalid; return 1 to not break anything
        }
        // look for the best fitting array size to hold one or more full cosine cycles:
        val cycleLength = sampleRate / abs(cosineFrequency.toDouble())
        var bestLength = cycleLength.toInt()
        var bestLengthError = abs(bestLength - cycleLength)
        var i = 1
        while (i * cycleLength < MAX_COSINE_LENGTH) {
            if (abs(i * cycleLength - (i * cycleLength).toInt()) < bestLengthError) {
                bestLength = (i * cycleLength).toInt()
                bestLengthError = abs(bestLength - (i * cycleLength))
            }
            i++
        }
        return bestLength
    }

    abstract fun fillPacketIntoSamplePacket(packet: ByteArray, samplePacket: SamplePacket): Int
    abstract fun fillPacketIntoInterleavedBuffer(
        packet: ByteArray,
        interleavedBuffer: FloatArray
    ): Boolean

    abstract fun mixPacketIntoSamplePacket(
        packet: ByteArray,
        samplePacket: SamplePacket,
        channelFrequency: Long
    ): Int

    protected abstract fun generateLookupTable()

    protected abstract fun generateMixerLookupTable(mixFrequency: Int)


    companion object {
        protected const val MAX_COSINE_LENGTH: Int = 500 // Max length of the cosine lookup table
    }
}