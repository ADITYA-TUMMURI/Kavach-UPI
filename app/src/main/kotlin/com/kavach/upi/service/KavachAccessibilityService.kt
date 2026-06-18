package com.kavach.upi.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.kavach.upi.audio.WarningAudioPlayer
import com.kavach.upi.detection.ThreatSignatureStore
import com.kavach.upi.overlay.ThreatOverlayManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class KavachAccessibilityService : AccessibilityService() {

    private var serviceJob = SupervisorJob()
    private var serviceScope = CoroutineScope(serviceJob + Dispatchers.Default)

    private lateinit var overlayManager: ThreatOverlayManager
    private lateinit var audioPlayer: WarningAudioPlayer

    private val pollingIntervalMs = 2000L

    override fun onCreate() {
        super.onCreate()
        overlayManager = ThreatOverlayManager(this)
        audioPlayer = WarningAudioPlayer(this)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        
        // Start continuous background polling fallback loop
        startPollingThreats()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return

        // Dispatch threat assessment to background thread
        serviceScope.launch {
            assessThreat(packageName)
        }
    }

    override fun onInterrupt() {
        // Required callback for accessibility service interrupt scenarios
    }

    /**
     * Periodically inspects active windows to catch threats running in the background.
     */
    private fun startPollingThreats() {
        serviceScope.launch {
            while (serviceJob.isActive) {
                ensureActive()
                delay(pollingIntervalMs)

                // Retrieve all interactive window layers across the screen
                val currentWindows = windows
                var threatFound = false

                for (window in currentWindows) {
                    val rootNode = window.root ?: continue
                    val pkgName = rootNode.packageName?.toString()
                    if (ThreatSignatureStore.isThreatPackage(pkgName)) {
                        threatFound = true
                        break
                    }
                }

                if (threatFound) {
                    escalateThreat()
                } else if (!isAnyEventThreatActive()) {
                    // Only lower threat escalation if no event-driven threat remains
                    deescalateThreat()
                }
            }
        }
    }

    /**
     * Checks if a package name is a threat and triggers UI overlays accordingly.
     */
    private suspend fun assessThreat(packageName: String) {
        if (ThreatSignatureStore.isThreatPackage(packageName)) {
            escalateThreat()
        } else {
            deescalateThreat()
        }
    }

    /**
     * Deploys the visual overlay blocker and sounds the alarm on the main UI thread.
     */
    private suspend fun escalateThreat() {
        withContext(Dispatchers.Main) {
            if (!overlayManager.isOverlayActive()) {
                overlayManager.showOverlay()
                audioPlayer.startAlarm()
            }
        }
    }

    /**
     * Dismisses the visual overlay blocker and halts the alarm on the main UI thread.
     */
    private suspend fun deescalateThreat() {
        withContext(Dispatchers.Main) {
            if (overlayManager.isOverlayActive()) {
                overlayManager.hideOverlay()
                audioPlayer.stopAlarm()
            }
        }
    }

    /**
     * Helper to verify if any running window context matches signature store.
     */
    private fun isAnyEventThreatActive(): Boolean {
        val currentWindows = windows
        for (window in currentWindows) {
            val rootNode = window.root ?: continue
            if (ThreatSignatureStore.isThreatPackage(rootNode.packageName?.toString())) {
                return true
            }
        }
        return false
    }

    override fun onDestroy() {
        // Cancel the supervisor job to stop the polling loop and prevent leaks
        serviceJob.cancel()
        serviceScope.cancel()

        // Clean up resources immediately
        if (this::overlayManager.isInitialized) {
            overlayManager.hideOverlay()
        }
        if (this::audioPlayer.isInitialized) {
            audioPlayer.stopAlarm()
        }

        super.onDestroy()
    }
}
