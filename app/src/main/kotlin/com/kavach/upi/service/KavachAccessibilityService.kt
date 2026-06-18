package com.kavach.upi.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.kavach.upi.audio.WarningAudioPlayer
import com.kavach.upi.detection.ThreatSignatureStore
import com.kavach.upi.detection.UpiTargetRegistry
import com.kavach.upi.detection.PaymentScreenVerifier
import com.kavach.upi.overlay.ThreatOverlayManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

class KavachAccessibilityService : AccessibilityService() {

    private var serviceJob = SupervisorJob()
    private var serviceScope = CoroutineScope(serviceJob + Dispatchers.Default)

    private lateinit var overlayManager: ThreatOverlayManager
    private lateinit var audioPlayer: WarningAudioPlayer

    // State tracking flags
    private val isUpiContextActive = AtomicBoolean(false)
    private var lastProcessedPackage: String? = null
    private var lastProcessedTime: Long = 0

    // Consecutives clean checks needed for threat clearance to prevent flicker
    private var consecutiveCleanChecks = 0

    // System package names to ignore
    private val systemPackages = setOf(
        "com.android.systemui",
        "com.google.android.inputmethod.latin",
        "android",
        "com.android.launcher3"
    )

    override fun onCreate() {
        super.onCreate()
        overlayManager = ThreatOverlayManager(this)
        audioPlayer = WarningAudioPlayer(this)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        
        // Start continuous background dual-gear polling fallback loop
        startPollingThreats()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // 1. Event Type Gate
        val eventType = event.eventType
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            return
        }

        // 2. Safe Package Name Extraction
        val packageName = event.packageName?.toString() ?: return
        if (packageName.isBlank() || systemPackages.contains(packageName)) {
            return
        }

        // 3. Debounce Guard (500ms)
        val currentTime = System.currentTimeMillis()
        if (packageName == lastProcessedPackage && (currentTime - lastProcessedTime) < 500) {
            return
        }
        lastProcessedPackage = packageName
        lastProcessedTime = currentTime

        // 4. Offload to background coroutine scope
        serviceScope.launch {
            assessContextAndThreats(packageName, event)
        }
    }

    override fun onInterrupt() {
        // Required callback for accessibility service interrupt scenarios
    }

    /**
     * Dual-Gear background polling loop.
     * Passive Gear: Polls every 3000ms.
     * Aggressive Gear: Polls every 1000ms when a transaction is active, looking for screen-sharing/recording apps.
     */
    private fun startPollingThreats() {
        serviceScope.launch {
            while (serviceJob.isActive) {
                ensureActive()

                val isActiveContext = isUpiContextActive.get()
                val interval = if (isActiveContext) 1000L else 3000L
                delay(interval)

                // Sweep the window hierarchy
                var threatFound = false
                val currentWindows = windows

                for (window in currentWindows) {
                    val rootNode = window.root ?: continue
                    val pkgName = rootNode.packageName?.toString()
                    
                    // If target UPI app is running but we missed the event-driven check, enable active context
                    if (UpiTargetRegistry.isUpiTarget(pkgName)) {
                        if (PaymentScreenVerifier.isPaymentScreenActive(rootNode)) {
                            isUpiContextActive.compareAndSet(false, true)
                        }
                    }

                    if (ThreatSignatureStore.isThreatPackage(pkgName)) {
                        threatFound = true
                    }
                    rootNode.recycle()
                }

                // If UPI transaction context is active, evaluate threats
                if (isUpiContextActive.get()) {
                    if (threatFound) {
                        consecutiveCleanChecks = 0
                        escalateThreat()
                    } else {
                        consecutiveCleanChecks++
                        // Ensure overlay is only removed if clean for two consecutive ticks
                        if (consecutiveCleanChecks >= 2) {
                            deescalateThreat()
                        }
                    }
                } else {
                    // No active transaction context, hide shield and alarm unconditionally
                    deescalateThreat()
                    consecutiveCleanChecks = 0
                }
            }
        }
    }

    /**
     * Event-driven threat assessment checks.
     */
    private suspend fun assessContextAndThreats(packageName: String, event: AccessibilityEvent) {
        val isUpiTarget = UpiTargetRegistry.isUpiTarget(packageName)

        if (isUpiTarget) {
            val root = event.source ?: rootInActiveWindow
            val isPaymentScreen = PaymentScreenVerifier.isPaymentScreenActive(root)
            root?.recycle()

            val previousContext = isUpiContextActive.getAndSet(isPaymentScreen)
            
            // If transitioned to active transaction context, run an immediate one-shot scan
            if (!previousContext && isPaymentScreen) {
                runOneShotThreatScan()
            }
        } else if (ThreatSignatureStore.isThreatPackage(packageName)) {
            // A threat application moved to foreground
            if (isUpiContextActive.get()) {
                consecutiveCleanChecks = 0
                escalateThreat()
            }
        } else {
            // Not a UPI app and not a threat
            // If the user navigated away from the UPI app entirely
            val activeWindows = windows
            var isUpiStillVisible = false
            for (window in activeWindows) {
                val rootNode = window.root ?: continue
                val pkg = rootNode.packageName?.toString()
                if (UpiTargetRegistry.isUpiTarget(pkg) && PaymentScreenVerifier.isPaymentScreenActive(rootNode)) {
                    isUpiStillVisible = true
                }
                rootNode.recycle()
            }
            isUpiContextActive.set(isUpiStillVisible)
        }
    }

    /**
     * Quick threat scan triggered immediately on transitioning into a UPI context.
     */
    private suspend fun runOneShotThreatScan() {
        val currentWindows = windows
        var threatFound = false
        for (window in currentWindows) {
            val rootNode = window.root ?: continue
            val pkg = rootNode.packageName?.toString()
            if (ThreatSignatureStore.isThreatPackage(pkg)) {
                threatFound = true
            }
            rootNode.recycle()
        }

        if (threatFound) {
            consecutiveCleanChecks = 0
            escalateThreat()
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
