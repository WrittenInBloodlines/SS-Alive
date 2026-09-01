package com.ss.alive.account

import android.content.Context

object AliveAccountStore {
    private const val PREFS = "ss_alive_account"
    private const val UID = "uid"
    private const val USERNAME = "username"
    private const val DISPLAY_NAME = "display_name"
    private const val ROLE = "role"

    fun save(context: Context, account: AliveAccount) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(UID, account.uid)
            .putString(USERNAME, account.username)
            .putString(DISPLAY_NAME, account.displayName)
            .putString(ROLE, account.role)
            .apply()
    }

    fun get(context: Context): AliveAccount? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val uid = prefs.getString(UID, null) ?: return null
        return AliveAccount(
            uid = uid,
            username = prefs.getString(USERNAME, "S•S User") ?: "S•S User",
            displayName = prefs.getString(DISPLAY_NAME, "") ?: "",
            role = prefs.getString(ROLE, "user") ?: "user"
        )
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}
