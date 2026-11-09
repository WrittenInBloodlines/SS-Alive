package com.ss.alive.alive

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.content.ContextCompat

/** Restores previously started Alives after unlock, reboot or app replacement. */
class AliveRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_USER_PRESENT,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> Unit
            else -> return
        }

        val runtime = context.getSharedPreferences(AliveService.RUNTIME_PREFS, Context.MODE_PRIVATE)
        if (!runtime.getBoolean(AliveService.KEY_AUTO_RESTORE, false)) return
        if (!Settings.canDrawOverlays(context)) return
        if (AliveRepository.equipped(context).isEmpty()) return

        val serviceIntent = Intent(context, AliveService::class.java)
            .setAction(AliveService.ACTION_RESTORE_EQUIPPED)
        runCatching { ContextCompat.startForegroundService(context, serviceIntent) }
    }
}
