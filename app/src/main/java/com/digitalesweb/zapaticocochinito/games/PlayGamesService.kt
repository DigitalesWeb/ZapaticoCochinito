package com.digitalesweb.zapaticocochinito.games

import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.digitalesweb.zapaticocochinito.R
import com.digitalesweb.zapaticocochinito.data.AppPreferencesRepository
import com.digitalesweb.zapaticocochinito.model.AchievementKey
import com.digitalesweb.zapaticocochinito.model.GameAchievementEvent
import com.digitalesweb.zapaticocochinito.model.PendingAchievementEntry
import com.digitalesweb.zapaticocochinito.model.PendingAchievementType
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.games.GamesClientStatusCodes
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.PlayGamesSdk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class PlayGamesService(private val activity: ComponentActivity) {

    private val signInClient by lazy { PlayGames.getGamesSignInClient(activity) }
    private val leaderboardsClient by lazy { PlayGames.getLeaderboardsClient(activity) }
    private var hasTriedSignIn = false

    init {
        PlayGamesSdk.initialize(activity)
    }

    fun signInIfNeeded(onSignInRequired: () -> Unit) {
        if (hasTriedSignIn) return
        hasTriedSignIn = true

        signInClient.isAuthenticated.addOnCompleteListener(activity) { task ->
            val result = task.result
            val isAuthenticated = task.isSuccessful && result?.isAuthenticated == true
            if (isAuthenticated) {
                notifySignInSuccess()
            } else {
                if (!task.isSuccessful) {
                    Log.w(TAG, "Google Play Juegos: error al verificar autenticacion", task.exception)
                }
                onSignInRequired()
            }
        }
    }

    fun requestUserSignIn(onSignInSuccess: () -> Unit = {}) {
        signInClient.signIn().addOnCompleteListener(activity) { signInTask ->
            if (signInTask.isSuccessful && signInTask.result?.isAuthenticated == true) {
                notifySignInSuccess()
                onSignInSuccess()
            } else {
                Log.w(TAG, "Google Play Juegos: error al iniciar sesion", signInTask.exception)
                Toast.makeText(activity, R.string.play_games_sign_in_failed, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun submitBestScore(score: Int) {
        if (score <= 0) return
        val leaderboardId = activity.getString(R.string.leaderboard_high_score_id)
        if (leaderboardId.isBlank() || leaderboardId.startsWith("REEMPLAZA")) {
            Log.d(TAG, "ID de leaderboard no configurado, omitiendo envio de puntuacion")
            return
        }
        leaderboardsClient.submitScore(leaderboardId, score.toLong())
    }

    fun showLeaderboard(onSignInRequired: () -> Unit = {}) {
        val leaderboardId = activity.getString(R.string.leaderboard_high_score_id)
        if (leaderboardId.isBlank() || leaderboardId.startsWith("REEMPLAZA")) {
            Toast.makeText(activity, R.string.play_games_leaderboard_unavailable, Toast.LENGTH_SHORT).show()
            return
        }
        leaderboardsClient.getLeaderboardIntent(leaderboardId)
            .addOnSuccessListener(activity) { intent ->
                activity.startActivity(intent)
            }
            .addOnFailureListener(activity) { error ->
                val statusCode = (error as? ApiException)?.statusCode
                if (statusCode == GamesClientStatusCodes.SIGN_IN_REQUIRED) {
                    Log.i(TAG, "El usuario necesita iniciar sesion para abrir el leaderboard")
                    onSignInRequired()
                } else {
                    Log.w(TAG, "No se pudo abrir el leaderboard", error)
                    Toast.makeText(activity, R.string.play_games_leaderboard_unavailable, Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun notifySignInSuccess() {
        Toast.makeText(activity, R.string.play_games_sign_in_success, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "PlayGamesService"
    }
}

class AchievementsService(
    private val activity: ComponentActivity,
    private val preferencesRepository: AppPreferencesRepository,
    private val nowMillisProvider: () -> Long = { System.currentTimeMillis() }
) {

    private val signInClient by lazy { PlayGames.getGamesSignInClient(activity) }
    private val achievementsClient by lazy { PlayGames.getAchievementsClient(activity) }
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun submit(event: GameAchievementEvent) {
        serviceScope.launch {
            submitInternal(event)
        }
    }

    fun flushPending(trigger: String = "manual") {
        serviceScope.launch {
            flushPendingInternal(trigger)
        }
    }

    private suspend fun submitInternal(event: GameAchievementEvent) {
        when (event) {
            is GameAchievementEvent.Unlock -> submitUnlock(event.achievement)
            is GameAchievementEvent.Increment -> submitIncrement(event.achievement, event.amount)
        }
    }

    private suspend fun submitUnlock(achievement: AchievementKey) {
        if (preferencesRepository.isAchievementUnlockSent(achievement)) {
            logStructured("submit_unlock", "idempotent_skip", achievement)
            return
        }

        if (!isAuthenticated()) {
            enqueueUnlock(achievement, reason = "no_auth")
            return
        }

        when (sendUnlock(achievement)) {
            DeliveryResult.Sent -> {
                preferencesRepository.markAchievementUnlockSent(achievement)
                logStructured("submit_unlock", "sent", achievement)
            }
            DeliveryResult.RetryableFailure -> enqueueUnlock(achievement, reason = "api_error")
            DeliveryResult.PermanentSkip -> logStructured("submit_unlock", "skipped_no_id", achievement)
        }
    }

    private suspend fun submitIncrement(achievement: AchievementKey, amount: Int) {
        val safeAmount = amount.coerceAtLeast(0)
        if (safeAmount == 0) {
            logStructured("submit_increment", "ignored_zero", achievement)
            return
        }

        val currentSent = preferencesRepository.getAchievementIncrementSent(achievement)
        val currentQueue = preferencesRepository.getPendingAchievementQueue()
        val queuedTarget = AchievementsHardeningPolicy.findQueuedIncrementTarget(currentQueue, achievement)
        val desiredTarget = maxOf(currentSent, queuedTarget) + safeAmount

        if (!isAuthenticated()) {
            enqueueIncrementTarget(achievement, desiredTarget, reason = "no_auth")
            return
        }

        when (sendIncrement(achievement, safeAmount)) {
            DeliveryResult.Sent -> {
                preferencesRepository.setAchievementIncrementSent(achievement, currentSent + safeAmount)
                logStructured(
                    action = "submit_increment",
                    status = "sent",
                    achievement = achievement,
                    amount = safeAmount,
                    target = currentSent + safeAmount
                )
            }
            DeliveryResult.RetryableFailure -> enqueueIncrementTarget(achievement, desiredTarget, reason = "api_error")
            DeliveryResult.PermanentSkip -> logStructured("submit_increment", "skipped_no_id", achievement)
        }
    }

    private suspend fun flushPendingInternal(trigger: String) {
        if (!isAuthenticated()) {
            logStructured("flush", "skipped_no_auth", reason = trigger)
            return
        }

        val now = nowMillisProvider()
        val queue = preferencesRepository.getPendingAchievementQueue()
        val ready = AchievementsHardeningPolicy.readyEntries(queue, now)
        if (ready.isEmpty()) {
            logStructured("flush", "no_ready_events", reason = trigger, queueSize = queue.size)
            return
        }

        val queueByKey = queue.associateBy { queueKey(it) }.toMutableMap()
        for (entry in ready) {
            val existing = queueByKey[queueKey(entry)] ?: continue
            when (existing.type) {
                PendingAchievementType.Unlock -> processPendingUnlock(existing, queueByKey, now)
                PendingAchievementType.IncrementTarget -> processPendingIncrement(existing, queueByKey, now)
            }
        }

        preferencesRepository.savePendingAchievementQueue(queueByKey.values.sortedBy { it.createdAtMillis })
        logStructured(
            action = "flush",
            status = "done",
            reason = trigger,
            queueSize = queueByKey.size,
            amount = ready.size
        )
    }

    private suspend fun processPendingUnlock(
        entry: PendingAchievementEntry,
        queueByKey: MutableMap<String, PendingAchievementEntry>,
        now: Long
    ) {
        if (preferencesRepository.isAchievementUnlockSent(entry.achievement)) {
            queueByKey.remove(queueKey(entry))
            logStructured("flush_unlock", "idempotent_skip", entry.achievement, attempt = entry.attemptCount)
            return
        }

        when (sendUnlock(entry.achievement)) {
            DeliveryResult.Sent -> {
                preferencesRepository.markAchievementUnlockSent(entry.achievement)
                queueByKey.remove(queueKey(entry))
                logStructured("flush_unlock", "sent", entry.achievement, attempt = entry.attemptCount)
            }
            DeliveryResult.RetryableFailure -> {
                val retry = AchievementsHardeningPolicy.markForRetry(entry, now)
                queueByKey[queueKey(entry)] = retry
                logStructured("flush_unlock", "retry_scheduled", entry.achievement, attempt = retry.attemptCount)
            }
            DeliveryResult.PermanentSkip -> {
                queueByKey.remove(queueKey(entry))
                logStructured("flush_unlock", "skipped_no_id", entry.achievement, attempt = entry.attemptCount)
            }
        }
    }

    private suspend fun processPendingIncrement(
        entry: PendingAchievementEntry,
        queueByKey: MutableMap<String, PendingAchievementEntry>,
        now: Long
    ) {
        val currentSent = preferencesRepository.getAchievementIncrementSent(entry.achievement)
        val pendingAmount = entry.targetProgress - currentSent
        if (pendingAmount <= 0) {
            queueByKey.remove(queueKey(entry))
            logStructured(
                action = "flush_increment",
                status = "idempotent_skip",
                achievement = entry.achievement,
                attempt = entry.attemptCount,
                target = entry.targetProgress
            )
            return
        }

        when (sendIncrement(entry.achievement, pendingAmount)) {
            DeliveryResult.Sent -> {
                preferencesRepository.setAchievementIncrementSent(entry.achievement, entry.targetProgress)
                queueByKey.remove(queueKey(entry))
                logStructured(
                    action = "flush_increment",
                    status = "sent",
                    achievement = entry.achievement,
                    amount = pendingAmount,
                    attempt = entry.attemptCount,
                    target = entry.targetProgress
                )
            }
            DeliveryResult.RetryableFailure -> {
                val retry = AchievementsHardeningPolicy.markForRetry(entry, now)
                queueByKey[queueKey(entry)] = retry
                logStructured(
                    action = "flush_increment",
                    status = "retry_scheduled",
                    achievement = entry.achievement,
                    amount = pendingAmount,
                    attempt = retry.attemptCount,
                    target = entry.targetProgress
                )
            }
            DeliveryResult.PermanentSkip -> {
                queueByKey.remove(queueKey(entry))
                logStructured(
                    action = "flush_increment",
                    status = "skipped_no_id",
                    achievement = entry.achievement,
                    amount = pendingAmount,
                    attempt = entry.attemptCount,
                    target = entry.targetProgress
                )
            }
        }
    }

    private suspend fun enqueueUnlock(achievement: AchievementKey, reason: String) {
        val queue = preferencesRepository.getPendingAchievementQueue()
        val newEntry = PendingAchievementEntry(
            type = PendingAchievementType.Unlock,
            achievement = achievement,
            createdAtMillis = nowMillisProvider()
        )
        val merged = AchievementsHardeningPolicy.mergeForEnqueue(queue, newEntry)
        preferencesRepository.savePendingAchievementQueue(merged)
        logStructured("enqueue_unlock", "queued", achievement, reason = reason, queueSize = merged.size)
    }

    private suspend fun enqueueIncrementTarget(achievement: AchievementKey, targetProgress: Int, reason: String) {
        val queue = preferencesRepository.getPendingAchievementQueue()
        val newEntry = PendingAchievementEntry(
            type = PendingAchievementType.IncrementTarget,
            achievement = achievement,
            targetProgress = targetProgress,
            createdAtMillis = nowMillisProvider()
        )
        val merged = AchievementsHardeningPolicy.mergeForEnqueue(queue, newEntry)
        preferencesRepository.savePendingAchievementQueue(merged)
        logStructured(
            action = "enqueue_increment",
            status = "queued",
            achievement = achievement,
            target = targetProgress,
            reason = reason,
            queueSize = merged.size
        )
    }

    private suspend fun sendUnlock(achievement: AchievementKey): DeliveryResult {
        val achievementId = resolveAchievementId(achievement)
        if (achievementId.isNullOrPlaceholder()) return DeliveryResult.PermanentSkip
        return runCatching {
            achievementsClient.unlock(achievementId)
        }.fold(
            onSuccess = { DeliveryResult.Sent },
            onFailure = { error ->
                Log.w(TAG, "Error de API en achievements", error)
                DeliveryResult.RetryableFailure
            }
        )
    }

    private suspend fun sendIncrement(achievement: AchievementKey, amount: Int): DeliveryResult {
        val safeAmount = amount.coerceAtLeast(1)
        val achievementId = resolveAchievementId(achievement)
        if (achievementId.isNullOrPlaceholder()) return DeliveryResult.PermanentSkip
        return runCatching {
            achievementsClient.increment(achievementId, safeAmount)
        }.fold(
            onSuccess = { DeliveryResult.Sent },
            onFailure = { error ->
                Log.w(TAG, "Error de API en achievements", error)
                DeliveryResult.RetryableFailure
            }
        )
    }

    private suspend fun isAuthenticated(): Boolean {
        return suspendCancellableCoroutine { continuation ->
            signInClient.isAuthenticated.addOnCompleteListener(activity) { task ->
                val authenticated = task.isSuccessful && task.result?.isAuthenticated == true
                if (!task.isSuccessful) {
                    Log.d(TAG, "No autenticado en Play Games para achievements", task.exception)
                }
                if (continuation.isActive) {
                    continuation.resume(authenticated)
                }
            }
        }
    }

    private fun queueKey(entry: PendingAchievementEntry): String {
        return "${entry.type.name}:${entry.achievement.name}"
    }

    private fun resolveAchievementId(achievement: AchievementKey): String {
        val resourceId = when (achievement) {
            AchievementKey.ACH_PRIMERA_PARTIDA -> R.string.achievement_id_primera_partida
            AchievementKey.ACH_SCORE_50 -> R.string.achievement_id_score_50
            AchievementKey.ACH_SCORE_100 -> R.string.achievement_id_score_100
            AchievementKey.ACH_RACHA_15 -> R.string.achievement_id_racha_15
            AchievementKey.ACH_CAMBIA_ACEPTADO -> R.string.achievement_id_cambia_aceptado
            AchievementKey.ACH_PRO_100 -> R.string.achievement_id_pro_100
            AchievementKey.ACH_BPM_180 -> R.string.achievement_id_bpm_180
            AchievementKey.ACH_PARTIDAS_25 -> R.string.achievement_id_partidas_25
            AchievementKey.ACH_ACIERTOS_200 -> R.string.achievement_id_aciertos_200
            AchievementKey.ACH_CAMBIA_50 -> R.string.achievement_id_cambia_50
            AchievementKey.ACH_MAESTRO_300 -> R.string.achievement_id_maestro_300
        }
        return activity.getString(resourceId)
    }

    private fun String?.isNullOrPlaceholder(): Boolean {
        return this.isNullOrBlank() || this.startsWith("REEMPLAZA")
    }

    private fun logStructured(
        action: String,
        status: String,
        achievement: AchievementKey? = null,
        amount: Int? = null,
        attempt: Int? = null,
        target: Int? = null,
        reason: String? = null,
        queueSize: Int? = null
    ) {
        val payload = listOfNotNull(
            "action=$action",
            "status=$status",
            achievement?.let { "achievement=${it.name}" },
            amount?.let { "amount=$it" },
            attempt?.let { "attempt=$it" },
            target?.let { "target=$it" },
            reason?.let { "reason=$it" },
            queueSize?.let { "queueSize=$it" }
        ).joinToString(separator = " ")
        Log.i(TAG, payload)
    }

    private enum class DeliveryResult {
        Sent,
        RetryableFailure,
        PermanentSkip
    }

    companion object {
        private const val TAG = "AchievementsService"
    }
}
