package com.ss.alive.account

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object AccountBridge {
    private const val HUB_PACKAGE = "com.ss.hub"
    private const val HUB_BRIDGE = "com.ss.hub.AccountBridgeActivity"
    private const val REQUEST_CODE = 1901

    fun request(context: Context): Boolean {
        val hubIntent = Intent().apply {
            setClassName(HUB_PACKAGE, HUB_BRIDGE)
        }

        val callbackIntent = Intent(context, AccountBridgeReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val callback = PendingIntent.getBroadcast(context, REQUEST_CODE, callbackIntent, flags)

        hubIntent.putExtra("com.ss.hub.account_bridge.CALLBACK", callback)

        return try {
            context.startActivity(hubIntent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun hasHubInstalled(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= 33) {
                context.packageManager.getPackageInfo(
                    HUB_PACKAGE,
                    android.content.pm.PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(HUB_PACKAGE, 0)
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
