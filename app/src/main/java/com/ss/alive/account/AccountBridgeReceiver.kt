package com.ss.alive.account

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AccountBridgeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val error = intent.getStringExtra(EXTRA_ERROR)
        val uid = intent.getStringExtra(EXTRA_UID)

        if (error != null || uid.isNullOrBlank()) {
            if (error == "NOT_SIGNED_IN") {
                AliveAccountStore.clear(context)
            }
            return
        }

        AliveAccountStore.save(
            context,
            AliveAccount(
                uid = uid,
                username = intent.getStringExtra(EXTRA_USERNAME) ?: "S•S User",
                displayName = intent.getStringExtra(EXTRA_DISPLAY_NAME) ?: "",
                role = intent.getStringExtra(EXTRA_ROLE) ?: "user"
            )
        )
    }

    companion object {
        const val EXTRA_UID = "com.ss.hub.account_bridge.UID"
        const val EXTRA_USERNAME = "com.ss.hub.account_bridge.USERNAME"
        const val EXTRA_DISPLAY_NAME = "com.ss.hub.account_bridge.DISPLAY_NAME"
        const val EXTRA_ROLE = "com.ss.hub.account_bridge.ROLE"
        const val EXTRA_ERROR = "com.ss.hub.account_bridge.ERROR"
    }
}
