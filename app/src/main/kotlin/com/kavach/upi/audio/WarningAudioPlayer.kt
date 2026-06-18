package com.kavach.upi.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri

class WarningAudioPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var toneGenerator: ToneGenerator? = null
    private var isToneFallbackActive = false

    /**
     * Starts playing the warning alarm in a loop.
     */
    fun startAlarm() {
        if (mediaPlayer != null || isToneFallbackActive) {
            return // Already playing
        }

        val alarmUri: Uri? = try {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        } catch (e: Exception) {
            null
        }

        if (alarmUri != null) {
            try {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(context, alarmUri)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    isLooping = true
                    prepare()
                    start()
                }
                return // Successfully started
            } catch (e: Exception) {
                e.printStackTrace()
                releaseMediaPlayer()
            }
        }

        // Fallback: Use ToneGenerator to beep continuously
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            isToneFallbackActive = true
            // Start a separate thread or rely on periodic start calls.
            // For ToneGenerator, we play a continuous tone or beep.
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD)
        } catch (e: Exception) {
            e.printStackTrace()
            isToneFallbackActive = false
        }
    }

    /**
     * Stops the playback and cleanly releases native resources.
     */
    fun stopAlarm() {
        releaseMediaPlayer()
        releaseToneGenerator()
    }

    private fun releaseMediaPlayer() {
        val player = mediaPlayer ?: return
        try {
            if (player.isPlaying) {
                player.stop()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                player.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            mediaPlayer = null
        }
    }

    private fun releaseToneGenerator() {
        try {
            toneGenerator?.stopTone()
            toneGenerator?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            toneGenerator = null
            isToneFallbackActive = false
        }
    }
}
