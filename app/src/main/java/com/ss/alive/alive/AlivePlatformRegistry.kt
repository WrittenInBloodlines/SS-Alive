package com.ss.alive.alive

import android.graphics.Rect

/** Stores only visible screen rectangles; no text, labels, or app content is collected. */
object AlivePlatformRegistry {
    @Volatile
    private var current: List<Rect> = emptyList()

    fun update(rectangles: List<Rect>) {
        current = rectangles.map { Rect(it) }
    }

    fun snapshot(): List<Rect> = current.map { Rect(it) }
}
