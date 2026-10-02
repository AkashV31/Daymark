package com.daymark.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color

object Motion {
    val EmphasizedEasing: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val DecelerateEasing: Easing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)

    fun <T> fastTween(durationMillis: Int = 120) = tween<T>(durationMillis = durationMillis, easing = FastOutSlowInEasing)
    fun <T> standardTween(durationMillis: Int = 220) = tween<T>(durationMillis = durationMillis, easing = EmphasizedEasing)

    val fast = tween<Float>(durationMillis = 120, easing = FastOutSlowInEasing)
    val standard = tween<Float>(durationMillis = 220, easing = EmphasizedEasing)
    val standardColor = tween<Color>(durationMillis = 220, easing = EmphasizedEasing)
    val spring = spring<Float>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
}
