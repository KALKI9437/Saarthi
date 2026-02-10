package com.saarthi.app.backup

object RetryManager {

    fun run(
        times: Int = 3,
        task: () -> Boolean
    ): Boolean {

        repeat(times) {

            if (task()) return true

            Thread.sleep(2000)
        }

        return false
    }
}