package com.ss.alive.alive

import kotlin.random.Random

class PetBehavior(var speedPxPerTick: Int = 4) {
    enum class State { WALKING, IDLE, RUNNING, SIT, JUMP, FALLING, LANDING, HELD }

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

        if (state == State.FALLING || state == State.JUMP) {
            velocityY += if (state == State.JUMP) 0.75f else 0.9f
            var nextY = currentY + velocityY.toInt()
            if (nextY >= maxY) {
                nextY = maxY
                velocityY = 0f
                state = State.LANDING
                landingTicksRemaining = 8
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

        if (state == State.HELD || state == State.SIT) {
            return Position(currentX.coerceIn(0, maxX), currentY.coerceIn(0, maxY))
        }

        if (reversing) return Position(currentX, currentY)

        if (state == State.IDLE) {
            if (idleTicksRemaining > 0) {
                idleTicksRemaining--
                return Position(currentX.coerceIn(0, maxX), currentY.coerceIn(0, maxY))
            }
            state = State.WALKING
        } else if (Random.nextFloat() < 0.0015f) {
            state = State.IDLE
            idleTicksRemaining = randomIdleDuration()
            return Position(currentX.coerceIn(0, maxX), currentY.coerceIn(0, maxY))
        }

        val stepSize = if (state == State.RUNNING) (speedPxPerTick * 2).coerceAtLeast(2) else speedPxPerTick.coerceAtLeast(1)
        var x = currentX
        var y = currentY
        when (routeSegment) {
            0 -> {
                y = maxY; x += stepSize * direction
                if (direction > 0 && x >= maxX) { x = maxX; routeSegment = 1 }
                else if (direction < 0 && x <= 0) { x = 0; routeSegment = 3 }
            }
            1 -> {
                x = maxX; y -= stepSize * direction
                if (direction > 0 && y <= 0) { y = 0; routeSegment = 2 }
                else if (direction < 0 && y >= maxY) { y = maxY; routeSegment = 0 }
            }
            2 -> {
                y = 0; x -= stepSize * direction
                if (direction > 0 && x <= 0) { x = 0; routeSegment = 3 }
                else if (direction < 0 && x >= maxX) { x = maxX; routeSegment = 1 }
            }
            3 -> {
                x = 0; y += stepSize * direction
                if (direction > 0 && y >= maxY) { y = maxY; routeSegment = 0 }
                else if (direction < 0 && y <= 0) { y = 0; routeSegment = 2 }
            }
        }
        return Position(x.coerceIn(0, maxX), y.coerceIn(0, maxY))
    }

    fun reverse() {
        if (state != State.FALLING && state != State.HELD && state != State.LANDING && !reversing) {
            reversing = true
            direction *= -1
        }
    }
    fun finishReverse() { reversing = false }
    fun startFalling() { if (state == State.HELD) { state = State.FALLING; velocityY = 0f; idleTicksRemaining = 0; landingTicksRemaining = 0 } }
    fun setHeld() { state = State.HELD; velocityY = 0f; idleTicksRemaining = 0; landingTicksRemaining = 0 }
    fun resumeWalking() { state = State.WALKING; idleTicksRemaining = 0; landingTicksRemaining = 0 }
    fun setRunning() { state = State.RUNNING }
    fun setSitting() { state = State.SIT }
    fun jump() { if (state != State.FALLING && state != State.HELD) { state = State.JUMP; velocityY = -14f } }
    private fun randomIdleDuration(): Int = Random.nextInt(25, 110)
    data class Position(val x: Int, val y: Int)
}
