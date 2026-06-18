package com.kavach.upi.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView

class ThreatOverlayManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null

    /**
     * Deploys the opaque safety overlay blocking remote touches.
     */
    fun showOverlay() {
        if (overlayView != null) return // Already showing

        if (!Settings.canDrawOverlays(context)) {
            return // Cannot draw overlay without permission
        }

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.OPAQUE
        ).apply {
            gravity = Gravity.CENTER
        }

        val frame = FrameLayout(context).apply {
            setBackgroundColor(Color.parseColor("#121212")) // Harmonious Dark Background
        }

        val warningText = TextView(context).apply {
            text = "SECURITY SHIELD ACTIVE\n\nPotential screen-sharing app detected. Screen access is temporarily blocked to protect your transaction."
            setTextColor(Color.WHITE)
            textSize = 20f
            gravity = Gravity.CENTER
            setPadding(64, 64, 64, 64)
        }
        frame.addView(warningText)

        try {
            windowManager.addView(frame, params)
            overlayView = frame
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Removes the active safety overlay.
     */
    fun hideOverlay() {
        val view = overlayView ?: return
        try {
            windowManager.removeView(view)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            overlayView = null
        }
    }

    /**
     * Checks whether the overlay is currently active.
     */
    fun isOverlayActive(): Boolean {
        return overlayView != null
    }
}
