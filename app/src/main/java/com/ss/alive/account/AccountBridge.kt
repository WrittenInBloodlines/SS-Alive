package com.ss.alive.account

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.UUID

object AccountBridge {
    private const val HUB_PACKAGE = "com.ss.hub"
    private const val HUB_BRIDGE = "com.ss.hub.AccountBridgeActivity"
    private const val REQUEST_CODE = 1901

    const val TEMPLATE_RESULT_ACTION = "com.ss.alive.TEMPLATE_RESULT"
    const val EXTRA_TEMPLATE_JSON = "com.ss.hub.template_bridge.JSON"
    const val EXTRA_TEMPLATE_ERROR = "com.ss.hub.template_bridge.ERROR"
    const val ACTION_LIST_TEMPLATES = "LIST_TEMPLATES"
    const val ACTION_CREATE_TEMPLATE = "CREATE_TEMPLATE"

    private const val FEEDBACK_PREFS = "ss_alive_account_bridge_feedback"
    private const val KEY_PENDING = "pending"
    private const val KEY_RESULT = "result"
    private const val KEY_ERROR = "error"
    private const val KEY_REQUEST_ID = "request_id"

    const val EXTRA_FEEDBACK_REQUEST_ID = "com.ss.alive.account_bridge.FEEDBACK_REQUEST_ID"

    fun request(context: Context): Boolean = launch(context, null as String?)

    /**
     * Starts an account refresh and records that the next callback should be shown to the user.
     */
    fun requestWithFeedback(context: Context): Boolean {
        val prefs = context.getSharedPreferences(FEEDBACK_PREFS, Context.MODE_PRIVATE)
        val requestId = UUID.randomUUID().toString()
        prefs.edit()
            .putBoolean(KEY_PENDING, true)
            .putString(KEY_REQUEST_ID, requestId)
            .remove(KEY_RESULT)
            .remove(KEY_ERROR)
            .apply()

        val started = launch(context, null as String?, requestId)
        if (!started) {
            prefs.edit()
                .putBoolean(KEY_PENDING, false)
                .putString(KEY_RESULT, "ERROR")
                .putString(KEY_ERROR, "HUB_UNAVAILABLE")
                .apply()
        }
        return started
    }

    /**
     * Returns one result exactly once. If Hub returned without sending a callback,
     * the still-pending request is reported as NO_RESPONSE.
     */
    fun hasPendingFeedback(context: Context): Boolean {
        return context.getSharedPreferences(FEEDBACK_PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_PENDING, false)
    }

    /**
     * A timed-out request keeps its request ID, allowing a late callback to be
     * accepted unless a newer refresh has replaced that request.
     */
    fun consumeFeedback(context: Context, pendingTimeoutReached: Boolean = false): String? {
        val prefs = context.getSharedPreferences(FEEDBACK_PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_PENDING, false)) {
            if (!pendingTimeoutReached) return null
            // Keep KEY_REQUEST_ID so a delayed Hub callback can still be accepted.
            prefs.edit().putBoolean(KEY_PENDING, false).apply()
            return "NO_RESPONSE"
        }

        val result = prefs.getString(KEY_RESULT, null) ?: return null
        val error = prefs.getString(KEY_ERROR, null)
        prefs.edit()
            .remove(KEY_RESULT)
            .remove(KEY_ERROR)
            .remove(KEY_REQUEST_ID)
            .apply()
        return if (result == "ERROR") "ERROR:${error ?: "UNKNOWN"}" else result
    }

    internal fun saveFeedbackResult(context: Context, error: String?, requestId: String?) {
        val prefs = context.getSharedPreferences(FEEDBACK_PREFS, Context.MODE_PRIVATE)
        if (requestId.isNullOrBlank() || prefs.getString(KEY_REQUEST_ID, null) != requestId) return

        prefs.edit()
            .putBoolean(KEY_PENDING, false)
            .putString(KEY_RESULT, if (error == null) "SUCCESS" else "ERROR")
            .apply {
                if (error == null) remove(KEY_ERROR) else putString(KEY_ERROR, error)
            }
            .apply()
    }

    fun requestTemplates(context: Context): Boolean = launch(context, ACTION_LIST_TEMPLATES)

    fun createTemplate(context: Context, name: String, description: String, imageData: String): Boolean {
        val hubIntent = baseIntent(context, ACTION_CREATE_TEMPLATE)
        hubIntent.putExtra("com.ss.hub.template_bridge.NAME", name)
        hubIntent.putExtra("com.ss.hub.template_bridge.DESCRIPTION", description)
        hubIntent.putExtra("com.ss.hub.template_bridge.IMAGE_DATA", imageData)
        return launch(context, hubIntent)
    }

    private fun launch(
        context: Context,
        action: String?,
        feedbackRequestId: String? = null
    ): Boolean {
        val intent = baseIntent(context, action)
        return launch(context, intent, feedbackRequestId)
    }

    private fun launch(
        context: Context,
        intent: Intent,
        feedbackRequestId: String? = null
    ): Boolean {
        val callbackIntent = Intent(context, AccountBridgeReceiver::class.java).apply {
            if (feedbackRequestId != null) putExtra(EXTRA_FEEDBACK_REQUEST_ID, feedbackRequestId)
        }
        // Hub returns the account/template payload through this explicit callback.
        // Mutable is required so PendingIntent.send(..., fillInIntent) can attach result extras.
        // A unique request code prevents an older callback from replacing a newer refresh.
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        val callbackCode = feedbackRequestId?.hashCode() ?: REQUEST_CODE
        val callback = PendingIntent.getBroadcast(context, callbackCode, callbackIntent, flags)
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
