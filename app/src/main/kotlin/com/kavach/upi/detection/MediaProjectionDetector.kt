package com.kavach.upi.detection

import android.app.ActivityManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ServiceInfo
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
        val manager = usageStatsManager ?: return false
        val currentTime = System.currentTimeMillis()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10 (API 29) or higher
            val startTime = currentTime - 10000 // look back 10 seconds (compressed window)
            try {
                val usageEvents = manager.queryEvents(startTime, currentTime)
                val event = UsageEvents.Event()
                while (usageEvents.hasNextEvent()) {
                    usageEvents.getNextEvent(event)
                    if (event.eventType == UsageEvents.Event.FOREGROUND_SERVICE_START) {
                        val pkgName = event.packageName
                        val className = event.className
                        if (pkgName != null && className != null) {
                            try {
                                val component = android.content.ComponentName(pkgName, className)
                                val serviceInfo = context.packageManager.getServiceInfo(component, 0)
                                val serviceType = serviceInfo.foregroundServiceType
                                if ((serviceType and ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION) != 0) {
                                    return true
                                }
                            } catch (e: Exception) {
                                // Gracefully ignore lookup failures
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            // Fallback for API below 29: look up known threat signatures from local process registry
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val runningServices = try {
                activityManager?.getRunningServices(50)
            } catch (e: Exception) {
                null
            }
            if (runningServices != null) {
                for (serviceInfo in runningServices) {
                    val pkgName = serviceInfo.service.packageName
                    if (ThreatSignatureStore.isThreatPackage(pkgName)) {
                        return true
                    }
                }
            }
        }
        return false
    }
}
