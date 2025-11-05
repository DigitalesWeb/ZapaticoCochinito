package com.digitalesweb.zapaticocochinito.games

import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.digitalesweb.zapaticocochinito.R
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.PlayGamesSdk

class PlayGamesService(private val activity: ComponentActivity) {

    private val signInClient by lazy { PlayGames.getGamesSignInClient(activity) }
    private val leaderboardsClient by lazy { PlayGames.getLeaderboardsClient(activity) }
    private var hasTriedSignIn = false

    init {
        PlayGamesSdk.initialize(activity)
    }

    fun signInIfNeeded() {
        if (hasTriedSignIn) return
        hasTriedSignIn = true

        signInClient.isAuthenticated.addOnCompleteListener(activity) { task ->
            val result = task.result
            val isAuthenticated = task.isSuccessful && result?.isAuthenticated == true
            if (isAuthenticated) {
                notifySignInSuccess()
            } else {
                signInClient.signIn().addOnCompleteListener(activity) { signInTask ->
                    if (signInTask.isSuccessful && signInTask.result?.isAuthenticated == true) {
                        notifySignInSuccess()
                    } else {
                        Log.w(TAG, "Google Play Juegos: error al iniciar sesión", signInTask.exception)
                        Toast.makeText(activity, R.string.play_games_sign_in_failed, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    fun submitBestScore(score: Int) {
        if (score <= 0) return
        val leaderboardId = activity.getString(R.string.leaderboard_high_score_id)
        if (leaderboardId.isBlank() || leaderboardId.startsWith("REEMPLAZA")) {
            Log.d(TAG, "ID de leaderboard no configurado, omitiendo envío de puntuación")
            return
        }
        leaderboardsClient.submitScore(leaderboardId, score.toLong())
    }

    fun showLeaderboard() {
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
                Log.w(TAG, "No se pudo abrir el leaderboard", error)
                Toast.makeText(activity, R.string.play_games_leaderboard_unavailable, Toast.LENGTH_SHORT).show()
            }
    }

    private fun notifySignInSuccess() {
        Toast.makeText(activity, R.string.play_games_sign_in_success, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "PlayGamesService"
    }
}
