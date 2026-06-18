package com.kavach.upi

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Simple programmatic UI for onboarding
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(32, 32, 32, 32)
        }

        val titleText = TextView(this).apply {
            text = "Kavach-UPI Security Shield"
            textSize = 24f
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 48)
        }
        layout.addView(titleText)

        val descText = TextView(this).apply {
            text = "To protect your financial transactions, please grant the following permissions:"
            textSize = 16f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 48)
        }
        layout.addView(descText)

        val btnOverlay = Button(this).apply {
            text = "Grant Draw Over Apps Permission"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                } else {
                    Toast.makeText(this@MainActivity, "Overlay permission already granted!", Toast.LENGTH_SHORT).show()
                }
            }
        }
        layout.addView(btnOverlay)

        val btnAccessibility = Button(this).apply {
            text = "Enable Accessibility Service"
            setOnClickListener {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
            }
        }
        layout.addView(btnAccessibility)

        setContentView(layout)
    }
}
