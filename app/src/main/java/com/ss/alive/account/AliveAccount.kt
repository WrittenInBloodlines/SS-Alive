package com.ss.alive.account

/** Account identity received from S•S Hub. No password or Firebase credential is stored. */
data class AliveAccount(
    val uid: String,
    val username: String,
    val displayName: String,
    val role: String
) {
    val isCreator: Boolean
        get() = role == "S•S"
}
