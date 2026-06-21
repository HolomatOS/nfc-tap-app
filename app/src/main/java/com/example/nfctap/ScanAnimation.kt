package com.example.nfctap

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private enum class ScanState { IDLE, CONVERGING, SUCCESS }

private data class Particle(
    val baseAngleDeg: Float,
    val baseRadius: Float,
    val jitterAmp: Float,
    val jitterFreq: Float,
    val phase: Float,
    val speed: Float,
    val size: Float,
    val convergeAngleDeg: Float
)

@Composable
fun NfcScanScreen(scanTrigger: Int, onVibrate: (Long) -> Unit) {
    var state by remember { mutableStateOf(ScanState.IDLE) }

    val particles = remember {
        List(40) {
            Particle(
                baseAngleDeg = Random.nextFloat() * 360f,
                baseRadius = Random.nextFloat() * 260f + 170f,
                jitterAmp = Random.nextFloat() * 18f + 6f,
                jitterFreq = Random.nextFloat() * 1.5f + 0.5f,
                phase = Random.nextFloat() * 6.28f,
                speed = Random.nextFloat() * 0.35f + 0.12f,
                size = Random.nextFloat() * 3.5f + 2f,
                convergeAngleDeg = Random.nextFloat() * 360f
            )
        }
    }

    // Slow infinite clock that drives the idle floating motion
    val infiniteTransition = rememberInfiniteTransition(label = "clock")
    val time by infiniteTransition.animateFloatLoop()

    val convergeProgress = remember { Animatable(0f) }
    val circleScale = remember { Animatable(1f) }
    val checkProgress = remember { Animatable(0f) }

    LaunchedEffect(scanTrigger) {
        if (scanTrigger == 0) return@LaunchedEffect

        checkProgress.snapTo(0f)
        state = ScanState.CONVERGING
        onVibrate(60)

        convergeProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing)
        )

        onVibrate(120)
        state = ScanState.SUCCESS
        circleScale.snapTo(1f)
        circleScale.animateTo(1.18f, tween(140, easing = FastOutSlowInEasing))
        circleScale.animateTo(1f, tween(180, easing = FastOutSlowInEasing))

        checkProgress.animateTo(1f, tween(350, easing = FastOutSlowInEasing))

        delay(1100)

        convergeProgress.animateTo(0f, tween(600, easing = FastOutSlowInEasing))
        checkProgress.snapTo(0f)
        state = ScanState.IDLE
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val circleRadius = min(size.width, size.height) * 0.16f

        particles.forEach { p ->
            val t = time * p.speed + p.phase
            val floatRadius = p.baseRadius + sin(t * p.jitterFreq) * p.jitterAmp
            val floatAngleRad = Math.toRadians((p.baseAngleDeg + time * 4f * p.speed).toDouble())

            val idleX = center.x + (cos(floatAngleRad) * floatRadius).toFloat()
            val idleY = center.y + (sin(floatAngleRad) * floatRadius).toFloat()

            val convergeAngleRad = Math.toRadians(p.convergeAngleDeg.toDouble())
            val convergeRadius = circleRadius * 0.25f
            val targetX = center.x + (cos(convergeAngleRad) * convergeRadius).toFloat()
            val targetY = center.y + (sin(convergeAngleRad) * convergeRadius).toFloat()

            val progress = convergeProgress.value
            val drawX = idleX + (targetX - idleX) * progress
            val drawY = idleY + (targetY - idleY) * progress
            val alpha = (1f - progress * 0.9f).coerceIn(0f, 1f)

            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = p.size,
                center = Offset(drawX, drawY)
            )
        }

        drawCircle(
            color = Color.White.copy(alpha = 0.9f),
            radius = circleRadius * circleScale.value,
            center = center,
            style = Stroke(width = 4f)
        )

        if (checkProgress.value > 0f) {
            val r = circleRadius * 0.55f
            val p1 = Offset(center.x - r * 0.55f, center.y + r * 0.05f)
            val p2 = Offset(center.x - r * 0.1f, center.y + r * 0.5f)
            val p3 = Offset(center.x + r * 0.6f, center.y - r * 0.45f)

            val path = Path().apply {
                moveTo(p1.x, p1.y)
                val firstLeg = (checkProgress.value / 0.45f).coerceIn(0f, 1f)
                lineTo(
                    p1.x + (p2.x - p1.x) * firstLeg,
                    p1.y + (p2.y - p1.y) * firstLeg
                )
                if (checkProgress.value > 0.45f) {
                    val secondLeg = ((checkProgress.value - 0.45f) / 0.55f).coerceIn(0f, 1f)
                    lineTo(
                        p2.x + (p3.x - p2.x) * secondLeg,
                        p2.y + (p3.y - p2.y) * secondLeg
                    )
                }
            }

            drawPath(
                path = path,
                color = Color.White,
                style = Stroke(width = 8f, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
private fun InfiniteTransition.animateFloatLoop() =
    animateFloat(
        initialValue = 0f,
        targetValue = 100000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_000_000, easing = LinearEasing)
        ),
        label = "time"
    )
