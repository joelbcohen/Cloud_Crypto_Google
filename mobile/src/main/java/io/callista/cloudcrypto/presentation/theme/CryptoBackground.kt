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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin

/**
 * A subtle crypto-themed background with gradient, floating hexagons,
 * and faint connection lines. Designed to be dark but visually interesting.
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
                            Color(0xFF0D1B2A),  // Deep navy
                            Color(0xFF1B2838),  // Dark blue-grey
                            Color(0xFF131F2E),  // Mid dark
                            Color(0xFF162032),  // Slightly lighter
                            Color(0xFF0B1622),  // Dark base
                        )
                    )
                )
        )

        // Radial glow accents
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Top-right teal glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x1500D4AA),  // Teal, very transparent
                        Color(0x0800D4AA),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.85f, size.height * 0.08f),
                    radius = size.width * 0.5f
                ),
                center = Offset(size.width * 0.85f, size.height * 0.08f),
                radius = size.width * 0.5f
            )

            // Bottom-left purple glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x127C4DFF),  // Purple, very transparent
                        Color(0x067C4DFF),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.15f, size.height * 0.85f),
                    radius = size.width * 0.55f
                ),
                center = Offset(size.width * 0.15f, size.height * 0.85f),
                radius = size.width * 0.55f
            )

            // Center subtle blue glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x0A3B82F6),  // Blue, barely visible
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.4f),
                    radius = size.width * 0.6f
                ),
                center = Offset(size.width * 0.5f, size.height * 0.4f),
                radius = size.width * 0.6f
            )

            // Draw subtle hexagonal grid pattern
            drawHexGrid(
                color = Color(0x08FFFFFF),
                hexSize = 60f,
                strokeWidth = 0.5f
            )

            // Draw floating hexagon accents
            drawHexagon(
                center = Offset(size.width * 0.15f, size.height * 0.15f),
                radius = 35f,
                color = Color(0x1500D4AA),
                strokeWidth = 1f
            )
            drawHexagon(
                center = Offset(size.width * 0.82f, size.height * 0.28f),
                radius = 25f,
                color = Color(0x127C4DFF),
                strokeWidth = 1f
            )
            drawHexagon(
                center = Offset(size.width * 0.7f, size.height * 0.72f),
                radius = 40f,
                color = Color(0x1000D4AA),
                strokeWidth = 1f
            )
            drawHexagon(
                center = Offset(size.width * 0.25f, size.height * 0.6f),
                radius = 20f,
                color = Color(0x0F3B82F6),
                strokeWidth = 1f
            )
            drawHexagon(
                center = Offset(size.width * 0.55f, size.height * 0.9f),
                radius = 30f,
                color = Color(0x107C4DFF),
                strokeWidth = 1f
            )

            // Subtle connection lines between hexagons
            val nodes = listOf(
                Offset(size.width * 0.15f, size.height * 0.15f),
                Offset(size.width * 0.82f, size.height * 0.28f),
                Offset(size.width * 0.7f, size.height * 0.72f),
                Offset(size.width * 0.25f, size.height * 0.6f),
                Offset(size.width * 0.55f, size.height * 0.9f),
            )
            val lineColor = Color(0x08FFFFFF)
            drawLine(lineColor, nodes[0], nodes[1], strokeWidth = 0.5f)
            drawLine(lineColor, nodes[0], nodes[3], strokeWidth = 0.5f)
            drawLine(lineColor, nodes[1], nodes[2], strokeWidth = 0.5f)
            drawLine(lineColor, nodes[3], nodes[2], strokeWidth = 0.5f)
            drawLine(lineColor, nodes[3], nodes[4], strokeWidth = 0.5f)
            drawLine(lineColor, nodes[2], nodes[4], strokeWidth = 0.5f)

            // Small dot nodes at connection points
            val dotColor = Color(0x1500D4AA)
            nodes.forEach { node ->
                drawCircle(dotColor, radius = 3f, center = node)
            }
        }

        // Content on top
        content()
    }
}

private fun DrawScope.drawHexagon(
    center: Offset,
    radius: Float,
    color: Color,
    strokeWidth: Float
) {
    val path = Path()
    for (i in 0..5) {
        val angle = Math.toRadians((60.0 * i) - 30.0)
        val x = center.x + radius * cos(angle).toFloat()
        val y = center.y + radius * sin(angle).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color, style = Stroke(width = strokeWidth))
}

private fun DrawScope.drawHexGrid(
    color: Color,
    hexSize: Float,
    strokeWidth: Float
) {
    val hexWidth = hexSize * 2
    val hexHeight = (hexSize * kotlin.math.sqrt(3.0)).toFloat()
    val cols = (size.width / (hexWidth * 0.75f)).toInt() + 2
    val rows = (size.height / hexHeight).toInt() + 2

    for (row in 0..rows) {
        for (col in 0..cols) {
            val offsetX = if (row % 2 == 0) 0f else hexWidth * 0.375f
            val cx = col * hexWidth * 0.75f + offsetX
            val cy = row * hexHeight * 0.5f
            if (cx > -hexSize && cx < size.width + hexSize &&
                cy > -hexSize && cy < size.height + hexSize
            ) {
                drawHexagon(
                    center = Offset(cx, cy),
                    radius = hexSize * 0.48f,
                    color = color,
                    strokeWidth = strokeWidth
                )
            }
        }
    }
}
