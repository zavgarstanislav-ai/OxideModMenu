package com.oxide.modmenu

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var panelView: View? = null
    private var bubbleView: View? = null
    private lateinit var panelParams: WindowManager.LayoutParams
    private lateinit var bubbleParams: WindowManager.LayoutParams
    private var selectedTab = 0
    private var themeIndex = 0
    private var expanded = true

    private val accents = intArrayOf(
        Color.rgb(255, 65, 65),
        Color.rgb(0, 210, 190),
        Color.rgb(170, 110, 255)
    )
    private val themeNames = arrayOf("Crimson", "Neon teal", "Violet")
    private val accent: Int get() = accents[themeIndex]
    private val dark = Color.rgb(18, 18, 18)
    private val muted = Color.rgb(165, 165, 165)

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
                NotificationChannel(channelId, "Oxide floating menu", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder.setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("Oxide Menu")
            .setContentText("Floating demo interface is active")
            .setOngoing(true)
            .build()
    }

    private fun showMenu() {
        panelParams = WindowManager.LayoutParams(
            dp(310), WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(18)
            y = dp(90)
        }
        bubbleParams = WindowManager.LayoutParams(
            dp(58), dp(58),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(18)
            y = dp(110)
        }
        showExpanded(animate = true)
    }

    private fun makePanel(): LinearLayout {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(12))
            background = rounded(dark, accent, 18)
            alpha = 0f
            scaleX = 0.92f
            scaleY = 0.92f
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = TextView(this).apply {
            text = "t.me/game_soft_svk"
            textSize = 19f
            setTextColor(accent)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(2), 0, 0, 0)
        }
        val collapse = smallButton("−") { collapseMenu() }
        val close = smallButton("×") { stopSelf() }
        header.addView(title, LinearLayout.LayoutParams(0, dp(42), 1f))
        header.addView(collapse, LinearLayout.LayoutParams(dp(42), dp(42)))
        header.addView(close, LinearLayout.LayoutParams(dp(42), dp(42)))
        panel.addView(header)
        attachDrag(title, panelParams)

        val tabs = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, dp(8))
        }
        listOf("HOME", "VISUAL", "SETTINGS").forEachIndexed { index, label ->
            val tab = TextView(this).apply {
                text = label
                textSize = 11f
                gravity = Gravity.CENTER
                setTextColor(if (selectedTab == index) accent else muted)
                typeface = if (selectedTab == index) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                setPadding(dp(5), dp(10), dp(5), dp(10))
                background = if (selectedTab == index) rounded(0xFF292020.toInt(), accent, 8) else null
                setOnClickListener {
                    selectedTab = index
                    refreshPanel()
                }
            }
            tabs.addView(tab, LinearLayout.LayoutParams(0, dp(38), 1f))
        }
        panel.addView(tabs)

        val divider = View(this).apply { setBackgroundColor(0xFF383838.toInt()) }
        panel.addView(divider, LinearLayout.LayoutParams(-1, dp(1)))

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(2))
        }
        when (selectedTab) {
            0 -> buildHome(content)
            1 -> buildVisual(content)
            else -> buildSettings(content)
        }
        panel.addView(content)
        return panel
    }

    private fun buildHome(container: LinearLayout) {
        addSectionLabel(container, "DEMO CONTROLS")
        listOf("Aimbot", "ESP / Wallhack", "Speedhack", "God Mode", "No Recoil").forEach { feature ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(2), dp(2), dp(2), dp(2))
            }
            val label = TextView(this).apply {
                text = "$feature  ·  demo"
                textSize = 13f
                setTextColor(Color.WHITE)
            }
            val toggle = Switch(this).apply {
                isChecked = false
                setOnCheckedChangeListener { _, enabled ->
                    Toast.makeText(this@OverlayService,
                        "$feature: ${if (enabled) "ON" else "OFF"} · visual demo only",
                        Toast.LENGTH_SHORT).show()
                }
            }
            row.addView(label, LinearLayout.LayoutParams(0, dp(42), 1f))
            row.addView(toggle)
            container.addView(row)
        }
        addHint(container, "These switches only demonstrate the interface.")
    }

    private fun buildVisual(container: LinearLayout) {
        addSectionLabel(container, "INTERFACE")
        addHint(container, "Floating window · draggable header · animated open/close")
        addActionRow(container, "Collapse to red floating button") { collapseMenu() }
        addActionRow(container, "Play opening animation") {
            panelView?.let { animateIn(it) }
        }
        addHint(container, "Visual-only controls; no game interaction.")
    }

    private fun buildSettings(container: LinearLayout) {
        addSectionLabel(container, "ACCENT COLOR")
        themeNames.forEachIndexed { index, name ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(4), dp(7), dp(4), dp(7))
            }
            val swatch = View(this).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(accents[index])
                }
            }
            val label = TextView(this).apply {
                text = name
                textSize = 14f
                setTextColor(Color.WHITE)
                setPadding(dp(12), 0, 0, 0)
            }
            val selected = TextView(this).apply {
                text = if (themeIndex == index) "✓" else ""
                textSize = 18f
                setTextColor(accents[index])
            }
            row.addView(swatch, LinearLayout.LayoutParams(dp(18), dp(18)))
            row.addView(label, LinearLayout.LayoutParams(0, dp(34), 1f))
            row.addView(selected)
            row.setOnClickListener {
                themeIndex = index
                refreshPanel()
            }
            container.addView(row)
        }
        addHint(container, "Theme changes apply to the floating menu.")
    }

    private fun addSectionLabel(parent: LinearLayout, text: String) {
        parent.addView(TextView(this).apply {
            this.text = text
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(accent)
            setPadding(dp(2), dp(5), 0, dp(7))
        })
    }

    private fun addHint(parent: LinearLayout, text: String) {
        parent.addView(TextView(this).apply {
            this.text = text
            textSize = 11f
            setTextColor(muted)
            setPadding(dp(3), dp(8), dp(3), dp(5))
        })
    }

    private fun addActionRow(parent: LinearLayout, text: String, action: () -> Unit) {
        val button = TextView(this).apply {
            this.text = text
            textSize = 13f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(10), dp(10), dp(10))
            background = rounded(0xFF292929.toInt(), 0xFF444444.toInt(), 9)
            setOnClickListener { action() }
        }
        val params = LinearLayout.LayoutParams(-1, dp(44))
        params.topMargin = dp(7)
        parent.addView(button, params)
    }

    private fun smallButton(text: String, action: () -> Unit) = Button(this).apply {
        this.text = text
        textSize = 20f
        setTextColor(Color.WHITE)
        setBackgroundColor(Color.TRANSPARENT)
        setOnClickListener { action() }
    }

    private fun rounded(fill: Int, stroke: Int, radius: Int) = GradientDrawable().apply {
        setColor(fill)
        cornerRadius = dp(radius).toFloat()
        setStroke(dp(2), stroke)
    }

    private fun refreshPanel() {
        if (!expanded) return
        val old = panelView
        if (old != null && old.isAttachedToWindow) windowManager.removeView(old)
        val fresh = makePanel()
        panelView = fresh
        windowManager.addView(fresh, panelParams)
        animateIn(fresh)
    }

    private fun showExpanded(animate: Boolean) {
        bubbleView?.let { if (it.isAttachedToWindow) windowManager.removeView(it) }
        bubbleView = null
        expanded = true
        val panel = makePanel()
        panelView = panel
        windowManager.addView(panel, panelParams)
        if (animate) animateIn(panel) else {
            panel.alpha = 1f
            panel.scaleX = 1f
            panel.scaleY = 1f
        }
    }

    private fun animateIn(view: View) {
        view.animate().cancel()
        view.alpha = 0f
        view.scaleX = 0.88f
        view.scaleY = 0.88f
        view.animate().alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(220).setInterpolator(DecelerateInterpolator()).start()
    }

    private fun collapseMenu() {
        val old = panelView ?: return
        old.animate().alpha(0f).scaleX(0.85f).scaleY(0.85f).setDuration(130)
            .withEndAction {
                if (old.isAttachedToWindow) windowManager.removeView(old)
                panelView = null
                showBubble()
            }.start()
    }

    private fun showBubble() {
        expanded = false
        val bubble = TextView(this).apply {
            text = "O"
            textSize = 25f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(accent)
                setStroke(dp(2), Color.WHITE)
            }
            alpha = 0f
            scaleX = 0.7f
            scaleY = 0.7f
            setOnClickListener { showExpanded(animate = true) }
        }
        bubbleView = bubble
        windowManager.addView(bubble, bubbleParams)
        bubble.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(180)
            .setInterpolator(DecelerateInterpolator()).start()
        attachDrag(bubble, bubbleParams)
    }

    private fun attachDrag(view: View, params: WindowManager.LayoutParams) {
        view.setOnTouchListener(object : View.OnTouchListener {
            private var startX = 0
            private var startY = 0
            private var touchX = 0f
            private var touchY = 0f
            private var moved = false
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = params.x
                        startY = params.y
                        touchX = event.rawX
                        touchY = event.rawY
                        moved = false
                        return false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - touchX).toInt()
                        val dy = (event.rawY - touchY).toInt()
                        if (kotlin.math.abs(dx) > dp(5) || kotlin.math.abs(dy) > dp(5)) moved = true
                        if (moved) {
                            params.x = startX + dx
                            params.y = startY + dy
                            if (view.isAttachedToWindow) windowManager.updateViewLayout(view, params)
                            return true
                        }
                    }
                    MotionEvent.ACTION_UP -> if (moved) return true
                }
                return false
            }
        })
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        panelView?.let { if (it.isAttachedToWindow) windowManager.removeView(it) }
        bubbleView?.let { if (it.isAttachedToWindow) windowManager.removeView(it) }
        panelView = null
        bubbleView = null
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}
