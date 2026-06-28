package com.pz64.rfa.data.dsp

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.IOException
import java.util.concurrent.ArrayBlockingQueue

/**
 * <h1>RF Analyzer - Scheduler</h1>
 *
 * Module:      Scheduler.kt
 * Description: This class is responsible for forwarding the samples from the input hardware
 * to the Demodulator and to the Processing Loop and at the correct speed and format.
 * Sample packets are passed to other blocks by using blocking queues. The samples passed
 * to the Demodulator will be shifted to baseband first.
 * If the Demodulator or the Processing Loop are too slow, the scheduler will automatically
 * drop incoming samples to keep the buffer of the source from being filled up.
 *
 * @author Dennis Mantz
 *
 * Copyright (C) 2025 Dennis Mantz
 * License: http://www.gnu.org/licenses/gpl.html GPL version 2 or higher
 */
class Scheduler(
    private val source: RtlsdrSource,
    private val putNewFftSamples: (FloatArray) -> Unit
) {

    companion object {
        private const val DEMOD_QUEUE_SIZE = 20
        private const val SQUELCH_DEBOUNCE_COUNT =
            50  // number of loop iterations to wait before squelch goes from true to false
        private const val LOGTAG = "Scheduler"
    }

    val demodOutputQueue: ArrayBlockingQueue<SamplePacket> =
        ArrayBlockingQueue(DEMOD_QUEUE_SIZE) // Queue that delivers samples to the Demodulator block
    val demodInputQueue: ArrayBlockingQueue<SamplePacket> =
        ArrayBlockingQueue(DEMOD_QUEUE_SIZE)  // Queue that collects used buffers from the Demodulator block

    @Volatile
    var channelFrequency: Long =
        0 // Shift frequency to this value when passing packets to demodulator
    @Volatile
    var isDemodulationActivated: Boolean =
        false // Indicates if samples should be forwarded to the demodulator queues or not.
    @Volatile
    var squelchSatisfied: Boolean =
        false // indicates whether the current signal is strong enough to cross the squelch threshold

    private val interleavedFftBuffer =
        FloatArray(source.getPacketSize() / source.getBytesPerSample() * 2)

    @Volatile
    private var stopRequested = false

    // Recording
    @Volatile
    private var isStopRecordingRequested = false
    private var bufferedOutputStream: BufferedOutputStream? = null          // Used for recording
    private var recordedFileSize: Long =
        0                                  // Number of bytes written to the recording file
    private var recordedStartTimestamp: Long =
        0                            // Timestamp of when recording was started
    private var maxRecordingTime: Long? =
        null                              // Maximum time to record (in milliseconds). null -> never stop
    private var maxRecordingFileSize: Long? =
        null                          // Maximum file size for the recording (in bytes). null -> never stop
    private var onlyWhenSquelchIsSatisfied: Boolean =
        false                 // only write samples to file when squelch is satisfied
    private var onRecordingStopped: ((finalSize: Long) -> Unit)? =
        null     // callback when recording stops (with final file size in bytes)
    private var onFileSizeUpdate: ((currentFileSize: Long) -> Unit)? =
        null // periodical callback during recording to report file size (in bytes) to ui
    private var squelchDebounceCounter: Int =
        0                             // helper counter to debounce squelch changes

    init {
        val samplesPerPacket = source.getPacketSize() / source.getBytesPerSample()
        // allocate the buffer packets.
        for (i in 0 until DEMOD_QUEUE_SIZE) demodInputQueue.offer(
            SamplePacket(samplesPerPacket)
        )
    }

    fun stopScheduler() {
        this.stopRequested = true
        source.stopSampling()
    }

    /**
     * Will stop writing samples to the bufferedOutputStream and close it.
     */
    fun stopRecording() {
        this.isStopRecordingRequested = true
        Log.i(LOGTAG, "stopRecording")
    }

    fun startRecording(
        bufferedOutputStream: BufferedOutputStream,
        onlyWhenSquelchIsSatisfied: Boolean,                     // only write samples to file when squelch is satisfied
        maxRecordingTime: Long? = null,                          // Maximum time to record (in milliseconds). null -> never stop
        maxRecordingFileSize: Long? = null,                      // Maximum file size for the recording (in bytes). null -> never stop
        onRecordingStopped: (finalSize: Long) -> Unit,           // callback when recording stops (with final file size in bytes)
        onFileSizeUpdate: (currentFileSize: Long) -> Unit
    ) {     // periodical callback during recording to report file size (in bytes) to ui
        isStopRecordingRequested = false
        recordedFileSize = 0
        recordedStartTimestamp = System.currentTimeMillis()
        this.bufferedOutputStream = bufferedOutputStream
        this.onlyWhenSquelchIsSatisfied = onlyWhenSquelchIsSatisfied
        this.maxRecordingTime = maxRecordingTime
        this.maxRecordingFileSize = maxRecordingFileSize
        this.onRecordingStopped = onRecordingStopped
        this.onFileSizeUpdate = onFileSizeUpdate
        Log.i(LOGTAG, "startRecording: Recording started.")
    }

    /**
     * Main processing loop. Suspend until cancelled or stopRequested.
     */
    suspend fun start() = withContext(Dispatchers.IO) {
        Log.i(LOGTAG, "Scheduler started.")
        var counter: Long = 0

        val nsPerPacket =
            (source.getPacketSize() / source.getBytesPerSample()) * 1_000_000_000f / source.getSampleRate()

        source.startSampling()
        try {
            while (isActive && !stopRequested) {
                // Get a new packet from the source:
                val packet = source.getPacket(1000)
                if (packet == null) {
                    if (isActive && !stopRequested) {
                        Log.e(LOGTAG, "run: No more packets from source. Shutting down...")
                        stopScheduler()
                    }
                    break
                }
                val startTimestamp = System.nanoTime()

                // Squelch debounce: When squelchSatisfied goes from true to false, wait SQUELCH_DEBOUNCE_COUNT loop iterations before actually stop demodulation/recording
                if (squelchSatisfied)
                    squelchDebounceCounter = 0
                else if (squelchDebounceCounter < SQUELCH_DEBOUNCE_COUNT)
                    squelchDebounceCounter++

                ///// Recording ////////////////////////////////////////////////////////////////////////
                if (bufferedOutputStream != null) {
                    if (squelchSatisfied || !onlyWhenSquelchIsSatisfied || squelchDebounceCounter < SQUELCH_DEBOUNCE_COUNT) {
                        try {
                            bufferedOutputStream!!.write(packet)
                            recordedFileSize += packet.size.toLong()
                        } catch (e: IOException) {
                            Log.e(
                                LOGTAG,
                                "run: Error while writing to output stream (recording): " + e.message
                            )
                            stopRecording()
                        }
                    }
                    // report file size every 100 packets:
                    if (counter % 100 == 0L) onFileSizeUpdate?.let { it(recordedFileSize) }
                    // check if recording should stop:
                    maxRecordingTime?.let {
                        if (it <= (System.currentTimeMillis() - recordedStartTimestamp)) {
                            Log.i(LOGTAG, "run: Max Recording Time reached!")
                            stopRecording()
                        }
                    }
                    maxRecordingFileSize?.let {
                        if (it <= recordedFileSize) {
                            Log.i(LOGTAG, "run: Max Recording File Size reached!")
                            stopRecording()
                        }
                    }
                    if (isStopRecordingRequested) {
                        try {
                            bufferedOutputStream!!.close()
                        } catch (e: IOException) {
                            Log.e(
                                LOGTAG,
                                "run: Error while closing output stream (recording): " + e.message
                            )
                        }
                        bufferedOutputStream = null
                        Log.i(LOGTAG, "run: Recording stopped.")
                        onRecordingStopped?.let { it(recordedFileSize) }
                    }
                    counter++
                }

                ///// Demodulation /////////////////////////////////////////////////////////////////////
                if (isDemodulationActivated && (squelchSatisfied || squelchDebounceCounter < SQUELCH_DEBOUNCE_COUNT)) {
                    // Get a buffer from the demodulator inputQueue
                    val demodBuffer = demodInputQueue.poll()
                    if (demodBuffer != null) {
                        demodBuffer.setSize(0) // mark buffer as empty
                        // fill the packet into the buffer and shift its spectrum by mixFrequency:
                        source.mixPacketIntoSamplePacket(packet, demodBuffer, channelFrequency)
                        demodOutputQueue.offer(demodBuffer) // deliver packet
                    } else {
                        Log.d(LOGTAG, "run: Flush the demod queue because demodulator is too slow!")
                        generateSequence { demodOutputQueue.poll() }
                            .forEach { demodInputQueue.offer(it) }
                    }
                }

                ///// FFT //////////////////////////////////////////////////////////////////////////////
                source.fillPacketIntoInterleavedBuffer(packet, interleavedFftBuffer)
                putNewFftSamples(interleavedFftBuffer)

                // Return the packet back to the source buffer pool:
                source.returnPacket(packet)

                // Performance Tracking:
                val processingTime = System.nanoTime() - startTimestamp
                val load = processingTime / nsPerPacket
            }
        } finally {
            stopRequested = true
            if (bufferedOutputStream != null) {
                try {
                    bufferedOutputStream!!.close()
                } catch (e: IOException) {
                    Log.e(
                        LOGTAG,
                        "run: Error while closing output stream (cleanup)(recording): " + e.message
                    )
                }
                bufferedOutputStream = null
                Log.i(LOGTAG, "run: Recording stopped (Scheduler shutting down).")
                onRecordingStopped?.let { it(recordedFileSize) }
            }
            source.stopSampling()
            Log.i(LOGTAG, "Scheduler stopped.")
        }
    }
}
