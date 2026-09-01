package com.ss.alive.alive

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri

/** Draws user-selected image frames, falling back to the built-in Cat Template. */
class CustomPetSprite(private val context: Context, private val profile: AliveProfile) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val cache = mutableMapOf<String, MutableList<Bitmap>>()

    fun frameCount(state: PetBehavior.State): Int {
        val frames = loadFrames(stateKey(state))
        return if (frames.isNotEmpty()) frames.size else BuiltInTemplateSprite.frameCount(profile.templateKind, state)
    }

    fun draw(canvas: Canvas, state: PetBehavior.State, frame: Int, direction: Int) {
        val key = stateKey(state)
        val bitmaps = loadFrames(key)
        if (bitmaps.isEmpty()) {
            BuiltInTemplateSprite.draw(profile.templateKind, canvas, frame, state, direction)
            return
        }
        val bitmap = bitmaps[frame % bitmaps.size]
        // Fit the whole source image inside the overlay view, preserving transparent padding.
        val scale = minOf(canvas.width.toFloat() / bitmap.width, canvas.height.toFloat() / bitmap.height)
            .coerceAtMost(2.0f)
        val width = bitmap.width * scale
        val height = bitmap.height * scale
        val left = (canvas.width - width) / 2f
        val top = (canvas.height - height) / 2f
        canvas.save()
        if (direction < 0) canvas.scale(-1f, 1f, canvas.width / 2f, canvas.height / 2f)
        canvas.drawBitmap(bitmap, null, RectF(left, top, left + width, top + height), paint)
        canvas.restore()
    }

    private fun stateKey(state: PetBehavior.State): String = when (state) {
        PetBehavior.State.WALKING -> AliveProfile.WALK
        PetBehavior.State.CLIMBING -> AliveProfile.WALK
        PetBehavior.State.IDLE -> AliveProfile.IDLE
        PetBehavior.State.RUNNING -> AliveProfile.RUN
        PetBehavior.State.SIT -> AliveProfile.SIT
        PetBehavior.State.JUMP -> AliveProfile.JUMP
        PetBehavior.State.FALLING -> AliveProfile.FALL
        PetBehavior.State.LANDING -> AliveProfile.LANDING
        PetBehavior.State.HELD -> AliveProfile.HELD
    }

    private fun loadFrames(state: String): List<Bitmap> {
        cache[state]?.let { return it }
        val loaded = mutableListOf<Bitmap>()
        profile.frameUris(state).forEach { rawUri ->
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(rawUri)).use { stream ->
                    if (stream != null) BitmapFactory.decodeStream(stream)?.let(loaded::add)
                }
            }
        }
        cache[state] = loaded
        return loaded
    }
}
