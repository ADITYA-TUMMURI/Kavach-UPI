package com.kavach.upi.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.SharedPreferences
import android.view.accessibility.AccessibilityEvent
import com.kavach.upi.audio.WarningAudioPlayer
import com.kavach.upi.detection.BackgroundProcessScanner
import com.kavach.upi.detection.MediaProjectionDetector
import com.kavach.upi.detection.PaymentScreenVerifier
import com.kavach.upi.detection.ThreatSignatureStore
import com.kavach.upi.detection.UpiTargetRegistry
import com.kavach.upi.overlay.ThreatOverlayManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

class KavachAccessibilityService : AccessibilityService() {

    private var serviceJob = SupervisorJob()
    private var serviceScope = CoroutineScope(serviceJob + Dispatchers.Default)

    // Nullable references to enable clean 7-stage teardown
    private var overlayManager: ThreatOverlayManager? = null
    private var audioPlayer: WarningAudioPlayer? = null
    private var processScanner: BackgroundProcessScanner? = null
    private var projectionDetector: MediaProjectionDetector? = null
    private var stateEvaluator: ThreatStateEvaluator? = null

    // Channel-based UI Command pipeline
    private val commandChannel = Channel<UICommand>(Channel.CONFLATED)

    // State Tracking
    private var lastProcessedPackage: String? = null
    private var lastProcessedTime: Long = 0
    private var consecutiveCleanChecks = 0

    private lateinit var sharedPreferences: SharedPreferences

    private val systemPackages = setOf(
        "com.android.systemui",
        "com.google.android.inputmethod.latin",
        "android",
        "com.android.launcher3"
    )

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = getSharedPreferences("kavach_settings", Context.MODE_PRIVATE)

        overlayManager = ThreatOverlayManager(this)
        audioPlayer = WarningAudioPlayer(this)
        processScanner = BackgroundProcessScanner(this)
        projectionDetector = MediaProjectionDetector(this)
        stateEvaluator = ThreatStateEvaluator(commandChannel)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        // Restore State from Cache
        restoreCachedState()

        // 1. Start command collector on Main Thread
        startCommandCollector()

        // 2. Start background dual-gear threat scanning loop
        startPollingThreats()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // 1. Notification Projection Interceptor
        if (event.eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            val detector = projectionDetector ?: return
            val evaluator = stateEvaluator ?: return
            if (detector.isNotificationProjectionActive(event)) {
                evaluator.mediaProjectionThreatDetected.set(true)
                evaluator.evaluate()
            }
            return
        }

        // 2. Event Type Gate for Window States
        val eventType = event.eventType
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            return
        }

        // 3. Safe Package Name Extraction
        val packageName = event.packageName?.toString() ?: return
        if (packageName.isBlank() || systemPackages.contains(packageName)) {
            return
        }

        // 4. Debounce Guard (500ms)
        val currentTime = System.currentTimeMillis()
        if (packageName == lastProcessedPackage && (currentTime - lastProcessedTime) < 500) {
            return
        }
        lastProcessedPackage = packageName
        lastProcessedTime = currentTime

        // 5. Offload to background coroutine scope
        serviceScope.launch {
            assessContextAndThreats(packageName, event)
        }
    }

    override fun onInterrupt() {
        // Required callback for accessibility service interrupt scenarios
    }

    /**
     * Dedicated main-thread coroutine collector for serial execution of UI operations.
     */
    private fun startCommandCollector() {
        serviceScope.launch(Dispatchers.Main) {
            for (command in commandChannel) {
                ensureActive()
                when (command) {
                    is UICommand.DeployOverlay -> {
                        if (overlayManager?.isOverlayActive() == false) {
                            overlayManager?.showOverlay()
                            // Wait 100ms layout settle delay
                            delay(100)
                            audioPlayer?.startAlarm()
                            persistShieldState("SHIELDED")
                        }
                    }
                    is UICommand.DismissOverlay -> {
                        // Managed inside StopAlarm sequence to enforce 200ms audio-stop-to-dismiss delay
                    }
                    is UICommand.StartAlarm -> {
                        audioPlayer?.startAlarm()
                    }
                    is UICommand.StopAlarm -> {
                        audioPlayer?.stopAlarm()
                        // Wait 200ms wind-down gap before removing view
                        delay(200)
                        overlayManager?.hideOverlay()
                        persistShieldState("IDLE")
                    }
                    is UICommand.FullTeardown -> {
                        // Handled inside onDestroy
                    }
                }
            }
        }
    }

    /**
     * Dual-Gear background polling loop.
     * Passive Gear: Polls every 3000ms.
     * Aggressive Gear: Polls every 1000ms when a transaction is active.
     */
    private fun startPollingThreats() {
        serviceScope.launch {
            while (serviceJob.isActive) {
                ensureActive()

                val evaluator = stateEvaluator ?: break
                val scanner = processScanner ?: break

                val isActiveContext = evaluator.isUpiContextActive()
                val interval = if (isActiveContext) 1000L else 3000L
                delay(interval)

                // 1. Scan window stack for direct threats and passive UPI context detection
                val currentWindows = windows
                var windowThreatFound = false

                for (window in currentWindows) {
                    val rootNode = window.root ?: continue
                    val pkgName = rootNode.packageName?.toString()

                    if (UpiTargetRegistry.isUpiTarget(pkgName)) {
                        if (PaymentScreenVerifier.isPaymentScreenActive(rootNode)) {
                            evaluator.setUpiContextActive(true)
                        }
                    }

                    if (ThreatSignatureStore.isThreatPackage(pkgName)) {
                        windowThreatFound = true
                    }
                    rootNode.recycle()
                }

                // Update Window Stack Threat Channel
                evaluator.windowStackThreatDetected.set(windowThreatFound)

                // 2. Scan background services and virtual displays
                val processThreatFound = scanner.isThreatActive()
                evaluator.backgroundServiceThreatDetected.set(processThreatFound)

                // 3. Evaluate combined results if UPI context is active
                if (evaluator.isUpiContextActive()) {
                    val combinedThreat = evaluator.isScreenShareActive()
                    if (combinedThreat) {
                        consecutiveCleanChecks = 0
                        evaluator.evaluate()
                    } else {
                        consecutiveCleanChecks++
                        // Flicker prevention: require two consecutive clean cycles
                        if (consecutiveCleanChecks >= 2) {
                            evaluator.evaluate()
                        }
                    }
                } else {
                    // No active transaction, clear shield unconditionally
                    evaluator.evaluate()
                    consecutiveCleanChecks = 0
                }
            }
        }
    }

    /**
     * Event-driven threat assessment checks.
     */
    private suspend fun assessContextAndThreats(packageName: String, event: AccessibilityEvent) {
        val evaluator = stateEvaluator ?: return

        if (UpiTargetRegistry.isUpiTarget(packageName)) {
            val root = event.source ?: rootInActiveWindow
            val isPaymentScreen = PaymentScreenVerifier.isPaymentScreenActive(root)
            root?.recycle()

            val previousContext = evaluator.isUpiContextActive()
            evaluator.setUpiContextActive(isPaymentScreen)

            // If transitioned to active transaction context, run an immediate one-shot scan
            if (!previousContext && isPaymentScreen) {
                runOneShotThreatScan()
            }
        } else if (ThreatSignatureStore.isThreatPackage(packageName)) {
            evaluator.windowStackThreatDetected.set(true)
            evaluator.evaluate()
        } else {
            // Check if user navigated away from UPI context entirely
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
            evaluator.setUpiContextActive(isUpiStillVisible)
        }
    }

    /**
     * Quick threat scan triggered immediately on transitioning into a UPI context.
     */
    private suspend fun runOneShotThreatScan() {
        val evaluator = stateEvaluator ?: return
        val scanner = processScanner ?: return

        val currentWindows = windows
        var windowThreat = false
        for (window in currentWindows) {
            val rootNode = window.root ?: continue
            val pkg = rootNode.packageName?.toString()
            if (ThreatSignatureStore.isThreatPackage(pkg)) {
                windowThreat = true
            }
            rootNode.recycle()
        }

        evaluator.windowStackThreatDetected.set(windowThreat)
        evaluator.backgroundServiceThreatDetected.set(scanner.isThreatActive())
        evaluator.evaluate()
    }

    /**
     * Persists current shield state to cache.
     */
    private fun persistShieldState(state: String) {
        sharedPreferences.edit()
            .putString("shield_state", state)
            .putLong("state_timestamp", System.currentTimeMillis())
            .apply()
    }

    /**
     * Restores state from SharedPreferences cache, ignoring if older than 30s.
     */
    private fun restoreCachedState() {
        val cachedState = sharedPreferences.getString("shield_state", "IDLE") ?: "IDLE"
        val timestamp = sharedPreferences.getLong("state_timestamp", 0)
        val elapsed = System.currentTimeMillis() - timestamp

        if (cachedState == "SHIELDED" && elapsed < 30000) {
            stateEvaluator?.setUpiContextActive(true)
            stateEvaluator?.windowStackThreatDetected?.set(true)
            stateEvaluator?.evaluate()
        }
    }

    /**
     * Strict 7-Stage teardown sequence to prevent memory leaks and unhook OS systems.
     */
    override fun onDestroy() {
        // Stage 1: Cancel Coroutine Job
        serviceJob.cancel()
        serviceScope.cancel()

        // Stage 2: Close command channel
        commandChannel.close()

        // Stage 3: Stop alarm audio & release decoder resources
        audioPlayer?.stopAlarm()

        // Stage 4: Remove overlay from window manager
        overlayManager?.hideOverlay()

        // Stage 5: Clear SharedPreferences cached state
        sharedPreferences.edit()
            .putString("shield_state", "IDLE")
            .putLong("state_timestamp", 0)
            .apply()

        // Stage 6: Nullify all component references
        overlayManager = null
        audioPlayer = null
        processScanner = null
        projectionDetector = null
        stateEvaluator = null

        // Stage 7: Call superclass onDestroy
        super.onDestroy()
    }
}
