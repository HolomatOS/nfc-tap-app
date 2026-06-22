package com.example.nfctap

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    val convergeAngleDeg: Float,
    val colorIndex: Int
)

private data class ConfettiPiece(
    val angleDeg: Float,
    val speed: Float,
    val size: Float,
    val color: Color
)

private val idleColors = listOf(
    Color(0xFF6C63FF),
    Color(0xFF00D4FF),
    Color(0xFF8B5CF6),
    Color(0xFF06B6D4)
)
private val successColor = Color(0xFF00FF88)
private val goldColor = Color(0xFFFFD700)
private val confettiColors = listOf(
    Color(0xFFFF6B6B), Color(0xFFFFE66D), Color(0xFF4ECDC4),
    Color(0xFF45B7D1), Color(0xFFFF8B94), Color(0xFFA8E6CF)
)

@Composable
fun NfcScanScreen(scanTrigger: Int, onVibrate: (Long) -> Unit) {
    var state by remember { mutableStateOf(ScanState.IDLE) }

    val particles = remember {
        List(50) {
            Particle(
                baseAngleDeg = Random.nextFloat() * 360f,
                baseRadius = Random.nextFloat() * 280f + 160f,
                jitterAmp = Random.nextFloat() * 20f + 8f,
                jitterFreq = Random.nextFloat() * 1.5f + 0.5f,
                phase = Random.nextFloat() * 6.28f,
                speed = Random.nextFloat() * 0.35f + 0.12f,
                size = Random.nextFloat() * 4f + 2f,
                convergeAngleDeg = Random.nextFloat() * 360f,
                colorIndex = it % 4
            )
        }
    }

    val confetti = remember {
        List(30) {
            ConfettiPiece(
                angleDeg = Random.nextFloat() * 360f,
                speed = Random.nextFloat() * 0.6f + 0.4f,
                size = Random.nextFloat() * 6f + 3f,
                color = confettiColors[it % confettiColors.size]
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "clock")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_000_000, easing = LinearEasing)
        ),
        label = "time"
    )
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    val convergeProgress = remember { Animatable(0f) }
    val circleScale = remember { Animatable(1f) }
    val checkProgress = remember { Animatable(0f) }
    val glowAlpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(1f) }
    val confettiProgress = remember { Animatable(0f) }
    val ripple1Radius = remember { Animatable(0f) }
    val ripple1Alpha = remember { Animatable(0f) }
    val ripple2Radius = remember { Animatable(0f) }
    val ripple2Alpha = remember { Animatable(0f) }
    val ripple3Radius = remember { Animatable(0f) }
    val ripple3Alpha = remember { Animatable(0f) }

    LaunchedEffect(scanTrigger) {
        if (scanTrigger == 0) return@LaunchedEffect
        checkProgress.snapTo(0f)
        confettiProgress.snapTo(0f)
        glowAlpha.snapTo(0f)
        ripple1Radius.snapTo(0f); ripple1Alpha.snapTo(0f)
        ripple2Radius.snapTo(0f); ripple2Alpha.snapTo(0f)
        ripple3Radius.snapTo(0f); ripple3Alpha.snapTo(0f)
        state = ScanState.CONVERGING
        onVibrate(40)
        launch { textAlpha.animateTo(0f, tween(300)) }
        convergeProgress.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
        onVibrate(80)
        delay(90)
        onVibrate(180)
        state = ScanState.SUCCESS
        circleScale.snapTo(1f)
        launch {
            circleScale.animateTo(1.28f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
            circleScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow))
        }
        launch { glowAlpha.animateTo(0.22f, tween(400)) }
        launch {
            ripple1Alpha.snapTo(0.8f)
            ripple1Radius.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
            ripple1Alpha.animateTo(0f, tween(900, easing = FastOutSlowInEasing))
        }
        launch {
            delay(220)
            ripple2Alpha.snapTo(0.55f)
            ripple2Radius.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
            ripple2Alpha.animateTo(0f, tween(900, easing = FastOutSlowInEasing))
        }
        launch {
            delay(440)
            ripple3Alpha.snapTo(0.35f)
            ripple3Radius.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
            ripple3Alpha.animateTo(0f, tween(900, easing = FastOutSlowInEasing))
        }
        checkProgress.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
        launch { confettiProgress.animateTo(1f, tween(750, easing = FastOutSlowInEasing)) }
        delay(1500)
        launch { glowAlpha.animateTo(0f, tween(500)) }
        convergeProgress.animateTo(0f, tween(700, easing = FastOutSlowInEasing))
        checkProgress.snapTo(0f)
        confettiProgress.snapTo(0f)
        textAlpha.animateTo(1f, tween(500))
        state = ScanState.IDLE
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val circleRadius = min(size.width, size.height) * 0.16f
            val maxRipple = min(size.width, size.height) * 0.52f
            particles.forEach { p ->
                val t = time * p.speed + p.phase
                val floatRadius = p.baseRadius + sin(t * p.jitterFreq) * p.jitterAmp
                val floatAngle = Math.toRadians((p.baseAngleDeg + time * 4f * p.speed).toDouble())
                val idleX = center.x + (cos(floatAngle) * floatRadius).toFloat()
                val idleY = center.y + (sin(floatAngle) * floatRadius).toFloat()
                val convAngle = Math.toRadians(p.convergeAngleDeg.toDouble())
                val targetX = center.x + (cos(convAngle) * circleRadius * 0.3f).toFloat()
                val targetY = center.y + (sin(convAngle) * circleRadius * 0.3f).toFloat()
                val prog = convergeProgress.value
                val drawX = idleX + (targetX - idleX) * prog
                val drawY = idleY + (targetY - idleY) * prog
                val alpha = (1f - prog * 0.9f).coerceIn(0f, 1f)
                val particleColor = lerp(idleColors[p.colorIndex], goldColor, prog)
                drawCircle(color = particleColor.copy(alpha = alpha * 0.25f), radius = p.size * 3.5f, center = Offset(drawX, drawY))
                drawCircle(color = particleColor.copy(alpha = alpha), radius = p.size, center = Offset(drawX, drawY))
            }
            if (ripple1Alpha.value > 0f) drawCircle(color = successColor.copy(alpha = ripple1Alpha.value), radius = circleRadius + (maxRipple - circleRadius) * ripple1Radius.value, center = center, style = Stroke(width = 3f))
            if (ripple2Alpha.value > 0f) drawCircle(color = successColor.copy(alpha = ripple2Alpha.value), radius = circleRadius + (maxRipple - circleRadius) * ripple2Radius.value, center = center, style = Stroke(width = 2f))
            if (ripple3Alpha.value > 0f) drawCircle(color = successColor.copy(alpha = ripple3Alpha.value), radius = circleRadius + (maxRipple - circleRadius) * ripple3Radius.value, center = center, style = Stroke(width = 1.5f))
            if (glowAlpha.value > 0f) {
                drawCircle(color = successColor.copy(alpha = glowAlpha.value * 0.4f), radius = circleRadius * circleScale.value * 1.4f, center = center)
                drawCircle(color = successColor.copy(alpha = glowAlpha.value), radius = circleRadius * circleScale.value, center = center)
            }
            val circleColor = lerp(Color.White, successColor, convergeProgress.value)
            val effectiveBreath = if (state == ScanState.IDLE) breathScale else 1f
            drawCircle(color = circleColor.copy(alpha = 0.9f), radius = circleRadius * circleScale.value * effectiveBreath, center = center, style = Stroke(width = 4f))
            if (checkProgress.value > 0f) {
                val r = circleRadius * 0.55f
                val p1 = Offset(center.x - r * 0.55f, center.y + r * 0.05f)
                val p2 = Offset(center.x - r * 0.1f, center.y + r * 0.5f)
                val p3 = Offset(center.x + r * 0.6f, center.y - r * 0.45f)
                val path = Path().apply {
                    moveTo(p1.x, p1.y)
                    val leg1 = (checkProgress.value / 0.45f).coerceIn(0f, 1f)
                    lineTo(p1.x + (p2.x - p1.x) * leg1, p1.y + (p2.y - p1.y) * leg1)
                    if (checkProgress.value > 0.45f) {
                        val leg2 = ((checkProgress.value - 0.45f) / 0.55f).coerceIn(0f, 1f)
                        lineTo(p2.x + (p3.x - p2.x) * leg2, p2.y + (p3.y - p2.y) * leg2)
                    }
                }
                drawPath(path = path, color = successColor, style = Stroke(width = 8f, cap = StrokeCap.Round))
            }
            if (confettiProgress.value > 0f) {
                confetti.forEach { piece ->
                    val angle = Math.toRadians(piece.angleDeg.toDouble())
                    val dist = confettiProgress.value * piece.speed * 420f
                    val cx = center.x + (cos(angle) * dist).toFloat()
                    val cy = center.y + (sin(angle) * dist).toFloat()
                    val fade = (1f - confettiProgress.value).coerceIn(0f, 1f)
                    drawCircle(color = piece.color.copy(alpha = fade), radius = piece.size, center = Offset(cx, cy))
                }
            }
        }
        Text(
            text = "Hold card to back of phone",
            color = Color.White.copy(alpha = textAlpha.value * 0.45f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Light,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp)
        )
    }
}
