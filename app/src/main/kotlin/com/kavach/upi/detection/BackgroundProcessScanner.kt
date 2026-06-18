package com.kavach.upi.detection

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.view.Display

class BackgroundProcessScanner(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager

    private var lastScanTime: Long = 0
    private var cachedScanResult = false

    private val scanThrottleMs = 5000L

    /**
     * Scans for running services of known threat packages and checks active virtual displays.
     */
    fun isThreatActive(): Boolean {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastScanTime < scanThrottleMs) {
            return cachedScanResult || isVirtualDisplayActive()
        }
        lastScanTime = currentTime

        var threatServiceFound = false
        val runningServices = try {
            activityManager.getRunningServices(50)
        } catch (e: Exception) {
            null
        }

        if (runningServices != null) {
            for (serviceInfo in runningServices) {
                val pkgName = serviceInfo.service.packageName
                if (ThreatSignatureStore.isThreatPackage(pkgName)) {
                    threatServiceFound = true
                    break
                }
            }
        }

        // If no running service found, double check by verifying package installs
        if (!threatServiceFound) {
            threatServiceFound = checkThreatInstalls()
        }

        cachedScanResult = threatServiceFound
        return cachedScanResult || isVirtualDisplayActive()
    }

    /**
     * Checks if any known threat packages are installed.
     * Targeted query is allowed without QUERY_ALL_PACKAGES permission.
     */
    private fun checkThreatInstalls(): Boolean {
        // Iterate through signature packages
        for (pkg in ThreatSignatureStore.getThreatPackages()) {
            try {
                packageManager.getPackageInfo(pkg, PackageManager.GET_SERVICES)
                // If package exists, run a service query to see if active
                return true
            } catch (e: PackageManager.NameNotFoundException) {
                // Not installed, continue checking
            }
        }
        return false
    }

    /**
     * Checks display manager for active virtual or cast displays.
     */
    private fun isVirtualDisplayActive(): Boolean {
        return try {
            val displays = displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
            displays.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
}
