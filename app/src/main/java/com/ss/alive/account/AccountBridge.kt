package com.ss.alive.account

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object AccountBridge {
    private const val HUB_PACKAGE = "com.ss.hub"
    private const val HUB_BRIDGE = "com.ss.hub.AccountBridgeActivity"
    private const val REQUEST_CODE = 1901

    const val TEMPLATE_RESULT_ACTION = "com.ss.alive.TEMPLATE_RESULT"
    const val EXTRA_TEMPLATE_JSON = "com.ss.hub.template_bridge.JSON"
    const val EXTRA_TEMPLATE_ERROR = "com.ss.hub.template_bridge.ERROR"
    const val ACTION_LIST_TEMPLATES = "LIST_TEMPLATES"
    const val ACTION_CREATE_TEMPLATE = "CREATE_TEMPLATE"

    fun request(context: Context): Boolean = launch(context, null)

    fun requestTemplates(context: Context): Boolean = launch(context, ACTION_LIST_TEMPLATES)

    fun createTemplate(context: Context, name: String, description: String, imageData: String): Boolean {
        val hubIntent = baseIntent(context, ACTION_CREATE_TEMPLATE)
        hubIntent.putExtra("com.ss.hub.template_bridge.NAME", name)
        hubIntent.putExtra("com.ss.hub.template_bridge.DESCRIPTION", description)
        hubIntent.putExtra("com.ss.hub.template_bridge.IMAGE_DATA", imageData)
        return launch(context, hubIntent)
    }

    private fun launch(context: Context, action: String?): Boolean {
        val intent = baseIntent(context, action)
        return launch(context, intent)
    }

    private fun launch(context: Context, intent: Intent): Boolean {
        val callbackIntent = Intent(context, AccountBridgeReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val callback = PendingIntent.getBroadcast(context, REQUEST_CODE, callbackIntent, flags)
        intent.putExtra("com.ss.hub.account_bridge.CALLBACK", callback)
        return try {
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun baseIntent(context: Context, action: String?): Intent = Intent().apply {
        setClassName(HUB_PACKAGE, HUB_BRIDGE)
        if (action != null) putExtra("com.ss.hub.template_bridge.ACTION", action)
    }

    fun hasHubInstalled(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= 33) {
                context.packageManager.getPackageInfo(HUB_PACKAGE, android.content.pm.PackageManager.PackageInfoFlags.of(0))
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
