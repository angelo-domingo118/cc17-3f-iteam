package com.example.neuralnotesproject.ui

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import com.example.neuralnotesproject.R

class TypingIndicator @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val dot1: View
    private val dot2: View
    private val dot3: View
    private val animators = mutableListOf<ValueAnimator>()
    var isAnimating: Boolean = false
        private set

    init {
        LayoutInflater.from(context).inflate(R.layout.typing_indicator, this, true)
        
        dot1 = findViewById(R.id.dot1)
        dot2 = findViewById(R.id.dot2)
        dot3 = findViewById(R.id.dot3)

        setupAnimations()
    }

    private fun setupAnimations() {
        val duration = 800L
        val delay = 100L

        fun createAnimator(target: View, startDelay: Long): ValueAnimator {
            return ValueAnimator.ofFloat(1f, 0.2f).apply {
                this.duration = duration
                this.startDelay = startDelay
                this.repeatMode = ValueAnimator.REVERSE
                this.repeatCount = ValueAnimator.INFINITE
                this.interpolator = LinearInterpolator()
                addUpdateListener { animation ->
                    val value = animation.animatedValue as Float
                    target.alpha = value
                    target.scaleX = value
                    target.scaleY = value
                }
            }
        }

        animators.add(createAnimator(dot1, 0))
        animators.add(createAnimator(dot2, delay))
        animators.add(createAnimator(dot3, delay * 2))
    }

    fun startAnimation() {
        isAnimating = true
        visibility = View.VISIBLE
        animators.forEach { it.start() }
    }

    fun stopAnimation() {
        isAnimating = false
        visibility = View.GONE
        animators.forEach { it.cancel() }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }
} 