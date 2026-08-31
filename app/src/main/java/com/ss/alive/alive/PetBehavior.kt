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
    private var routeSegment = 0

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
                routeSegment = 0
            }

            return Position(currentX.coerceIn(0, maxX), nextY)
        }

        if (state == State.HELD) {
            return Position(currentX.coerceIn(0, maxX), currentY.coerceIn(0, maxY))
        }

        var x = currentX
        var y = currentY

        when (routeSegment) {
            0 -> {
                // Bottom edge: walk from left to right.
                x += speedPxPerTick
                y = maxY
                if (x >= maxX) {
                    x = maxX
                    routeSegment = 1
                }
            }

            1 -> {
                // Right edge: walk upward.
                y -= speedPxPerTick
                x = maxX
                if (y <= 0) {
                    y = 0
                    routeSegment = 2
                }
            }

            2 -> {
                // Top edge: walk from right to left.
                x -= speedPxPerTick
                y = 0
                if (x <= 0) {
                    x = 0
                    routeSegment = 3
                }
            }

            3 -> {
                // Left edge: walk downward back to the bottom.
                y += speedPxPerTick
                x = 0
                if (y >= maxY) {
                    y = maxY
                    routeSegment = 0
                }
            }
        }

        direction = when (routeSegment) {
            0 -> 1
            1 -> 0
            2 -> -1
            else -> 0
        }

        return Position(x.coerceIn(0, maxX), y.coerceIn(0, maxY))
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

    fun resumeWalking() {
        state = State.WALKING
    }

    fun reverse() {
        if (routeSegment == 0 || routeSegment == 2) {
            direction *= -1
        }
    }

    data class Position(val x: Int, val y: Int)
}
