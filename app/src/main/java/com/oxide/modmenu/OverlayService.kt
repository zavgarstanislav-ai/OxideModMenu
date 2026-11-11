package com.oxide.modmenu

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var menuView: View? = null
    private val red = Color.rgb(255, 65, 65)
    private val dark = Color.rgb(18, 18, 18)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startForeground(NOTIFICATION_ID, buildNotification())
        showMenu()
    }

    private fun buildNotification(): Notification {
        val channelId = "oxide_floating_menu"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "Oxide floating menu",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("Oxide Menu")
            .setContentText("Floating demo menu is active")
            .setOngoing(true)
            .build()
    }

    private fun showMenu() {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(12))
            background = GradientDrawable().apply {
                setColor(dark)
                cornerRadius = dp(18).toFloat()
                setStroke(dp(2), red)
            }
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = TextView(this).apply {
            text = "OXIDE MENU"
            textSize = 19f
            setTextColor(red)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        val close = Button(this).apply {
            text = "×"
            textSize = 20f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { stopSelf() }
        }
        header.addView(title, LinearLayout.LayoutParams(0, dp(48), 1f))
        header.addView(close, LinearLayout.LayoutParams(dp(48), dp(48)))
        panel.addView(header)

        val demoFeatures = listOf(
            "Aimbot (demo switch)",
            "ESP / Wallhack (demo switch)",
            "Speedhack (demo switch)",
            "God Mode (demo switch)",
            "No Recoil (demo switch)"
        )

        demoFeatures.forEach { feature ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(2), dp(3), dp(2), dp(3))
            }
            val label = TextView(this).apply {
                text = feature
                textSize = 13f
                setTextColor(Color.WHITE)
            }
            val toggle = Switch(this).apply {
                isChecked = false
                setOnCheckedChangeListener { _, enabled ->
                    Toast.makeText(
                        this@OverlayService,
                        "$feature: ${if (enabled) "ON" else "OFF"} (visual demo only)",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            row.addView(label, LinearLayout.LayoutParams(0, dp(46), 1f))
            row.addView(toggle)
            panel.addView(row)
        }

        val params = WindowManager.LayoutParams(
            dp(300),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(18)
            y = dp(110)
        }

        header.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var touchX = 0f
            private var touchY = 0f

            override fun onTouch(view: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        touchX = event.rawX
                        touchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - touchX).toInt()
                        params.y = initialY + (event.rawY - touchY).toInt()
                        if (panel.isAttachedToWindow) {
                            windowManager.updateViewLayout(panel, params)
                        }
                        return true
                    }
                }
                return false
            }
        })

        menuView = panel
        windowManager.addView(panel, params)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        menuView?.let { view ->
            if (view.isAttachedToWindow) windowManager.removeView(view)
        }
        menuView = null
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}
