package com.ss.alive.alive

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.ss.alive.R

class AliveService : Service() {
    private lateinit var windowManager: WindowManager
    private var petView: PetView? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        showPet()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (petView == null && Settings.canDrawOverlays(this)) showPet()
        return START_STICKY
    }

    private fun showPet() {
        if (!Settings.canDrawOverlays(this)) return
        val profile = AliveRepository.active(this) ?: AliveRepository.createTemplate(this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val view = PetView(this, profile)
        petView = view

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val params = WindowManager.LayoutParams(
            150,
            150,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 180
            y = 500
        }
        view.layoutParams = params
        windowManager.addView(view, params)
    }

    override fun onDestroy() {
        petView?.let { runCatching { windowManager.removeView(it) } }
        petView = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "S•S Alive", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_menu_compass)
        .setContentTitle("S•S Alive")
        .setContentText("Your Alive is active")
        .setOngoing(true)
        .build()

    companion object {
        private const val CHANNEL_ID = "ss_alive"
        private const val NOTIFICATION_ID = 1001
    }
}
