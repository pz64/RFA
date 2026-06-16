package com.pz64.rfa.data.dsp

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

/**
 * <h1>RF Analyzer - Audio Sink</h1>
 *
 * Module:      AudioSink.kt
 * Description: This class implements the interface to the systems audio API.
 *              It will run in a coroutine and buffer incoming sample packets
 *              in a blocking queue. Input packets are demodulated (real) signals.
 *              This class will decimate the incoming sample rate according to the
 *              audio rate.
 *
 * @author Dennis Mantz
 *
 * Copyright (C) 2025 Dennis Mantz
 * License: http://www.gnu.org/licenses/gpl.html GPL version 2 or higher
 */
class AudioSink(
    private val packetSize: Int,
    private val sampleRate: Int,
    private val lowPerformanceMode: Boolean
) {
    private var audioTrack: AudioTrack? = null
    private val inputQueue: ArrayBlockingQueue<SamplePacket>
    private val outputQueue: ArrayBlockingQueue<SamplePacket>
    private val audioFilter1: FirFilter?
    private val audioFilter2: FirFilter?
    private val tmpAudioSamples: SamplePacket

    companion object {
        private const val QUEUE_SIZE = 2
        private const val LOGTAG = "AudioSink"
    }

    init {
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_FLOAT
        )
        var audioBufferSize = minBufferSize
        var queueSize = QUEUE_SIZE
        if (lowPerformanceMode) {
            audioBufferSize = minBufferSize * 10
            queueSize = 10
        }

        // Create the queues and fill them
        inputQueue = ArrayBlockingQueue(queueSize)
        outputQueue = ArrayBlockingQueue(queueSize)
        for (i in 0 until queueSize) {
            outputQueue.offer(SamplePacket(packetSize))
        }

        // Create an instance of the AudioTrack class:
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(audioAttributes)
            .setAudioFormat(format)
            .setBufferSizeInBytes(audioBufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        // Create the audio filters:
        audioFilter1 = FirFilter.createLowPass(2, 1.0f, 1.0f, 0.1f, 0.15f, 30.0f)
        Log.d(
            LOGTAG,
            "constructor: created audio filter 1 with ${audioFilter1?.numberOfTaps} Taps."
        )
        audioFilter2 = FirFilter.createLowPass(4, 1.0f, 1.0f, 0.1f, 0.1f, 30.0f)
        Log.d(
            LOGTAG,
            "constructor: created audio filter 2 with ${audioFilter2?.numberOfTaps} Taps."
        )
        tmpAudioSamples = SamplePacket(packetSize)
    }

    /**
     * The AudioSink allocates the buffers for audio playback. Use this method to request
     * a free buffer. This method will block if no buffer is available.
     *
     * @param timeout    max time this method will block
     * @return free buffer or null if no buffer available
     */
    fun getPacketBuffer(timeout: Int): SamplePacket? {
        return try {
            outputQueue.poll(timeout.toLong(), TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            Log.e(LOGTAG, "getPacketBuffer: Interrupted. return null...")
            null
        }
    }

    /**
     * Enqueues a packet buffer for being played on the audio track.
     *
     * @param packet    the packet buffer from getPacketBuffer() filled with samples
     * @return true if success, false if error
     */
    fun enqueuePacket(packet: SamplePacket?): Boolean {
        if (packet == null) {
            Log.e(LOGTAG, "enqueuePacket: Packet is null.")
            return false
        }
        if (!inputQueue.offer(packet)) {
            Log.e(LOGTAG, "enqueuePacket: Queue is full.")
            return false
        }
        return true
    }

    suspend fun run() = withContext(Dispatchers.Default) {
        val tempPacket = SamplePacket(packetSize)
        var filteredPacket: SamplePacket

        Log.i(LOGTAG, "AudioSink started.")

        if (lowPerformanceMode) {
            Log.i(LOGTAG, "AudioSink is in low performance mode. Wait for queue to fill up.")
            while (inputQueue.size < inputQueue.remainingCapacity() && isActive) {
                delay(100.milliseconds)
            }
        }

        if (!isActive) return@withContext

        // start audio playback:
        audioTrack?.play()

        try {
            // Continuously write the data from the queue to the audio track:
            while (isActive) {
                // Get the next packet from the queue
                val packet = withContext(Dispatchers.IO) {
                    try {
                        inputQueue.poll(1000, TimeUnit.MILLISECONDS)
                    } catch (e: InterruptedException) {
                        null
                    }
                }

                if (packet == null) {
                    if (lowPerformanceMode) {
                        Log.i(
                            LOGTAG,
                            "[low performance mode] AudioSink queue is empty. Wait for queue to fill up."
                        )
                        while (inputQueue.size < inputQueue.remainingCapacity() && isActive) {
                            delay(100.milliseconds)
                        }
                    }
                    continue
                }

                // apply audio filter (decimation)
                if (packet.sampleRate > sampleRate) {
                    applyAudioFilter(packet, tempPacket)
                    filteredPacket = tempPacket
                } else {
                    filteredPacket = packet
                }

                // Write it to the audioTrack:
                var samplesWritten = 0
                while (samplesWritten < filteredPacket.size() && isActive) {
                    val finalSamplesWritten = samplesWritten
                    val written = withContext(Dispatchers.IO) {
                        audioTrack?.write(
                            filteredPacket.re(),
                            finalSamplesWritten,
                            filteredPacket.size() - finalSamplesWritten,
                            AudioTrack.WRITE_NON_BLOCKING
                        ) ?: 0
                    }
                    if (written < 0) {
                        Log.e(LOGTAG, "run: Error writing to AudioTrack: $written")
                        break
                    }
                    samplesWritten += written
                }

                // Return the buffer to the output queue
                withContext(Dispatchers.IO) {
                    outputQueue.offer(packet)
                }
            }
        } finally {
            // stop audio playback:
            try {
                audioTrack?.stop()
            } catch (e: Exception) {
                Log.e(LOGTAG, "Error stopping AudioTrack: ${e.message}")
            }
            Log.i(LOGTAG, "AudioSink stopped.")
        }
    }

    /**
     * Will filter the real array contained in input and decimate them to the audio rate.
     *
     * @param input        incoming (unfiltered) samples at the incoming rate (quadrature rate)
     * @param output    outgoing (filtered, decimated) samples at audio rate
     */
    private fun applyAudioFilter(input: SamplePacket, output: SamplePacket) {
        val filter1 = audioFilter1
        val filter2 = audioFilter2

        // if we need a decimation of 8: apply first and second filter (decimate to input_rate/8)
        if (input.sampleRate / sampleRate == 8) {
            if (filter1 == null || filter2 == null) {
                Log.e(LOGTAG, "applyAudioFilter: Filters not initialized for decimation by 8.")
                return
            }
            // apply first filter (decimate to input_rate/2)
            tmpAudioSamples.setSize(0) // mark buffer as empty
            if (filter1.filterReal(input, tmpAudioSamples, 0, input.size()) < input.size()) {
                Log.e(
                    LOGTAG,
                    "applyAudioFilter: [audioFilter1] could not filter all samples from input packet."
                )
            }

            // apply second filter (decimate to input_rate/8)
            output.setSize(0)
            if (filter2.filterReal(
                    tmpAudioSamples,
                    output,
                    0,
                    tmpAudioSamples.size()
                ) < tmpAudioSamples.size()
            ) {
                Log.e(
                    LOGTAG,
                    "applyAudioFilter: [audioFilter2] could not filter all samples from input packet."
                )
            }
        } else if (input.sampleRate / sampleRate == 2) {
            if (filter1 == null) {
                Log.e(LOGTAG, "applyAudioFilter: filter1 not initialized for decimation by 2.")
                return
            }
            // apply first filter (decimate to input_rate/2 )
            output.setSize(0)
            if (filter1.filterReal(input, output, 0, input.size()) < input.size()) {
                Log.e(
                    LOGTAG,
                    "applyAudioFilter: [audioFilter1] could not filter all samples from input packet."
                )
            }
        } else {
            Log.e(
                LOGTAG,
                "applyAudioFilter: incoming sample rate is not supported: ${input.sampleRate}"
            )
        }
    }
}
