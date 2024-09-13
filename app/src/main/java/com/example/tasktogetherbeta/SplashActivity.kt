package com.example.tasktogetherbeta

import android.content.Intent
import android.os.Bundle
import android.view.animation.AccelerateInterpolator
import android.view.animation.TranslateAnimation
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

// Android 12 (SDK 31) and higher does not support custom splash screens like this :(
// I'm still keeping this activity to modify, adapt, and use in the future

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val splashIcon: ImageView = findViewById(R.id.splashIcon)

        splashIcon.postDelayed({
            // Animation to move the icon to the top
            val moveUp = TranslateAnimation(0f, 0f, 0f, -1000f).apply {
                duration = 1500
                interpolator = AccelerateInterpolator()
                fillAfter = true
            }

            splashIcon.startAnimation(moveUp)

            splashIcon.postDelayed({
                startActivity(Intent(this, LoginScreen::class.java))
                finish()
            }, 1500) // Important: Delay equal to the animation duration
        }, 50) // Delay to allow the icon to appear centered
    }
}
