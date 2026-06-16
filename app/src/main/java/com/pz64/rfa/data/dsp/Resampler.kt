package com.pz64.rfa.data.dsp

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * <h1>RF Analyzer - Resampler</h1>
 *
 * Module:      Resampler.kt
 * Description: Generalized resampler block that downsamples/upsamples the incoming
 *              signal to the desired output sample rate using a RationalResampler.
 *              Runs in its own thread like the old Decimator.
 *
 * @author Dennis Mantz
 *
 * Copyright (C) 2025 Dennis Mantz
 * License: http://www.gnu.org/licenses/gpl.html GPL version 2 or higher
 */
class Resampler(
    @Volatile var outputSampleRate: Int,
    private val packetSize: Int,
    private val inputQueue: ArrayBlockingQueue<SamplePacket>,
    private val inputReturnQueue: ArrayBlockingQueue<SamplePacket>,
    @Volatile var maxTaps: Int = 500
) {

    private var resampler: RationalResampler? = null
    private var inputRate: Int = 0
    private var lastOutputRate: Int = 0
    private var lastMaxTaps = maxTaps

    private val outputQueue = ArrayBlockingQueue<SamplePacket>(OUTPUT_QUEUE_SIZE)
    private val outputReturnQueue = ArrayBlockingQueue<SamplePacket>(OUTPUT_QUEUE_SIZE)

    init {
        repeat(OUTPUT_QUEUE_SIZE) {
            outputReturnQueue.offer(SamplePacket(packetSize))
        }
    }

    fun getResampledPacket(timeout: Int): SamplePacket? {
        return try {
            outputQueue.poll(timeout.toLong(), TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            Log.e(LOGTAG, "getPacket: Interrupted while waiting on queue")
            null
        }
    }

    fun returnResampledPacket(packet: SamplePacket) {
        outputReturnQueue.offer(packet)
    }

    /**
     * Main processing loop. Suspend until cancelled.
     */
    suspend fun run() = withContext(Dispatchers.Default) {
        Log.i(LOGTAG, "Resampler started.")

        try {
            while (isActive) {
                val inputSamples: SamplePacket = withContext(Dispatchers.IO) {
                    try {
                        inputQueue.poll(1000, TimeUnit.MILLISECONDS)
                    } catch (e: InterruptedException) {
                        null
                    }
                } ?: continue

                val inRate = inputSamples.sampleRate
                if (inRate == 0) {
                    Log.d(LOGTAG, "run: inputSamples.sampleRate == 0. skipping..")
                    withContext(Dispatchers.IO) {
                        inputReturnQueue.offer(inputSamples)
                    }
                    continue
                }

                // Check if reconfiguration of Resampler is necessary
                if (resampler == null || inRate != inputRate || outputSampleRate != lastOutputRate || maxTaps != lastMaxTaps) {
                    Log.d(
                        LOGTAG,
                        "run: (Re)creating resampler: new rates: inRate=$inRate, outRate=$outputSampleRate"
                    )
                    // Limit the maximum interpolation factor to 10000 to keep memory usage of filter bank in bounds:
                    val (interpolation, decimation) = RationalResampler.limitDenominator(
                        outputSampleRate,
                        inRate,
                        10000
                    )
                    val error =
                        abs(outputSampleRate.toDouble() / inRate - interpolation.toDouble() / decimation)
                    Log.d(
                        LOGTAG,
                        "run: (Re)creating resampler: interpolation=$interpolation, decimation=$decimation (error=$error or ${(outputSampleRate * error).toInt()} Sps)"
                    )
                    resampler = RationalResampler(
                        interpolation,
                        decimation,
                        maxTaps = maxTaps
                    ) // limiting tap count to max. 500 per FirFilter
                    inputRate = inRate
                    lastOutputRate = outputSampleRate
                    lastMaxTaps = maxTaps
                }

                var inputBufferIndex = 0 // start processing from the beginning of the input packet
                var totalProcessingTimeNs = 0L
                while (inputBufferIndex < inputSamples.size() && isActive) {
                    // Grab output buffer
                    val outputSamples: SamplePacket? = withContext(Dispatchers.IO) {
                        try {
                            outputReturnQueue.poll(1000, TimeUnit.MILLISECONDS)
                        } catch (e: InterruptedException) {
                            null
                        }
                    }

                    if (outputSamples == null) {
                        Log.d(
                            LOGTAG,
                            "run: No packets from outputReturnQueue. Skipping input packet."
                        )
                        break
                    }

                    outputSamples.setSize(0) // mark as empty
                    val remainingSamples = inputSamples.size() - inputBufferIndex
                    val startTimestamp = System.nanoTime()
                    val consumed = resampler!!.resample(
                        inputSamples,
                        outputSamples,
                        inputBufferIndex,
                        remainingSamples
                    )
                    totalProcessingTimeNs += System.nanoTime() - startTimestamp
                    outputSamples.sampleRate =
                        outputSampleRate  // set the desired output sample rate

                    withContext(Dispatchers.IO) {
                        outputQueue.offer(outputSamples)
                    }
                    inputBufferIndex += consumed
                }

                // performance tracking
                val nsPerPacket = inputSamples.size() * 1_000_000_000f / inputSamples.sampleRate

                withContext(Dispatchers.IO) {
                    inputReturnQueue.offer(inputSamples)
                }
            }
        } finally {
            Log.i(LOGTAG, "Resampler stopped.")
        }
    }

    companion object {
        private const val LOGTAG = "Resampler"
        private const val OUTPUT_QUEUE_SIZE = 2 // double buffer
    }
}
