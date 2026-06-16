package com.pz64.rfa.data.dsp

import android.util.Log
import kotlin.math.cos
import kotlin.math.sin

/**
 * <h1>RF Analyzer - complex FIR Filter</h1>
 * 
 * Module:      ComplexFirFilter.java
 * Description: This class implements a FIR filter with complex taps. Most of the code is
 * copied from the firdes and firfilter module from GNU Radio.
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
class ComplexFirFilter private constructor(
    tapsReal: FloatArray,
    tapsImag: FloatArray,
    decimation: Int,
    gain: Float,
    sampleRate: Float,
    lowCutOffFrequency: Float,
    highCutOffFrequency: Float,
    transitionWidth: Float,
    attenuation: Float
) {
    private var tapCounter = 0
    private val tapsReal: FloatArray
    private val tapsImag: FloatArray
    private val delaysReal: FloatArray
    private val delaysImag: FloatArray
    val decimation: Int
    private var decimationCounter = 1
    val gain: Float
    val sampleRate: Float
    val lowCutOffFrequency: Float
    val highCutOffFrequency: Float
    val transitionWidth: Float
    val attenuation: Float

    /**
     * Private Constructor. Creates a new complex FIR Filter with the given taps and decimation.
     * Use create*Filter() to calculate taps and create the filter.
     * @param tapsReal                filter taps real part
     * @param tapsImag                filter taps imaginary part
     * @param decimation            decimation factor
     * @param gain                    filter pass band gain
     * @param sampleRate            sample rate
     * @param lowCutOffFrequency    lower cut off frequency (start of pass band)
     * @param highCutOffFrequency    upper cut off frequency (end of pass band)
     * @param transitionWidth        width from end of pass band to start stop band
     * @param attenuation            attenuation of stop band
     */
    init {
        require(tapsReal.size == tapsImag.size) { "real and imag filter taps have to be of the same length!" }
        this.tapsReal = tapsReal
        this.tapsImag = tapsImag
        this.delaysReal = FloatArray(tapsReal.size)
        this.delaysImag = FloatArray(tapsImag.size)
        this.decimation = decimation
        this.gain = gain
        this.sampleRate = sampleRate
        this.lowCutOffFrequency = lowCutOffFrequency
        this.highCutOffFrequency = highCutOffFrequency
        this.transitionWidth = transitionWidth
        this.attenuation = attenuation
    }

    val numberOfTaps: Int
        /**
         * @return length of the taps array
         */
        get() = tapsReal.size

    /**
     * Filters the samples from the input sample packet and appends filter output to the output
     * sample packet. Stops automatically if output sample packet is full.
     * @param in        input sample packet
     * @param out        output sample packet
     * @param offset    offset to use as start index for the input packet
     * @param length    max number of samples processed from the input packet
     * @return number of samples consumed from the input packet
     */
    fun filter(`in`: SamplePacket, out: SamplePacket, offset: Int, length: Int): Int {
        var index: Int
        var indexOut = out.size()
        val outputCapacity = out.capacity()
        val reIn = `in`.re()
        val imIn = `in`.im()
        val reOut = out.re()
        val imOut = out.im()

        // insert each input sample into the delay line:
        for (i in 0..<length) {
            delaysReal[tapCounter] = reIn[offset + i]
            delaysImag[tapCounter] = imIn[offset + i]

            // Calculate the filter output for every Mth element (were M = decimation)
            if (decimationCounter == 0) {
                // first check if we have enough space in the output buffers:
                if (indexOut == outputCapacity) {
                    out.setSize(indexOut) // update size of output sample packet
                    out.sampleRate =
                        `in`.sampleRate / decimation // update the sample rate of the output sample packet
                    return i // We return the number of consumed samples from the input buffers
                }

                // Calculate the results:
                reOut[indexOut] = 0f
                imOut[indexOut] = 0f
                index = tapCounter
                for (j in tapsReal.indices) {
                    reOut[indexOut] += tapsReal[j] * delaysReal[index] - tapsImag[j] * delaysImag[index]
                    imOut[indexOut] += tapsImag[j] * delaysReal[index] + tapsReal[j] * delaysImag[index]
                    index--
                    if (index < 0) index = tapsReal.size - 1
                }

                // increase indexOut:
                indexOut++
            }

            // update counters:
            decimationCounter++
            if (decimationCounter >= decimation) decimationCounter = 0
            tapCounter++
            if (tapCounter >= tapsReal.size) tapCounter = 0
        }
        out.setSize(indexOut) // update size of output sample packet
        out.sampleRate =
            `in`.sampleRate / decimation // update the sample rate of the output sample packet
        return length // We return the number of consumed samples from the input buffers
    }

    companion object {
        private const val LOGTAG = "ComplexFirFilter"

        /**
         * FROM GNU Radio firdes::band_pass_2:
         * 
         * Will calculate the tabs for the specified low pass filter and return a FirFilter instance
         * 
         * @param decimation            decimation factor
         * @param gain                    filter pass band gain
         * @param sampling_freq            sample rate
         * @param low_cutoff_freq        cut off frequency (beginning of pass band)
         * @param high_cutoff_freq        cut off frequency (end of pass band)
         * @param transition_width        width from end of pass band to start stop band
         * @param attenuation_dB        attenuation of stop band
         * @param maxTaps            Maximum number of Filter Taps (0 = no limit)
         * @return instance of FirFilter
         */
        /**
         * FROM GNU Radio firdes::band_pass_2:
         * 
         * Will calculate the tabs for the specified low pass filter and return a FirFilter instance
         * 
         * @param decimation            decimation factor
         * @param gain                    filter pass band gain
         * @param sampling_freq            sample rate
         * @param low_cutoff_freq        cut off frequency (beginning of pass band)
         * @param high_cutoff_freq        cut off frequency (end of pass band)
         * @param transition_width        width from end of pass band to start stop band
         * @param attenuation_dB        attenuation of stop band
         * @return instance of FirFilter
         */
        @JvmOverloads
        fun createBandPass(
            decimation: Int,
            gain: Float,
            sampling_freq: Float,  // Hz
            low_cutoff_freq: Float,  // Hz BEGINNING of transition band
            high_cutoff_freq: Float,  // Hz END of transition band
            transition_width: Float,  // Hz width of transition band
            attenuation_dB: Float,  // attenuation dB
            maxTaps: Int = 0 // Tap count will be limited by maxTaps. Value '0' means no limit
        ): ComplexFirFilter? {
            if (sampling_freq <= 0.0) {
                Log.e(LOGTAG, "createBandPass: firdes check failed: sampling_freq > 0")
                return null
            }

            if (low_cutoff_freq < sampling_freq * -0.5 || high_cutoff_freq > sampling_freq * 0.5) {
                Log.e(
                    LOGTAG,
                    "createBandPass: firdes check failed: -sampling_freq / 2 < fa <= sampling_freq / 2"
                )
                return null
            }

            if (low_cutoff_freq >= high_cutoff_freq) {
                Log.e(
                    LOGTAG,
                    "createBandPass: firdes check failed: low_cutoff_freq >= high_cutoff_freq"
                )
                return null
            }

            if (transition_width <= 0) {
                Log.e(LOGTAG, "createBandPass: firdes check failed: transition_width > 0")
                return null
            }

            // Calculate number of tabs
            // Based on formula from Multirate Signal Processing for
            // Communications Systems, fredric j harris
            var ntaps = (attenuation_dB * sampling_freq / (22.0 * transition_width)).toInt()
            if (maxTaps > 0 && ntaps > maxTaps) ntaps = maxTaps
            if ((ntaps and 1) == 0)  // if even...
                ntaps++ // ...make odd


            // construct the truncated ideal impulse response
            // [sin(x)/x for the low pass case]
            // Note: we calculate the real taps for a low pass and shift them
            val low_pass_cut_off = (high_cutoff_freq - low_cutoff_freq) / 2f
            val tapsLowPass = FloatArray(ntaps)
            val w: FloatArray = makeWindow(ntaps)

            val M = (ntaps - 1) / 2
            val fwT0 = 2 * Math.PI.toFloat() * low_pass_cut_off / sampling_freq
            for (n in -M..M) {
                if (n == 0) tapsLowPass[n + M] = fwT0 / Math.PI.toFloat() * w[n + M]
                else {
                    // a little algebra gets this into the more familiar sin(x)/x form
                    tapsLowPass[n + M] =
                        sin((n * fwT0).toDouble()).toFloat() / (n * Math.PI.toFloat()) * w[n + M]
                }
            }

            // find the factor to normalize the gain, fmax.
            // For low-pass, gain @ zero freq = 1.0
            var fmax = tapsLowPass[0 + M]
            for (n in 1..M) fmax += 2 * tapsLowPass[n + M]
            val actualGain = gain / fmax // normalize
            for (i in 0..<ntaps) tapsLowPass[i] *= actualGain

            // calc the band pass taps:
            val tapsReal = FloatArray(ntaps)
            val tapsImag = FloatArray(ntaps)
            val freq = Math.PI.toFloat() * (high_cutoff_freq + low_cutoff_freq) / sampling_freq
            var phase = -freq * (ntaps / 2)

            for (i in 0..<ntaps) {
                tapsReal[i] = tapsLowPass[i] * cos(phase.toDouble()).toFloat()
                tapsImag[i] = tapsLowPass[i] * sin(phase.toDouble()).toFloat()
                phase += freq
                //Log.d(LOGTAG, "createBandPass: Filter Taps [i="+i+"]: " + tapsReal[i] + "   " + tapsImag[i]);
            }

            return ComplexFirFilter(
                tapsReal,
                tapsImag,
                decimation,
                gain,
                sampling_freq,
                low_cutoff_freq,
                high_cutoff_freq,
                transition_width,
                attenuation_dB
            )
        }

        /**
         * Creates a Blackman Window for a FIR Filter
         * 
         * @param ntabs number of taps of the filter
         * @return window samples
         */
        private fun makeWindow(ntabs: Int): FloatArray {
            // Make a blackman window:
            // w(n)=0.42-0.5cos{(2*PI*n)/(N-1)}+0.08cos{(4*PI*n)/(N-1)};
            val window = FloatArray(ntabs)
            for (i in window.indices) window[i] =
                (0.42f - 0.5f * cos(2 * Math.PI * i / (ntabs - 1)).toFloat()
                        + 0.08f * cos(4 * Math.PI * i / (ntabs - 1)).toFloat())
            return window
        }
    }
}