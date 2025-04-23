package com.example.civicvoice

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.civicvoice.databinding.ActivitySplashBinding
import kotlinx.coroutines.*

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setFullScreen()
        startAnimations()
        activityScope.launch {
            delay(SPLASH_DURATION)
            navigateToSelectionActivity()
        }
    }

    private fun setFullScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                    )
        }
    }

    private fun startAnimations() {
        binding.logoImageView.apply {
            alpha = 0f
            animate().alpha(1f).setDuration(ANIMATION_DURATION).start()
        }
        binding.appNameTextView.apply {
            alpha = 0f
            translationY = 50f
            animate().alpha(1f).translationY(0f).setDuration(ANIMATION_DURATION).setStartDelay(ANIMATION_DELAY).start()
        }
        binding.backgroundImageView.apply {
            alpha = 0f
            animate().alpha(1f).setDuration(ANIMATION_DURATION).setStartDelay(ANIMATION_DELAY * 2).start()
        }
    }

    private fun navigateToSelectionActivity() {
        startActivity(Intent(this@SplashActivity, SelectionActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    override fun onDestroy() {
        activityScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val SPLASH_DURATION = 3000L
        private const val ANIMATION_DURATION = 800L
        private const val ANIMATION_DELAY = 200L
    }
}