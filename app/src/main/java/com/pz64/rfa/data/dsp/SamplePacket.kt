package com.pz64.rfa.data.dsp

import kotlin.math.min

/**
 * <h1>RF Analyzer - Sample Packet</h1>
 * 
 * Module:      SamplePacket.java
 * Description: This class encapsulates a packet of complex samples.
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
class SamplePacket {
    private val re: FloatArray // real values
    private val im: FloatArray // imag values
    /**
     * @return center frequency at which these samples where recorded
     */
    /**
     * Sets the center frequency for this sample packet
     * @param frequency        center frequency at which these samples were recorded
     */
    var frequency: Long // center frequency
    /**
     * @return sample rate at which these samples were recorded
     */
    /**
     * Sets the sample rate for this sample packet
     * @param sampleRate        sample rate at which these samples were recorded
     */
    var sampleRate: Int // sample rate
    private var size: Int // number of samples in this packet

    /**
     * Constructor. This constructor wraps existing arrays and allows to set the
     * number of samples in this packet to something smaller than the array length
     * 
     * @param re            array of real parts of the sample values
     * @param im            array of imaginary parts of the sample values
     * @param frequency        center frequency
     * @param sampleRate    sample rate
     * @param size    number of samples in this packet ( <= arrays.length )
     */
    /**
     * Constructor. This constructor wraps existing arrays and set the number of
     * samples to the length of the arrays
     * 
     * @param re            array of real parts of the sample values
     * @param im            array of imaginary parts of the sample values
     * @param frequency        center frequency
     * @param sampleRate    sample rate
     */
    @JvmOverloads
    constructor(
        re: FloatArray,
        im: FloatArray?,
        frequency: Long,
        sampleRate: Int,
        size: Int = re.size
    ) {
        require(re.size == im!!.size) { "Arrays must be of the same length" }
        require(size <= re.size) { "Size must be of the smaller or equal the array length" }
        this.re = re
        this.im = im
        this.frequency = frequency
        this.sampleRate = sampleRate
        this.size = size
    }

    /**
     * Constructor. This constructor allocates two fresh arrays
     * 
     * @param size    Number of samples in this packet
     */
    constructor(size: Int) {
        this.re = FloatArray(size)
        this.im = FloatArray(size)
        this.frequency = 0
        this.sampleRate = 0
        this.size = 0
    }

    /**
     * @return the reference to the array of real parts
     */
    fun re(): FloatArray {
        return re
    }

    /**
     * Returns the real part at the specified index
     * 
     * @param i        index
     * @return real part of the sample with the given index
     */
    fun re(i: Int): Float {
        return re[i]
    }

    /**
     * @return the reference to the array of imaginary parts
     */
    fun im(): FloatArray {
        return im
    }

    /**
     * Returns the imaginary part at the specified index
     * 
     * @param i        index
     * @return imaginary part of the sample with the given index
     */
    fun im(i: Int): Float {
        return im[i]
    }

    /**
     * @return the length of the arrays
     */
    fun capacity(): Int {
        return re.size
    }

    /**
     * @return number of samples in this packet
     */
    fun size(): Int {
        return size
    }

    /**
     * Sets a new size (number of samples in this packet)
     * @param size    number of (valid) samples in this packet
     */
    fun setSize(size: Int) {
        this.size = min(size, re.size)
    }
}
