package com.ss.alive.alive

import kotlin.math.abs
import kotlin.random.Random

class PetBehavior(
    var speedPxPerTick: Int = 4,
    private val idleChance: Float = 0.0015f,
    private val idleDurationMultiplier: Float = 1f,
    private val curiosityLevel: Int = 50,
    private val gravityMultiplier: Float = 1f
) {
    enum class State { WALKING, CLIMBING, IDLE, RUNNING, SIT, JUMP, FALLING, LANDING, HELD, INTERACTING, APP_SIT }

    var direction: Int = 1
        private set
    var state: State = State.WALKING
        private set

    private var velocityY = 0f
    private var routeSegment = 0
    private var reversing = false
    private var idleTicksRemaining = 0
    private var landingTicksRemaining = 0

    fun step(currentX: Int, currentY: Int, petWidth: Int, petHeight: Int, screenWidth: Int, screenHeight: Int): Position {
        val maxX = (screenWidth - petWidth).coerceAtLeast(0)
        val maxY = (screenHeight - petHeight).coerceAtLeast(0)
        if (state == State.INTERACTING || state == State.SIT || state == State.HELD || state == State.APP_SIT) {
            return Position(currentX.coerceIn(0, maxX), currentY.coerceIn(0, maxY))
        }
        if (state == State.FALLING || state == State.JUMP) {
            velocityY += (if (state == State.JUMP) 0.75f else 0.9f) * gravityMultiplier.coerceIn(0.5f, 1.5f)
            var nextY = currentY + velocityY.toInt()
            if (nextY >= maxY) {
                nextY = maxY
                velocityY = 0f
                state = State.LANDING
                landingTicksRemaining = 12
                routeSegment = 0
            }
            return Position(currentX.coerceIn(0, maxX), nextY)
        }
        if (state == State.LANDING) {
            if (landingTicksRemaining > 0) {
                landingTicksRemaining--
                return Position(currentX.coerceIn(0, maxX), maxY)
            }
            state = State.IDLE
            idleTicksRemaining = randomIdleDuration()
            return Position(currentX.coerceIn(0, maxX), maxY)
        }
        if (reversing) return Position(currentX, currentY)
        state = if (routeSegment == 1 || routeSegment == 3) State.CLIMBING else if (state == State.CLIMBING) State.WALKING else state
        if (state == State.IDLE) {
            if (idleTicksRemaining > 0) {
                idleTicksRemaining--
                return Position(currentX.coerceIn(0, maxX), currentY.coerceIn(0, maxY))
            }
            state = State.WALKING
        } else if (Random.nextFloat() < idleChance) {
            state = State.IDLE
            idleTicksRemaining = randomIdleDuration()
            return Position(currentX.coerceIn(0, maxX), currentY.coerceIn(0, maxY))
        }
        if (state == State.WALKING && Random.nextFloat() < curiosityLevel.coerceIn(0, 100) * 0.000002f) {
            direction *= -1
        }
        val stepSize = if (state == State.RUNNING) (speedPxPerTick * 2).coerceAtLeast(2) else speedPxPerTick.coerceAtLeast(1)
        var x = currentX
        var y = currentY
        when (routeSegment) {
            0 -> { y = maxY; x += stepSize * direction; if (direction > 0 && x >= maxX) { x = maxX; routeSegment = 1 } else if (direction < 0 && x <= 0) { x = 0; routeSegment = 3 } }
            1 -> { x = maxX; y -= stepSize * direction; if (direction > 0 && y <= 0) { y = 0; routeSegment = 2 } else if (direction < 0 && y >= maxY) { y = maxY; routeSegment = 0 } }
            2 -> { y = 0; x -= stepSize * direction; if (direction > 0 && x <= 0) { x = 0; routeSegment = 3 } else if (direction < 0 && x >= maxX) { x = maxX; routeSegment = 1 } }
            3 -> { x = 0; y += stepSize * direction; if (direction > 0 && y >= maxY) { y = maxY; routeSegment = 0 } else if (direction < 0 && y <= 0) { y = 0; routeSegment = 2 } }
        }
        return Position(x.coerceIn(0, maxX), y.coerceIn(0, maxY))
    }

    fun stepToward(currentX: Int, currentY: Int, targetX: Int, targetY: Int, run: Boolean): Position {
        if (state == State.FALLING || state == State.JUMP || state == State.LANDING || state == State.HELD || state == State.SIT || state == State.INTERACTING) return Position(currentX, currentY)
        val dx = targetX - currentX
        if (abs(dx) <= 34) {
            state = State.WALKING
            return Position(currentX, currentY)
        }
        direction = if (dx > 0) 1 else -1
        val distance = abs(dx).toFloat().coerceAtLeast(1f)
        val speed = if (run) speedPxPerTick * 2.55f else speedPxPerTick * 1.20f
        val move = speed.coerceAtMost(distance)
        state = if (run) State.RUNNING else State.WALKING
        return Position((currentX + direction * move).toInt(), currentY)
    }

    fun setClimbing() { state = State.CLIMBING }
    fun reverse() { if (state != State.FALLING && state != State.HELD && state != State.LANDING && state != State.INTERACTING && !reversing) { reversing = true; direction *= -1 } }
    fun finishReverse() { reversing = false }
    fun startFalling() { if (state == State.HELD) { state = State.FALLING; velocityY = 0f; idleTicksRemaining = 0; landingTicksRemaining = 0 } }
    fun setHeld() { state = State.HELD; velocityY = 0f; idleTicksRemaining = 0; landingTicksRemaining = 0 }
    fun resumeWalking() { state = State.WALKING; idleTicksRemaining = 0; landingTicksRemaining = 0 }
    fun setRunning() { state = State.RUNNING }
    fun setSitting() { state = State.SIT }
    fun setInteracting() { state = State.INTERACTING; velocityY = 0f }
    fun setAppSitting() { state = State.APP_SIT; velocityY = 0f }
    fun resumeFromAppSitting() { if (state == State.APP_SIT) { state = State.FALLING; velocityY = 0f } }
    fun jump() { if (state != State.FALLING && state != State.HELD && state != State.INTERACTING) { state = State.JUMP; velocityY = -14f } }
    private fun randomIdleDuration(): Int = (Random.nextInt(25, 110) * idleDurationMultiplier.coerceIn(0.5f, 2.5f)).toInt().coerceAtLeast(10)
    data class Position(val x: Int, val y: Int)
}
