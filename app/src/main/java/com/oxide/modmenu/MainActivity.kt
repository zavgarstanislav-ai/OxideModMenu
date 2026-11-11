package com.oxide.modmenu

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 36, 28, 36)
            setBackgroundColor(0xFF101010.toInt())
        }

        val title = TextView(this).apply {
            text = "OXIDE MENU"
            textSize = 28f
            setTextColor(0xFFFF4545.toInt())
            gravity = Gravity.CENTER
        }
        val subtitle = TextView(this).apply {
            text = "Floating demo interface"
            textSize = 14f
            setTextColor(0xFFBBBBBB.toInt())
            gravity = Gravity.CENTER
        }
        val launch = Button(this).apply {
            text = "OPEN FLOATING MENU"
            setOnClickListener { openOverlay() }
        }

        root.addView(title)
        root.addView(subtitle)
        root.addView(launch)
        setContentView(root)
    }

    private fun openOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            Toast.makeText(
                this,
                "Allow display over other apps, then return and tap OPEN.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val intent = Intent(this, OverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}
