package com.digitalesweb.zapaticocochinito

import com.digitalesweb.zapaticocochinito.games.AchievementsHardeningPolicy
import com.digitalesweb.zapaticocochinito.model.AchievementKey
import com.digitalesweb.zapaticocochinito.model.PendingAchievementEntry
import com.digitalesweb.zapaticocochinito.model.PendingAchievementType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementsHardeningPolicyTest {

    @Test
    fun `merge unlock keeps a single queued entry`() {
        val initial = listOf(
            PendingAchievementEntry(
                type = PendingAchievementType.Unlock,
                achievement = AchievementKey.ACH_SCORE_50,
                createdAtMillis = 10
            )
        )

        val merged = AchievementsHardeningPolicy.mergeForEnqueue(
            existing = initial,
            newEntry = PendingAchievementEntry(
                type = PendingAchievementType.Unlock,
                achievement = AchievementKey.ACH_SCORE_50,
                createdAtMillis = 99
            )
        )

        assertEquals(1, merged.size)
        assertEquals(10L, merged.first().createdAtMillis)
    }

    @Test
    fun `merge increment keeps maximum target`() {
        val initial = listOf(
            PendingAchievementEntry(
                type = PendingAchievementType.IncrementTarget,
                achievement = AchievementKey.ACH_ACIERTOS_200,
                targetProgress = 20,
                createdAtMillis = 50
            )
        )

        val merged = AchievementsHardeningPolicy.mergeForEnqueue(
            existing = initial,
            newEntry = PendingAchievementEntry(
                type = PendingAchievementType.IncrementTarget,
                achievement = AchievementKey.ACH_ACIERTOS_200,
                targetProgress = 42,
                createdAtMillis = 80
            )
        )

        assertEquals(1, merged.size)
        assertEquals(42, merged.first().targetProgress)
        assertEquals(50L, merged.first().createdAtMillis)
    }

    @Test
    fun `next retry uses short bounded backoff`() {
        val now = 1_000L

        val first = AchievementsHardeningPolicy.nextRetryAtMillis(now, nextAttemptCount = 1)
        val second = AchievementsHardeningPolicy.nextRetryAtMillis(now, nextAttemptCount = 2)
        val third = AchievementsHardeningPolicy.nextRetryAtMillis(now, nextAttemptCount = 3)
        val capped = AchievementsHardeningPolicy.nextRetryAtMillis(now, nextAttemptCount = 10)

        assertEquals(3_000L, first)
        assertEquals(6_000L, second)
        assertEquals(16_000L, third)
        assertEquals(16_000L, capped)
    }

    @Test
    fun `ready entries respect next attempt time and batch size`() {
        val queue = listOf(
            PendingAchievementEntry(
                type = PendingAchievementType.Unlock,
                achievement = AchievementKey.ACH_SCORE_50,
                nextAttemptAtMillis = 100,
                createdAtMillis = 2
            ),
            PendingAchievementEntry(
                type = PendingAchievementType.Unlock,
                achievement = AchievementKey.ACH_SCORE_100,
                nextAttemptAtMillis = 100,
                createdAtMillis = 1
            ),
            PendingAchievementEntry(
                type = PendingAchievementType.Unlock,
                achievement = AchievementKey.ACH_RACHA_15,
                nextAttemptAtMillis = 500,
                createdAtMillis = 3
            )
        )

        val ready = AchievementsHardeningPolicy.readyEntries(queue, nowMillis = 200, maxBatch = 1)

        assertEquals(1, ready.size)
        assertEquals(AchievementKey.ACH_SCORE_100, ready.first().achievement)
        assertTrue(ready.first().nextAttemptAtMillis <= 200)
    }
}
