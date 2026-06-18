package com.kavach.upi.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri

class WarningAudioPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null

    /**
     * Starts playing the warning alarm in a loop.
     */
    fun startAlarm() {
        if (mediaPlayer != null) return // Already playing

        val alarmUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

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
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: If Alarm setup fails, clean references
            mediaPlayer = null
        }
    }

    /**
     * Stops the playback and cleanly releases native resources.
     */
    fun stopAlarm() {
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
}
