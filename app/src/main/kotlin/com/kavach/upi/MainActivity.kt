package com.kavach.upi

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.kavach.upi.audio.WarningAudioPlayer
import com.kavach.upi.overlay.ThreatOverlayManager
import com.kavach.upi.service.KavachAccessibilityService

class MainActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences

    // UI Elements for dynamic updates
    private lateinit var statusCircle: LinearLayout
    private lateinit var statusText: TextView
    private lateinit var statusSubtext: TextView
    private lateinit var overlayBtn: Button
    private lateinit var accessibilityBtn: Button
    private lateinit var usageBtn: Button

    // Language Toggle Buttons
    private lateinit var btnLangEn: Button
    private lateinit var btnLangHi: Button

    // Alert Mode Toggle Buttons
    private lateinit var btnModeVoice: Button
    private lateinit var btnModeAlarm: Button
    private lateinit var btnModeBoth: Button

    // Test Sandbox references
    private var testOverlayManager: ThreatOverlayManager? = null
    private var testAudioPlayer: WarningAudioPlayer? = null
    private val sandboxHandler = Handler(Looper.getMainLooper())
    private var isSandboxRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences("kavach_settings", Context.MODE_PRIVATE)

        // Set window background to dark charcoal
        window.decorView.setBackgroundColor(Color.parseColor("#0F0F12"))

        // Root View Container
        val scrollView = android.widget.ScrollView(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            isFillViewport = true
        }

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(48, 64, 48, 64)
        }

        // Header Title
        val headerTitle = TextView(this).apply {
            text = "🛡️ KAVACH-UPI"
            textSize = 28f
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 48)
        }
        rootLayout.addView(headerTitle)

        // 1. Status Indicator Ring Card
        statusCircle = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 64, 48, 64)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 64)
            }
        }
        
        statusText = TextView(this).apply {
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        statusCircle.addView(statusText)

        statusSubtext = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.parseColor("#8E8E93"))
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 0)
        }
        statusCircle.addView(statusSubtext)
        rootLayout.addView(statusCircle)

        // Section Title: PERMISSIONS
        val permTitle = TextView(this).apply {
            text = "REQUIRED SECURITY ACCESS"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#8E8E93"))
            setPadding(12, 0, 0, 16)
        }
        rootLayout.addView(permTitle)

        // 2. Permission Card 1: Draw Over Apps
        val cardOverlay = createCard(
            "Display Over Other Apps",
            "Required to deploy the security shield and block remote controls when threat is detected."
        )
        overlayBtn = Button(this).apply {
            text = "Grant Permission"
            textSize = 13f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(32, 16, 32, 16)
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                } else {
                    Toast.makeText(this@MainActivity, "Permission already granted", Toast.LENGTH_SHORT).show()
                }
            }
        }
        cardOverlay.addView(overlayBtn)
        rootLayout.addView(cardOverlay)

        // Permission Card 2: Accessibility
        val cardA11y = createCard(
            "Accessibility Service",
            "Required to detect payment windows and scan background applications in real-time."
        )
        accessibilityBtn = Button(this).apply {
            text = "Enable Service"
            textSize = 13f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(32, 16, 32, 16)
            setOnClickListener {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
            }
        }
        cardA11y.addView(accessibilityBtn)
        rootLayout.addView(cardA11y)

        // Permission Card 3: Usage Access
        val cardUsage = createCard(
            "Usage Stats Access",
            "Required to check active media projection and screen mirroring tools on newer Android versions. Locate Kavach-UPI in the list and allow usage access to complete the security configuration."
        )
        usageBtn = Button(this).apply {
            text = "Grant Usage Access"
            textSize = 13f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(32, 16, 32, 16)
            setOnClickListener {
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                startActivity(intent)
            }
        }
        cardUsage.addView(usageBtn)
        rootLayout.addView(cardUsage)

        // Section Title: SETTINGS
        val settingsTitle = TextView(this).apply {
            text = "SHIELD ALERTS CONFIGURATION"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#8E8E93"))
            setPadding(12, 48, 0, 16)
        }
        rootLayout.addView(settingsTitle)

        // Settings Container Card
        val settingsCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createCardBackground(Color.parseColor("#15FFFFFF"), Color.parseColor("#25FFFFFF"))
            setPadding(40, 40, 40, 40)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 64)
            }
        }

        // Language Label
        val lblLang = TextView(this).apply {
            text = "Voice Warning Language"
            textSize = 14f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 16)
        }
        settingsCard.addView(lblLang)

        // Language Toggle Group
        val langLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 2f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 40)
            }
        }

        btnLangEn = Button(this).apply {
            text = "English"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0, 0, 12, 0)
            }
            setOnClickListener { selectLanguage("en") }
        }
        btnLangHi = Button(this).apply {
            text = "हिन्दी (Hindi)"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(12, 0, 0, 0)
            }
            setOnClickListener { selectLanguage("hi") }
        }
        langLayout.addView(btnLangEn)
        langLayout.addView(btnLangHi)
        settingsCard.addView(langLayout)

        // Alert Mode Label
        val lblMode = TextView(this).apply {
            text = "Alert Warning Type"
            textSize = 14f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 16)
        }
        settingsCard.addView(lblMode)

        // Alert Mode Toggle Group
        val modeLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 3f
        }

        btnModeVoice = Button(this).apply {
            text = "Voice"
            textSize = 11f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0, 0, 8, 0)
            }
            setOnClickListener { selectAlertMode("VOICE") }
        }
        btnModeAlarm = Button(this).apply {
            text = "Siren"
            textSize = 11f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(4, 0, 4, 0)
            }
            setOnClickListener { selectAlertMode("ALARM") }
        }
        btnModeBoth = Button(this).apply {
            text = "Both"
            textSize = 11f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(8, 0, 0, 0)
            }
            setOnClickListener { selectAlertMode("BOTH") }
        }
        modeLayout.addView(btnModeVoice)
        modeLayout.addView(btnModeAlarm)
        modeLayout.addView(btnModeBoth)
        settingsCard.addView(modeLayout)

        rootLayout.addView(settingsCard)

        // 3. Test Sandbox Button
        val testSandboxBtn = Button(this).apply {
            text = "🚨 TEST SHIELD OVERLAY (5s)"
            textSize = 16f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(48, 36, 48, 36)
            background = createButtonBackground(Color.parseColor("#D32F2F"), Color.parseColor("#FF5252"))
            setOnClickListener {
                runSandboxTest()
            }
        }
        rootLayout.addView(testSandboxBtn)

        scrollView.addView(rootLayout)
        setContentView(scrollView)

        // Initialize state of toggles
        updateSettingToggles()
    }

    override fun onResume() {
        super.onResume()
        updateStatusUi()
    }

    private fun isUsageAccessGranted(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as? android.app.AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        }
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    private fun updateStatusUi() {
        val overlayOk = Settings.canDrawOverlays(this)
        val a11yOk = isAccessibilityServiceEnabled()
        val usageOk = isUsageAccessGranted()

        if (overlayOk && a11yOk && usageOk) {
            statusCircle.background = createCardBackground(Color.parseColor("#152ECC71"), Color.parseColor("#402ECC71"))
            statusText.text = "🛡️ SHIELD ACTIVE & SECURE"
            statusText.setTextColor(Color.parseColor("#2ECC71"))
            statusSubtext.text = "Dual-gear threat scanner is actively monitoring background processes."
        } else if (overlayOk && a11yOk && !usageOk) {
            statusCircle.background = createCardBackground(Color.parseColor("#15F39C12"), Color.parseColor("#40F39C12"))
            statusText.text = "⚠️ EXTRA SETUP REQUIRED"
            statusText.setTextColor(Color.parseColor("#F39C12"))
            statusSubtext.text = "Usage Access permission missing. Enable it to detect media-projection attacks."
        } else {
            statusCircle.background = createCardBackground(Color.parseColor("#15F39C12"), Color.parseColor("#40F39C12"))
            statusText.text = "⚠️ SETUP REQUIRED"
            statusText.setTextColor(Color.parseColor("#F39C12"))
            statusSubtext.text = "Permissions missing. Security scanner cannot operate."
        }

        // Update Button overlay status
        if (overlayOk) {
            overlayBtn.text = "Permission Granted"
            overlayBtn.background = createCardBackground(Color.parseColor("#202ECC71"), Color.parseColor("#2ECC71"))
            overlayBtn.isEnabled = false
        } else {
            overlayBtn.text = "Grant Permission"
            overlayBtn.background = createButtonBackground(Color.parseColor("#2980B9"), Color.parseColor("#3498DB"))
            overlayBtn.isEnabled = true
        }

        // Update Button accessibility status
        if (a11yOk) {
            accessibilityBtn.text = "Service Enabled"
            accessibilityBtn.background = createCardBackground(Color.parseColor("#202ECC71"), Color.parseColor("#2ECC71"))
            accessibilityBtn.isEnabled = false
        } else {
            accessibilityBtn.text = "Enable Service"
            accessibilityBtn.background = createButtonBackground(Color.parseColor("#2980B9"), Color.parseColor("#3498DB"))
            accessibilityBtn.isEnabled = true
        }

        // Update Button usage stats status
        if (usageOk) {
            usageBtn.text = "Permission Granted"
            usageBtn.background = createCardBackground(Color.parseColor("#202ECC71"), Color.parseColor("#2ECC71"))
            usageBtn.isEnabled = false
        } else {
            usageBtn.text = "Grant Usage Access"
            usageBtn.background = createButtonBackground(Color.parseColor("#2980B9"), Color.parseColor("#3498DB"))
            usageBtn.isEnabled = true
        }
    }

    private fun updateSettingToggles() {
        val currentLang = sharedPreferences.getString("pref_alert_language", "en") ?: "en"
        val currentMode = sharedPreferences.getString("pref_alert_type", "VOICE") ?: "VOICE"

        // Update Language Selector Visuals
        if (currentLang == "hi") {
            btnLangHi.background = createButtonBackground(Color.parseColor("#2C3E50"), Color.parseColor("#34495E"))
            btnLangHi.setTextColor(Color.WHITE)
            btnLangEn.background = createCardBackground(Color.TRANSPARENT, Color.parseColor("#35FFFFFF"))
            btnLangEn.setTextColor(Color.parseColor("#8E8E93"))
        } else {
            btnLangEn.background = createButtonBackground(Color.parseColor("#2C3E50"), Color.parseColor("#34495E"))
            btnLangEn.setTextColor(Color.WHITE)
            btnLangHi.background = createCardBackground(Color.TRANSPARENT, Color.parseColor("#35FFFFFF"))
            btnLangHi.setTextColor(Color.parseColor("#8E8E93"))
        }

        // Update Alert Mode Visuals
        val activeBg = createButtonBackground(Color.parseColor("#2C3E50"), Color.parseColor("#34495E"))
        val inactiveBg = { createCardBackground(Color.TRANSPARENT, Color.parseColor("#35FFFFFF")) }
        
        btnModeVoice.background = if (currentMode == "VOICE") activeBg else inactiveBg()
        btnModeVoice.setTextColor(if (currentMode == "VOICE") Color.WHITE else Color.parseColor("#8E8E93"))

        btnModeAlarm.background = if (currentMode == "ALARM") activeBg else inactiveBg()
        btnModeAlarm.setTextColor(if (currentMode == "ALARM") Color.WHITE else Color.parseColor("#8E8E93"))

        btnModeBoth.background = if (currentMode == "BOTH") activeBg else inactiveBg()
        btnModeBoth.setTextColor(if (currentMode == "BOTH") Color.WHITE else Color.parseColor("#8E8E93"))
    }

    private fun selectLanguage(lang: String) {
        sharedPreferences.edit().putString("pref_alert_language", lang).apply()
        updateSettingToggles()
        Toast.makeText(this, "Language updated to: " + if (lang == "hi") "Hindi" else "English", Toast.LENGTH_SHORT).show()
    }

    private fun selectAlertMode(mode: String) {
        sharedPreferences.edit().putString("pref_alert_type", mode).apply()
        updateSettingToggles()
        Toast.makeText(this, "Alert type set to: $mode", Toast.LENGTH_SHORT).show()
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponentName = ComponentName(this, KavachAccessibilityService::class.java)
        val enabledServicesSetting = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServicesSetting)
        while (colonSplitter.hasNext()) {
            val componentNameString = colonSplitter.next()
            val enabledService = ComponentName.unflattenFromString(componentNameString)
            if (enabledService != null && enabledService == expectedComponentName) {
                return true
            }
        }
        return false
    }

    private fun runSandboxTest() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Draw over other apps permission required to run overlay test!", Toast.LENGTH_LONG).show()
            return
        }

        if (isSandboxRunning) return
        isSandboxRunning = true

        Toast.makeText(this, "Starting 5-second shield test...", Toast.LENGTH_SHORT).show()

        testOverlayManager = ThreatOverlayManager(this)
        testAudioPlayer = WarningAudioPlayer(this)

        // Deploy testing units
        testOverlayManager?.showOverlay()
        testAudioPlayer?.startAlarm()

        // Schedule auto dismissal
        sandboxHandler.postDelayed({
            testAudioPlayer?.stopAlarm()
            testAudioPlayer?.shutdown()
            testOverlayManager?.hideOverlay()
            
            testAudioPlayer = null
            testOverlayManager = null
            isSandboxRunning = false
            Toast.makeText(this, "Test completed successfully.", Toast.LENGTH_SHORT).show()
        }, 5000)
    }

    override fun onDestroy() {
        if (isSandboxRunning) {
            sandboxHandler.removeCallbacksAndMessages(null)
            testAudioPlayer?.shutdown()
            testOverlayManager?.hideOverlay()
        }
        super.onDestroy()
    }

    // Programmatic view generation utility methods
    private fun createCard(title: String, desc: String): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createCardBackground(Color.parseColor("#15FFFFFF"), Color.parseColor("#25FFFFFF"))
            setPadding(40, 40, 40, 40)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 36)
            }
        }

        val cardTitle = TextView(this).apply {
            text = title
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 8)
        }
        card.addView(cardTitle)

        val cardDesc = TextView(this).apply {
            text = desc
            textSize = 13f
            setTextColor(Color.parseColor("#8E8E93"))
            setPadding(0, 0, 0, 24)
        }
        card.addView(cardDesc)

        return card
    }

    private fun createCardBackground(backgroundColor: Int, borderColor: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(backgroundColor)
            cornerRadius = 24f
            setStroke(2, borderColor)
        }
    }

    private fun createButtonBackground(startColor: Int, endColor: Int): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(startColor, endColor)
        ).apply {
            cornerRadius = 16f
        }
    }
}

