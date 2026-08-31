package com.ss.alive.alive

class PetBehavior(
    var speedPxPerTick: Int = 4
) {
    var direction: Int = 1
        private set

    fun step(currentX: Int, petWidth: Int, screenWidth: Int): Int {
        var nextX = currentX + (speedPxPerTick * direction)
        val maxX = (screenWidth - petWidth).coerceAtLeast(0)

        if (nextX <= 0) {
            nextX = 0
            direction = 1
        } else if (nextX >= maxX) {
            nextX = maxX
            direction = -1
        }

        return nextX
    }

    fun reverse() {
        direction *= -1
    }
}
