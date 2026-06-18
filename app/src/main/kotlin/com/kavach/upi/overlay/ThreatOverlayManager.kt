package com.kavach.upi.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
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
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.OPAQUE
        ).apply {
            gravity = Gravity.CENTER
        }

        // Parent container with solid dark gradient background
        val frame = FrameLayout(context).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.parseColor("#0F0F12"), Color.parseColor("#1C1E26"))
            )
            // Block all touches and prevent pass-through
            setOnTouchListener { _, _ -> true }
        }

        // Center card for layout details
        val cardLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(64, 80, 64, 80)
            
            // Premium semi-transparent card background with accent border
            val cardBg = GradientDrawable().apply {
                setColor(Color.parseColor("#15FFFFFF"))
                cornerRadius = 32f
                setStroke(2, Color.parseColor("#30FFFFFF"))
            }
            background = cardBg
        }

        // Set card dimensions
        val cardParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
            setMargins(48, 0, 48, 0)
        }

        // Add custom Pulsing Shield View
        val shieldView = PulsingShieldView(context).apply {
            val size = (140 * context.resources.displayMetrics.density).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                setMargins(0, 0, 0, 48)
            }
        }
        cardLayout.addView(shieldView)

        // Title text
        val titleText = TextView(context).apply {
            text = "KAVACH SHIELD ACTIVE"
            setTextColor(Color.parseColor("#FF3B30")) // Deep warning red
            textSize = 22f
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }
        cardLayout.addView(titleText)

        // Description body text
        val descText = TextView(context).apply {
            text = "Potential screen-sharing app detected. Screen access and touches have been blocked to protect your financial transaction.\n\nDisconnect the active call or remote access session to resume."
            setTextColor(Color.parseColor("#E0E0E0")) // Off-white
            textSize = 15f
            gravity = Gravity.CENTER
            lineSpacingMultiplier = 1.2f
        }
        cardLayout.addView(descText)

        frame.addView(cardLayout, cardParams)

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

    /**
     * Custom View to render a glowing warning shield with a pulsing ring micro-animation.
     */
    private class PulsingShieldView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val path = Path()
        private var pulseFraction = 0f
        private var animator: ValueAnimator? = null

        init {
            animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 1800
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.RESTART
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { animation ->
                    pulseFraction = animation.animatedValue as Float
                    invalidate()
                }
            }
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            animator?.start()
        }

        override fun onDetachedFromWindow() {
            animator?.cancel()
            super.onDetachedFromWindow()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width / 2f
            val cy = height / 2f
            val baseRadius = Math.min(width, height) / 4.5f

            // 1. Draw glowing pulsing outer rings
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 6f
            paint.color = Color.parseColor("#FF3B30")
            
            // Outer ring 1
            paint.alpha = ((1f - pulseFraction) * 180).toInt()
            val ringRadius1 = baseRadius + (pulseFraction * baseRadius * 0.8f)
            canvas.drawCircle(cx, cy, ringRadius1, paint)

            // Outer ring 2 (offset phase)
            val pulseFraction2 = (pulseFraction + 0.5f) % 1.0f
            paint.alpha = ((1f - pulseFraction2) * 120).toInt()
            val ringRadius2 = baseRadius + (pulseFraction2 * baseRadius * 0.8f)
            canvas.drawCircle(cx, cy, ringRadius2, paint)

            // 2. Draw solid shield background
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#FF3B30")
            paint.alpha = 255

            path.reset()
            val yOffset = baseRadius * 0.1f // Slightly shift down to center visual weight
            val r = baseRadius

            val x0 = cx
            val y0 = cy - r + yOffset
            val x1 = cx + r * 0.85f
            val y1 = cy - r * 0.45f + yOffset
            val x2 = cx + r * 0.85f
            val y2 = cy + r * 0.2f + yOffset
            val x3 = cx
            val y3 = cy + r * 0.95f + yOffset
            val x4 = cx - r * 0.85f
            val y4 = cy + r * 0.2f + yOffset
            val x5 = cx - r * 0.85f
            val y5 = cy - r * 0.45f + yOffset

            path.moveTo(x0, y0)
            path.lineTo(x1, y1)
            path.lineTo(x2, y2)
            path.quadTo(cx + r * 0.45f, cy + r * 0.65f, x3, y3)
            path.quadTo(cx - r * 0.45f, cy + r * 0.65f, x4, y2)
            path.lineTo(x5, y5)
            path.close()

            canvas.drawPath(path, paint)

            // 3. Draw a white exclamation point symbol inside the shield
            paint.color = Color.WHITE
            paint.style = Paint.Style.FILL
            
            // Draw exclamation bar
            val barTop = cy - r * 0.35f + yOffset
            val barBottom = cy + r * 0.1f + yOffset
            val barWidth = 10f
            canvas.drawRect(cx - barWidth / 2, barTop, cx + barWidth / 2, barBottom, paint)
            
            // Draw exclamation dot
            val dotCenterY = cy + r * 0.35f + yOffset
            val dotRadius = 9f
            canvas.drawCircle(cx, dotCenterY, dotRadius, paint)
        }
    }
}

