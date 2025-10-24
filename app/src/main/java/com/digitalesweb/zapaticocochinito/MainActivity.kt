package com.digitalesweb.zapaticocochinito

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    // UI Elements
    private lateinit var scoreText: TextView
    private lateinit var livesText: TextView
    private lateinit var streakText: TextView
    private lateinit var currentFootText: TextView
    private lateinit var cambiaText: TextView
    private lateinit var leftFootButton: Button
    private lateinit var rightFootButton: Button
    private lateinit var gameOverLayout: LinearLayout
    private lateinit var finalScoreText: TextView
    private lateinit var playAgainButton: Button

    // Game State
    private var score = 0
    private var lives = 3
    private var streak = 0
    private var isGameActive = false
    private var isInverted = false
    private var currentFoot = 0 // 0 = left, 1 = right
    private var baseInterval = 1500L // milliseconds between foot changes
    private var currentInterval = baseInterval

    // Handlers
    private val handler = Handler(Looper.getMainLooper())
    private var gameRunnable: Runnable? = null
    private var cambiaRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupClickListeners()
        updateUI()
    }

    private fun initializeViews() {
        scoreText = findViewById(R.id.scoreText)
        livesText = findViewById(R.id.livesText)
        streakText = findViewById(R.id.streakText)
        currentFootText = findViewById(R.id.currentFootText)
        cambiaText = findViewById(R.id.cambiaText)
        leftFootButton = findViewById(R.id.leftFootButton)
        rightFootButton = findViewById(R.id.rightFootButton)
        gameOverLayout = findViewById(R.id.gameOverLayout)
        finalScoreText = findViewById(R.id.finalScoreText)
        playAgainButton = findViewById(R.id.playAgainButton)
    }

    private fun setupClickListeners() {
        leftFootButton.setOnClickListener {
            if (!isGameActive) {
                startGame()
            } else {
                onFootTapped(0)
            }
        }

        rightFootButton.setOnClickListener {
            if (!isGameActive) {
                startGame()
            } else {
                onFootTapped(1)
            }
        }

        playAgainButton.setOnClickListener {
            resetGame()
        }
    }

    private fun startGame() {
        isGameActive = true
        score = 0
        lives = 3
        streak = 0
        isInverted = false
        currentInterval = baseInterval
        
        updateUI()
        scheduleNextFoot()
    }

    private fun scheduleNextFoot() {
        if (!isGameActive) return

        // Randomly decide if CAMBIA should appear (20% chance)
        if (Random.nextInt(100) < 20 && !isInverted) {
            showCambia()
        } else {
            currentFoot = Random.nextInt(2)
            showCurrentFoot()
            
            // Speed up slightly as the game progresses
            if (streak > 0 && streak % 10 == 0) {
                currentInterval = maxOf(800L, currentInterval - 50L)
            }
        }
    }

    private fun showCambia() {
        cambiaText.visibility = View.VISIBLE
        currentFootText.visibility = View.GONE
        isInverted = true

        // Flash animation
        val animator = ObjectAnimator.ofObject(
            cambiaText,
            "backgroundColor",
            ArgbEvaluator(),
            ContextCompat.getColor(this, android.R.color.white),
            ContextCompat.getColor(this, R.color.cambia_color)
        )
        animator.duration = 300
        animator.repeatCount = 3
        animator.repeatMode = ObjectAnimator.REVERSE
        animator.start()

        // Hide CAMBIA after 2 seconds and continue with inverted controls
        cambiaRunnable = Runnable {
            cambiaText.visibility = View.GONE
            currentFoot = Random.nextInt(2)
            showCurrentFoot()
            
            // End inversion after a few more taps (3-5)
            val tapsUntilNormal = Random.nextInt(3, 6)
            handler.postDelayed({
                isInverted = false
            }, currentInterval * tapsUntilNormal)
        }
        handler.postDelayed(cambiaRunnable!!, 2000)
    }

    private fun showCurrentFoot() {
        currentFootText.visibility = View.VISIBLE
        
        val displayFoot = if (isInverted) {
            1 - currentFoot // Invert the display
        } else {
            currentFoot
        }
        
        currentFootText.text = if (displayFoot == 0) {
            getString(R.string.left_foot)
        } else {
            getString(R.string.right_foot)
        }

        val textColor = if (displayFoot == 0) {
            R.color.left_foot_color
        } else {
            R.color.right_foot_color
        }
        currentFootText.setTextColor(ContextCompat.getColor(this, textColor))

        // Start timer for missed tap
        gameRunnable = Runnable {
            if (isGameActive) {
                onMissedTap()
            }
        }
        handler.postDelayed(gameRunnable!!, currentInterval)
    }

    private fun onFootTapped(tappedFoot: Int) {
        if (!isGameActive || currentFootText.visibility != View.VISIBLE) return

        // Cancel the missed tap timer
        gameRunnable?.let { handler.removeCallbacks(it) }

        val correctFoot = if (isInverted) {
            1 - currentFoot // If inverted, the opposite is correct
        } else {
            currentFoot
        }

        if (tappedFoot == correctFoot) {
            onCorrectTap()
        } else {
            onIncorrectTap()
        }
    }

    private fun onCorrectTap() {
        score += 10
        streak++
        
        // Visual feedback
        flashButton(if (currentFoot == 0) leftFootButton else rightFootButton, R.color.correct_color)
        
        updateUI()
        scheduleNextFoot()
    }

    private fun onIncorrectTap() {
        lives--
        streak = 0
        
        // Visual feedback
        val wrongButton = if (currentFoot == 0) rightFootButton else leftFootButton
        flashButton(wrongButton, R.color.incorrect_color)
        
        updateUI()
        
        if (lives <= 0) {
            endGame()
        } else {
            scheduleNextFoot()
        }
    }

    private fun onMissedTap() {
        lives--
        streak = 0
        
        updateUI()
        
        if (lives <= 0) {
            endGame()
        } else {
            scheduleNextFoot()
        }
    }

    private fun flashButton(button: Button, colorResId: Int) {
        val originalColor = when (button.id) {
            R.id.leftFootButton -> R.color.left_foot_color
            else -> R.color.right_foot_color
        }
        
        button.backgroundTintList = ContextCompat.getColorStateList(this, colorResId)
        
        handler.postDelayed({
            button.backgroundTintList = ContextCompat.getColorStateList(this, originalColor)
        }, 200)
    }

    private fun updateUI() {
        scoreText.text = getString(R.string.score, score)
        livesText.text = getString(R.string.lives, lives)
        streakText.text = getString(R.string.streak, streak)
    }

    private fun endGame() {
        isGameActive = false
        
        // Clear all scheduled callbacks
        gameRunnable?.let { handler.removeCallbacks(it) }
        cambiaRunnable?.let { handler.removeCallbacks(it) }
        
        // Show game over screen
        currentFootText.visibility = View.GONE
        cambiaText.visibility = View.GONE
        gameOverLayout.visibility = View.VISIBLE
        finalScoreText.text = getString(R.string.final_score, score)
    }

    private fun resetGame() {
        gameOverLayout.visibility = View.GONE
        currentFootText.visibility = View.VISIBLE
        currentFootText.text = getString(R.string.tap_to_start)
        currentFootText.setTextColor(ContextCompat.getColor(this, R.color.primary_color))
        isGameActive = false
        updateUI()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up handlers
        gameRunnable?.let { handler.removeCallbacks(it) }
        cambiaRunnable?.let { handler.removeCallbacks(it) }
    }
}
