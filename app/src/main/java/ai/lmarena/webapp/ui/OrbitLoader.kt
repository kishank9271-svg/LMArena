package ai.lmarena.webapp.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier.Companion.size
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun OrbitLoader(
    sizeDp: Dp = 88.dp,
    color: Color = Color(0xFF5E81AC),
    secondary: Color = Color(0xFF88C0D0)
) {
    val transition = rememberInfiniteTransition(label = "orbit")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    Canvas(modifier = Modifier.size(sizeDp)) {
        val radius = (size.minDimension / 2f)
        val center = Offset(size.width / 2f, size.height / 2f)
        val dots = 6
        val orbitR = radius * 0.75f

        repeat(dots) { i ->
            val phase = angle + (360f / dots) * i
            val rad = Math.toRadians(phase.toDouble())
            val x = center.x + orbitR * cos(rad).toFloat()
            val y = center.y + orbitR * sin(rad).toFloat()
            val dotSize = (radius * 0.18f) * (0.6f + 0.4f * (1f + sin(rad).toFloat()) / 2f)
            val c = if (i % 2 == 0) color else secondary
            drawCircle(color = c, radius = dotSize, center = Offset(x, y))
        }

        // core pulse
        drawCircle(color = color.copy(alpha = 0.12f), radius = radius * 0.55f, center = center)
        drawCircle(color = secondary.copy(alpha = 0.10f), radius = radius * 0.70f, center = center)
    }
}