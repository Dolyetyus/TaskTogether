package com.example.tasktogetherbeta

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.animation.AccelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import android.widget.Toast

object CustomToast {
    fun show(context: Context?, message: String?) {
        val inflater = LayoutInflater.from(context)
        val layout = inflater.inflate(R.layout.custom_toast, null)

        val text = layout.findViewById<TextView>(R.id.toast_text)
        text.text = message

        val fadeIn = AlphaAnimation(0f, 1f).apply {
            interpolator = DecelerateInterpolator()
            duration = 400
        }

        val fadeOut = AlphaAnimation(1f, 0f).apply {
            interpolator = AccelerateInterpolator()
            startOffset = 1300
            duration = 500
        }

        // Combine animations into an AnimationSet
        val animationSet = AnimationSet(false).apply {
            addAnimation(fadeIn)
            addAnimation(fadeOut)
        }

        layout.startAnimation(animationSet)

        val toast = Toast(context)
        toast.setGravity(Gravity.CENTER or Gravity.BOTTOM, 0, 250)
        toast.duration = Toast.LENGTH_SHORT
        toast.view = layout
        toast.show()
    }
}