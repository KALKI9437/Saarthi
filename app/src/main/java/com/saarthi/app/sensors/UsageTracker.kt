package com.saarthi.app.sensors

import android.app.usage.UsageStatsManager
import android.content.Context

class UsageTracker(private val ctx: Context) {

    fun getDailyUsage(): Long {

        val manager =
            ctx.getSystemService(
                Context.USAGE_STATS_SERVICE
            ) as UsageStatsManager

        val end = System.currentTimeMillis()
        val start = end - 86400000

        val stats = manager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            start,
            end
        )

        var total = 0L

        for (s in stats) {
            total += s.totalTimeInForeground
        }

        return total / 1000 // seconds
    }
}