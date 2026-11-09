package com.ss.alive.alive

import android.app.Notification
import android.app.NotificationChannel
import android.app.PendingIntent
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import kotlin.math.hypot

class AliveService : Service() {
    private lateinit var windowManager: WindowManager
    private val petViews = mutableListOf<PetView>()
    private val interactionHandler = Handler(Looper.getMainLooper())
    private val pairCooldownUntil = mutableMapOf<String, Long>()
    private var screenReceiverRegistered = false
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: android.content.Intent?) {
            if (getSharedPreferences("ss_alive_tools", MODE_PRIVATE).getBoolean("pause_screen_off", true)) {
                when (intent?.action) {
                    android.content.Intent.ACTION_SCREEN_OFF -> {
                        removePets()
                        updateNotification("Paused while screen is off")
                    }
                    android.content.Intent.ACTION_SCREEN_ON -> {
                        if (Settings.canDrawOverlays(this@AliveService)) {
                            showPets()
                            updateNotification("Your equipped Alives are active")
                        }
                    }
                }
            }
        }
    }

    private val interactionRunnable = object : Runnable {
        override fun run() {
            checkInteractions()
            interactionHandler.postDelayed(this, 120L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        serviceIsRunning = true
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        registerReceiver(screenReceiver, IntentFilter().apply {
            addAction(android.content.Intent.ACTION_SCREEN_OFF)
            addAction(android.content.Intent.ACTION_SCREEN_ON)
        })
        screenReceiverRegistered = true
        interactionHandler.post(interactionRunnable)
    }

    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        when (intent?.action) {
            ACTION_HIDE -> {
                removePets()
                updateNotification("Alives hidden")
                return START_STICKY
            }
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_SHOW_EQUIPPED, null -> {
                removePets()
                pairCooldownUntil.clear()
                showPets()
                updateNotification("Your equipped Alives are active")
            }
            else -> {
                removePets()
                pairCooldownUntil.clear()
                showPets()
                updateNotification("Your equipped Alives are active")
            }
        }
        return START_STICKY
    }

    private fun showPets() {
        val profiles = AliveRepository.equipped(this)
        profiles.forEachIndexed { index, profile ->
            val view = PetView(this, profile)
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
            val baseSize = 250
            val size = (baseSize * profile.sizePercent.coerceIn(25, 200) / 100).coerceIn(110, 500)
            val touchFlags = if (profile.ghostMode) WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE else 0
            val params = WindowManager.LayoutParams(
                size,
                size,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or touchFlags,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                val screenWidth = resources.displayMetrics.widthPixels
                val spacing = (screenWidth / (profiles.size + 1)).coerceAtLeast(110)
                x = (spacing * (index + 1) - size / 2).coerceIn(0, (screenWidth - size).coerceAtLeast(0))
                y = (resources.displayMetrics.heightPixels * 0.58f).toInt()
                AlivePositionStore.load(this@AliveService, profile.id)?.let { saved ->
                    x = saved.first.coerceIn(0, (screenWidth - size).coerceAtLeast(0))
                    y = saved.second.coerceIn(0, (resources.displayMetrics.heightPixels - size).coerceAtLeast(0))
                }
            }
            runCatching {
                windowManager.addView(view, params)
                petViews += view
            }
        }
    }

    private fun checkInteractions() {
        val now = System.currentTimeMillis()
        for (i in petViews.indices) {
            for (j in i + 1 until petViews.size) {
                val first = petViews[i]
                val second = petViews[j]
                if (!first.canStartInteraction() || !second.canStartInteraction()) continue

                val a = first.layoutParams as? WindowManager.LayoutParams ?: continue
                val b = second.layoutParams as? WindowManager.LayoutParams ?: continue

                // Measure the empty gap between the rendered rectangles, not their centers.
                // Since each rectangle already includes sizePercent, larger Alives get a
                // proportionally larger approach distance without touching first.
                val gapX = maxOf(0, maxOf(a.x - (b.x + b.width), b.x - (a.x + a.width)))
                val gapY = maxOf(0, maxOf(a.y - (b.y + b.height), b.y - (a.y + a.height)))
                val gap = hypot(gapX.toDouble(), gapY.toDouble())
                val approachMargin = maxOf(28.0, (a.width + b.width) * 0.10)
                if (gap > approachMargin) continue

                if (AliveRelationshipStore.level(this, first.profileId, second.profileId) == "Unknown") continue
                val pairKey = listOf(first.profileId, second.profileId).sorted().joinToString(":")
                if (now < (pairCooldownUntil[pairKey] ?: 0L)) continue

                val sharedDuration = maxOf(first.interactionDurationMs(), second.interactionDurationMs())
                if (first.startInteraction(sharedDuration) && second.startInteraction(sharedDuration)) {
                    pairCooldownUntil[pairKey] = now + sharedDuration + INTERACTION_COOLDOWN_MS
                }
            }
        }
    }

    private fun removePets() {
        petViews.forEach { runCatching { windowManager.removeViewImmediate(it) } }
        petViews.clear()
    }

    override fun onDestroy() {
        interactionHandler.removeCallbacksAndMessages(null)
        pairCooldownUntil.clear()
        if (screenReceiverRegistered) {
            runCatching { unregisterReceiver(screenReceiver) }
            screenReceiverRegistered = false
        }
        removePets()
        serviceIsRunning = false
        super.onDestroy()
    }

    override fun onBind(intent: android.content.Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "S•S Alive", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun updateNotification(message: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(message))
    }

    private fun serviceAction(action: String, requestCode: Int): PendingIntent {
        val intent = android.content.Intent(this, AliveService::class.java).setAction(action)
        return PendingIntent.getService(
            this, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildNotification(message: String = "Your equipped Alives are active"): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("S•S Alive")
            .setContentText(message)
            .setOngoing(true)
            .addAction(0, "Show", serviceAction(ACTION_SHOW_EQUIPPED, 1))
            .addAction(0, "Hide", serviceAction(ACTION_HIDE, 2))
            .addAction(0, "Stop", serviceAction(ACTION_STOP, 3))
            .build()

    companion object {
        @Volatile
        private var serviceIsRunning = false
        val isRunning: Boolean get() = serviceIsRunning
        private const val INTERACTION_COOLDOWN_MS = 5000L
        const val ACTION_SHOW_EQUIPPED = "com.ss.alive.action.SHOW_EQUIPPED"
        const val ACTION_HIDE = "com.ss.alive.action.HIDE"
        const val ACTION_STOP = "com.ss.alive.action.STOP"
        private const val CHANNEL_ID = "ss_alive"
        private const val NOTIFICATION_ID = 1001
    }
}
