package com.ss.alive.alive

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat

class AliveService : Service() {

    private lateinit var windowManager: WindowManager
    private val petViews = mutableListOf<PetView>()
    private val interactionHandler = Handler(Looper.getMainLooper())

    private val interactionRunnable = object : Runnable {
        override fun run() {
            checkInteractions()
            interactionHandler.postDelayed(this, 700L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        interactionHandler.post(interactionRunnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        removePets()
        showPets()
        return START_STICKY
    }

    private fun showPets() {
        val profiles = AliveRepository.equipped(this)

        profiles.forEachIndexed { index, profile ->
            val view = PetView(this, profile)

            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val baseSize = 230
            val size = (
                baseSize * profile.sizePercent.coerceIn(25, 200) / 100
            ).coerceIn(90, 460)

            val params = WindowManager.LayoutParams(
                size,
                size,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START

                val screenWidth = resources.displayMetrics.widthPixels
                val spacing = (screenWidth / (profiles.size + 1)).coerceAtLeast(90)

                x = (spacing * (index + 1) - size / 2).coerceAtLeast(0)
                y = (resources.displayMetrics.heightPixels * 0.58f).toInt()
            }

            runCatching {
                windowManager.addView(view, params)
                petViews += view
            }
        }
    }

    private fun checkInteractions() {
        for (i in petViews.indices) {
            for (j in i + 1 until petViews.size) {
                val a = petViews[i].layoutParams as? WindowManager.LayoutParams ?: continue
                val b = petViews[j].layoutParams as? WindowManager.LayoutParams ?: continue

                val dx = a.x - b.x
                val dy = a.y - b.y

                if (dx * dx + dy * dy < 190 * 190) {
                    petViews[i].startInteraction()
                    petViews[j].startInteraction()
                }
            }
        }
    }

    private fun removePets() {
        petViews.forEach {
            runCatching {
                windowManager.removeViewImmediate(it)
            }
        }
        petViews.clear()
    }

    override fun onDestroy() {
        interactionHandler.removeCallbacksAndMessages(null)
        removePets()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "S•S Alive",
                NotificationManager.IMPORTANCE_LOW
            )

            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("S•S Alive")
            .setContentText("Your equipped Alives are active")
            .setOngoing(true)
            .build()
    }

    companion object {
        const val ACTION_SHOW_EQUIPPED = "com.ss.alive.action.SHOW_EQUIPPED"
        private const val CHANNEL_ID = "ss_alive"
        private const val NOTIFICATION_ID = 1001
    }
}
