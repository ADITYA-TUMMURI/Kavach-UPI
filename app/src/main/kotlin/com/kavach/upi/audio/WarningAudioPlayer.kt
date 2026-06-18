package com.kavach.upi.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class WarningAudioPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var toneGenerator: ToneGenerator? = null
    private var isToneFallbackActive = false

    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private var isAlarmRunning = false
    private val mainHandler = Handler(Looper.getMainLooper())

    private val sharedPreferences = context.getSharedPreferences("kavach_settings", Context.MODE_PRIVATE)

    private val warningTextEn = "Attention! Kavach security shield is active. Screen sharing detected during payment. Please disconnect the call immediately to protect your bank account."
    private val warningTextHi = "ध्यान दें! कवच सुरक्षा कवच सक्रिय है। भुगतान के दौरान स्क्रीन शेयरिंग का पता चला है। कृपया अपने बैंक खाते को सुरक्षित रखने के लिए तुरंत कॉल काट दें।"

    init {
        initializeTts()
    }

    private fun initializeTts() {
        try {
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isTtsInitialized = true
                    setupTtsListener()
                    // If startAlarm was called before initialization completed, start speaking now
                    if (isAlarmRunning) {
                        speakCurrentWarning()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupTtsListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            
            override fun onDone(utteranceId: String?) {
                if (isAlarmRunning) {
                    mainHandler.postDelayed({
                        if (isAlarmRunning) {
                            speakCurrentWarning()
                        }
                    }, 1500) // 1.5s delay before repeating the message
                }
            }

            override fun onError(utteranceId: String?) {
                // If TTS fails during playback, fallback to alarm sound
                mainHandler.post {
                    startAlarmFallback()
                }
            }
        })
    }

    /**
     * Starts playing the warning alarm/TTS warning in a loop based on settings.
     */
    fun startAlarm() {
        if (isAlarmRunning) return
        isAlarmRunning = true

        val alertType = sharedPreferences.getString("pref_alert_type", "VOICE") ?: "VOICE"

        when (alertType) {
            "ALARM" -> {
                startAlarmFallback()
            }
            "BOTH" -> {
                startAlarmFallback()
                triggerTtsSpeech()
            }
            "VOICE" -> {
                triggerTtsSpeech()
            }
        }
    }

    private fun triggerTtsSpeech() {
        if (isTtsInitialized) {
            speakCurrentWarning()
        } else {
            // TTS not ready yet, initialize or fallback temporarily
            initializeTts()
            startAlarmFallback()
        }
    }

    private fun speakCurrentWarning() {
        val language = sharedPreferences.getString("pref_alert_language", "en") ?: "en"
        val text = if (language == "hi") warningTextHi else warningTextEn
        val locale = if (language == "hi") Locale("hi", "IN") else Locale.US

        tts?.let {
            it.language = locale
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "KavachWarningUtterance")
            }
            it.speak(text, TextToSpeech.QUEUE_FLUSH, params, "KavachWarningUtterance")
        }
    }

    private fun startAlarmFallback() {
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
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD)
        } catch (e: Exception) {
            e.printStackTrace()
            isToneFallbackActive = false
        }
    }

    /**
     * Stops the playback and cleanly stops alarms/TTS.
     */
    fun stopAlarm() {
        isAlarmRunning = false
        mainHandler.removeCallbacksAndMessages(null)
        
        try {
            tts?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        releaseMediaPlayer()
        releaseToneGenerator()
    }

    /**
     * Shuts down the TTS engine and releases resources (call in service onDestroy).
     */
    fun shutdown() {
        stopAlarm()
        try {
            tts?.shutdown()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            tts = null
            isTtsInitialized = false
        }
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

