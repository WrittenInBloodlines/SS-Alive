package com.ss.alive.alive

class PetBehavior(
    var speedPxPerTick: Int = 4
) {
    enum class State {
        WALKING,
        FALLING,
        HELD
    }

    var direction: Int = 1
        private set

    var state: State = State.WALKING
        private set

    private var velocityY = 0f

    fun step(
        currentX: Int,
        currentY: Int,
        petWidth: Int,
        petHeight: Int,
        screenWidth: Int,
        screenHeight: Int
    ): Position {
        val maxX = (screenWidth - petWidth).coerceAtLeast(0)
        val maxY = (screenHeight - petHeight).coerceAtLeast(0)

        if (state == State.FALLING) {
            velocityY += 0.9f
            var nextY = currentY + velocityY.toInt()

            if (nextY >= maxY) {
                nextY = maxY
                velocityY = 0f
                state = State.WALKING
            }

            return Position(currentX.coerceIn(0, maxX), nextY)
        }

        if (state == State.HELD) {
            return Position(currentX.coerceIn(0, maxX), currentY.coerceIn(0, maxY))
        }

        var nextX = currentX + (speedPxPerTick * direction)
        var nextY = currentY

        if (nextX <= 0) {
            nextX = 0
            direction = 1
        } else if (nextX >= maxX) {
            nextX = maxX
            direction = -1
        }

        nextY = when {
            currentY <= 0 -> 0
            currentY >= maxY -> maxY
            else -> currentY
        }

        return Position(nextX, nextY)
    }

    fun startFalling() {
        if (state == State.HELD) {
            state = State.FALLING
            velocityY = 0f
        }
    }

    fun setHeld() {
        state = State.HELD
        velocityY = 0f
    }

    fun reverse() {
        direction *= -1
    }

    data class Position(val x: Int, val y: Int)
}
