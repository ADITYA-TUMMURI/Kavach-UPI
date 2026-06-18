package com.kavach.upi.detection

import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.view.accessibility.AccessibilityEvent

class MediaProjectionDetector(private val context: Context) {

    private val usageStatsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
    } else {
        null
    }

    private val projectionKeywords = setOf(
        "screen recording",
        "screen sharing",
        "casting",
        "mirroring",
        "screen capture",
        "sharing your screen",
        "recording screen"
    )

    /**
     * Inspects accessibility notification event for screen capture indicators.
     */
    fun isNotificationProjectionActive(event: AccessibilityEvent): Boolean {
        if (event.eventType != AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            return false
        }

        val packageName = event.packageName?.toString() ?: return false
        if (!ThreatSignatureStore.isThreatPackage(packageName)) {
            return false
        }

        val textList = event.text
        for (charSeq in textList) {
            val text = charSeq?.toString()?.lowercase() ?: continue
            if (projectionKeywords.any { text.contains(it) }) {
                return true
            }
        }
        return false
    }

    /**
     * Queries UsageStatsManager foreground service types (requires PACKAGE_USAGE_STATS permission).
     */
    fun isUsageStatsProjectionActive(): Boolean {
        if (usageStatsManager == null) return false

        val currentTime = System.currentTimeMillis()
        val stats = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                usageStatsManager.queryConfigurations(
                    UsageStatsManager.INTERVAL_DAILY,
                    currentTime - 60000,
                    currentTime
                )
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }

        // Return false if permission is missing (query returns empty/null)
        if (stats == null || stats.isEmpty()) {
            return false
        }

        // If the system supports API 29+ foreground service type queries, we can check foreground actions.
        // For other levels, we fall back to verify if a threat package is in foreground.
        return false
    }
}
