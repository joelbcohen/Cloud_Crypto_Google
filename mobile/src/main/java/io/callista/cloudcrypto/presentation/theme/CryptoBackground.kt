package io.callista.cloudcrypto.presentation.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin

/**
 * A crypto-themed background with circuit-board style digital wires,
 * connection nodes, and subtle coin outlines.
 */
@Composable
fun CryptoBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Base gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0A0F1A),
                            Color(0xFF0D1520),
                            Color(0xFF101B28),
                            Color(0xFF0D1520),
                            Color(0xFF080D15),
                        )
                    )
                )
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val wireColor = Color(0x1200D4AA)
            val wireColorAlt = Color(0x0E7C4DFF)
            val wireColorBlue = Color(0x0C3B82F6)
            val nodeGlow = Color(0x1800D4AA)
            val nodeGlowPurple = Color(0x147C4DFF)

            // --- Ambient glow spots ---
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x1200D4AA), Color.Transparent),
                    center = Offset(w * 0.12f, h * 0.18f),
                    radius = w * 0.3f
                ),
                center = Offset(w * 0.12f, h * 0.18f),
                radius = w * 0.3f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x0E7C4DFF), Color.Transparent),
                    center = Offset(w * 0.88f, h * 0.75f),
                    radius = w * 0.35f
                ),
                center = Offset(w * 0.88f, h * 0.75f),
                radius = w * 0.35f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x083B82F6), Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.45f),
                    radius = w * 0.4f
                ),
                center = Offset(w * 0.5f, h * 0.45f),
                radius = w * 0.4f
            )

            // --- Circuit board trace network ---
            // Horizontal bus lines
            drawCircuitWire(Offset(0f, h * 0.12f), Offset(w, h * 0.12f), wireColor, 0.7f)
            drawCircuitWire(Offset(0f, h * 0.35f), Offset(w * 0.6f, h * 0.35f), wireColorAlt, 0.5f)
            drawCircuitWire(Offset(w * 0.3f, h * 0.58f), Offset(w, h * 0.58f), wireColor, 0.5f)
            drawCircuitWire(Offset(0f, h * 0.78f), Offset(w * 0.75f, h * 0.78f), wireColorBlue, 0.6f)
            drawCircuitWire(Offset(w * 0.15f, h * 0.92f), Offset(w, h * 0.92f), wireColor, 0.4f)

            // Vertical bus lines
            drawCircuitWire(Offset(w * 0.08f, 0f), Offset(w * 0.08f, h * 0.65f), wireColor, 0.5f)
            drawCircuitWire(Offset(w * 0.3f, h * 0.2f), Offset(w * 0.3f, h), wireColorAlt, 0.5f)
            drawCircuitWire(Offset(w * 0.55f, 0f), Offset(w * 0.55f, h * 0.45f), wireColorBlue, 0.4f)
            drawCircuitWire(Offset(w * 0.75f, h * 0.1f), Offset(w * 0.75f, h * 0.85f), wireColor, 0.6f)
            drawCircuitWire(Offset(w * 0.92f, h * 0.3f), Offset(w * 0.92f, h), wireColorAlt, 0.4f)

            // Angled trace wires (circuit board routes)
            drawAngledTrace(Offset(w * 0.08f, h * 0.12f), Offset(w * 0.3f, h * 0.35f), wireColor, 0.7f)
            drawAngledTrace(Offset(w * 0.55f, h * 0.12f), Offset(w * 0.75f, h * 0.35f), wireColorAlt, 0.5f)
            drawAngledTrace(Offset(w * 0.3f, h * 0.58f), Offset(w * 0.55f, h * 0.35f), wireColorBlue, 0.5f)
            drawAngledTrace(Offset(w * 0.75f, h * 0.58f), Offset(w * 0.92f, h * 0.78f), wireColor, 0.5f)
            drawAngledTrace(Offset(w * 0.08f, h * 0.58f), Offset(w * 0.3f, h * 0.78f), wireColorAlt, 0.6f)
            drawAngledTrace(Offset(w * 0.55f, h * 0.78f), Offset(w * 0.75f, h * 0.58f), wireColor, 0.4f)
            drawAngledTrace(Offset(w * 0.3f, h * 0.78f), Offset(w * 0.55f, h * 0.92f), wireColorBlue, 0.5f)

            // --- Junction nodes (where wires cross) ---
            val junctions = listOf(
                Offset(w * 0.08f, h * 0.12f),
                Offset(w * 0.3f, h * 0.35f),
                Offset(w * 0.55f, h * 0.12f),
                Offset(w * 0.75f, h * 0.35f),
                Offset(w * 0.55f, h * 0.35f),
                Offset(w * 0.3f, h * 0.58f),
                Offset(w * 0.75f, h * 0.58f),
                Offset(w * 0.08f, h * 0.58f),
                Offset(w * 0.92f, h * 0.78f),
                Offset(w * 0.3f, h * 0.78f),
                Offset(w * 0.55f, h * 0.78f),
                Offset(w * 0.75f, h * 0.78f),
                Offset(w * 0.55f, h * 0.92f),
            )

            junctions.forEachIndexed { i, pos ->
                // Outer glow
                drawCircle(
                    color = if (i % 3 == 0) nodeGlow else nodeGlowPurple,
                    radius = 8f,
                    center = pos
                )
                // Inner bright dot
                drawCircle(
                    color = if (i % 3 == 0) Color(0x2500D4AA) else Color(0x207C4DFF),
                    radius = 3f,
                    center = pos
                )
                // Tiny solid core
                drawCircle(
                    color = if (i % 3 == 0) Color(0x3500D4AA) else Color(0x307C4DFF),
                    radius = 1.5f,
                    center = pos
                )
            }

            // --- Crypto coin outlines scattered around edges ---
            drawCoinOutline(Offset(w * 0.88f, h * 0.08f), 22f, Color(0x10F7931A)) // BTC orange
            drawCoinOutline(Offset(w * 0.14f, h * 0.42f), 18f, Color(0x1000D4AA)) // Teal
            drawCoinOutline(Offset(w * 0.85f, h * 0.52f), 15f, Color(0x107C4DFF)) // Purple
            drawCoinOutline(Offset(w * 0.18f, h * 0.88f), 20f, Color(0x103B82F6)) // Blue
            drawCoinOutline(Offset(w * 0.65f, h * 0.95f), 14f, Color(0x10F7931A)) // BTC orange
            drawCoinOutline(Offset(w * 0.42f, h * 0.05f), 16f, Color(0x1000D4AA)) // Teal

            // --- Tiny hash/data ticks along some wires ---
            drawDataTicks(Offset(0f, h * 0.12f), Offset(w, h * 0.12f), wireColor, 18)
            drawDataTicks(Offset(w * 0.3f, h * 0.58f), Offset(w, h * 0.58f), wireColorAlt, 14)
            drawDataTicks(Offset(w * 0.75f, h * 0.1f), Offset(w * 0.75f, h * 0.85f), wireColor, 16)
            drawDataTicks(Offset(0f, h * 0.78f), Offset(w * 0.75f, h * 0.78f), wireColorBlue, 12)
        }

        content()
    }
}

/** Draws a straight circuit wire. */
private fun DrawScope.drawCircuitWire(
    start: Offset,
    end: Offset,
    color: Color,
    strokeWidth: Float
) {
    drawLine(color, start, end, strokeWidth = strokeWidth, cap = StrokeCap.Round)
}

/** Draws an angled trace: goes horizontal first, then turns 90 degrees to the end. */
private fun DrawScope.drawAngledTrace(
    start: Offset,
    end: Offset,
    color: Color,
    strokeWidth: Float
) {
    val mid = Offset(end.x, start.y)
    drawLine(color, start, mid, strokeWidth = strokeWidth, cap = StrokeCap.Round)
    drawLine(color, mid, end, strokeWidth = strokeWidth, cap = StrokeCap.Round)
}

/** Draws a coin outline with a currency symbol inside. */
private fun DrawScope.drawCoinOutline(
    center: Offset,
    radius: Float,
    color: Color
) {
    // Outer ring
    drawCircle(color, radius = radius, center = center, style = Stroke(width = 1f))
    // Inner ring
    drawCircle(color, radius = radius * 0.7f, center = center, style = Stroke(width = 0.5f))
    // Center dot
    drawCircle(color, radius = 2f, center = center)
}

/** Draws tiny perpendicular tick marks along a wire to simulate data flow. */
private fun DrawScope.drawDataTicks(
    start: Offset,
    end: Offset,
    color: Color,
    count: Int
) {
    val tickColor = color.copy(alpha = color.alpha * 0.7f)
    val dx = end.x - start.x
    val dy = end.y - start.y
    val isHorizontal = kotlin.math.abs(dx) > kotlin.math.abs(dy)
    val tickLen = 3f

    for (i in 1 until count) {
        val t = i.toFloat() / count
        val px = start.x + dx * t
        val py = start.y + dy * t
        if (i % 3 == 0) {
            if (isHorizontal) {
                drawLine(tickColor, Offset(px, py - tickLen), Offset(px, py + tickLen), strokeWidth = 0.4f)
            } else {
                drawLine(tickColor, Offset(px - tickLen, py), Offset(px + tickLen, py), strokeWidth = 0.4f)
            }
        }
    }
}
