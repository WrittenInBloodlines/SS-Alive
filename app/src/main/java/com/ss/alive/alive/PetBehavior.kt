package com.ss.alive.alive

class PetBehavior(
    var speedPxPerTick: Int = 4
) {
    enum class State { WALKING, FALLING, HELD }

    var direction: Int = 1
        private set
    var state: State = State.WALKING
        private set

    private var velocityY = 0f
    private var routeSegment = 0
    private var reversing = false

    fun step(currentX: Int, currentY: Int, petWidth: Int, petHeight: Int, screenWidth: Int, screenHeight: Int): Position {
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
        if (state == State.HELD) return Position(currentX.coerceIn(0, maxX), currentY.coerceIn(0, maxY))

        if (reversing) return Position(currentX, currentY)

        val step = speedPxPerTick.coerceAtLeast(1)
        var x = currentX
        var y = currentY
        when (routeSegment) {
            0 -> { y = maxY; x += step * direction; if (direction > 0 && x >= maxX) { x = maxX; routeSegment = 1 } else if (direction < 0 && x <= 0) { x = 0; routeSegment = 3 } }
            1 -> { x = maxX; y -= step * direction; if (direction > 0 && y <= 0) { y = 0; routeSegment = 2 } else if (direction < 0 && y >= maxY) { y = maxY; routeSegment = 0 } }
            2 -> { y = 0; x -= step * direction; if (direction > 0 && x <= 0) { x = 0; routeSegment = 3 } else if (direction < 0 && x >= maxX) { x = maxX; routeSegment = 1 } }
            3 -> { x = 0; y += step * direction; if (direction > 0 && y >= maxY) { y = maxY; routeSegment = 0 } else if (direction < 0 && y <= 0) { y = 0; routeSegment = 2 } }
        }
        return Position(x.coerceIn(0, maxX), y.coerceIn(0, maxY))
    }

    fun reverse() {
        if (state != State.FALLING && state != State.HELD && !reversing) {
            reversing = true
            direction *= -1
        }
    }

    fun finishReverse() {
        reversing = false
    }

    fun startFalling() { if (state == State.HELD) { state = State.FALLING; velocityY = 0f } }
    fun setHeld() { state = State.HELD; velocityY = 0f }
    fun resumeWalking() { state = State.WALKING }
    data class Position(val x: Int, val y: Int)
}
