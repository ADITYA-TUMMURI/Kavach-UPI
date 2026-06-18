package com.kavach.upi.service

import kotlinx.coroutines.channels.Channel
import java.util.concurrent.atomic.AtomicBoolean

class ThreatStateEvaluator(private val commandChannel: Channel<UICommand>) {

    // Four independent detection channels
    val windowStackThreatDetected = AtomicBoolean(false)
    val backgroundServiceThreatDetected = AtomicBoolean(false)
    val mediaProjectionThreatDetected = AtomicBoolean(false)
    val virtualDisplayThreatDetected = AtomicBoolean(false)

    // Current states
    private val isUpiContextActive = AtomicBoolean(false)
    private var currentOverlayStateActive = false

    /**
     * Updates the UPI transaction context status.
     */
    fun setUpiContextActive(isActive: Boolean) {
        isUpiContextActive.set(isActive)
        evaluate()
    }

    /**
     * Gets the current UPI transaction context status.
     */
    fun isUpiContextActive(): Boolean {
        return isUpiContextActive.get()
    }

    /**
     * Reads all four threat flags and returns true if any are active.
     */
    fun isScreenShareActive(): Boolean {
        return windowStackThreatDetected.get() ||
                backgroundServiceThreatDetected.get() ||
                mediaProjectionThreatDetected.get() ||
                virtualDisplayThreatDetected.get()
    }

    /**
     * Executes the overlay logic formula: UPI App Open AND Screen Share Active.
     */
    fun evaluate() {
        val upiOpen = isUpiContextActive.get()
        val screenShareActive = isScreenShareActive()

        val shouldBeActive = upiOpen && screenShareActive

        if (shouldBeActive != currentOverlayStateActive) {
            currentOverlayStateActive = shouldBeActive
            val command = if (shouldBeActive) {
                UICommand.DeployOverlay
            } else {
                UICommand.DismissOverlay
            }
            commandChannel.trySend(command)
        }
    }
}
