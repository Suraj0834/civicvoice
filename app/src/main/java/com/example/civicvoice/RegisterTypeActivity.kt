package com.example.civicvoice

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.example.civicvoice.databinding.ActivityRegisterTypeBinding

class RegisterTypeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterTypeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterTypeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupButtonListeners()
    }

    private fun setupButtonListeners() {
        val scaleUp = AnimationUtils.loadAnimation(this, R.anim.scale_up)
        val scaleDown = AnimationUtils.loadAnimation(this, R.anim.scale_down)

        binding.normalUserButton.setOnTouchListener { view, event ->
            handleButtonTouch(view, event, scaleUp, scaleDown) {
                navigateTo(RegisterNormalActivity::class.java)
            }
        }

        binding.officialButton.setOnTouchListener { view, event ->
            handleButtonTouch(view, event, scaleUp, scaleDown) {
                navigateTo(RegisterOfficialActivity::class.java)
            }
        }
    }

    private fun handleButtonTouch(view: View, event: MotionEvent, scaleUp: android.view.animation.Animation, scaleDown: android.view.animation.Animation, action: () -> Unit): Boolean {
        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                view.startAnimation(scaleUp)
                true
            }
            MotionEvent.ACTION_UP -> {
                view.startAnimation(scaleDown)
                view.postDelayed(action, 200L)
                true
            }
            MotionEvent.ACTION_CANCEL -> {
                view.clearAnimation()
                true
            }
            else -> false
        }
    }

    private fun navigateTo(activityClass: Class<*>) {
        try {
            val intent = Intent(this, activityClass)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        } catch (e: Exception) {
            startActivity(Intent(this, activityClass))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        try {
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        } catch (e: Exception) {
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
}