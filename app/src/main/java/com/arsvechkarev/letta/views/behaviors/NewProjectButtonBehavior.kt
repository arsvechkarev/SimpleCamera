package com.arsvechkarev.letta.views.behaviors

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.arsvechkarev.letta.utils.doOnEnd

class NewProjectButtonBehavior<V : View>() : CoordinatorLayout.Behavior<V>() {
  
  private var scrolled = 0
  private var isAnimating = false
  
  @Suppress("unused") // Accessible through xml
  constructor(context: Context, attrs: AttributeSet) : this()
  
  override fun onStartNestedScroll(
    coordinatorLayout: CoordinatorLayout,
    child: V,
    directTargetChild: View,
    target: View,
    axes: Int,
    type: Int
  ): Boolean {
    return axes and View.SCROLL_AXIS_VERTICAL != 0
  }
  
  override fun onNestedPreScroll(
    coordinatorLayout: CoordinatorLayout,
    child: V,
    target: View,
    dx: Int,
    dy: Int,
    consumed: IntArray,
    type: Int
  ) {
    val range = getRange(child)
    scrolled = (scrolled + dy).coerceIn(-range.toInt(), range.toInt())
    val isScrollingDown = dy > 0
    animateIfNeeded(child, isScrollingDown)
  }
  
  override fun onStopNestedScroll(
    coordinatorLayout: CoordinatorLayout,
    child: V,
    target: View,
    type: Int
  ) {
    scrolled = 0
  }
  
  private fun animateIfNeeded(child: V, isScrollingDown: Boolean) {
    if (isAnimating) return
    val range = getRange(child)
    if (isScrollingDown) {
      if (child.translationY <= 0f && scrolled >= range) {
        performAnimation(child, range)
      }
    } else {
      scrolled = 0
      if (child.translationY > 0f) {
        performAnimation(child, -range)
      }
    }
  }
  
  private fun performAnimation(child: V, translation: Float) {
    scrolled = 0
    isAnimating = true
    child.animate()
        .translationYBy(translation)
        .doOnEnd { isAnimating = false }
        .start()
  }
  
  private fun getRange(child: V): Float {
    return child.height * 1.5f
  }
}