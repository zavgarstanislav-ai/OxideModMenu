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
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private val prefs by lazy { getSharedPreferences("oxide_ui", MODE_PRIVATE) }
    private var panel: LinearLayout? = null
    private var bubble: TextView? = null
    private lateinit var panelParams: WindowManager.LayoutParams
    private lateinit var bubbleParams: WindowManager.LayoutParams
    private var tab = 0
    private var expanded = true

    private val accents = intArrayOf(Color.rgb(255,65,65), Color.rgb(0,210,190), Color.rgb(170,110,255), Color.rgb(255,150,35))
    private val themeNames = listOf("Красный", "Бирюзовый", "Фиолетовый", "Оранжевый")
    private val accent get() = accents[prefs.getInt("theme", 0).coerceIn(accents.indices)]
    private val dark = Color.rgb(18,18,18)
    private val muted = Color.rgb(165,165,165)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return }
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        startForeground(1001, notification())
        panelParams = WindowManager.LayoutParams(
            dp(prefs.getInt("width", 320)), WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = dp(18); y = dp(90) }
        bubbleParams = WindowManager.LayoutParams(
            dp(58), dp(58), WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = dp(18); y = dp(110) }
        showExpanded(true)
    }

    private fun notification(): Notification {
        val id = "oxide_floating_menu"
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(id, "Oxide floating menu", NotificationManager.IMPORTANCE_LOW))
        }
        val b = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, id) else {
            @Suppress("DEPRECATION") Notification.Builder(this)
        }
        return b.setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("t.me/game_soft_svk").setContentText("Floating interface is active")
            .setOngoing(true).build()
    }

    private fun makePanel(): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(12))
            background = shape(dark, accent, prefs.getInt("radius", 18))
            alpha = prefs.getInt("opacity", 96) / 100f
        }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val title = TextView(this).apply {
            text = prefs.getString("title", "t.me/game_soft_svk")
            textSize = 18f; setTextColor(accent); typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(2), 0, 0, 0)
        }
        header.addView(title, LinearLayout.LayoutParams(0, dp(44), 1f))
        header.addView(actionButton("−") { collapse() }, LinearLayout.LayoutParams(dp(42), dp(42)))
        header.addView(actionButton("×") { stopSelf() }, LinearLayout.LayoutParams(dp(42), dp(42)))
        root.addView(header)
        attachDrag(title, panelParams)

        val tabs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(7), 0, dp(8)) }
        listOf("HOME", "VISUAL", "SETTINGS").forEachIndexed { i, name ->
            val item = TextView(this).apply {
                text = name; gravity = Gravity.CENTER; textSize = 11f
                setTextColor(if (tab == i) accent else muted)
                typeface = if (tab == i) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                setPadding(dp(4), dp(10), dp(4), dp(10))
                background = if (tab == i) shape(0xFF292020.toInt(), accent, 8) else null
                setOnClickListener { tab = i; rebuild() }
            }
            tabs.addView(item, LinearLayout.LayoutParams(0, dp(38), 1f))
        }
        root.addView(tabs)
        root.addView(View(this).apply { setBackgroundColor(0xFF383838.toInt()) }, LinearLayout.LayoutParams(-1, dp(1)))
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(8), 0, 0) }
        when (tab) { 0 -> home(body); 1 -> visual(body); else -> settings(body) }
        root.addView(body)
        return root
    }

    private fun home(body: LinearLayout) {
        section(body, "ПАРАМЕТРЫ ИНТЕРФЕЙСА")
        prefSwitch(body, "Показывать подписи", "labels", true)
        prefSwitch(body, "Компактный вид", "compact", false)
        prefSwitch(body, "Подтверждать переключения", "feedback", true)
        hint(body, "Это настройки самого плавающего меню. Они не изменяют игровой процесс.")
    }

    private fun visual(body: LinearLayout) {
        section(body, "ВНЕШНИЙ ВИД")
        slider(body, "Прозрачность окна", "opacity", 55, 100, 96, "%") { value ->
            panel?.alpha = value / 100f
        }
        slider(body, "Ширина меню", "width", 270, 390, 320, " dp") { value ->
            panelParams.width = dp(value)
            panel?.let { if (it.isAttachedToWindow) wm.updateViewLayout(it, panelParams) }
        }
        slider(body, "Скругление углов", "radius", 8, 30, 18, " dp") { value -> panel?.background = shape(dark, accent, value) }
        actionRow(body, "Свернуть в красную кнопку") { collapse() }
        actionRow(body, "Показать анимацию открытия") { panel?.let { animateIn(it) } }
    }

    private fun settings(body: LinearLayout) {
        section(body, "ЦВЕТОВАЯ ТЕМА")
        themeNames.forEachIndexed { i, name ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(4), dp(5), dp(4), dp(5)) }
            val dot = View(this).apply { background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(accents[i]) } }
            row.addView(dot, LinearLayout.LayoutParams(dp(18), dp(18)))
            val label = TextView(this).apply { text = name; textSize = 14f; setTextColor(Color.WHITE); setPadding(dp(12),0,0,0) }
            row.addView(label, LinearLayout.LayoutParams(0, dp(34), 1f))
            row.addView(TextView(this).apply { text = if (prefs.getInt("theme",0)==i) "✓" else ""; setTextColor(accents[i]); textSize = 18f })
            row.setOnClickListener { prefs.edit().putInt("theme", i).apply(); rebuild() }
            body.addView(row)
        }
        section(body, "ПОВЕДЕНИЕ")
        slider(body, "Скорость анимации", "anim", 100, 500, 220, " мс") {}
        actionRow(body, "Сбросить оформление") {
            prefs.edit().clear().apply(); panelParams.width = dp(320); tab = 2; rebuild()
            Toast.makeText(this, "Настройки сброшены", Toast.LENGTH_SHORT).show()
        }
        hint(body, "Все изменения сохраняются автоматически на устройстве.")
    }

    private fun prefSwitch(parent: LinearLayout, label: String, key: String, default: Boolean) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val text = TextView(this).apply { text = label; textSize = 13f; setTextColor(Color.WHITE) }
        val sw = Switch(this).apply {
            isChecked = prefs.getBoolean(key, default)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean(key, checked).apply()
                if (prefs.getBoolean("feedback", true)) Toast.makeText(this@OverlayService, "$label: ${if (checked) "ВКЛ" else "ВЫКЛ"}", Toast.LENGTH_SHORT).show()
            }
        }
        row.addView(text, LinearLayout.LayoutParams(0, dp(44), 1f)); row.addView(sw); parent.addView(row)
    }

    private fun slider(parent: LinearLayout, label: String, key: String, min: Int, max: Int, default: Int, suffix: String, changed: (Int)->Unit) {
        val title = TextView(this).apply { textSize = 12f; setTextColor(Color.WHITE) }
        val bar = SeekBar(this).apply {
            this.max = max - min
            progress = (prefs.getInt(key, default).coerceIn(min,max) - min)
            progressTintList = android.content.res.ColorStateList.valueOf(accent)
            thumbTintList = android.content.res.ColorStateList.valueOf(accent)
        }
        fun update(v: Int) { title.text = "$label: $v$suffix"; prefs.edit().putInt(key,v).apply(); changed(v) }
        update(bar.progress + min)
        bar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) { update(progress+min) }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        parent.addView(title); parent.addView(bar)
    }

    private fun section(parent: LinearLayout, text: String) {
        parent.addView(TextView(this).apply { this.text=text; textSize=10f; typeface=Typeface.DEFAULT_BOLD; setTextColor(accent); setPadding(dp(2),dp(7),0,dp(5)) })
    }
    private fun hint(parent: LinearLayout, text: String) {
        if (!prefs.getBoolean("labels", true)) return
        parent.addView(TextView(this).apply { this.text=text; textSize=10f; setTextColor(muted); setPadding(dp(3),dp(8),dp(3),dp(4)) })
    }
    private fun actionRow(parent: LinearLayout, text: String, action: ()->Unit) {
        val v = TextView(this).apply { this.text=text; textSize=12f; setTextColor(Color.WHITE); gravity=Gravity.CENTER_VERTICAL; setPadding(dp(10),dp(9),dp(10),dp(9)); background=shape(0xFF292929.toInt(),0xFF444444.toInt(),9); setOnClickListener { action() } }
        val lp=LinearLayout.LayoutParams(-1,dp(42)); lp.topMargin=dp(6); parent.addView(v,lp)
    }
    private fun actionButton(label: String, action: ()->Unit) = Button(this).apply {
        text=label; textSize=20f; setTextColor(Color.WHITE); setBackgroundColor(Color.TRANSPARENT); setOnClickListener { action() }
    }
    private fun shape(fill: Int, stroke: Int, radius: Int) = GradientDrawable().apply {
        setColor(fill); cornerRadius=dp(radius).toFloat(); setStroke(dp(2),stroke)
    }

    private fun rebuild() {
        if (!expanded) return
        panel?.let { if (it.isAttachedToWindow) wm.removeView(it) }
        val next=makePanel(); panel=next; wm.addView(next,panelParams); animateIn(next)
    }
    private fun showExpanded(animate: Boolean) {
        bubble?.let { if (it.isAttachedToWindow) wm.removeView(it) }; bubble=null; expanded=true
        val v=makePanel(); panel=v; wm.addView(v,panelParams)
        if (animate) animateIn(v) else { v.alpha=1f; v.scaleX=1f; v.scaleY=1f }
    }
    private fun animateIn(v: View) {
        v.animate().cancel(); v.alpha=0f; v.scaleX=.88f; v.scaleY=.88f
        v.animate().alpha(prefs.getInt("opacity",96)/100f).scaleX(1f).scaleY(1f)
            .setDuration(prefs.getInt("anim",220).toLong()).setInterpolator(DecelerateInterpolator()).start()
    }
    private fun collapse() {
        val old=panel ?: return
        old.animate().alpha(0f).scaleX(.85f).scaleY(.85f).setDuration(130).withEndAction {
            if (old.isAttachedToWindow) wm.removeView(old); panel=null; showBubble()
        }.start()
    }
    private fun showBubble() {
        expanded=false
        val v=TextView(this).apply {
            text="O"; textSize=25f; typeface=Typeface.DEFAULT_BOLD; gravity=Gravity.CENTER; setTextColor(Color.WHITE)
            background=GradientDrawable().apply { shape=GradientDrawable.OVAL; setColor(accent); setStroke(dp(2),Color.WHITE) }
            alpha=0f; scaleX=.7f; scaleY=.7f
            setOnClickListener { showExpanded(true) }
        }
        bubble=v; wm.addView(v,bubbleParams)
        v.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(180).start()
        attachDrag(v,bubbleParams)
    }
    private fun attachDrag(view: View, params: WindowManager.LayoutParams) {
        view.setOnTouchListener(object: View.OnTouchListener {
            var sx=0; var sy=0; var tx=0f; var ty=0f; var moved=false
            override fun onTouch(v: View, e: MotionEvent): Boolean {
                when(e.action) {
                    MotionEvent.ACTION_DOWN -> { sx=params.x; sy=params.y; tx=e.rawX; ty=e.rawY; moved=false; return false }
                    MotionEvent.ACTION_MOVE -> {
                        val dx=(e.rawX-tx).toInt(); val dy=(e.rawY-ty).toInt()
                        if (kotlin.math.abs(dx)>dp(5)||kotlin.math.abs(dy)>dp(5)) moved=true
                        if (moved) { params.x=sx+dx; params.y=sy+dy; if(view.isAttachedToWindow) wm.updateViewLayout(view,params); return true }
                    }
                    MotionEvent.ACTION_UP -> if(moved) return true
                }; return false
            }
        })
    }
    private fun dp(v: Int)= (v*resources.displayMetrics.density).toInt()
    override fun onDestroy() {
        panel?.let { if(it.isAttachedToWindow) wm.removeView(it) }
        bubble?.let { if(it.isAttachedToWindow) wm.removeView(it) }
        panel=null; bubble=null; super.onDestroy()
    }
}
