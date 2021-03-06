package com.arsvechkarev.letta.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_CANCEL
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_UP
import android.view.View
import com.arsvechkarev.letta.R
import com.arsvechkarev.letta.core.Colors
import com.arsvechkarev.letta.core.DURATION_ON_CLICK
import com.arsvechkarev.letta.core.VIEW_CLICK_SCALE_FACTOR
import com.arsvechkarev.letta.extensions.AccelerateDecelerateInterpolator
import com.arsvechkarev.letta.extensions.STROKE_PAINT
import com.arsvechkarev.letta.extensions.execute
import com.arsvechkarev.letta.extensions.happenedIn

open class ImageButton @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
  
  private val drawStroke: Boolean
  private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
  private val colorFilterDisabled = PorterDuffColorFilter(Colors.Disabled, PorterDuff.Mode.SRC_ATOP)
  private var image: Drawable?
  private var scaleFactor = 1f
  private val scaleAnimator = ValueAnimator().apply {
    interpolator = AccelerateDecelerateInterpolator
    duration = DURATION_ON_CLICK
    addUpdateListener {
      scaleFactor = animatedValue as Float
      invalidate()
    }
  }
  
  init {
    val arr = context.obtainStyledAttributes(attrs, R.styleable.ImageButton, defStyleAttr, 0)
    image = arr.getDrawable(R.styleable.ImageButton_imageSrc)?.mutate()
    val defaultColor = Colors.Background
    backgroundPaint.color = arr.getColor(R.styleable.ImageButton_backgroundColor, defaultColor)
    drawStroke = arr.getBoolean(R.styleable.ImageButton_drawStroke, true)
    arr.recycle()
  }
  
  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val max = maxOf(image?.intrinsicWidth ?: 0, image?.intrinsicHeight ?: 0)
    val size = max + paddingStart + paddingEnd
    setMeasuredDimension(
      resolveSize(size, widthMeasureSpec),
      resolveSize(size, heightMeasureSpec)
    )
  }
  
  override fun onDraw(canvas: Canvas) {
    if (image?.bounds?.width() == 0) {
      image?.setBounds(paddingLeft, paddingTop, width - paddingRight, height - paddingBottom)
    }
    canvas.execute {
      val halfWidth = width / 2f
      val halfHeight = height / 2f
      scale(scaleFactor, scaleFactor, halfWidth, halfHeight)
      drawCircle(halfWidth, halfHeight, halfWidth, backgroundPaint)
      image?.let { image ->
        if (isEnabled) {
          image.colorFilter = null
        }
        image.draw(canvas)
        if (!isEnabled) {
          image.colorFilter = colorFilterDisabled
          image.draw(canvas)
        }
      }
      if (drawStroke) {
        drawCircle(halfWidth, halfHeight, halfWidth, STROKE_PAINT)
      }
    }
  }
  
  override fun onTouchEvent(event: MotionEvent): Boolean {
    if (!isEnabled || !isClickable) return true
    when (event.action) {
      ACTION_DOWN -> {
        animate(down = true)
        return true
      }
      ACTION_UP, ACTION_CANCEL -> {
        if (event.action == ACTION_UP && event happenedIn this) {
          performClick()
        }
        animate(down = false)
        return true
      }
    }
    return false
  }
  
  private fun animate(down: Boolean = true) {
    val endScale = if (down) VIEW_CLICK_SCALE_FACTOR else 1.0f
    scaleAnimator.setFloatValues(scaleFactor, endScale)
    scaleAnimator.start()
  }
}