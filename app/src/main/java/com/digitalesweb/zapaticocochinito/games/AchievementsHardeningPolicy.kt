package com.digitalesweb.zapaticocochinito.games

import com.digitalesweb.zapaticocochinito.model.AchievementKey
import com.digitalesweb.zapaticocochinito.model.PendingAchievementEntry
import com.digitalesweb.zapaticocochinito.model.PendingAchievementType

internal object AchievementsHardeningPolicy {

    const val MAX_FLUSH_BATCH = 12
    private val RETRY_DELAYS_MILLIS = longArrayOf(2_000L, 5_000L, 15_000L)

    fun nextRetryAtMillis(nowMillis: Long, nextAttemptCount: Int): Long {
        val index = (nextAttemptCount - 1).coerceIn(0, RETRY_DELAYS_MILLIS.lastIndex)
        return nowMillis + RETRY_DELAYS_MILLIS[index]
    }

    fun markForRetry(entry: PendingAchievementEntry, nowMillis: Long): PendingAchievementEntry {
        val nextAttemptCount = entry.attemptCount + 1
        return entry.copy(
            attemptCount = nextAttemptCount,
            nextAttemptAtMillis = nextRetryAtMillis(nowMillis, nextAttemptCount)
        )
    }

    fun mergeForEnqueue(
        existing: List<PendingAchievementEntry>,
        newEntry: PendingAchievementEntry
    ): List<PendingAchievementEntry> {
        val mutable = existing.toMutableList()
        val sameIndex = mutable.indexOfFirst {
            it.type == newEntry.type && it.achievement == newEntry.achievement
        }

        if (sameIndex < 0) {
            mutable.add(newEntry)
            return mutable
        }

        val current = mutable[sameIndex]
        val merged = when (newEntry.type) {
            PendingAchievementType.Unlock -> current
            PendingAchievementType.IncrementTarget -> current.copy(
                targetProgress = maxOf(current.targetProgress, newEntry.targetProgress),
                createdAtMillis = minOf(current.createdAtMillis, newEntry.createdAtMillis)
            )
        }
        mutable[sameIndex] = merged
        return mutable
    }

    fun readyEntries(
        queue: List<PendingAchievementEntry>,
        nowMillis: Long,
        maxBatch: Int = MAX_FLUSH_BATCH
    ): List<PendingAchievementEntry> {
        return queue
            .filter { it.nextAttemptAtMillis <= nowMillis }
            .sortedWith(compareBy<PendingAchievementEntry> { it.nextAttemptAtMillis }.thenBy { it.createdAtMillis })
            .take(maxBatch)
    }

    fun findQueuedIncrementTarget(
        queue: List<PendingAchievementEntry>,
        achievement: AchievementKey
    ): Int {
        return queue
            .firstOrNull {
                it.type == PendingAchievementType.IncrementTarget && it.achievement == achievement
            }
            ?.targetProgress
            ?: 0
    }
}
